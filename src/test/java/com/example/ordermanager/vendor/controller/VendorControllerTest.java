package com.example.ordermanager.vendor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.service.CompanyService;
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

@ExtendWith(MockitoExtension.class)
class VendorControllerTest {

  @Mock
  private VendorService vendorService;
  @Mock
  private CompanyService companyService;
  @Mock
  private SecurityContextHelper securityContextHelper;

  private VendorController vendorController;

  @BeforeEach
  void setUp() {
    vendorController = new VendorController(vendorService, companyService, securityContextHelper);
  }

  @Test
  void saveVendor_success_redirectsToVendorList() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, model);

    assertThat(view).isEqualTo("redirect:/vendors");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_phoneValidationFailure_returnsFormWithInlinePhoneError() {
    Vendor vendor = new Vendor();
    vendor.setPhone("+9777894561230");
    Model model = new ConcurrentModel();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    doThrow(new IllegalArgumentException(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210)."))
        .when(vendorService).saveVendorWithCompany(vendor, 7L);

    String view = vendorController.saveVendor(vendor, model);

    assertThat(view).isEqualTo("vendors/form");
    assertThat(model.getAttribute("error")).isEqualTo(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("phoneError")).isEqualTo(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("vendor")).isEqualTo(vendor);
  }
}

