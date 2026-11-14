package com.example.ordermanager.vendor.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.repository.CompanyAddressRepository;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.repository.VendorPoDeliveryTermRepository;
import com.example.ordermanager.vendor.repository.VendorPoTermsTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VendorPoSettingsServiceTest {
  @Mock
  SecurityContextHelper securityContextHelper;
  @Mock
  CompanyAddressRepository addressRepository;
  @Mock
  CompanyRepository companyRepository;
  @Mock
  VendorPoTermsTemplateRepository termsRepository;
  @Mock
  VendorPoDeliveryTermRepository deliveryRepository;

  /** Confirms PO address choices are queried only for the authenticated Company. */
  @Test
  void activeAddressesUsesTenantContext() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(4L);
    VendorPoSettingsService service = service();

    service.getActiveAddresses();

    verify(addressRepository).findByCompany_IdAndActiveTrueOrderByIdAsc(4L);
  }

  /** Rejects template updates whose ID is owned by a different Company. */
  @Test
  void termsUpdateRejectsOtherCompanyTemplate() {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(4L);
    when(termsRepository.findByIdAndCompany_Id(77L, 4L)).thenReturn(Optional.empty());
    VendorPoSettingsService service = service();

    assertThrows(IllegalArgumentException.class, () -> service.saveTermsTemplate(77L, "Terms", "Line"));
  }

  /** Creates the tested settings service with isolated repository collaborators. */
  private VendorPoSettingsService service() {
    return new VendorPoSettingsService(securityContextHelper, addressRepository, companyRepository,
        termsRepository, deliveryRepository, new ObjectMapper());
  }
}
