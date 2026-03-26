package com.example.ordermanager.order.service;

import com.example.ordermanager.admin.dto.ClientOrderStatDTO;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.exception.OrderNotFoundException;
import com.example.ordermanager.order.repository.OrderRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // Apply formatting
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

      if (partialOrder.getProductName() != null)
        existingOrder.setProductName(helper.toTitleCase(partialOrder.getProductName()));

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

  public Order getOrderById(Long id) {
    return orderRepository.findById(id).orElse(null);
  }


  /**
   * TENANT-AWARE: Get aggregated order statistics per client for the admin Order Statistics page.
   * Orders are grouped by client (name), counted, totalled, and ranked by order count descending.
   * The "statusFilter" parameter accepts "ACTIVE", "COMPLETED", or "ALL" (default).
   *
   * @param companyId Company ID
   * @param startDate Start of order date range (inclusive)
   * @param endDate End of order date range (inclusive)
   * @param statusFilter "ACTIVE" | "COMPLETED" | "ALL"
   * @return Ordered list of per-client stats with percentage of total
   */
  public List<ClientOrderStatDTO> getClientOrderStats(Long companyId, LocalDate startDate,
      LocalDate endDate, String statusFilter) {
    List<Order> orders =
        orderRepository.findByCompanyIdAndOrderDateBetween(companyId, startDate, endDate);

    if ("ACTIVE".equals(statusFilter)) {
      orders = orders.stream().filter(o -> !o.getStatus().isFinal()).toList();
    } else if ("COMPLETED".equals(statusFilter)) {
      orders = orders.stream().filter(o -> o.getStatus().isFinal()).toList();
    }

    long total = orders.size();
    if (total == 0) {
      return List.of();
    }

    // Group by a type-safe key: (clientId, effectiveClientName)
    // Use a record to avoid any string-prefix collision risks
    record ClientKey(Long clientId, String name) {}

    Map<ClientKey, List<Order>> grouped = orders.stream().collect(Collectors.groupingBy(o -> {
      if (o.getClient() != null) {
        return new ClientKey(o.getClient().getId(), o.getClient().getName());
      }
      String name = (o.getCustomerName() != null) ? o.getCustomerName() : "Unknown";
      return new ClientKey(null, name);
    }, LinkedHashMap::new, Collectors.toList()));

    return grouped.entrySet().stream().map(entry -> {
      ClientKey key = entry.getKey();
      List<Order> clientOrders = entry.getValue();
      long count = clientOrders.size();
      double amount = clientOrders.stream()
          .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0).sum();
      double pct = Math.round((count * 10000.0 / total)) / 100.0;
      return new ClientOrderStatDTO(key.clientId(), key.name(), count, amount, pct);
    }).sorted(Comparator.comparingLong(ClientOrderStatDTO::getOrderCount).reversed()).toList();
  }

  /**
   * TENANT-AWARE: Get orders for a specific client within a date range, with optional status
   * filter. Used for the Order Statistics drill-down table.
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
    List<Order> orders =
        orderRepository.findByCompanyIdAndOrderDateBetween(companyId, startDate, endDate);

    orders = orders.stream().filter(o -> {
      if (clientId != null) {
        return o.getClient() != null && clientId.equals(o.getClient().getId());
      }
      String name = o.getCustomerName();
      return clientName != null && clientName.equalsIgnoreCase(name);
    }).toList();

    if ("ACTIVE".equals(statusFilter)) {
      orders = orders.stream().filter(o -> !o.getStatus().isFinal()).toList();
    } else if ("COMPLETED".equals(statusFilter)) {
      orders = orders.stream().filter(o -> o.getStatus().isFinal()).toList();
    }

    return orders.stream().sorted(
        Comparator.comparing(Order::getOrderDate, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
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
