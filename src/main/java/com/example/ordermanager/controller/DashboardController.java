package com.example.ordermanager.controller;

import com.example.ordermanager.entity.OrderStatus;
import com.example.ordermanager.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    private final OrderService orderService;

    public DashboardController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<OrderStatus> statuses = Arrays.asList(OrderStatus.values());
        model.addAttribute("statuses", statuses);
        model.addAttribute("orders", orderService.getAllOrders());
        return "dashboard";
    }

}
