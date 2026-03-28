package com.example.ordermanager.order.service;

import com.example.ordermanager.order.entity.ActivityType;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderActivity;
import com.example.ordermanager.order.repository.OrderActivityRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Writes and reads {@link OrderActivity} audit log entries. All write methods are fire-and-record
 * and should be called from controllers AFTER the main service operation succeeds so a failure here
 * never rolls back the order operation itself.
 */
@Service
public class OrderActivityService {

  private final OrderActivityRepository orderActivityRepository;

  public OrderActivityService(OrderActivityRepository orderActivityRepository) {
    this.orderActivityRepository = orderActivityRepository;
  }

  // ── write helpers ────────────────────────────────────────────────────────────

  /** Log that an order was created. */
  public void logCreated(Order order, String actorUsername, String actorFullName, Long companyId) {
    OrderActivity a = base(order.getId(), order.getPoOrderNo(), order.getCustomerName(),
        actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.CREATED);
    a.setDescription(buildCreatedDescription(order));
    orderActivityRepository.save(a);
  }

  /** Log that an order status was changed. */
  public void logStatusChanged(Order order, String oldStatus, String newStatus,
      String actorUsername, String actorFullName, Long companyId) {
    OrderActivity a = base(order.getId(), order.getPoOrderNo(), order.getCustomerName(),
        actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.STATUS_CHANGED);
    a.setFieldChanged("status");
    a.setOldValue(oldStatus);
    a.setNewValue(newStatus);
    a.setDescription("Status changed from '" + oldStatus + "' to '" + newStatus + "'");
    orderActivityRepository.save(a);
  }

  /** Log that order details (fields other than status) were updated. */
  public void logUpdated(Order order, String actorUsername, String actorFullName, Long companyId) {
    OrderActivity a = base(order.getId(), order.getPoOrderNo(), order.getCustomerName(),
        actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.UPDATED);
    a.setDescription("Order details updated");
    orderActivityRepository.save(a);
  }

  /**
   * Log that an order was deleted. Accepts denormalized fields so the log entry can be written
   * after the order row has already been removed.
   */
  public void logDeleted(Long orderId, String orderPoNo, String orderClientName,
      String actorUsername, String actorFullName, Long companyId) {
    OrderActivity a =
        base(orderId, orderPoNo, orderClientName, actorUsername, actorFullName, companyId);
    a.setActivityType(ActivityType.DELETED);
    a.setDescription("Order #" + orderId
        + (orderPoNo != null && !orderPoNo.isBlank() ? " (PO: " + orderPoNo + ")" : "")
        + " was deleted");
    orderActivityRepository.save(a);
  }

  // ── read helpers ─────────────────────────────────────────────────────────────

  /**
   * TENANT-AWARE: Fetch activity log entries for the given date range, optionally filtered by a
   * search term. Returns results sorted newest-first.
   */
  public List<OrderActivity> getActivities(Long companyId, LocalDate startDate, LocalDate endDate,
      String search) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.atTime(23, 59, 59);
    String term = (search != null && !search.isBlank()) ? search.trim() : null;
    return orderActivityRepository.findByCompanyAndDateRangeAndSearch(companyId, start, end, term);
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
}

