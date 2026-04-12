package com.example.ordermanager.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.order.entity.Order;
import com.example.ordermanager.order.entity.OrderItem;
import com.example.ordermanager.order.repository.OrderItemRepository;
import com.example.ordermanager.product.dto.ProductOrderRef;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.service.VendorService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
  void listProducts_keepsOnlyLatestThreeUniqueOrderRefsPerProduct() {
    Product product = new Product("Steel Rod", null, null, "SAIL", "Raw Material", 50.0);
    product.setId(1L);

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);
    when(productService.getProductsByCompanyId(3L)).thenReturn(List.of(product));
    when(orderItemRepository.findByProductCompanyId(3L))
        .thenReturn(List.of(orderItem(product, 10L, "PO-010"), orderItem(product, 7L, "PO-007"),
            orderItem(product, 9L, "PO-009"), orderItem(product, 8L, "PO-008"),
            orderItem(product, 9L, "PO-009")));

    Model model = new ConcurrentModel();
    productController.listProducts(model);

    @SuppressWarnings("unchecked")
    Map<Long, List<ProductOrderRef>> orderUsageMap =
        (Map<Long, List<ProductOrderRef>>) model.getAttribute("orderUsageMap");

    assertThat(orderUsageMap).containsKey(1L);
    assertThat(orderUsageMap.get(1L)).extracting(ProductOrderRef::getOrderId).containsExactly(10L,
        9L, 8L);
  }

  @Test
  void saveProduct_addSuccess_redirectsToProductListWithMessage() {
    Product product = new Product();
    product.setName("Steel Rod 12mm");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);

    String view = productController.saveProduct(product, null, model, redirectAttributes);

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

    String view = productController.saveProduct(product, null, model, redirectAttributes);

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

    String view = productController.saveProduct(product, "/orders/new", model, redirectAttributes);

    assertThat(view).isEqualTo("products/form");
    assertThat(model.getAttribute("error")).isEqualTo("Company not found");
    assertThat(model.getAttribute("product")).isEqualTo(product);
    assertThat(model.getAttribute("returnTo")).isEqualTo("/orders/new");
  }

  @Test
  void newProductForm_withReturnTo_loadsFormWithReturnContext() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);
    when(vendorService.getVendorsByCompanyId(3L)).thenReturn(List.of(new Vendor()));

    Model model = new ConcurrentModel();
    String view = productController.newProductForm("/orders/new", model);

    assertThat(view).isEqualTo("products/form");
    assertThat(model.getAttribute("returnTo")).isEqualTo("/orders/new");
    assertThat(model.getAttribute("product")).isNotNull();
  }

  @Test
  void saveProduct_withAllowedReturnTo_redirectsToProvidedPath() {
    Product product = new Product();
    product.setName("Steel Rod 12mm");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);

    String view = productController.saveProduct(product, "/orders/new", model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders/new");
    verify(productService).saveProductWithCompany(product, 3L);
  }

  @Test
  void saveProduct_withEncodedReturnTo_redirectsToDecodedOrderPath() {
    Product product = new Product();
    product.setName("Steel Rod 12mm");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);

    String view =
        productController.saveProduct(product, "%2Forders%2Fedit%2F42", model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders/edit/42");
    verify(productService).saveProductWithCompany(product, 3L);
  }

  @Test
  void saveProduct_withDuplicateCommaJoinedReturnTo_redirectsToSingleOrderPath() {
    Product product = new Product();
    product.setName("Steel Rod 12mm");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);

    String view = productController.saveProduct(product, "/orders/new,/orders/new", model,
        redirectAttributes);

    assertThat(view).isEqualTo("redirect:/orders/new");
    verify(productService).saveProductWithCompany(product, 3L);
  }

  @Test
  void saveProduct_withInvalidReturnTo_fallsBackToProductList() {
    Product product = new Product();
    product.setName("Steel Rod 12mm");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(3L);

    String view = productController.saveProduct(product, "https://evil.example/redirect", model,
        redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products");
    verify(productService).saveProductWithCompany(product, 3L);
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

  private OrderItem orderItem(Product product, Long orderId, String poNo) {
    Order order = new Order();
    order.setId(orderId);
    order.setPoOrderNo(poNo);

    OrderItem item = new OrderItem();
    item.setOrder(order);
    item.setProduct(product);
    return item;
  }
}
