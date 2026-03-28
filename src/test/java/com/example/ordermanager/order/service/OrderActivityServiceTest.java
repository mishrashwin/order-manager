package com.example.ordermanager.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.client.entity.Client;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    verify(orderActivityRepository).countByCompanyId(1L);
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
    verify(orderActivityRepository).countByCompanyId(3L);
  }

  // ── logUpdated ────────────────────────────────────────────────────────────────

  @Test
  void logUpdated_persistsDetailedBeforeAfterDescription() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    Order before = order(30L, "PO-003", "GAMMA", OrderStatus.CREATED);
    before.setOrderNote("Old note");
    before.setTotalAmount(100.0);

    Order after = order(30L, "PO-009", "OMEGA", OrderStatus.DISPATCHED);
    after.setOrderNote("New note");
    after.setTotalAmount(150.0);

    orderActivityService.logUpdated(before, after, "bob", "Bob Brown", 4L);

    ArgumentCaptor<OrderActivity> cap = ArgumentCaptor.forClass(OrderActivity.class);
    verify(orderActivityRepository).save(cap.capture());
    OrderActivity saved = cap.getValue();

    assertThat(saved.getActivityType()).isEqualTo(ActivityType.UPDATED);
    assertThat(saved.getOrderClientName()).isEqualTo("OMEGA");
    assertThat(saved.getDescription()).contains("Updated fields:")
        .contains("Client: 'GAMMA' → 'OMEGA'")
        .contains("PO / Order No: 'PO-003' → 'PO-009'")
        .contains("Status: 'Created' → 'Dispatched'")
        .contains("Order Note: 'Old note' → 'New note'")
        .contains("Total Amount: '₹100.00' → '₹150.00'");
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

  @Test
  void logCreated_whenCompanyHasMoreThan100Rows_trimsOlderRows() {
    when(orderActivityRepository.save(any(OrderActivity.class)))
        .thenAnswer(inv -> inv.getArgument(0));
    when(orderActivityRepository.countByCompanyId(9L)).thenReturn(120L);
    when(orderActivityRepository.findLatestIdsByCompanyId(org.mockito.ArgumentMatchers.eq(9L),
        org.mockito.ArgumentMatchers.any(PageRequest.class))).thenReturn(List.of(1L, 2L, 3L));

    Order order = order(77L, "PO-TRIM", "TRIM CLIENT", OrderStatus.CREATED);
    orderActivityService.logCreated(order, "admin", "Admin User", 9L);

    verify(orderActivityRepository).deleteByCompanyIdAndIdNotIn(9L, List.of(1L, 2L, 3L));
  }

  // ── getActivities ─────────────────────────────────────────────────────────────

  @Test
  void getActivitiesPage_passesTenantAndTimeWindowToRepository() {
    LocalDate start = LocalDate.of(2026, 3, 1);
    LocalDate end = LocalDate.of(2026, 3, 31);
    when(orderActivityRepository.countByCompanyId(7L)).thenReturn(10L);
    when(orderActivityRepository.findPageByCompanyAndDateRangeAndSearch(any(), any(), any(), any(),
        any())).thenReturn(Page.empty());

    Page<OrderActivity> result = orderActivityService.getActivitiesPage(7L, start, end, null, 0);

    assertThat(result.getContent()).isEmpty();
    ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> endCap = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<PageRequest> pageCap = ArgumentCaptor.forClass(PageRequest.class);
    verify(orderActivityRepository).findPageByCompanyAndDateRangeAndSearch(
        org.mockito.ArgumentMatchers.eq(7L), startCap.capture(), endCap.capture(),
        org.mockito.ArgumentMatchers.isNull(), pageCap.capture());
    assertThat(startCap.getValue().toLocalDate()).isEqualTo(start);
    assertThat(endCap.getValue().toLocalDate()).isEqualTo(end);
    assertThat(pageCap.getValue().getPageSize()).isEqualTo(20);
  }

  @Test
  void getActivitiesPage_withBlankSearch_passesNullTermToRepository() {
    when(orderActivityRepository.countByCompanyId(1L)).thenReturn(10L);
    when(orderActivityRepository.findPageByCompanyAndDateRangeAndSearch(any(), any(), any(), any(),
        any())).thenReturn(new PageImpl<>(List.of()));

    orderActivityService.getActivitiesPage(1L, LocalDate.now().minusDays(7), LocalDate.now(), "  ",
        0);

    verify(orderActivityRepository).findPageByCompanyAndDateRangeAndSearch(
        org.mockito.ArgumentMatchers.eq(1L), any(), any(),
        org.mockito.ArgumentMatchers.isNull(), any());
  }

  @Test
  void getActivitiesPage_whenPageIsNegative_normalizesToFirstPage() {
    when(orderActivityRepository.countByCompanyId(4L)).thenReturn(10L);
    when(orderActivityRepository.findPageByCompanyAndDateRangeAndSearch(any(), any(), any(), any(),
        any())).thenReturn(Page.empty());

    orderActivityService.getActivitiesPage(4L, LocalDate.now().minusDays(10), LocalDate.now(), null,
        -8);

    ArgumentCaptor<PageRequest> pageCap = ArgumentCaptor.forClass(PageRequest.class);
    verify(orderActivityRepository).findPageByCompanyAndDateRangeAndSearch(
        org.mockito.ArgumentMatchers.eq(4L), any(), any(), any(), pageCap.capture());
    assertThat(pageCap.getValue().getPageNumber()).isEqualTo(0);
  }

  // ── helpers ───────────────────────────────────────────────────────────────────

  private Order order(Long id, String poNo, String clientName, OrderStatus status) {
    Order o = new Order();
    o.setId(id);
    o.setPoOrderNo(poNo);
    if (clientName != null) {
      Client client = new Client();
      client.setId(id + 1000);
      client.setName(clientName);
      o.setClient(client);
    }
    o.setStatus(status);
    return o;
  }
}


