package com.example.ordermanager.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.order.repository.OrderItemRepository;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.service.VendorService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

  @Mock
  private ProductService productService;
  @Mock
  private VendorService vendorService;
  @Mock
  private OrderItemRepository orderItemRepository;
  @Mock
  private SecurityContextHelper securityContextHelper;
  @Mock
  private PasswordVerificationService passwordVerificationService;

  private ProductController productController;

  @BeforeEach
  void setUp() {
    productController = new ProductController(productService, vendorService, orderItemRepository,
        securityContextHelper, passwordVerificationService);
  }

  @Test
  void listProducts_success_returnsProductListWithOrderUsageMap() {
    Product product = new Product("Steel Rod", null, null, "SAIL", "Raw Material", 50.0);
    product.setId(1L);
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);
    when(productService.getProductsByCompanyId(3L)).thenReturn(List.of(product));
    when(orderItemRepository.findByProductCompanyId(3L)).thenReturn(Collections.emptyList());

    Model model = new ConcurrentModel();
    String view = productController.listProducts(model);

    assertThat(view).isEqualTo("products/list");
    assertThat(model.getAttribute("products")).isEqualTo(List.of(product));
    assertThat(model.getAttribute("orderUsageMap")).isNotNull();
  }

  @Test
  void saveProduct_addSuccess_redirectsToProductListWithMessage() {
    Product product = new Product();
    product.setName("Steel Rod 12mm");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);

    String view = productController.saveProduct(product, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Product added successfully.");
    verify(productService).saveProductWithCompany(product, 3L);
  }

  @Test
  void saveProduct_updateSuccess_redirectsToProductListWithMessage() {
    Product product = new Product();
    product.setId(10L);
    product.setName("Steel Rod 16mm");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);

    String view = productController.saveProduct(product, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Product updated successfully.");
    verify(productService).saveProductWithCompany(product, 3L);
  }

  @Test
  void saveProduct_serviceError_returnsFormWithError() {
    Product product = new Product();
    product.setName("Bad Product");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);
    doThrow(new IllegalArgumentException("Company not found")).when(productService)
        .saveProductWithCompany(product, 3L);

    String view = productController.saveProduct(product, model, redirectAttributes);

    assertThat(view).isEqualTo("products/form");
    assertThat(model.getAttribute("error")).isEqualTo("Company not found");
    assertThat(model.getAttribute("product")).isEqualTo(product);
  }

  @Test
  void deleteProduct_success_redirectsToProductListWithMessage() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("correct")).thenReturn(true);

    String view = productController.deleteProduct(5L, "correct", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Product deleted successfully.");
    verify(productService).deleteProduct(5L);
  }

  @Test
  void deleteProduct_wrongPassword_doesNotDeleteAndReturnsError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("wrong")).thenReturn(false);

    String view = productController.deleteProduct(5L, "wrong", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("Incorrect password. Product was not deleted.");
  }

  @Test
  void deleteProduct_correctPasswordButServiceFails_redirectsWithError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("correct")).thenReturn(true);
    doThrow(new IllegalArgumentException("Product not found")).when(productService)
        .deleteProduct(99L);

    String view = productController.deleteProduct(99L, "correct", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("Error deleting product: Product not found");
    verify(productService).deleteProduct(99L);
  }
}
