package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Company;
import com.example.ordermanager.entity.OrderStatus;
import com.example.ordermanager.service.CompanyService;
import com.example.ordermanager.service.OrderService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

  private final OrderService orderService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;

  public DashboardController(OrderService orderService, CompanyService companyService,
      SecurityContextHelper securityContextHelper) {
    this.orderService = orderService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
  }

  @GetMapping("/dashboard")
  public String dashboard(@RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate, Model model) {
    // TENANT-AWARE: Get company ID from authenticated user
    Long companyId = securityContextHelper.getCompanyIdFromContext();

    // Get company name for dashboard header
    String companyName =
        companyService.getCompanyById(companyId).map(Company::getName).orElse("Order Dashboard");

    // Set default date range: past one month
    LocalDate start =
        startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
    LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

    List<OrderStatus> statuses = Arrays.asList(OrderStatus.values());
    model.addAttribute("statuses", statuses);
    model.addAttribute("companyName", companyName);
    model.addAttribute("startDate", start);
    model.addAttribute("endDate", end);

    // Filter orders by company and date range - ensures tenant isolation
    model.addAttribute("orders",
        orderService.getOrdersByCompanyIdAndDateRange(companyId, start, end));

    // Send only minimal fields needed by dashboard notification JS.
    var urgentOrderNotifications = orderService.getUrgentOrdersByCompanyId(companyId).stream()
        .map(order -> Map.of("id", order.getId(), "customerName", order.getCustomerName(),
            "productName", order.getProductName(), "quantity", order.getQuantity(), "deliveryDate",
            order.getDeliveryDate().toString()))
        .toList();
    model.addAttribute("urgentOrders", urgentOrderNotifications);

    return "dashboard";
  }

}
