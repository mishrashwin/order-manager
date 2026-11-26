package com.example.ordermanager.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.dto.CompanyAddressForm;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyAddress;
import com.example.ordermanager.company.repository.CompanyAddressRepository;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.utils.IndianState;
import com.example.ordermanager.utils.SecurityContextHelper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyAddressServiceTest {

  private static final ValidatorFactory VALIDATOR_FACTORY =
      Validation.buildDefaultValidatorFactory();

  @Mock
  private CompanyAddressRepository addressRepository;
  @Mock
  private CompanyRepository companyRepository;
  @Mock
  private SecurityContextHelper securityContextHelper;

  private CompanyAddressService service;
  private Company company;

  /** Creates the service and an authenticated Company fixture for each test. */
  @BeforeEach
  void setUp() {
    service = new CompanyAddressService(addressRepository, companyRepository, securityContextHelper,
        VALIDATOR_FACTORY.getValidator());
    company = new Company("Tenant A");
    company.setId(10L);
    lenient().when(securityContextHelper.getCompanyIdFromContext()).thenReturn(10L);
  }

  /** Lists tenant addresses without exposing records from other Companies. */
  @Test
  void listAddresses_usesAuthenticatedCompanyId() {
    CompanyAddress ownAddress = activeAddress(22L);
    when(addressRepository.findByCompany_IdOrderByIdAsc(10L))
        .thenReturn(java.util.List.of(ownAddress));

    assertThat(service.listAddresses()).containsExactly(ownAddress);

    verify(addressRepository).findByCompany_IdOrderByIdAsc(10L);
  }

  /** Returns an address that is present in the authenticated Company's scoped query. */
  @Test
  void getAddress_returnsCompanyOwnedAddress() {
    CompanyAddress ownAddress = activeAddress(22L);
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(ownAddress));

    assertThat(service.getAddress(22L)).isSameAs(ownAddress);
  }

  /** Closes the validator resources shared by this test class. */
  @AfterAll
  static void closeValidatorFactory() {
    VALIDATOR_FACTORY.close();
  }

  /** Creates a tenant-owned address and assigns both missing default roles. */
  @Test
  void createAddress_assignsMissingDefaultsAndCompanyOwnership() {
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultBillToTrue(10L))
        .thenReturn(false);
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultShipToTrue(10L))
        .thenReturn(false);
    when(addressRepository.save(any(CompanyAddress.class))).thenAnswer(invocation -> {
      CompanyAddress saved = invocation.getArgument(0);
      saved.setId(20L);
      return saved;
    });

    CompanyAddress result = service.createAddress(validForm());

    assertThat(result.getCompany()).isSameAs(company);
    assertThat(result.getNickname()).isEqualTo("Warehouse");
    assertThat(result.isDefaultBillTo()).isTrue();
    assertThat(result.isDefaultShipTo()).isTrue();
    assertThat(result.getStateCode()).isEqualTo("27");
    assertThat(result.getDrugLicenseNumber()).isEqualTo("DL-TEST-1");
  }

  /** Allows the same GSTIN to be stored on separate addresses in the same Company. */
  @Test
  void createAddress_allowsDuplicateGstinValues() {
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultBillToTrue(10L))
        .thenReturn(false, true);
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultShipToTrue(10L))
        .thenReturn(false, true);
    when(addressRepository.save(any(CompanyAddress.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CompanyAddressForm firstForm = validForm();
    firstForm.setGstin("27ABCDE1234F1Z5");
    CompanyAddressForm secondForm = validForm();
    secondForm.setNickname("Second Warehouse");
    secondForm.setGstin("27ABCDE1234F1Z5");

    service.createAddress(firstForm);
    CompanyAddress second = service.createAddress(secondForm);

    assertThat(second.getGstin()).isEqualTo("27ABCDE1234F1Z5");
    assertThat(second.isDefaultBillTo()).isFalse();
    assertThat(second.isDefaultShipTo()).isFalse();
    verify(addressRepository, org.mockito.Mockito.times(2)).save(any(CompanyAddress.class));
  }

  /** Changes address fields without replacing the stored Company relationship. */
  @Test
  void updateAddress_preservesCompanyOwnership() {
    CompanyAddress existing = activeAddress(22L);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(existing));
    when(addressRepository.save(existing)).thenReturn(existing);

    CompanyAddress updated = service.updateAddress(22L, validForm());

    assertThat(updated.getCompany()).isSameAs(company);
    assertThat(updated.getNickname()).isEqualTo("Warehouse");
  }

  /** Rejects edits to deactivated addresses so inactive records remain unchanged. */
  @Test
  void updateAddress_rejectsInactiveAddress() {
    CompanyAddress inactive = activeAddress(22L);
    inactive.setActive(false);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(inactive));

    assertThatThrownBy(() -> service.updateAddress(22L, validForm()))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("Inactive");

    verify(addressRepository, never()).save(any(CompanyAddress.class));
  }

  /** Moves the Bill To role while leaving the Ship To role available on the same address. */
  @Test
  void setDefaultBillTo_keepsAddressEligibleForBothRoles() {
    CompanyAddress target = activeAddress(22L);
    target.setDefaultShipTo(true);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));
    when(addressRepository.save(target)).thenReturn(target);

    service.setDefaultBillTo(22L);

    assertThat(target.isDefaultBillTo()).isTrue();
    assertThat(target.isDefaultShipTo()).isTrue();
    verify(addressRepository).clearBillToDefaultForCompany(10L);
  }

  /** Moves the Ship To role to an active address in the authenticated Company. */
  @Test
  void setDefaultShipTo_updatesOnlyShipToRole() {
    CompanyAddress target = activeAddress(22L);
    target.setDefaultBillTo(true);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));
    when(addressRepository.save(target)).thenReturn(target);

    CompanyAddress result = service.setDefaultShipTo(22L);

    assertThat(result.isDefaultBillTo()).isTrue();
    assertThat(result.isDefaultShipTo()).isTrue();
    verify(addressRepository).clearShipToDefaultForCompany(10L);
  }

  /** Deactivates non-default addresses while retaining their database record. */
  @Test
  void deactivateAddress_marksNonDefaultInactive() {
    CompanyAddress target = activeAddress(22L);
    target.setGstin("27ABCDE1234F1Z5");
    target.setDrugLicenseNumber("DL-22");
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultBillToTrue(10L))
        .thenReturn(true);
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultShipToTrue(10L))
        .thenReturn(true);
    when(addressRepository.save(target)).thenReturn(target);

    CompanyAddress result = service.deactivateAddress(22L);

    assertThat(result.isActive()).isFalse();
    assertThat(result.getGstin()).isEqualTo("27ABCDE1234F1Z5");
    assertThat(result.getDrugLicenseNumber()).isEqualTo("DL-22");
    verify(addressRepository, never()).delete(any(CompanyAddress.class));
  }

  /** Reactivates the same address while preserving its data and unassigned default roles. */
  @Test
  void activateAddress_preservesFieldsWithoutAssigningDefaults() {
    CompanyAddress target = activeAddress(22L);
    target.setActive(false);
    target.setGstin("27ABCDE1234F1Z5");
    target.setDrugLicenseNumber("DL-123");
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));
    when(addressRepository.save(target)).thenReturn(target);

    CompanyAddress result = service.activateAddress(22L);

    assertThat(result.isActive()).isTrue();
    assertThat(result.getGstin()).isEqualTo("27ABCDE1234F1Z5");
    assertThat(result.getDrugLicenseNumber()).isEqualTo("DL-123");
    assertThat(result.isDefaultBillTo()).isFalse();
    assertThat(result.isDefaultShipTo()).isFalse();
  }

  /** Returns only the active Bill To default from the authenticated Company's scoped query. */
  @Test
  void getDefaultBillToAddress_usesTenantAndActiveDefaultQuery() {
    CompanyAddress target = activeAddress(22L);
    target.setDefaultBillTo(true);
    when(addressRepository.findByCompany_IdAndActiveTrueAndDefaultBillToTrue(10L))
        .thenReturn(java.util.Optional.of(target));

    assertThat(service.getDefaultBillToAddress()).containsSame(target);
    verify(addressRepository).findByCompany_IdAndActiveTrueAndDefaultBillToTrue(10L);
  }

  /** Requires both roles to move before a dual-default address can be deactivated. */
  @Test
  void deactivateAddress_rejectsDualDefaultUntilBothRolesAreMoved() {
    CompanyAddress target = activeAddress(22L);
    target.setDefaultBillTo(true);
    target.setDefaultShipTo(true);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));

    assertThatThrownBy(() -> service.deactivateAddress(22L))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("replacement default");
    assertThat(target.isActive()).isTrue();
  }

  /** Rejects default removal until the corresponding role has been reassigned. */
  @Test
  void deactivateAddress_rejectsCurrentDefault() {
    CompanyAddress target = activeAddress(22L);
    target.setDefaultBillTo(true);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));

    assertThatThrownBy(() -> service.deactivateAddress(22L))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("replacement default");

    verify(addressRepository, never()).save(any(CompanyAddress.class));
  }

  /** Preserves the default invariant if a Company already lacks one role in its stored state. */
  @Test
  void deactivateAddress_rejectsWhenEitherActiveDefaultIsMissing() {
    CompanyAddress target = activeAddress(22L);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultBillToTrue(10L))
        .thenReturn(true);
    when(addressRepository.existsByCompany_IdAndActiveTrueAndDefaultShipToTrue(10L))
        .thenReturn(false);

    assertThatThrownBy(() -> service.deactivateAddress(22L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("active Bill To and Ship To");
    verify(addressRepository, never()).save(any(CompanyAddress.class));
  }

  /** Rejects inactive addresses as defaults before clearing the existing role. */
  @Test
  void setDefaultBillTo_rejectsInactiveAddress() {
    CompanyAddress target = activeAddress(22L);
    target.setActive(false);
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(22L, 10L))
        .thenReturn(java.util.Optional.of(target));

    assertThatThrownBy(() -> service.setDefaultBillTo(22L))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("Inactive");

    verify(addressRepository, never()).clearBillToDefaultForCompany(10L);
  }

  /** Hides Company B addresses from Company A read, edit, default, and deactivate operations. */
  @Test
  void addressOperations_rejectAddressOutsideAuthenticatedCompany() {
    when(companyRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(company));
    when(addressRepository.findByIdAndCompany_Id(99L, 10L)).thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> service.getAddress(99L)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.updateAddress(99L, validForm()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.setDefaultShipTo(99L))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.deactivateAddress(99L))
        .isInstanceOf(IllegalArgumentException.class);

    verify(addressRepository, never()).save(any(CompanyAddress.class));
  }

  /** Rejects missing required fields even when a caller bypasses MVC binding validation. */
  @Test
  void createAddress_rejectsInvalidFormAtServiceBoundary() {
    CompanyAddressForm invalid = new CompanyAddressForm();

    assertThatThrownBy(() -> service.createAddress(invalid))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Address nickname is required");

    verify(companyRepository, never()).findByIdForUpdate(10L);
  }

  /** Supplies a valid address form for service test cases. */
  private CompanyAddressForm validForm() {
    CompanyAddressForm form = new CompanyAddressForm();
    form.setNickname("Warehouse");
    form.setAddressLine1("12 Market Road");
    form.setCity("Mumbai");
    form.setState(IndianState.MAHARASHTRA);
    form.setPostalCode("400001");
    form.setCountry("India");
    form.setDrugLicenseNumber("DL-TEST-1");
    return form;
  }

  /** Builds an active Company-owned test address with the supplied identifier. */
  private CompanyAddress activeAddress(Long id) {
    CompanyAddress address = new CompanyAddress();
    address.setId(id);
    address.setCompany(company);
    address.setNickname("Existing");
    address.setAddressLine1("1 Road");
    address.setActive(true);
    return address;
  }
}
