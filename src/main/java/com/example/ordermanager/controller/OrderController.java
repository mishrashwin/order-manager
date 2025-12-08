package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Order;
import com.example.ordermanager.entity.OrderStatus;
import com.example.ordermanager.service.ClientService;
import com.example.ordermanager.service.OrderService;
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

    public OrderController(OrderService orderService, ClientService clientService) {
        this.orderService = orderService;
        this.clientService = clientService;
    }

    // ✅ 1. List all orders
    @GetMapping
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "orders/list";
    }

    // ✅ 2. Show form to create a new order
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("order", new Order());
        model.addAttribute("clients", clientService.getAllClients());
        model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
        return "orders/form";
    }

    // ✅ 3. Handle new order submission
    @PostMapping
    public String saveOrder(@ModelAttribute("order") Order order) {
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.CREATED);
        }
        orderService.createOrder(order);
        return "redirect:/orders";
    }

    // ✅ 4. Show edit form
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("clients", clientService.getAllClients());
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
