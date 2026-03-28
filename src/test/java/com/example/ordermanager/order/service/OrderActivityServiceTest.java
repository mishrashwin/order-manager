package com.example.ordermanager.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.order.entity.ActivityType;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderActivity;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.repository.OrderActivityRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for OrderActivityService: happy paths and edge cases for each log method. */
@ExtendWith(MockitoExtension.class)
class OrderActivityServiceTest {

  @Mock
  private OrderActivityRepository orderActivityRepository;

  private OrderActivityService orderActivityService;

  @BeforeEach
  void setUp() {
    orderActivityService = new OrderActivityService(orderActivityRepository);
  }

  // ── logCreated ────────────────────────────────────────────────────────────────

  @Test
  void logCreated_persistsActivityWithCorrectType() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    Order order = order(10L, "PO-001", "ACME", OrderStatus.CREATED);
    order.setTotalAmount(500.0);

    orderActivityService.logCreated(order, "john", "John Doe", 1L);

    ArgumentCaptor<OrderActivity> cap = ArgumentCaptor.forClass(OrderActivity.class);
    verify(orderActivityRepository).save(cap.capture());
    OrderActivity saved = cap.getValue();

    assertThat(saved.getActivityType()).isEqualTo(ActivityType.CREATED);
    assertThat(saved.getOrderId()).isEqualTo(10L);
    assertThat(saved.getOrderPoNo()).isEqualTo("PO-001");
    assertThat(saved.getOrderClientName()).isEqualTo("ACME");
    assertThat(saved.getActorUsername()).isEqualTo("john");
    assertThat(saved.getActorFullName()).isEqualTo("John Doe");
    assertThat(saved.getCompanyId()).isEqualTo(1L);
    assertThat(saved.getDescription()).contains("Order created");
    assertThat(saved.getActivityAt()).isNotNull();
  }

  @Test
  void logCreated_withNullPoNo_descriptionDoesNotMentionPO() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    Order order = order(11L, null, "CLIENT", OrderStatus.CREATED);

    orderActivityService.logCreated(order, "jane", "Jane Doe", 2L);

    ArgumentCaptor<OrderActivity> cap = ArgumentCaptor.forClass(OrderActivity.class);
    verify(orderActivityRepository).save(cap.capture());
    assertThat(cap.getValue().getDescription()).doesNotContain("PO:");
  }

  // ── logStatusChanged ──────────────────────────────────────────────────────────

  @Test
  void logStatusChanged_persistsOldAndNewValueAndDescription() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    Order order = order(20L, "PO-002", "BETA", OrderStatus.DELIVERED);

    orderActivityService.logStatusChanged(order, "Dispatched", "Delivered", "alice", "Alice Smith",
        3L);

    ArgumentCaptor<OrderActivity> cap = ArgumentCaptor.forClass(OrderActivity.class);
    verify(orderActivityRepository).save(cap.capture());
    OrderActivity saved = cap.getValue();

    assertThat(saved.getActivityType()).isEqualTo(ActivityType.STATUS_CHANGED);
    assertThat(saved.getFieldChanged()).isEqualTo("status");
    assertThat(saved.getOldValue()).isEqualTo("Dispatched");
    assertThat(saved.getNewValue()).isEqualTo("Delivered");
    assertThat(saved.getDescription()).isEqualTo("Status changed from 'Dispatched' to 'Delivered'");
  }

  // ── logUpdated ────────────────────────────────────────────────────────────────

  @Test
  void logUpdated_persistsUpdateActivityWithDescription() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    Order order = order(30L, "PO-003", "GAMMA", OrderStatus.CREATED);

    orderActivityService.logUpdated(order, "bob", "Bob Brown", 4L);

    ArgumentCaptor<OrderActivity> cap = ArgumentCaptor.forClass(OrderActivity.class);
    verify(orderActivityRepository).save(cap.capture());
    OrderActivity saved = cap.getValue();

    assertThat(saved.getActivityType()).isEqualTo(ActivityType.UPDATED);
    assertThat(saved.getDescription()).isEqualTo("Order details updated");
    assertThat(saved.getOldValue()).isNull();
    assertThat(saved.getNewValue()).isNull();
  }

  // ── logDeleted ────────────────────────────────────────────────────────────────

  @Test
  void logDeleted_persistsDeletedActivityWithDenormalizedData() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    orderActivityService.logDeleted(40L, "PO-004", "DELTA", "carol", "Carol White", 5L);

    ArgumentCaptor<OrderActivity> cap = ArgumentCaptor.forClass(OrderActivity.class);
    verify(orderActivityRepository).save(cap.capture());
    OrderActivity saved = cap.getValue();

    assertThat(saved.getActivityType()).isEqualTo(ActivityType.DELETED);
    assertThat(saved.getOrderId()).isEqualTo(40L);
    assertThat(saved.getOrderPoNo()).isEqualTo("PO-004");
    assertThat(saved.getDescription()).contains("Order #40").contains("PO: PO-004");
  }

  @Test
  void logDeleted_withNullPoNo_descriptionDoesNotMentionPO() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    orderActivityService.logDeleted(50L, null, "CLIENT", "dave", "Dave Green", 6L);

    ArgumentCaptor<OrderActivity> cap = ArgumentCaptor.forClass(OrderActivity.class);
    verify(orderActivityRepository).save(cap.capture());
    assertThat(cap.getValue().getDescription()).doesNotContain("PO:");
  }

  // ── getActivities ─────────────────────────────────────────────────────────────

  @Test
  void getActivities_passesTenantAndTimeWindowToRepository() {
    LocalDate start = LocalDate.of(2026, 3, 1);
    LocalDate end = LocalDate.of(2026, 3, 31);
    when(orderActivityRepository.findByCompanyAndDateRangeAndSearch(any(), any(), any(), any()))
        .thenReturn(List.of());

    List<OrderActivity> result = orderActivityService.getActivities(7L, start, end, null);

    assertThat(result).isEmpty();
    ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> endCap = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(orderActivityRepository).findByCompanyAndDateRangeAndSearch(
        org.mockito.ArgumentMatchers.eq(7L), startCap.capture(), endCap.capture(),
        org.mockito.ArgumentMatchers.isNull());
    assertThat(startCap.getValue().toLocalDate()).isEqualTo(start);
    assertThat(endCap.getValue().toLocalDate()).isEqualTo(end);
  }

  @Test
  void getActivities_withBlankSearch_passesNullTermToRepository() {
    when(orderActivityRepository.findByCompanyAndDateRangeAndSearch(any(), any(), any(), any()))
        .thenReturn(List.of());

    orderActivityService.getActivities(1L, LocalDate.now().minusDays(7), LocalDate.now(), "  ");

    verify(orderActivityRepository).findByCompanyAndDateRangeAndSearch(
        org.mockito.ArgumentMatchers.eq(1L), any(), any(),
        org.mockito.ArgumentMatchers.isNull());
  }

  // ── helpers ───────────────────────────────────────────────────────────────────

  private Order order(Long id, String poNo, String clientName, OrderStatus status) {
    Order o = new Order();
    o.setId(id);
    o.setPoOrderNo(poNo);
    o.setCustomerName(clientName);
    o.setStatus(status);
    return o;
  }
}


