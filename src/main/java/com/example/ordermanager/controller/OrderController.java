package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Order;
import com.example.ordermanager.entity.OrderStatus;
import com.example.ordermanager.service.ClientService;
import com.example.ordermanager.service.CompanyService;
import com.example.ordermanager.service.OrderService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;

@Controller
@RequestMapping("/orders")
public class OrderController {

  private final OrderService orderService;
  private final ClientService clientService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;

  public OrderController(OrderService orderService, ClientService clientService,
      CompanyService companyService, SecurityContextHelper securityContextHelper) {
    this.orderService = orderService;
    this.clientService = clientService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
  }

  // ✅ 1. List all orders
  @GetMapping
  public String listOrders(@RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();

    // Set default date range: past one month
    LocalDate start =
        startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
    LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

    model.addAttribute("orders",
        orderService.getOrdersByCompanyIdAndDateRange(companyId, start, end));
    model.addAttribute("startDate", start);
    model.addAttribute("endDate", end);
    return "orders/list";
  }

  // ✅ 2. Show form to create a new order
  @GetMapping("/new")
  public String showCreateForm(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("order", new Order());
    model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
    model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
    return "orders/form";
  }

  // ✅ 3. Handle new order submission
  @PostMapping
  public String saveOrder(@ModelAttribute("order") Order order, Model model) {
    try {
      if (order.getStatus() == null) {
        order.setStatus(OrderStatus.CREATED);
      }
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      orderService.createOrderWithCompany(order, companyId);
      return "redirect:/orders";
    } catch (IllegalStateException e) {
      return "redirect:/login";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving order: " + e.getMessage());
      model.addAttribute("order", order);
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
      model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
      return "orders/form";
    }
  }

  // ✅ 4. Show edit form
  @GetMapping("/edit/{id}")
  public String showEditForm(@PathVariable Long id, Model model) {
    Order order = orderService.getOrderById(id);
    if (order == null) {
      return "redirect:/orders";
    }
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("order", order);
    model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
    model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
    return "orders/form";
  }

  // ✅ 5. Handle update
  @PostMapping("/update/{id}")
  public String updateOrder(@PathVariable Long id, @ModelAttribute("order") Order updatedOrder) {
    orderService.patchOrder(id, updatedOrder);
    return "redirect:/orders";
  }

  // ✅ 6. Delete order
  @GetMapping("/delete/{id}")
  public String deleteOrder(@PathVariable Long id) {
    orderService.deleteOrder(id);
    return "redirect:/orders";
  }

  // ✅ 7. Duplicate order - fetches existing order and pre-populates form
  @GetMapping("/duplicate/{id}")
  public String duplicateOrder(@PathVariable Long id, Model model) {
    Order existingOrder = orderService.getOrderById(id);
    if (existingOrder == null) {
      return "redirect:/orders";
    }

    // Create a new order copy (without ID so it creates a new one)
    Order newOrder = new Order();
    newOrder.setCustomerName(existingOrder.getCustomerName());
    newOrder.setProductName(existingOrder.getProductName());
    newOrder.setQuantity(existingOrder.getQuantity());
    newOrder.setTotalAmount(existingOrder.getTotalAmount());
    newOrder.setPoOrderNo(existingOrder.getPoOrderNo());
    newOrder.setOrderDate(LocalDate.now());
    newOrder.setDeliveryDate(existingOrder.getDeliveryDate());
    newOrder.setOrderNote(existingOrder.getOrderNote());
    newOrder.setStatus(OrderStatus.CREATED);

    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("order", newOrder);
    model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
    model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
    return "orders/form";
  }
}
