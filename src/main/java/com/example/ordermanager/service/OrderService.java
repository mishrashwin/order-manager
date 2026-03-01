package com.example.ordermanager.service;

import com.example.ordermanager.entity.Company;
import com.example.ordermanager.entity.Order;
import com.example.ordermanager.entity.OrderStatus;
import com.example.ordermanager.exception.OrderNotFoundException;
import com.example.ordermanager.repository.OrderRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrderService {

  private final OrderRepository orderRepository;
  private final CompanyService companyService;
  private Helper helper;

  public OrderService(OrderRepository orderRepository, CompanyService companyService,
      Helper helper) {
    this.orderRepository = orderRepository;
    this.companyService = companyService;
    this.helper = helper;
  }

  public List<Order> getAllOrders() {
    return orderRepository.findAll();
  }

  /**
   * TENANT-AWARE: Get all orders for a specific company NEVER call getAllOrders() - Always filter
   * by company!
   */
  public List<Order> getOrdersByCompanyId(Long companyId) {
    return orderRepository.findByCompanyId(companyId);
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

    // Apply formatting
    if (order.getProductName() != null)
      order.setProductName(helper.toTitleCase(order.getProductName()));

    return orderRepository.save(order);
  }

  public Order patchOrder(Long id, Order partialOrder) {
    return orderRepository.findById(id).map(existingOrder -> {
      if (partialOrder.getCustomerName() != null)
        existingOrder.setCustomerName(partialOrder.getCustomerName());

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

  public Map<OrderStatus, Long> getOrdersGroupedByStatus() {
    Map<OrderStatus, Long> map = new EnumMap<>(OrderStatus.class);
    List<Order> allOrders = orderRepository.findAll();

    for (OrderStatus status : OrderStatus.values()) {
      long count = allOrders.stream().filter(order -> order.getStatus() == status).count();
      map.put(status, count);
    }
    return map;
  }

}
