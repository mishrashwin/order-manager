package com.example.ordermanager.order.service;

import com.example.ordermanager.admin.dto.ClientOrderStatDTO;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderItem;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.exception.OrderNotFoundException;
import com.example.ordermanager.order.repository.OrderRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final CompanyService companyService;
  private final Helper helper;

  public OrderService(OrderRepository orderRepository, CompanyService companyService,
      Helper helper) {
    this.orderRepository = orderRepository;
    this.companyService = companyService;
    this.helper = helper;
  }

  /**
   * TENANT-AWARE: Get orders for a specific company filtered by date range
   *
   * @param companyId Company ID
   * @param startDate Start date (inclusive)
   * @param endDate End date (inclusive)
   * @return List of orders within the date range
   */
  public List<Order> getOrdersByCompanyIdAndDateRange(Long companyId, LocalDate startDate,
      LocalDate endDate) {
    return orderRepository.findByCompanyIdAndOrderDateBetween(companyId, startDate, endDate);
  }

  public Order createOrder(Order order) {
    if (order.getProductName() != null)
      order.setProductName(helper.toTitleCase(order.getProductName()));
    return orderRepository.save(order);
  }

  /**
   * TENANT-AWARE: Create order with company association The order is automatically assigned to the
   * authenticated user's company
   *
   * @param order Order entity
   * @param companyId Company ID
   */
  public Order createOrderWithCompany(Order order, Long companyId) {
    // Get company and assign to order
    Company company =
        companyService.getCompanyById(companyId).orElseThrow(() -> new IllegalArgumentException(
            "Company not found. Cannot create order without a company."));

    order.setCompany(company);

    // Sync customerName from client relationship for data integrity
    if (order.getClient() != null) {
      order.setCustomerName(order.getClient().getName());
    }

    // Sync productName and quantity from orderItems if present
    syncLegacyFieldsFromItems(order);

    // Apply formatting to legacy productName field
    if (order.getProductName() != null)
      order.setProductName(helper.toTitleCase(order.getProductName()));

    return orderRepository.save(order);
  }

  public Order patchOrder(Long id, Order partialOrder) {
    return orderRepository.findById(id).map(existingOrder -> {
      // Update client relationship first (this auto-syncs customerName via setter)
      if (partialOrder.getClient() != null) {
        existingOrder.setClient(partialOrder.getClient());
      }

      // Legacy support: if customerName is provided without client
      if (partialOrder.getCustomerName() != null && partialOrder.getClient() == null) {
        existingOrder.setCustomerName(partialOrder.getCustomerName());
      }

      // Update order items if provided
      if (partialOrder.getOrderItems() != null && !partialOrder.getOrderItems().isEmpty()) {
        existingOrder.getOrderItems().clear();
        for (var item : partialOrder.getOrderItems()) {
          item.setOrder(existingOrder);
          existingOrder.getOrderItems().add(item);
        }
        syncLegacyFieldsFromItems(existingOrder);
        if (existingOrder.getProductName() != null)
          existingOrder.setProductName(helper.toTitleCase(existingOrder.getProductName()));
      } else if (partialOrder.getProductName() != null) {
        existingOrder.setProductName(helper.toTitleCase(partialOrder.getProductName()));
      }

      if (partialOrder.getQuantity() != null)
        existingOrder.setQuantity(partialOrder.getQuantity());

      if (partialOrder.getTotalAmount() != null)
        existingOrder.setTotalAmount(partialOrder.getTotalAmount());

      if (partialOrder.getStatus() != null)
        existingOrder.setStatus(partialOrder.getStatus());

      if (partialOrder.getOrderDate() != null)
        existingOrder.setOrderDate(partialOrder.getOrderDate());

      if (partialOrder.getDeliveryDate() != null)
        existingOrder.setDeliveryDate(partialOrder.getDeliveryDate());

      if (partialOrder.getPoOrderNo() != null)
        existingOrder.setPoOrderNo(partialOrder.getPoOrderNo());

      if (partialOrder.getOrderNote() != null)
        existingOrder.setOrderNote(partialOrder.getOrderNote());

      return orderRepository.save(existingOrder);
    }).orElseThrow(() -> new OrderNotFoundException(id));
  }


  public void deleteOrder(Long id) {
    if (!orderRepository.existsById(id)) {
      throw new OrderNotFoundException(id);
    }
    orderRepository.deleteById(id);
  }

  /**
   * Syncs the legacy productName and quantity fields from orderItems when items are present. This
   * keeps dashboard/list display working for orders with multiple products.
   */
  private void syncLegacyFieldsFromItems(Order order) {
    if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
      return;
    }
    String names = order.getOrderItems().stream().map(OrderItem::getProductName)
        .filter(n -> n != null && !n.isEmpty()).collect(java.util.stream.Collectors.joining(", "));
    if (!names.isEmpty()) {
      order.setProductName(names);
    }
    int totalQty = order.getOrderItems().stream()
        .mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0).sum();
    if (totalQty > 0) {
      order.setQuantity(totalQty);
    }
  }

  public Order getOrderById(Long id) {
    return orderRepository.findById(id).orElse(null);
  }


  /**
   * TENANT-AWARE: Get aggregated order statistics per client for the admin Order Statistics page.
   * Orders are grouped by client (name), counted, totalled, and ranked by order count descending.
   * The "statusFilter" parameter accepts "ACTIVE", "COMPLETED", or "ALL" (default). Aggregation and
   * filtering are pushed to the database via a JPQL GROUP BY query.
   *
   * @param companyId Company ID
   * @param startDate Start of order date range (inclusive)
   * @param endDate End of order date range (inclusive)
   * @param statusFilter "ACTIVE" | "COMPLETED" | "ALL"
   * @return Ordered list of per-client stats with percentage of total
   */
  public List<ClientOrderStatDTO> getClientOrderStats(Long companyId, LocalDate startDate,
      LocalDate endDate, String statusFilter) {
    List<OrderStatus> statuses = resolveStatuses(statusFilter);
    List<Object[]> rows =
        orderRepository.findClientOrderStats(companyId, startDate, endDate, statuses);

    if (rows.isEmpty()) {
      return List.of();
    }

    long total = rows.stream().mapToLong(r -> (Long) r[3]).sum();

    return rows.stream().map(r -> {
      Long clientId = (Long) r[0];
      String clientName = r[1] != null ? (String) r[1] : (r[2] != null ? (String) r[2] : "Unknown");
      long count = (Long) r[3];
      double amount = ((Number) r[4]).doubleValue();
      double pct = Math.round((count * 10000.0 / total)) / 100.0;
      return new ClientOrderStatDTO(clientId, clientName, count, amount, pct);
    }).toList();
  }

  /**
   * TENANT-AWARE: Get orders for a specific client within a date range, with optional status
   * filter. Used for the Order Statistics drill-down table. Filtering is pushed to the database.
   *
   * @param companyId Company ID
   * @param clientId Client ID (may be null for legacy orders without client relationship)
   * @param clientName Fallback name match when clientId is null
   * @param startDate Start of order date range (inclusive)
   * @param endDate End of order date range (inclusive)
   * @param statusFilter "ACTIVE" | "COMPLETED" | "ALL"
   * @return List of matching orders sorted by order date descending
   */
  public List<Order> getOrdersByClientAndDateRange(Long companyId, Long clientId, String clientName,
      LocalDate startDate, LocalDate endDate, String statusFilter) {
    List<OrderStatus> statuses = resolveStatuses(statusFilter);
    if (clientId != null) {
      return orderRepository.findByCompanyAndDateRangeAndClientId(companyId, startDate, endDate,
          clientId, statuses);
    }
    if (clientName == null) {
      return List.of();
    }
    return orderRepository.findByCompanyAndDateRangeAndCustomerName(companyId, startDate, endDate,
        clientName, statuses);
  }

  /**
   * Maps a status filter string to the corresponding list of OrderStatus enum values.
   *
   * @param statusFilter "ACTIVE" | "COMPLETED" | anything else (treated as "ALL")
   * @return List of matching OrderStatus values
   */
  private List<OrderStatus> resolveStatuses(String statusFilter) {
    if ("ACTIVE".equals(statusFilter)) {
      return Arrays.stream(OrderStatus.values()).filter(s -> !s.isFinal()).toList();
    }
    if ("COMPLETED".equals(statusFilter)) {
      return Arrays.stream(OrderStatus.values()).filter(OrderStatus::isFinal).toList();
    }
    return List.of(OrderStatus.values());
  }

  /**
   * TENANT-AWARE: Get urgent orders for a company (non-final status + delivery date either in the
   * next 7 days OR already past due). Overdue orders continue to appear until delivered or their
   * status becomes final. Used for dashboard flash notifications.
   *
   * @param companyId Company ID
   * @return List of urgent orders sorted by delivery date ascending (overdue first)
   */
  public List<Order> getUrgentOrdersByCompanyId(Long companyId) {
    LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);

    return orderRepository.findByCompanyIdAndDeliveryDateLessThanEqual(companyId, sevenDaysFromNow)
        .stream().filter(order -> !order.getStatus().isFinal()) // Exclude final status orders
                                                                // (DELIVERED, COMPLETED, RETURNED,
                                                                // CANCELLED, PENDING_PAYMENT)
        .sorted((o1, o2) -> {
          // Sort by delivery date ascending (overdue first, then nearest upcoming)
          if (o1.getDeliveryDate() == null && o2.getDeliveryDate() == null)
            return 0;
          if (o1.getDeliveryDate() == null)
            return 1;
          if (o2.getDeliveryDate() == null)
            return -1;
          return o1.getDeliveryDate().compareTo(o2.getDeliveryDate());
        }).toList();
  }
}
