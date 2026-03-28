package com.example.ordermanager.order.service;

import com.example.ordermanager.order.entity.ActivityType;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderActivity;
import com.example.ordermanager.order.repository.OrderActivityRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads {@link OrderActivity} audit log entries. All write methods are fire-and-record
 * and should be called from controllers AFTER the main service operation succeeds so a failure here
 * never rolls back the order operation itself.
 */
@Service
public class OrderActivityService {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd MMM yyyy");
  private static final int MAX_ROWS_PER_COMPANY = 100;
  private static final int PAGE_SIZE = 20;

  private final OrderActivityRepository orderActivityRepository;

  public OrderActivityService(OrderActivityRepository orderActivityRepository) {
    this.orderActivityRepository = orderActivityRepository;
  }

  // ── write helpers ────────────────────────────────────────────────────────────

  /** Log that an order was created. */
  @Transactional
  public void logCreated(Order order, String actorUsername, String actorFullName, Long companyId) {
    OrderActivity a = base(order.getId(), order.getPoOrderNo(), order.getCustomerName(),
        actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.CREATED);
    a.setDescription(buildCreatedDescription(order));
    saveAndTrimToLatest100(a);
  }

  /** Log that an order status was changed. */
  @Transactional
  public void logStatusChanged(Order order, String oldStatus, String newStatus,
      String actorUsername, String actorFullName, Long companyId) {
    OrderActivity a = base(order.getId(), order.getPoOrderNo(), order.getCustomerName(),
        actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.STATUS_CHANGED);
    a.setFieldChanged("status");
    a.setOldValue(oldStatus);
    a.setNewValue(newStatus);
    a.setDescription("Status changed from '" + oldStatus + "' to '" + newStatus + "'");
    saveAndTrimToLatest100(a);
  }

  /** Log that order details (fields other than status) were updated. */
  @Transactional
  public void logUpdated(Order before, Order after, String actorUsername, String actorFullName,
      Long companyId) {
    OrderActivity a = base(after.getId(), after.getPoOrderNo(), after.getCustomerName(),
        actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.UPDATED);
    a.setDescription(buildUpdateDescription(before, after));
    saveAndTrimToLatest100(a);
  }

  /**
   * Log that an order was deleted. Accepts denormalized fields so the log entry can be written
   * after the order row has already been removed.
   */
  @Transactional
  public void logDeleted(Long orderId, String orderPoNo, String orderClientName,
      String actorUsername, String actorFullName, Long companyId) {
    OrderActivity a =
        base(orderId, orderPoNo, orderClientName, actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.DELETED);
    a.setDescription("Order #" + orderId
        + (orderPoNo != null && !orderPoNo.isBlank() ? " (PO: " + orderPoNo + ")" : "")
        + " was deleted");
    saveAndTrimToLatest100(a);
  }

  // ── read helpers ─────────────────────────────────────────────────────────────

  /**
   * TENANT-AWARE: Fetch activity log entries for the given date range, optionally filtered by a
   * search term. Returns results sorted newest-first.
   */
  @Transactional
  public Page<OrderActivity> getActivitiesPage(Long companyId, LocalDate startDate,
      LocalDate endDate, String search, int pageNumber) {
    trimCompanyLogs(companyId);

    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(23, 59, 59);
    String term = (search != null && !search.isBlank()) ? search.trim() : null;
    int normalizedPage = Math.max(0, pageNumber);
    return orderActivityRepository.findPageByCompanyAndDateRangeAndSearch(companyId, start, end,
        term, PageRequest.of(normalizedPage, PAGE_SIZE));
  }

  // ── private ──────────────────────────────────────────────────────────────────

  private OrderActivity base(Long orderId, String poNo, String clientName, String actorUsername,
      String actorFullName, Long companyId) {
    OrderActivity a = new OrderActivity();
    a.setOrderId(orderId);
    a.setOrderPoNo(poNo);
    a.setOrderClientName(clientName);
    a.setActorUsername(actorUsername);
    a.setActorFullName(actorFullName);
    a.setActivityAt(LocalDateTime.now());
    a.setCompanyId(companyId);
    return a;
  }

  private String buildCreatedDescription(Order order) {
    StringBuilder sb = new StringBuilder("Order created");
    if (order.getCustomerName() != null && !order.getCustomerName().isBlank()) {
      sb.append(" for '").append(order.getCustomerName()).append("'");
    }
    if (order.getPoOrderNo() != null && !order.getPoOrderNo().isBlank()) {
      sb.append(", PO: ").append(order.getPoOrderNo());
    }
    String productSummary = order.getDisplayProductSummary();
    if (!"-".equals(productSummary)) {
      sb.append(", Product: ").append(productSummary);
    }
    if (order.getTotalAmount() != null && order.getTotalAmount() > 0) {
      sb.append(String.format(", Total: %.2f", order.getTotalAmount()));
    }
    return sb.toString();
  }

  private String buildUpdateDescription(Order before, Order after) {
    List<String> changes = new ArrayList<>();

    addChange(changes, "Client", display(before.getCustomerName()),
        display(after.getCustomerName()));
    addChange(changes, "PO / Order No", display(before.getPoOrderNo()),
        display(after.getPoOrderNo()));
    addChange(changes, "Products", display(before.getDisplayProductSummary()),
        display(after.getDisplayProductSummary()));
    addChange(changes, "Status", displayStatus(before), displayStatus(after));
    addChange(changes, "Order Date", displayDate(before.getOrderDate()),
        displayDate(after.getOrderDate()));
    addChange(changes, "Delivery Date", displayDate(before.getDeliveryDate()),
        displayDate(after.getDeliveryDate()));
    addChange(changes, "Order Note", display(before.getOrderNote()), display(after.getOrderNote()));
    addChange(changes, "Total Amount", displayAmount(before.getTotalAmount()),
        displayAmount(after.getTotalAmount()));

    if (changes.isEmpty()) {
      return "Order details updated";
    }

    return "Updated fields:\n" + String.join("\n", changes);
  }

  private void addChange(List<String> changes, String field, String beforeValue,
      String afterValue) {
    if (!beforeValue.equals(afterValue)) {
      changes.add(field + ": '" + beforeValue + "' → '" + afterValue + "'");
    }
  }

  private String display(String value) {
    if (value == null || value.isBlank() || "-".equals(value)) {
      return "-";
    }
    return value;
  }

  private String displayStatus(Order order) {
    return order != null && order.getStatus() != null ? order.getStatus().getDisplayName() : "-";
  }

  private String displayDate(LocalDate value) {
    return value != null ? value.format(DATE_FORMATTER) : "-";
  }

  private String displayAmount(Double value) {
    return value != null ? String.format("₹%.2f", value) : "-";
  }

  private void saveAndTrimToLatest100(OrderActivity activity) {
    orderActivityRepository.save(activity);
    trimCompanyLogs(activity.getCompanyId());
  }

  private void trimCompanyLogs(Long companyId) {
    if (companyId == null) {
      return;
    }

    long totalRows = orderActivityRepository.countByCompanyId(companyId);
    if (totalRows <= MAX_ROWS_PER_COMPANY) {
      return;
    }

    List<Long> retainedIds = orderActivityRepository.findLatestIdsByCompanyId(companyId,
        PageRequest.of(0, MAX_ROWS_PER_COMPANY));
    if (retainedIds == null || retainedIds.isEmpty()) {
      return;
    }

    orderActivityRepository.deleteByCompanyIdAndIdNotIn(companyId, retainedIds);
  }
}

