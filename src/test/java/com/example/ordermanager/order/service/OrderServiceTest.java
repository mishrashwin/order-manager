package com.example.ordermanager.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.example.ordermanager.admin.dto.ClientOrderStatDTO;
import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderItem;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.repository.OrderRepository;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.Helper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for OrderService statistics methods introduced for the Admin Order Statistics page, and
 * urgent order notification logic.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private OrderRepository orderRepository;
  @Mock
  private CompanyService companyService;
  @Mock
  private Helper helper;

  private OrderService orderService;

  private final LocalDate START = LocalDate.of(2025, 1, 1);
  private final LocalDate END = LocalDate.of(2025, 1, 31);

  @BeforeEach
  void setUp() {
    orderService = new OrderService(orderRepository, companyService, helper);
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private Client client(Long id, String name) {
    Client c = new Client();
    c.setId(id);
    c.setName(name);
    return c;
  }

  private Order order(Client client, OrderStatus status, double amount) {
    Order o = new Order();
    o.setClient(client);
    o.setStatus(status);
    o.setTotalAmount(amount);
    o.setOrderDate(LocalDate.of(2025, 1, 10));
    return o;
  }

  /** Build an Object[] row as returned by the aggregate repository query. */
  private Object[] statsRow(Long clientId, String clientName, String customerName, long count,
      double amount) {
    return new Object[] {clientId, clientName, customerName, count, amount};
  }

  // ── getClientOrderStats ───────────────────────────────────────────────────────

  @Test
  void getClientOrderStats_noOrders_returnsEmptyList() {
    when(orderRepository.findClientOrderStats(eq(1L), eq(START), eq(END), anyCollection()))
        .thenReturn(List.of());

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ALL");

    assertThat(stats).isEmpty();
  }

  @Test
  void getClientOrderStats_singleClient_returns100Percent() {
    when(orderRepository.findClientOrderStats(eq(1L), eq(START), eq(END), anyCollection()))
        .thenReturn(List.<Object[]>of(statsRow(10L, "ACME", "ACME", 2L, 500.0)));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ALL");

    assertThat(stats).hasSize(1);
    ClientOrderStatDTO s = stats.get(0);
    assertThat(s.getClientId()).isEqualTo(10L);
    assertThat(s.getClientName()).isEqualTo("ACME");
    assertThat(s.getOrderCount()).isEqualTo(2);
    assertThat(s.getTotalAmount()).isEqualTo(500.0);
    assertThat(s.getPercentage()).isEqualTo(100.0);
  }

  @Test
  void getClientOrderStats_multipleClients_sumsToApproximately100() {
    when(orderRepository.findClientOrderStats(eq(1L), eq(START), eq(END), anyCollection()))
        .thenReturn(List.<Object[]>of(statsRow(1L, "ALPHA", "ALPHA", 2L, 200.0),
            statsRow(2L, "BETA", "BETA", 1L, 50.0),
            statsRow(3L, "GAMMA", "GAMMA", 1L, 50.0)));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ALL");

    assertThat(stats).hasSize(3);
    // ALPHA has 2 orders = 50%, BETA and GAMMA 1 each = 25%
    ClientOrderStatDTO alpha =
        stats.stream().filter(s -> "ALPHA".equals(s.getClientName())).findFirst().orElseThrow();
    assertThat(alpha.getOrderCount()).isEqualTo(2);
    assertThat(alpha.getPercentage()).isEqualTo(50.0);

    double totalPct = stats.stream().mapToDouble(ClientOrderStatDTO::getPercentage).sum();
    assertThat(totalPct).isBetween(99.0, 101.0);
  }

  @Test
  void getClientOrderStats_statusFilterActive_passesOnlyActiveStatuses() {
    // The repository is called with active-only statuses; returns pre-filtered rows
    when(orderRepository.findClientOrderStats(eq(1L), eq(START), eq(END), anyCollection()))
        .thenReturn(List.<Object[]>of(statsRow(1L, "ALPHA", "ALPHA", 1L, 100.0)));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ACTIVE");

    assertThat(stats).hasSize(1);
    assertThat(stats.get(0).getOrderCount()).isEqualTo(1);
    assertThat(stats.get(0).getTotalAmount()).isEqualTo(100.0);
  }

  @Test
  void getClientOrderStats_statusFilterCompleted_passesOnlyFinalStatuses() {
    // The repository is called with final-only statuses; returns pre-filtered rows
    when(orderRepository.findClientOrderStats(eq(1L), eq(START), eq(END), anyCollection()))
        .thenReturn(List.<Object[]>of(statsRow(1L, "ALPHA", "ALPHA", 1L, 200.0)));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "COMPLETED");

    assertThat(stats).hasSize(1);
    assertThat(stats.get(0).getOrderCount()).isEqualTo(1);
    assertThat(stats.get(0).getTotalAmount()).isEqualTo(200.0);
  }

  @Test
  void getClientOrderStats_sortedByOrderCountDescending() {
    // DB already returns rows sorted by count DESC; service preserves that order
    when(orderRepository.findClientOrderStats(eq(1L), eq(START), eq(END), anyCollection()))
        .thenReturn(List.<Object[]>of(statsRow(2L, "BETA", "BETA", 3L, 150.0),
            statsRow(1L, "ALPHA", "ALPHA", 1L, 100.0)));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ALL");

    assertThat(stats.get(0).getClientName()).isEqualTo("BETA");
    assertThat(stats.get(1).getClientName()).isEqualTo("ALPHA");
  }

  @Test
  void getClientOrderStats_legacyOrder_usesCustomerNameWhenClientNameNull() {
    // Row where client is null (legacy order): r[0]=null, r[1]=null, r[2]=customerName
    when(orderRepository.findClientOrderStats(eq(1L), eq(START), eq(END), anyCollection()))
        .thenReturn(List.<Object[]>of(statsRow(null, null, "Legacy Customer", 1L, 50.0)));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ALL");

    assertThat(stats).hasSize(1);
    assertThat(stats.get(0).getClientId()).isNull();
    assertThat(stats.get(0).getClientName()).isEqualTo("Legacy Customer");
  }

  // ── getOrdersByClientAndDateRange ─────────────────────────────────────────────

  @Test
  void getOrdersByClientAndDateRange_filtersByClientId() {
    Client a = client(1L, "ALPHA");
    Order oA = order(a, OrderStatus.CREATED, 100.0);

    when(orderRepository.findByCompanyAndDateRangeAndClientId(eq(1L), eq(START), eq(END), eq(1L),
        anyCollection())).thenReturn(List.of(oA));

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, 1L, "ALPHA", START, END, "ALL");

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(oA);
  }

  @Test
  void getOrdersByClientAndDateRange_statusFilter_active_excludesFinalOrders() {
    Client a = client(1L, "ALPHA");
    Order active = order(a, OrderStatus.CREATED, 100.0);

    // Repository returns only active orders (DB filtering)
    when(orderRepository.findByCompanyAndDateRangeAndClientId(eq(1L), eq(START), eq(END), eq(1L),
        anyCollection())).thenReturn(List.of(active));

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, 1L, "ALPHA", START, END, "ACTIVE");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.CREATED);
  }

  @Test
  void getOrdersByClientAndDateRange_noMatchingClient_returnsEmpty() {
    when(orderRepository.findByCompanyAndDateRangeAndClientId(eq(1L), eq(START), eq(END), eq(99L),
        anyCollection())).thenReturn(List.of());

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, 99L, "NONEXISTENT", START, END, "ALL");

    assertThat(result).isEmpty();
  }

  @Test
  void getOrdersByClientAndDateRange_sortedByOrderDateDescending() {
    Client a = client(1L, "ALPHA");

    Order o1 = order(a, OrderStatus.CREATED, 100.0);
    o1.setOrderDate(LocalDate.of(2025, 1, 5));

    Order o2 = order(a, OrderStatus.CREATED, 200.0);
    o2.setOrderDate(LocalDate.of(2025, 1, 20));

    // DB returns already sorted by orderDate DESC
    when(orderRepository.findByCompanyAndDateRangeAndClientId(eq(1L), eq(START), eq(END), eq(1L),
        anyCollection())).thenReturn(List.of(o2, o1));

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, 1L, "ALPHA", START, END, "ALL");

    assertThat(result.get(0).getOrderDate()).isEqualTo(LocalDate.of(2025, 1, 20));
    assertThat(result.get(1).getOrderDate()).isEqualTo(LocalDate.of(2025, 1, 5));
  }

  @Test
  void getOrdersByClientAndDateRange_legacyOrder_usesCustomerNameQuery() {
    Order legacy = new Order();
    legacy.setStatus(OrderStatus.CREATED);
    legacy.setTotalAmount(75.0);
    legacy.setOrderDate(LocalDate.of(2025, 1, 15));

    when(orderRepository.findByCompanyAndDateRangeAndCustomerName(eq(1L), eq(START), eq(END),
        eq("OLD CUSTOMER"), anyCollection())).thenReturn(List.of(legacy));

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, null, "OLD CUSTOMER", START, END, "ALL");

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(legacy);
  }

  @Test
  void getOrdersByClientAndDateRange_nullClientIdAndNullName_returnsEmpty() {
    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, null, null, START, END, "ALL");

    assertThat(result).isEmpty();
  }

  @Test
  void createOrderWithCompany_derivesQuantityAndTotalAmountFromOrderItems() {
    Order order = new Order();
    OrderItem first = item(order, "Steel Rod", 2, 10.5);
    OrderItem second = item(order, "Cement", 3, 5.0);
    order.getOrderItems().add(first);
    order.getOrderItems().add(second);

    Company company = new Company();
    company.setId(5L);

    when(companyService.getCompanyById(5L)).thenReturn(java.util.Optional.of(company));
    when(helper.toTitleCase("Steel Rod, Cement")).thenReturn("Steel Rod, Cement");
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Order savedOrder = orderService.createOrderWithCompany(order, 5L);

    assertThat(savedOrder.getQuantity()).isEqualTo(5);
    assertThat(savedOrder.getTotalAmount()).isEqualTo(36.0);
    assertThat(savedOrder.getProductName()).isEqualTo("Steel Rod, Cement");
    assertThat(savedOrder.getCompany()).isEqualTo(company);
  }

  @Test
  void patchOrder_withOrderItems_recalculatesQuantityAndTotalAmount() {
    Order existingOrder = new Order();
    existingOrder.setId(9L);
    existingOrder.setProductName("Old Product");
    existingOrder.setQuantity(1);
    existingOrder.setTotalAmount(25.0);

    Order partialOrder = new Order();
    OrderItem first = item(partialOrder, "Steel Rod", 4, 12.5);
    OrderItem second = item(partialOrder, "Paint", 2, 7.25);
    partialOrder.getOrderItems().add(first);
    partialOrder.getOrderItems().add(second);

    when(orderRepository.findById(9L)).thenReturn(java.util.Optional.of(existingOrder));
    when(helper.toTitleCase("Steel Rod, Paint")).thenReturn("Steel Rod, Paint");
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Order savedOrder = orderService.patchOrder(9L, partialOrder);

    assertThat(savedOrder.getQuantity()).isEqualTo(6);
    assertThat(savedOrder.getTotalAmount()).isEqualTo(64.5);
    assertThat(savedOrder.getProductName()).isEqualTo("Steel Rod, Paint");
    assertThat(savedOrder.getOrderItems()).hasSize(2);
    assertThat(savedOrder.getOrderItems()).allMatch(item -> item.getOrder() == savedOrder);
  }

  @Test
  void createOrderWithCompany_whenDeliveryDateOnOrAfterOrderDate_savesSuccessfully() {
    Order order = new Order();
    order.setOrderDate(LocalDate.of(2026, 3, 27));
    order.setDeliveryDate(LocalDate.of(2026, 3, 27));

    Company company = new Company();
    company.setId(5L);

    when(companyService.getCompanyById(5L)).thenReturn(java.util.Optional.of(company));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Order savedOrder = orderService.createOrderWithCompany(order, 5L);

    assertThat(savedOrder.getCompany()).isEqualTo(company);
    assertThat(savedOrder.getOrderDate()).isEqualTo(LocalDate.of(2026, 3, 27));
    assertThat(savedOrder.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 3, 27));
  }

  @Test
  void createOrderWithCompany_whenDeliveryDateBeforeOrderDate_throwsValidationError() {
    Order order = new Order();
    order.setOrderDate(LocalDate.of(2026, 3, 27));
    order.setDeliveryDate(LocalDate.of(2026, 3, 26));

    assertThatThrownBy(() -> orderService.createOrderWithCompany(order, 5L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Delivery date must be on or after order date.");
  }

  private OrderItem item(Order order, String productName, int quantity, double unitPrice) {
    OrderItem item = new OrderItem();
    item.setOrder(order);
    item.setProductName(productName);
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);
    return item;
  }
}
