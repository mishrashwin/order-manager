package com.example.ordermanager.controller;

import com.example.ordermanager.entity.OrderStatus;
import com.example.ordermanager.service.OrderService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

@Controller
public class DashboardController {

  private final OrderService orderService;
  private final SecurityContextHelper securityContextHelper;

  public DashboardController(OrderService orderService,
      SecurityContextHelper securityContextHelper) {
    this.orderService = orderService;
    this.securityContextHelper = securityContextHelper;
  }

  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    // TENANT-AWARE: Get company ID from authenticated user
    Long companyId = securityContextHelper.getCompanyIdFromContext();

    List<OrderStatus> statuses = Arrays.asList(OrderStatus.values());
    model.addAttribute("statuses", statuses);

    // Filter orders by company - ensures tenant isolation
    model.addAttribute("orders", orderService.getOrdersByCompanyId(companyId));
    return "dashboard";
  }

}
