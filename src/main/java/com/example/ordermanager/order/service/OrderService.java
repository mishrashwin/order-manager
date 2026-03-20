package com.example.ordermanager.order.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.exception.OrderNotFoundException;
import com.example.ordermanager.order.repository.OrderRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
   * TENANT-AWARE: Get urgent orders for a company (non-final status + delivery date â‰¤ 7 days from
   * today). Used for dashboard flash notifications.
   *
   * @param companyId Company ID
   * @return List of urgent orders sorted by delivery date ascending
   */
  public List<Order> getUrgentOrdersByCompanyId(Long companyId) {
    LocalDate today = LocalDate.now();
    LocalDate sevenDaysFromNow = today.plusDays(7);

    return orderRepository.findByCompanyIdAndDeliveryDateBetween(companyId, today, sevenDaysFromNow)
        .stream().filter(order -> !order.getStatus().isFinal()) // Exclude final status orders
                                                                // (DELIVERED, COMPLETED, RETURNED,
                                                                // CANCELLED, PENDING_PAYMENT)
        .sorted((o1, o2) -> {
          // Sort by delivery date ascending (earliest first)
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
