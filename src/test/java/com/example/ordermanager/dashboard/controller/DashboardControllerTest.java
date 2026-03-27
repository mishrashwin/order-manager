package com.example.ordermanager.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderItem;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

  @Mock
  private OrderService orderService;
  @Mock
  private CompanyService companyService;
  @Mock
  private SecurityContextHelper securityContextHelper;

  private DashboardController dashboardController;

  @BeforeEach
  void setUp() {
    dashboardController =
        new DashboardController(orderService, companyService, securityContextHelper);
  }

  @Test
  void dashboard_filtersUrgentOrdersBySelectedDateRange() {
    LocalDate start = LocalDate.of(2026, 3, 1);
    LocalDate end = LocalDate.of(2026, 3, 31);

    Order inRange = urgentOrder(1L, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 2, 28));
    Order outOfRange = urgentOrder(2L, LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 20));

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(5L);
    when(companyService.getCompanyById(5L)).thenReturn(Optional.of(company("ACME")));
    when(orderService.getOrdersByCompanyIdAndDateRange(5L, start, end)).thenReturn(List.of());
    when(orderService.getUrgentOrdersByCompanyId(5L)).thenReturn(List.of(inRange, outOfRange));

    Model model = new ConcurrentModel();
    String view = dashboardController.dashboard(start.toString(), end.toString(), model);

    assertThat(view).isEqualTo("dashboard");
    assertThat(model.getAttribute("companyName")).isEqualTo("ACME");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> urgentOrders =
        (List<Map<String, Object>>) model.getAttribute("urgentOrders");

    assertThat(urgentOrders).hasSize(1);
    assertThat(urgentOrders.get(0).get("id")).isEqualTo(1L);
    assertThat(urgentOrders.get(0).get("productName")).isEqualTo("Product [2]");
    assertThat(urgentOrders.get(0).get("quantity")).isEqualTo("Product [2]");
  }

  @Test
  void dashboard_excludesUrgentOrdersWithNullOrderDate() {
    LocalDate today = LocalDate.now();
    LocalDate defaultStart = today.minusMonths(1);

    Order nullOrderDate = urgentOrder(1L, null, today.minusDays(1));
    Order inRange = urgentOrder(2L, today.minusDays(2), today.minusDays(1));

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    when(companyService.getCompanyById(7L)).thenReturn(Optional.of(company("BETA")));
    when(orderService.getOrdersByCompanyIdAndDateRange(7L, defaultStart, today))
        .thenReturn(List.of());
    when(orderService.getUrgentOrdersByCompanyId(7L)).thenReturn(List.of(nullOrderDate, inRange));

    Model model = new ConcurrentModel();
    dashboardController.dashboard(null, null, model);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> urgentOrders =
        (List<Map<String, Object>>) model.getAttribute("urgentOrders");

    assertThat(urgentOrders).hasSize(1);
    assertThat(urgentOrders.get(0).get("id")).isEqualTo(2L);
  }

  private Company company(String name) {
    Company company = new Company();
    company.setName(name);
    return company;
  }

  private Order urgentOrder(Long id, LocalDate orderDate, LocalDate deliveryDate) {
    Order order = new Order();
    order.setId(id);
    order.setOrderDate(orderDate);
    order.setDeliveryDate(deliveryDate);
    order.setStatus(OrderStatus.CREATED);
    order.setCustomerName("Customer");
    order.setProductName("Product");
    order.setQuantity(1);
    OrderItem item = new OrderItem();
    item.setOrder(order);
    item.setProductName("Product");
    item.setQuantity(2);
    order.getOrderItems().add(item);
    return order;
  }
}

