package com.example.ordermanager.order.controller;

import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderItem;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.order.service.OrderActivityService;
import com.example.ordermanager.client.service.ClientService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.ordermanager.utils.PasswordVerificationService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

  private static final Logger log = LoggerFactory.getLogger(OrderController.class);

  private final OrderService orderService;
  private final OrderActivityService orderActivityService;
  private final ClientService clientService;
  private final ProductService productService;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;

  public OrderController(OrderService orderService, OrderActivityService orderActivityService,
      ClientService clientService, ProductService productService,
      SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService) {
    this.orderService = orderService;
    this.orderActivityService = orderActivityService;
    this.clientService = clientService;
    this.productService = productService;
    this.securityContextHelper = securityContextHelper;
    this.passwordVerificationService = passwordVerificationService;
  }

  @GetMapping
  public String listOrders(@RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();

    LocalDate start =
        startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusMonths(1);
    LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

    model.addAttribute("orders",
        orderService.getOrdersByCompanyIdAndDateRange(companyId, start, end));
    model.addAttribute("startDate", start);
    model.addAttribute("endDate", end);
    return "orders/list";
  }

  @GetMapping("/new")
  public String showCreateForm(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("order", new Order());
    model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
    model.addAttribute("products", productService.getProductsByCompanyId(companyId));
    model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
    return "orders/form";
  }

  @PostMapping
  public String saveOrder(@ModelAttribute("order") Order order,
      @RequestParam(required = false) List<Long> itemProductIds,
      @RequestParam(required = false) List<Integer> itemQuantities,
      @RequestParam(required = false) List<Double> itemUnitPrices, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      if (order.getStatus() == null) {
        order.setStatus(OrderStatus.CREATED);
      }
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      buildOrderItems(order, itemProductIds, itemQuantities, itemUnitPrices);
      Order savedOrder = orderService.createOrderWithCompany(order, companyId);
      // ── audit ──
      try {
        User actor = securityContextHelper.getUserFromContext();
        orderActivityService.logCreated(savedOrder, actor.getUsername(),
            actor.getFirstName() + " " + actor.getLastName(), companyId);
      } catch (Exception e) {
        log.warn("Audit logging failed for order creation. orderId={}, companyId={}",
            savedOrder.getId(), companyId, e);
      }
      redirectAttributes.addFlashAttribute("message", "Order created successfully");
      return "redirect:/orders";
    } catch (IllegalStateException e) {
      return "redirect:/login";
    } catch (IllegalArgumentException e) {
      model.addAttribute("error", e.getMessage());
      if (e.getMessage() != null && e.getMessage().contains("Delivery date")) {
        model.addAttribute("dateError", e.getMessage());
      }
      model.addAttribute("order", order);
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
      model.addAttribute("products", productService.getProductsByCompanyId(companyId));
      model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
      return "orders/form";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving order: " + e.getMessage());
      model.addAttribute("order", order);
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
      model.addAttribute("products", productService.getProductsByCompanyId(companyId));
      model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
      return "orders/form";
    }
  }

  @GetMapping("/edit/{id}")
  public String showEditForm(@PathVariable Long id, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Order order = orderService.getOrderByIdAndCompanyId(id, companyId);
    if (order == null) {
      return "redirect:/orders";
    }
    model.addAttribute("order", order);
    model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
    model.addAttribute("products", productService.getProductsByCompanyId(companyId));
    model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
    return "orders/form";
  }

  @PostMapping("/update/{id}")
  public String updateOrder(@PathVariable Long id, @ModelAttribute("order") Order updatedOrder,
      @RequestParam(required = false) List<Long> itemProductIds,
      @RequestParam(required = false) List<Integer> itemQuantities,
      @RequestParam(required = false) List<Double> itemUnitPrices, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      Order oldOrder = orderService.getOrderByIdAndCompanyId(id, companyId);
      if (oldOrder == null) {
        redirectAttributes.addFlashAttribute("error", "Order not found.");
        return "redirect:/orders";
      }
      Order oldOrderSnapshot = createAuditSnapshot(oldOrder);

      buildOrderItems(updatedOrder, itemProductIds, itemQuantities, itemUnitPrices);
      Order savedOrder = orderService.patchOrderForCompany(id, updatedOrder, companyId);

      // ── audit ──
      try {
        User actor = securityContextHelper.getUserFromContext();
        orderActivityService.logUpdated(oldOrderSnapshot, savedOrder, actor.getUsername(),
            actor.getFirstName() + " " + actor.getLastName(), companyId);
      } catch (Exception e) {
        log.warn("Audit logging failed for order update. orderId={}, companyId={}", id, companyId,
            e);
      }

      redirectAttributes.addFlashAttribute("message", "Order updated successfully");
      return "redirect:/orders";
    } catch (IllegalStateException e) {
      return "redirect:/login";
    } catch (IllegalArgumentException e) {
      model.addAttribute("error", e.getMessage());
      if (e.getMessage() != null && e.getMessage().contains("Delivery date")) {
        model.addAttribute("dateError", e.getMessage());
      }
      model.addAttribute("order", updatedOrder);
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
      model.addAttribute("products", productService.getProductsByCompanyId(companyId));
      model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
      return "orders/form";
    }
  }

  @PostMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public String deleteOrder(@PathVariable Long id, @RequestParam String password,
      RedirectAttributes redirectAttributes) {
    if (!passwordVerificationService.verifyCurrentUserPassword(password)) {
      redirectAttributes.addFlashAttribute("error", "Incorrect password. Order was not deleted.");
      return "redirect:/orders";
    }
    // Capture info before deletion for the audit log
    String capturedPoNo = null;
    String capturedClientName = null;
    Long capturedCompanyId = null;
    try {
      capturedCompanyId = securityContextHelper.getCompanyIdFromContext();
      Order existing = orderService.getOrderByIdAndCompanyId(id, capturedCompanyId);
      if (existing != null) {
        capturedPoNo = existing.getPoOrderNo();
        capturedClientName = existing.getCustomerName();
      }
    } catch (Exception e) {
      log.warn("Failed to capture pre-delete audit context. orderId={}, companyId={}", id,
          capturedCompanyId, e);
    }

    orderService.deleteOrder(id);

    // ── audit ──
    try {
      User actor = securityContextHelper.getUserFromContext();
      orderActivityService.logDeleted(id, capturedPoNo, capturedClientName, actor.getUsername(),
          actor.getFirstName() + " " + actor.getLastName(), capturedCompanyId);
    } catch (Exception e) {
      log.warn("Audit logging failed for order deletion. orderId={}, companyId={}", id,
          capturedCompanyId, e);
    }

    redirectAttributes.addFlashAttribute("message", "Order deleted successfully");
    return "redirect:/orders";
  }

  @GetMapping("/duplicate/{id}")
  public String duplicateOrder(@PathVariable Long id, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Order existingOrder = orderService.getOrderByIdAndCompanyId(id, companyId);
    if (existingOrder == null) {
      return "redirect:/orders";
    }

    Order newOrder = new Order();
    newOrder.setClient(existingOrder.getClient());
    newOrder.setProductName(existingOrder.getProductName());
    newOrder.setQuantity(existingOrder.getQuantity());
    newOrder.setTotalAmount(existingOrder.getTotalAmount());
    newOrder.setPoOrderNo(existingOrder.getPoOrderNo());
    newOrder.setOrderDate(LocalDate.now());
    newOrder.setDeliveryDate(existingOrder.getDeliveryDate());
    newOrder.setOrderNote(existingOrder.getOrderNote());
    newOrder.setStatus(OrderStatus.CREATED);

    // Copy order items (without order reference, will be re-linked on save)
    for (OrderItem existingItem : existingOrder.getOrderItems()) {
      OrderItem newItem = new OrderItem();
      newItem.setProduct(existingItem.getProduct());
      newItem.setProductName(existingItem.getProductName());
      newItem.setQuantity(existingItem.getQuantity());
      newItem.setUnitPrice(existingItem.getUnitPrice());
      newItem.setOrder(newOrder);
      newOrder.getOrderItems().add(newItem);
    }

    model.addAttribute("order", newOrder);
    model.addAttribute("clients", clientService.getClientsByCompanyId(companyId));
    model.addAttribute("products", productService.getProductsByCompanyId(companyId));
    model.addAttribute("statuses", Arrays.asList(OrderStatus.values()));
    return "orders/form";
  }

  /**
   * Builds OrderItem objects from form arrays and attaches them to the order. Resolves each product
   * by ID (if provided) and syncs productName and unitPrice from the Product entity.
   */
  private void buildOrderItems(Order order, List<Long> itemProductIds, List<Integer> itemQuantities,
      List<Double> itemUnitPrices) {
    order.getOrderItems().clear();

    if (itemProductIds == null || itemProductIds.isEmpty()) {
      return;
    }

    for (int i = 0; i < itemProductIds.size(); i++) {
      Long productId = itemProductIds.get(i);
      Integer qty =
          (itemQuantities != null && i < itemQuantities.size()) ? itemQuantities.get(i) : null;
      Double unitPrice =
          (itemUnitPrices != null && i < itemUnitPrices.size()) ? itemUnitPrices.get(i) : null;

      if (productId == null && (qty == null || qty <= 0)) {
        continue; // skip empty rows
      }

      if (qty == null || qty <= 0) {
        continue; // skip rows with no valid quantity
      }

      OrderItem item = new OrderItem();
      item.setOrder(order);

      if (productId != null && productId > 0) {
        Product product = productService.getProductById(productId);
        if (product != null) {
          item.setProduct(product);
          item.setProductName(product.getName());
          if (unitPrice == null) {
            unitPrice = product.getPrice();
          }
        }
      }

      item.setQuantity(qty);
      item.setUnitPrice(unitPrice);
      order.getOrderItems().add(item);
    }
  }

  /**
   * Creates a detached copy used for audit diffs so before/after comparisons are stable even when
   * JPA returns the same managed instance for subsequent reads in the same request.
   */
  private Order createAuditSnapshot(Order source) {
    Order copy = new Order();
    copy.setId(source.getId());
    copy.setClient(source.getClient());
    copy.setPoOrderNo(source.getPoOrderNo());
    copy.setProductName(source.getProductName());
    copy.setQuantity(source.getQuantity());
    copy.setTotalAmount(source.getTotalAmount());
    copy.setStatus(source.getStatus());
    copy.setOrderDate(source.getOrderDate());
    copy.setDeliveryDate(source.getDeliveryDate());
    copy.setOrderNote(source.getOrderNote());

    if (source.getOrderItems() != null) {
      for (OrderItem item : source.getOrderItems()) {
        OrderItem itemCopy = new OrderItem();
        itemCopy.setProduct(item.getProduct());
        itemCopy.setProductName(item.getProductName());
        itemCopy.setQuantity(item.getQuantity());
        itemCopy.setUnitPrice(item.getUnitPrice());
        itemCopy.setOrder(copy);
        copy.getOrderItems().add(itemCopy);
      }
    }

    return copy;
  }
}
