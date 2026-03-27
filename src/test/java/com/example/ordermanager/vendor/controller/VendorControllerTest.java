package com.example.ordermanager.vendor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.service.VendorService;
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
class VendorControllerTest {

  @Mock
  private VendorService vendorService;
  @Mock
  private CompanyService companyService;
  @Mock
  private SecurityContextHelper securityContextHelper;
  @Mock
  private PasswordVerificationService passwordVerificationService;

  private VendorController vendorController;

  @BeforeEach
  void setUp() {
    vendorController = new VendorController(vendorService, companyService, securityContextHelper,
        passwordVerificationService);
  }

  @Test
  void saveVendor_addSuccess_redirectsToVendorListWithMessage() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Vendor added successfully.");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_updateSuccess_redirectsToVendorListWithMessage() {
    Vendor vendor = new Vendor();
    vendor.setId(11L);
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Vendor updated successfully.");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_phoneValidationFailure_returnsFormWithInlinePhoneError() {
    Vendor vendor = new Vendor();
    vendor.setPhone("+9777894561230");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    doThrow(new IllegalArgumentException(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210)."))
        .when(vendorService).saveVendorWithCompany(vendor, 7L);

    String view = vendorController.saveVendor(vendor, "/products/new", model, redirectAttributes);

    assertThat(view).isEqualTo("vendors/form");
    assertThat(model.getAttribute("error")).isEqualTo(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("phoneError")).isEqualTo(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("vendor")).isEqualTo(vendor);
    assertThat(model.getAttribute("returnTo")).isEqualTo("/products/new");
  }

  @Test
  void newVendorForm_withReturnTo_loadsFormWithReturnContext() {
    Model model = new ConcurrentModel();

    String view = vendorController.newVendorForm("/products/new?returnTo=%2Forders%2Fnew", model);

    assertThat(view).isEqualTo("vendors/form");
    assertThat(model.getAttribute("vendor")).isNotNull();
    assertThat(model.getAttribute("returnTo")).isEqualTo("/products/new?returnTo=/orders/new");
  }

  @Test
  void saveVendor_withAllowedReturnTo_redirectsToProductFlow() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, "/products/new?returnTo=%2Forders%2Fnew",
        model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products/new?returnTo=/orders/new");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_withDuplicateCommaJoinedReturnTo_redirectsToSingleProductPath() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor,
        "/products/new?returnTo=%2Forders%2Fnew,/products/new?returnTo=%2Forders%2Fnew", model,
        redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products/new?returnTo=/orders/new");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_withInvalidReturnTo_fallsBackToVendorsList() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, "https://evil.example/redirect", model,
        redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void deleteVendor_success_redirectsToVendorListWithMessage() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("correct")).thenReturn(true);

    String view = vendorController.deleteVendor(21L, "correct", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Vendor deleted successfully.");
    verify(vendorService).deleteVendor(21L);
  }

  @Test
  void deleteVendor_wrongPassword_doesNotDeleteAndReturnsError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("wrong")).thenReturn(false);

    String view = vendorController.deleteVendor(999L, "wrong", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("Incorrect password. Vendor was not deleted.");
  }

  @Test
  void deleteVendor_correctPasswordButServiceFails_redirectsWithError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("correct")).thenReturn(true);
    doThrow(new IllegalArgumentException("Vendor not found")).when(vendorService)
        .deleteVendor(999L);

    String view = vendorController.deleteVendor(999L, "correct", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("Error deleting vendor: Vendor not found");
    verify(vendorService).deleteVendor(999L);
  }
}

