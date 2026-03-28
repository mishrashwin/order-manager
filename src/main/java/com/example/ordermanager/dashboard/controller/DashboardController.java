package com.example.ordermanager.dashboard.controller;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.payment.service.PaymentService;
import com.example.ordermanager.payment.service.PaymentService.PaymentReminderInfo;
import com.example.ordermanager.user.entity.User;
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
  private final PaymentService paymentService;
  private final SecurityContextHelper securityContextHelper;

  public DashboardController(OrderService orderService, CompanyService companyService,
      PaymentService paymentService, SecurityContextHelper securityContextHelper) {
    this.orderService = orderService;
    this.companyService = companyService;
    this.paymentService = paymentService;
    this.securityContextHelper = securityContextHelper;
  }

  @GetMapping("/dashboard")
  public String dashboard(@RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();

    String companyName =
        companyService.getCompanyById(companyId).map(Company::getName).orElse("Order Dashboard");

    LocalDate start =
        startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
    LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

    List<OrderStatus> statuses = Arrays.asList(OrderStatus.values());
    model.addAttribute("statuses", statuses);
    model.addAttribute("companyName", companyName);
    model.addAttribute("startDate", start);
    model.addAttribute("endDate", end);

    model.addAttribute("orders",
        orderService.getOrdersByCompanyIdAndDateRange(companyId, start, end));

    User currentUser = securityContextHelper.getUserFromContext();
    PaymentReminderInfo reminder = "ADMIN".equalsIgnoreCase(currentUser.getRole())
        ? paymentService.getPaymentReminderInfo(companyId)
        : new PaymentReminderInfo(false, 0, 0, null, null);
    model.addAttribute("paymentReminder", reminder);

    var urgentOrderNotifications = orderService.getUrgentOrdersByCompanyId(companyId).stream()
        .filter(order -> isWithinSelectedDateRange(order, start, end))
        .map(order -> Map.of("id", order.getId(), "customerName", order.getCustomerName(),
            "productName", order.getDisplayProductSummary(), "quantity",
            order.getDisplayQuantitySummary(), "deliveryDate", order.getDeliveryDate().toString()))
        .toList();
    model.addAttribute("urgentOrders", urgentOrderNotifications);

    return "dashboard";
  }

  private boolean isWithinSelectedDateRange(Order order, LocalDate start, LocalDate end) {
    if (order.getOrderDate() == null) {
      return false;
    }
    return !order.getOrderDate().isBefore(start) && !order.getOrderDate().isAfter(end);
  }

}
