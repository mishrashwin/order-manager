package com.example.ordermanager.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ordermanager.admin.dto.ClientOrderStatDTO;
import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.order.entity.Order;
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
 * Tests for OrderService statistics methods introduced for the Admin Order Statistics page.
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

  // ── getClientOrderStats ───────────────────────────────────────────────────────

  @Test
  void getClientOrderStats_noOrders_returnsEmptyList() {
    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END))
        .thenReturn(List.of());

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ALL");

    assertThat(stats).isEmpty();
  }

  @Test
  void getClientOrderStats_singleClient_returns100Percent() {
    Client acme = client(10L, "ACME");
    List<Order> orders =
        List.of(order(acme, OrderStatus.CREATED, 200.0), order(acme, OrderStatus.DELIVERED, 300.0));

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END)).thenReturn(orders);

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
    Client a = client(1L, "ALPHA");
    Client b = client(2L, "BETA");
    Client c = client(3L, "GAMMA");

    List<Order> orders =
        List.of(order(a, OrderStatus.CREATED, 100.0), order(a, OrderStatus.CREATED, 100.0),
            order(b, OrderStatus.CREATED, 50.0), order(c, OrderStatus.CREATED, 50.0));

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END)).thenReturn(orders);

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
  void getClientOrderStats_statusFilterActive_excludesFinalOrders() {
    Client a = client(1L, "ALPHA");
    Order activeOrder = order(a, OrderStatus.CREATED, 100.0);
    Order finalOrder = order(a, OrderStatus.DELIVERED, 200.0);

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END))
        .thenReturn(List.of(activeOrder, finalOrder));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ACTIVE");

    assertThat(stats).hasSize(1);
    assertThat(stats.get(0).getOrderCount()).isEqualTo(1);
    assertThat(stats.get(0).getTotalAmount()).isEqualTo(100.0);
  }

  @Test
  void getClientOrderStats_statusFilterCompleted_excludesActiveOrders() {
    Client a = client(1L, "ALPHA");
    Order activeOrder = order(a, OrderStatus.CREATED, 100.0);
    Order finalOrder = order(a, OrderStatus.COMPLETED, 200.0);

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END))
        .thenReturn(List.of(activeOrder, finalOrder));

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "COMPLETED");

    assertThat(stats).hasSize(1);
    assertThat(stats.get(0).getOrderCount()).isEqualTo(1);
    assertThat(stats.get(0).getTotalAmount()).isEqualTo(200.0);
  }

  @Test
  void getClientOrderStats_sortedByOrderCountDescending() {
    Client a = client(1L, "ALPHA");
    Client b = client(2L, "BETA");

    // BETA has 3, ALPHA has 1
    List<Order> orders =
        List.of(order(b, OrderStatus.CREATED, 50.0), order(b, OrderStatus.CREATED, 50.0),
            order(b, OrderStatus.CREATED, 50.0), order(a, OrderStatus.CREATED, 100.0));

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END)).thenReturn(orders);

    List<ClientOrderStatDTO> stats = orderService.getClientOrderStats(1L, START, END, "ALL");

    assertThat(stats.get(0).getClientName()).isEqualTo("BETA");
    assertThat(stats.get(1).getClientName()).isEqualTo("ALPHA");
  }

  // ── getOrdersByClientAndDateRange ─────────────────────────────────────────────

  @Test
  void getOrdersByClientAndDateRange_filtersByClientId() {
    Client a = client(1L, "ALPHA");
    Client b = client(2L, "BETA");
    Order oA = order(a, OrderStatus.CREATED, 100.0);
    Order oB = order(b, OrderStatus.CREATED, 200.0);

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END))
        .thenReturn(List.of(oA, oB));

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, 1L, "ALPHA", START, END, "ALL");

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(oA);
  }

  @Test
  void getOrdersByClientAndDateRange_statusFilter_active_excludesFinalOrders() {
    Client a = client(1L, "ALPHA");
    Order active = order(a, OrderStatus.CREATED, 100.0);
    Order done = order(a, OrderStatus.COMPLETED, 200.0);

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END))
        .thenReturn(List.of(active, done));

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, 1L, "ALPHA", START, END, "ACTIVE");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getStatus()).isEqualTo(OrderStatus.CREATED);
  }

  @Test
  void getOrdersByClientAndDateRange_noMatchingClient_returnsEmpty() {
    Client b = client(2L, "BETA");
    Order oB = order(b, OrderStatus.CREATED, 200.0);

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END))
        .thenReturn(List.of(oB));

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

    when(orderRepository.findByCompanyIdAndOrderDateBetween(1L, START, END))
        .thenReturn(List.of(o1, o2));

    List<Order> result =
        orderService.getOrdersByClientAndDateRange(1L, 1L, "ALPHA", START, END, "ALL");

    assertThat(result.get(0).getOrderDate()).isEqualTo(LocalDate.of(2025, 1, 20));
    assertThat(result.get(1).getOrderDate()).isEqualTo(LocalDate.of(2025, 1, 5));
  }
}
