package com.example.ordermanager.order.controller;

import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.service.OrderActivityService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.utils.SecurityContextHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "Endpoints for managing customer orders")
public class OrderRestController {

  private static final Logger log = LoggerFactory.getLogger(OrderRestController.class);

  private final OrderService orderService;
  private final OrderActivityService orderActivityService;
  private final SecurityContextHelper securityContextHelper;

  public OrderRestController(OrderService orderService, OrderActivityService orderActivityService,
      SecurityContextHelper securityContextHelper) {
    this.orderService = orderService;
    this.orderActivityService = orderActivityService;
    this.securityContextHelper = securityContextHelper;
  }


  @Operation(summary = "Create a new order", description = "Add a new order to the system")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "201", description = "Order created successfully")})
  @PostMapping
  public Order createOrder(@Parameter(description = "Order details to be created", required = true)
  @RequestBody Order order) {
    return orderService.createOrder(order);
  }

  @Operation(summary = "Partially update an order",
      description = "Update only specific fields of an existing order")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Order updated successfully"),
          @ApiResponse(responseCode = "404", description = "Order not found")})
  @PatchMapping("/{id}")
  public ResponseEntity<Map<String, String>> patchOrder(
      @Parameter(description = "ID of the order to be updated", required = true)
      @PathVariable Long id,
      @Parameter(description = "Partial order fields to update", required = true)
      @RequestBody Order partialOrder) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();

    // Capture old status for audit comparison
    Order oldOrder = orderService.getOrderByIdAndCompanyId(id, companyId);
    OrderStatus oldStatus = oldOrder != null ? oldOrder.getStatus() : null;

    Order saved = orderService.patchOrderForCompany(id, partialOrder, companyId);

    // ── audit (best-effort; must not break the API response) ──
    try {
      OrderStatus newStatus = saved.getStatus();
      if (oldStatus != null && newStatus != null && !oldStatus.equals(newStatus)) {
        User actor = securityContextHelper.getUserFromContext();
        orderActivityService.logStatusChanged(saved, oldStatus.getDisplayName(),
            newStatus.getDisplayName(), actor.getUsername(),
            actor.getFirstName() + " " + actor.getLastName(), companyId);
      }
    } catch (Exception e) {
      // Keep status update successful even if audit write fails.
      log.warn("Audit logging failed for order status patch. orderId={}, companyId={}", id,
          companyId, e);
    }

    Map<String, String> response = new HashMap<>();
    response.put("status", saved.getStatus().name());
    response.put("displayName", saved.getStatus().getDisplayName());
    response.put("badgeColor", saved.getStatus().getBadgeColor());
    return ResponseEntity.ok(response);
  }


  @Operation(summary = "Delete an order", description = "Remove an order by its ID")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "204", description = "Order deleted successfully"),
          @ApiResponse(responseCode = "404", description = "Order not found")})
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteOrder(
      @Parameter(description = "ID of the order to delete", required = true)
      @PathVariable Long id) {
    orderService.deleteOrder(id);
    return ResponseEntity.noContent().build(); // HTTP 204
  }

  @GetMapping("/api/order-statuses")
  @ResponseBody
  public List<Map<String, String>> getOrderStatuses() {
    return Arrays.stream(OrderStatus.values()).map(status -> {
      Map<String, String> map = new HashMap<>();
      map.put("name", status.name());
      map.put("displayName", status.getDisplayName());
      map.put("isFinal", String.valueOf(status.isFinal()));
      return map;
    }).collect(Collectors.toList());
  }
}
