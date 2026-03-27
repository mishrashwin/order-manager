package com.example.ordermanager.product.controller;

import com.example.ordermanager.order.repository.OrderItemRepository;
import com.example.ordermanager.product.dto.ProductOrderRef;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.service.VendorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

  private final ProductService productService;
  private final VendorService vendorService;
  private final OrderItemRepository orderItemRepository;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;

  public ProductController(ProductService productService, VendorService vendorService,
      OrderItemRepository orderItemRepository, SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService) {
    this.productService = productService;
    this.vendorService = vendorService;
    this.orderItemRepository = orderItemRepository;
    this.securityContextHelper = securityContextHelper;
    this.passwordVerificationService = passwordVerificationService;
  }

  @GetMapping
  public String listProducts(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    List<Product> products = productService.getProductsByCompanyId(companyId);

    // Build order usage map: productId -> list of order refs
    Map<Long, List<ProductOrderRef>> orderUsageMap = new HashMap<>();
    for (Product product : products) {
      List<ProductOrderRef> refs = orderItemRepository.findByProductId(product.getId()).stream()
          .map(item -> new ProductOrderRef(item.getOrder().getId(), item.getOrder().getPoOrderNo()))
          .distinct().collect(Collectors.toList());
      orderUsageMap.put(product.getId(), refs);
    }

    model.addAttribute("products", products);
    model.addAttribute("orderUsageMap", orderUsageMap);
    return "products/list";
  }

  @GetMapping("/new")
  public String newProductForm(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("product", new Product());
    model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
    return "products/form";
  }

  @PostMapping
  public String saveProduct(@ModelAttribute Product product, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      boolean isUpdate = product.getId() != null;
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      productService.saveProductWithCompany(product, companyId);
      redirectAttributes.addFlashAttribute("message",
          isUpdate ? "Product updated successfully." : "Product added successfully.");
      return "redirect:/products";
    } catch (IllegalArgumentException e) {
      model.addAttribute("error", e.getMessage());
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
      model.addAttribute("product", product);
      return "products/form";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving product: " + e.getMessage());
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
      model.addAttribute("product", product);
      return "products/form";
    }
  }

  @GetMapping("/edit/{id}")
  public String editProduct(@PathVariable Long id, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("product", productService.getProductById(id));
    model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
    return "products/form";
  }

  @PostMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public String deleteProduct(@PathVariable Long id, @RequestParam String password,
      RedirectAttributes redirectAttributes) {
    if (!passwordVerificationService.verifyCurrentUserPassword(password)) {
      redirectAttributes.addFlashAttribute("error", "Incorrect password. Product was not deleted.");
      return "redirect:/products";
    }
    try {
      productService.deleteProduct(id);
      redirectAttributes.addFlashAttribute("message", "Product deleted successfully.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error deleting product: " + e.getMessage());
    }
    return "redirect:/products";
  }
}
