package com.example.ordermanager.controller;

import com.example.ordermanager.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final OrderService orderService;

    public DashboardController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "dashboard";
    }
}
