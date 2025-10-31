package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Order;
import com.example.ordermanager.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Endpoints for managing customer orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Get all Orders", description = "Retrieve a list of all customer orders in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of orders")
    })
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @Operation(summary = "Create a new order", description = "Add a new order to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully")
    })
    @PostMapping
    public Order createOrder(
            @Parameter(description = "Order details to be created", required = true)
            @RequestBody Order order) {
        return orderService.createOrder(order);
    }

    @Operation(summary = "Partially update an order", description = "Update only specific fields of an existing order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order updated successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @PatchMapping("/{id}")
    public Order patchOrder(
            @Parameter(description = "ID of the order to be updated", required = true)
            @PathVariable Long id,
            @Parameter(description = "Partial order fields to update", required = true)
            @RequestBody Order partialOrder) {
        return orderService.patchOrder(id, partialOrder);
    }


    @Operation(summary = "Delete an order", description = "Remove an order by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(
            @Parameter(description = "ID of the order to delete", required = true)
            @PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build(); // HTTP 204
    }


    @GetMapping("/ping")
    public String ping() {
        return "Order Manager running!";
    }
}