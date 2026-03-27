package com.example.ordermanager.order.controller;

import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderItem;
import com.example.ordermanager.order.entity.OrderStatus;
import com.example.ordermanager.client.service.ClientService;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.order.service.OrderService;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.utils.SecurityContextHelper;
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

  private final OrderService orderService;
  private final ClientService clientService;
  private final CompanyService companyService;
  private final ProductService productService;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;

  public OrderController(OrderService orderService, ClientService clientService,
      CompanyService companyService, ProductService productService,
      SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService) {
    this.orderService = orderService;
    this.clientService = clientService;
    this.companyService = companyService;
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
      buildOrderItems(order, itemProductIds, itemQuantities, itemUnitPrices, companyId);
      orderService.createOrderWithCompany(order, companyId);
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
    Order order = orderService.getOrderById(id);
    if (order == null) {
      return "redirect:/orders";
    }
    Long companyId = securityContextHelper.getCompanyIdFromContext();
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
      buildOrderItems(updatedOrder, itemProductIds, itemQuantities, itemUnitPrices, companyId);
      orderService.patchOrder(id, updatedOrder);
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
    orderService.deleteOrder(id);
    redirectAttributes.addFlashAttribute("message", "Order deleted successfully");
    return "redirect:/orders";
  }

  @GetMapping("/duplicate/{id}")
  public String duplicateOrder(@PathVariable Long id, Model model) {
    Order existingOrder = orderService.getOrderById(id);
    if (existingOrder == null) {
      return "redirect:/orders";
    }

    Order newOrder = new Order();
    newOrder.setCustomerName(existingOrder.getCustomerName());
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

    Long companyId = securityContextHelper.getCompanyIdFromContext();
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
      List<Double> itemUnitPrices, Long companyId) {
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
}
