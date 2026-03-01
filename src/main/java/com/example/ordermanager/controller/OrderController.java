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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
  public String listOrders(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("orders", orderService.getOrdersByCompanyId(companyId));
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

}
