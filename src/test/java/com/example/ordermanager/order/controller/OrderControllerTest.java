package com.example.ordermanager.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.client.service.ClientService;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.service.OrderActivityService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

  @Mock
  private OrderService orderService;
  @Mock
  private OrderActivityService orderActivityService;
  @Mock
  private ClientService clientService;
  @Mock
  private ProductService productService;
  @Mock
  private SecurityContextHelper securityContextHelper;
  @Mock
  private PasswordVerificationService passwordVerificationService;

  private OrderController orderController;

  @BeforeEach
  void setUp() {
    orderController = new OrderController(orderService, orderActivityService, clientService,
        productService, securityContextHelper, passwordVerificationService);
  }

  @Test
  void listOrders_success_returnsOrderListWithDefaultDateRange() {
    List<Order> orders = Arrays.asList(new Order(), new Order());
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrdersByCompanyIdAndDateRange(anyLong(), any(), any())).thenReturn(orders);

    Model model = new ConcurrentModel();
    String view = orderController.listOrders(null, null, model);

    assertThat(view).isEqualTo("orders/list");
    assertThat(model.getAttribute("orders")).isEqualTo(orders);
  }

  @Test
  void showCreateForm_success_returnsFormWithClientsAndStatuses() {
    List<Client> clients = List.of(new Client());
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(clientService.getClientsByCompanyId(5L)).thenReturn(clients);

    Model model = new ConcurrentModel();
    String view = orderController.showCreateForm(model);

    assertThat(view).isEqualTo("orders/form");
    assertThat(model.getAttribute("order")).isNotNull();
    assertThat(model.getAttribute("clients")).isEqualTo(clients);
    assertThat(model.getAttribute("statuses")).isEqualTo(Arrays.asList(OrderStatus.values()));
  }

  @Test
  void saveOrder_createSuccess_redirectsToOrdersWithFlashMessage() {
    Order order = new Order();
    order.setProductName("Widget");
    order.setQuantity(10);
    order.setTotalAmount(100.0);
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);

    String view = orderController.saveOrder(order, null, null, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Order created successfully");
    verify(orderService).createOrderWithCompany(order, 5L);
  }

  @Test
  void saveOrder_createFailure_returnsFormWithError() {
    Order order = new Order();
    order.setProductName("Widget");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    List<Client> clients = List.of(new Client());

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    doThrow(new IllegalArgumentException("Product name cannot be null")).when(orderService)
        .createOrderWithCompany(order, 5L);
    when(clientService.getClientsByCompanyId(5L)).thenReturn(clients);

    String view = orderController.saveOrder(order, null, null, null, model, redirectAttributes);

    assertThat(view).isEqualTo("orders/form");
    assertThat(model.getAttribute("error")).isEqualTo("Product name cannot be null");
    assertThat(model.getAttribute("order")).isEqualTo(order);
    assertThat(model.getAttribute("clients")).isEqualTo(clients);
    assertThat(model.getAttribute("statuses")).isEqualTo(Arrays.asList(OrderStatus.values()));
  }

  @Test
  void saveOrder_withDeliveryDateBeforeOrderDate_returnsFormWithDateValidationError() {
    Order order = new Order();
    order.setOrderDate(LocalDate.of(2026, 3, 27));
    order.setDeliveryDate(LocalDate.of(2026, 3, 26));
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    List<Client> clients = List.of(new Client());

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    doThrow(new IllegalArgumentException("Delivery date must be on or after order date."))
        .when(orderService).createOrderWithCompany(order, 5L);
    when(clientService.getClientsByCompanyId(5L)).thenReturn(clients);

    String view = orderController.saveOrder(order, null, null, null, model, redirectAttributes);

    assertThat(view).isEqualTo("orders/form");
    assertThat(model.getAttribute("error"))
        .isEqualTo("Delivery date must be on or after order date.");
    assertThat(model.getAttribute("dateError"))
        .isEqualTo("Delivery date must be on or after order date.");
  }

  @Test
  void saveOrder_unauthenticatedState_redirectsToLogin() {
    Order order = new Order();
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext())
        .thenThrow(new IllegalStateException("Not authenticated"));

    String view = orderController.saveOrder(order, null, null, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/login");
  }

  @Test
  void updateOrder_success_redirectsToOrdersWithFlashMessage() {
    Order existingOrder = new Order();
    existingOrder.setId(15L);
    Order updatedOrder = new Order();
    updatedOrder.setId(15L);
    updatedOrder.setProductName("Updated Widget");
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    Model model = new ConcurrentModel();
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(15L, 5L)).thenReturn(existingOrder);
    when(orderService.patchOrderForCompany(15L, updatedOrder, 5L)).thenReturn(updatedOrder);

    String view =
        orderController.updateOrder(15L, updatedOrder, null, null, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Order updated successfully");
    verify(orderService).patchOrderForCompany(15L, updatedOrder, 5L);
  }

  @Test
  void updateOrder_logsAuditUsingPreUpdateSnapshot() {
    Order existingOrder = new Order();
    existingOrder.setId(15L);
    existingOrder.setPoOrderNo("PO-OLD");
    existingOrder.setOrderNote("Old note");
    existingOrder.setStatus(OrderStatus.CREATED);

    Client existingClient = new Client();
    existingClient.setId(3L);
    existingClient.setName("OLD CLIENT");
    existingOrder.setClient(existingClient);

    Order updatedOrder = new Order();
    updatedOrder.setId(15L);
    updatedOrder.setPoOrderNo("PO-NEW");
    updatedOrder.setOrderNote("New note");
    updatedOrder.setStatus(OrderStatus.DISPATCHED);
    updatedOrder.setClient(existingClient);

    User actor = new User();
    actor.setUsername("admin1");
    actor.setFirstName("Ava");
    actor.setLastName("Shah");

    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    Model model = new ConcurrentModel();
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(securityContextHelper.getUserFromContext()).thenReturn(actor);
    when(orderService.getOrderByIdAndCompanyId(15L, 5L)).thenReturn(existingOrder);
    when(orderService.patchOrderForCompany(15L, updatedOrder, 5L)).thenReturn(updatedOrder);

    String view =
        orderController.updateOrder(15L, updatedOrder, null, null, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders");

    ArgumentCaptor<Order> beforeCaptor = ArgumentCaptor.forClass(Order.class);
    ArgumentCaptor<Order> afterCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderActivityService).logUpdated(beforeCaptor.capture(), afterCaptor.capture(),
        org.mockito.ArgumentMatchers.eq("admin1"), org.mockito.ArgumentMatchers.eq("Ava Shah"),
        org.mockito.ArgumentMatchers.eq(5L));

    Order before = beforeCaptor.getValue();
    Order after = afterCaptor.getValue();
    assertThat(before).isNotSameAs(existingOrder);
    assertThat(before.getPoOrderNo()).isEqualTo("PO-OLD");
    assertThat(before.getOrderNote()).isEqualTo("Old note");
    assertThat(before.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(after.getPoOrderNo()).isEqualTo("PO-NEW");
    assertThat(after.getOrderNote()).isEqualTo("New note");
    assertThat(after.getStatus()).isEqualTo(OrderStatus.DISPATCHED);
  }

  @Test
  void updateOrder_whenOrderNotFound_redirectsToOrdersWithError() {
    Order updatedOrder = new Order();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    Model model = new ConcurrentModel();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(15L, 5L)).thenReturn(null);

    String view =
        orderController.updateOrder(15L, updatedOrder, null, null, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders");
    assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Order not found.");
  }

  @Test
  void deleteOrder_success_redirectsToOrdersWithFlashMessage() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("correct")).thenReturn(true);

    String view = orderController.deleteOrder(25L, "correct", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Order deleted successfully");
    verify(orderService).deleteOrder(25L);
  }

  @Test
  void deleteOrder_wrongPassword_doesNotDeleteAndReturnsError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("wrong")).thenReturn(false);

    String view = orderController.deleteOrder(25L, "wrong", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("Incorrect password. Order was not deleted.");
  }

  @Test
  void showEditForm_success_returnsFormWithOrderAndClients() {
    Order order = new Order();
    order.setId(15L);
    order.setProductName("Widget");
    List<Client> clients = List.of(new Client());

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(15L, 5L)).thenReturn(order);
    when(clientService.getClientsByCompanyId(5L)).thenReturn(clients);

    Model model = new ConcurrentModel();
    String view = orderController.showEditForm(15L, model);

    assertThat(view).isEqualTo("orders/form");
    assertThat(model.getAttribute("order")).isEqualTo(order);
    assertThat(model.getAttribute("clients")).isEqualTo(clients);
    assertThat(model.getAttribute("statuses")).isEqualTo(Arrays.asList(OrderStatus.values()));
  }

  @Test
  void showEditForm_orderNotFound_redirectsToOrders() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(999L, 5L)).thenReturn(null);

    Model model = new ConcurrentModel();
    String view = orderController.showEditForm(999L, model);

    assertThat(view).isEqualTo("redirect:/orders");
  }

  @Test
  void duplicateOrder_success_returnsFormWithNewOrderData() {
    Order existingOrder = new Order();
    existingOrder.setId(15L);
    Client client = new Client();
    client.setId(3L);
    client.setName("Acme Corp");
    existingOrder.setClient(client);
    existingOrder.setProductName("Widget");
    existingOrder.setQuantity(10);
    existingOrder.setTotalAmount(100.0);
    existingOrder.setPoOrderNo("PO-001");
    existingOrder.setOrderDate(LocalDate.now());
    existingOrder.setDeliveryDate(LocalDate.now().plusDays(30));
    existingOrder.setOrderNote("Rush order");
    existingOrder.setStatus(OrderStatus.CREATED);

    List<Client> clients = List.of(new Client());

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(15L, 5L)).thenReturn(existingOrder);
    when(clientService.getClientsByCompanyId(5L)).thenReturn(clients);

    Model model = new ConcurrentModel();
    String view = orderController.duplicateOrder(15L, model);

    assertThat(view).isEqualTo("orders/form");
    Order newOrder = (Order) model.getAttribute("order");
    assertThat(newOrder.getClient()).isNotNull();
    assertThat(newOrder.getClient().getName()).isEqualTo("Acme Corp");
    assertThat(newOrder.getClient()).isEqualTo(client);
    assertThat(newOrder.getProductName()).isEqualTo("Widget");
    assertThat(newOrder.getQuantity()).isEqualTo(10);
    assertThat(newOrder.getTotalAmount()).isEqualTo(100.0);
    assertThat(newOrder.getPoOrderNo()).isEqualTo("PO-001");
    assertThat(newOrder.getOrderNote()).isEqualTo("Rush order");
    assertThat(newOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(newOrder.getId()).isNull();
    assertThat(newOrder.getOrderDate()).isEqualTo(LocalDate.now());
  }

  @Test
  void duplicateOrder_orderNotFound_redirectsToOrders() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(orderService.getOrderByIdAndCompanyId(999L, 5L)).thenReturn(null);

    Model model = new ConcurrentModel();
    String view = orderController.duplicateOrder(999L, model);

    assertThat(view).isEqualTo("redirect:/orders");
  }
}

