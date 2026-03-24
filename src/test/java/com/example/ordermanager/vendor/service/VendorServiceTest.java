package com.example.ordermanager.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.Helper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.repository.VendorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

  @Mock
  private VendorRepository vendorRepository;
  @Mock
  private CompanyService companyService;

  private VendorService vendorService;

  @BeforeEach
  void setUp() {
    vendorService = new VendorService(vendorRepository, companyService, new Helper());
  }

  @Test
  void saveVendorWithCompany_normalizesPhoneAndFormatsNames() {
    Company company = new Company();
    company.setId(5L);

    Vendor vendor = new Vendor();
    vendor.setCompanyName("beta supply");
    vendor.setContactPerson("jane SMITH");
    vendor.setPhone("+1 (202) 555-0185");

    when(companyService.getCompanyById(5L)).thenReturn(Optional.of(company));
    when(vendorRepository.save(any(Vendor.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Vendor saved = vendorService.saveVendorWithCompany(vendor, 5L);

    assertThat(saved.getCompanyName()).isEqualTo("BETA SUPPLY");
    assertThat(saved.getContactPerson()).isEqualTo("Jane Smith");
    assertThat(saved.getPhone()).isEqualTo("12025550185");
    assertThat(saved.getCompany().getId()).isEqualTo(5L);
  }

  @Test
  void saveVendorWithCompany_rejectsInvalidInternationalPhone() {
    Company company = new Company();
    company.setId(5L);

    Vendor vendor = new Vendor();
    vendor.setCompanyName("beta supply");
    vendor.setPhone("abc");

    when(companyService.getCompanyById(5L)).thenReturn(Optional.of(company));

    assertThatThrownBy(() -> vendorService.saveVendorWithCompany(vendor, 5L))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
            "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210)");

    verify(vendorRepository, never()).save(any(Vendor.class));
  }
}

