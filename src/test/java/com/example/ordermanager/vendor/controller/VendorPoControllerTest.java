package com.example.ordermanager.vendor.controller;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.BrevoEmailService;
import com.example.ordermanager.user.service.UserService;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.service.VendorPoService;
import com.example.ordermanager.vendor.service.VendorService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorPoController.class)
class VendorPoControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private VendorPoService vendorPoService;

  @MockBean
  private BrevoEmailService brevoEmailService;

  @MockBean
  private UserService userService;

  @MockBean
  private CompanyService companyService;

  @MockBean
  private VendorService vendorService;

  @MockBean
  private ProductService productService;

  private VendorPo testVendorPo;
  private Vendor testVendor;
  private Product testProduct;
  private Company testCompany;
  private User testUser;

  @BeforeEach
  void setUp() {
    testCompany = new Company();
    testCompany.setId(1L);
    testCompany.setName("Test Company");

    testUser = new User();
    testUser.setId(1L);
    testUser.setEmail("admin@test.com");

    testVendor = new Vendor();
    testVendor.setId(1L);
    testVendor.setCompanyName("Test Vendor");
    testVendor.setEmail("vendor@test.com");
    testVendor.setGstn("GSTN123");

    testProduct = new Product();
    testProduct.setId(1L);
    testProduct.setName("Test Product");
    testProduct.setHsnCode("HSN123");
    testProduct.setUnit("PCS");

    testVendorPo = new VendorPo();
    testVendorPo.setId(1L);
    testVendorPo.setPoNumber("PO-001");
    testVendorPo.setVendor(testVendor);
    testVendorPo.setCompany(testCompany);
    testVendorPo.setDeliveryDate(LocalDate.now().plusWeeks(2));
    testVendorPo.setTotalAmount(1000.00);
    testVendorPo.setEmails("vendor1@test.com, vendor2@test.com");
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void list_shouldReturnVendorPoListPage() throws Exception {
    Page<VendorPo> vendorPoPage = new PageImpl<>(Arrays.asList(testVendorPo));
    when(vendorPoService.listVendorPos(any(), any(), any(PageRequest.class)))
        .thenReturn(vendorPoPage);

    mockMvc.perform(get("/vendor/pos/list-ajax")).andExpect(status().isOk())
        .andExpect(view().name("vendor/pos-list-ajax :: pos-table"))
        .andExpect(model().attributeExists("pos"));

    verify(vendorPoService).listVendorPos(any(), any(), any(PageRequest.class));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void createForm_shouldReturnFormPageWithVendorsAndProducts() throws Exception {
    when(vendorService.getVendorsByCompanyId(1L)).thenReturn(Arrays.asList(testVendor));
    when(productService.getProductsByCompanyId(1L)).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(get("/vendor/pos/new"))
        .andExpect(status().isOk())
        .andExpect(view().name("vendor/pos-form-new"))
        .andExpect(model().attributeExists("vendorPo"))
        .andExpect(model().attributeExists("vendors"))
        .andExpect(model().attributeExists("products"));

    verify(vendorService).getVendorsByCompanyId(1L);
    verify(productService).getProductsByCompanyId(1L);
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void create_shouldSaveVendorPoAndRedirect() throws Exception {
    when(vendorPoService.createVendorPo(any(VendorPo.class))).thenReturn(testVendorPo);

    mockMvc.perform(post("/vendor/pos")
        .with(csrf())
        .param("poNumber", "PO-001")
        .param("deliveryDate", LocalDate.now().plusWeeks(2).toString())
        .param("vendor.id", "1")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/vendors#vendor-po"))
        .andExpect(flash().attributeExists("message"));

    verify(vendorPoService).createVendorPo(any(VendorPo.class));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void create_withSendEmail_shouldSendEmailAndRedirect() throws Exception {
    when(vendorPoService.createVendorPo(any(VendorPo.class))).thenReturn(testVendorPo);
    when(vendorPoService.generatePdf(1L)).thenReturn(new byte[]{1, 2, 3});
    when(userService.findPrimaryAdminByCompanyId(1L)).thenReturn(testUser);
    when(companyService.getCompanyById(1L)).thenReturn(Optional.ofNullable(testCompany));

    mockMvc.perform(post("/vendor/pos")
        .with(csrf())
        .param("poNumber", "PO-001")
        .param("deliveryDate", LocalDate.now().plusWeeks(2).toString())
        .param("vendor.id", "1")
        .param("emails", "vendor1@test.com, vendor2@test.com")
        .param("send", "true")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/vendors#vendor-po"))
        .andExpect(flash().attributeExists("message"));

    verify(vendorPoService).createVendorPo(any(VendorPo.class));
    verify(vendorPoService).generatePdf(1L);
    verify(brevoEmailService).sendEmailWithMultipleRecipients(
        any(String[].class), any(String[].class), any(String.class), any(String.class),
        any(byte[].class), any(String.class), any(String.class));
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void create_withSendEmailFailure_shouldSaveButShowError() throws Exception {
    when(vendorPoService.createVendorPo(any(VendorPo.class))).thenReturn(testVendorPo);
    when(vendorPoService.generatePdf(1L)).thenThrow(new RuntimeException("PDF generation failed"));

    mockMvc.perform(post("/vendor/pos")
        .with(csrf())
        .param("poNumber", "PO-001")
        .param("deliveryDate", LocalDate.now().plusWeeks(2).toString())
        .param("vendor.id", "1")
        .param("send", "true")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/vendors#vendor-po"))
        .andExpect(flash().attributeExists("message"))
        .andExpect(flash().attributeExists("error"));

    verify(vendorPoService).createVendorPo(any(VendorPo.class));
    verify(vendorPoService).generatePdf(1L);
    verify(brevoEmailService, never()).sendEmailWithMultipleRecipients(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void editForm_shouldReturnEditPageWithVendorsAndProducts() throws Exception {
    when(vendorPoService.getVendorPoById(1L)).thenReturn(testVendorPo);
    when(vendorService.getVendorsByCompanyId(1L)).thenReturn(Arrays.asList(testVendor));
    when(productService.getProductsByCompanyId(1L)).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(get("/vendor/pos/1/edit"))
        .andExpect(status().isOk())
        .andExpect(view().name("vendor/pos-form-new"))
        .andExpect(model().attributeExists("vendorPo"))
        .andExpect(model().attribute("vendorPo", testVendorPo));

    verify(vendorPoService).getVendorPoById(1L);
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void downloadPdf_shouldReturnPdfFile() throws Exception {
    byte[] pdfBytes = {1, 2, 3, 4, 5};
    when(vendorPoService.generatePdf(1L)).thenReturn(pdfBytes);

    mockMvc.perform(get("/vendor/pos/1/download")).andExpect(status().isOk())
        .andExpect(header().exists("Content-Disposition"))
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(content().bytes(pdfBytes));

    verify(vendorPoService).generatePdf(1L);
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void create_withInvalidData_shouldReturnFormWithErrors() throws Exception {
    when(vendorService.getVendorsByCompanyId(1L)).thenReturn(Arrays.asList(testVendor));
    when(productService.getProductsByCompanyId(1L)).thenReturn(Arrays.asList(testProduct));

    mockMvc.perform(post("/vendor/pos")
        .with(csrf())
        .param("poNumber", "")  // Invalid: empty
        .param("deliveryDate", LocalDate.now().plusWeeks(2).toString())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk())
        .andExpect(view().name("vendor/pos-form-new"))
        .andExpect(model().attributeExists("vendorPo"))
        .andExpect(model().attributeExists("vendors"))
        .andExpect(model().attributeExists("products"));

    verify(vendorPoService, never()).createVendorPo(any());
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void create_withoutVendorEmail_shouldUseEmailsField() throws Exception {
    Vendor vendorWithoutEmail = new Vendor();
    vendorWithoutEmail.setId(1L);
    vendorWithoutEmail.setCompanyName("Test Vendor");
    // No email set

    testVendorPo.setVendor(vendorWithoutEmail);

    when(vendorPoService.createVendorPo(any(VendorPo.class))).thenReturn(testVendorPo);
    when(vendorPoService.generatePdf(1L)).thenReturn(new byte[] {1, 2, 3});
    when(userService.findPrimaryAdminByCompanyId(1L)).thenReturn(testUser);
    when(companyService.getCompanyById(1L)).thenReturn(Optional.ofNullable(testCompany));

    mockMvc
        .perform(post("/vendor/pos").with(csrf()).param("poNumber", "PO-001")
            .param("deliveryDate", LocalDate.now().plusWeeks(2).toString()).param("vendor.id", "1")
            .param("emails", "backup@test.com").param("send", "true")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/vendors#vendor-po"));

    verify(brevoEmailService).sendEmailWithMultipleRecipients(eq(new String[] {"backup@test.com"}),
        any(String[].class), any(String.class), any(String.class), any(byte[].class),
        any(String.class), any(String.class));
  }

  @Test
  void list_withoutAuthentication_shouldRedirectToLogin() throws Exception {
    mockMvc.perform(get("/vendor/pos/list-ajax")).andExpect(status().is3xxRedirection());
  }

  @Test
  @WithMockUser(username = "test@example.com", roles = {"USER"})
  void create_withCsrfTokenMissing_shouldReturnForbidden() throws Exception {
    mockMvc.perform(post("/vendor/pos").param("poNumber", "PO-001")
        .param("deliveryDate", LocalDate.now().plusWeeks(2).toString()).param("vendor.id", "1")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)).andExpect(status().isForbidden());

    verify(vendorPoService, never()).createVendorPo(any());
  }
}


