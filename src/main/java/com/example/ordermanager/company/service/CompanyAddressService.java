package com.example.ordermanager.company.service;

import com.example.ordermanager.company.dto.CompanyAddressForm;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyAddress;
import com.example.ordermanager.company.repository.CompanyAddressRepository;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.utils.SecurityContextHelper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enforces tenant ownership and active default rules for Company Address operations. */
@Service
public class CompanyAddressService {

  private final CompanyAddressRepository addressRepository;
  private final CompanyRepository companyRepository;
  private final SecurityContextHelper securityContextHelper;
  private final Validator validator;

  /** Wires scoped repositories, tenant context, and service-level form validation. */
  public CompanyAddressService(CompanyAddressRepository addressRepository,
      CompanyRepository companyRepository, SecurityContextHelper securityContextHelper,
      Validator validator) {
    this.addressRepository = addressRepository;
    this.companyRepository = companyRepository;
    this.securityContextHelper = securityContextHelper;
    this.validator = validator;
  }

  /** Lists all active and inactive addresses belonging to the authenticated Company. */
  @Transactional(readOnly = true)
  public List<CompanyAddress> listAddresses() {
    return addressRepository
        .findByCompany_IdOrderByIdAsc(securityContextHelper.getCompanyIdFromContext());
  }

  /** Returns only the authenticated Company's active Bill To address, if configured. */
  @Transactional(readOnly = true)
  public Optional<CompanyAddress> getDefaultBillToAddress() {
    return addressRepository.findByCompany_IdAndActiveTrueAndDefaultBillToTrue(
        securityContextHelper.getCompanyIdFromContext());
  }

  /** Loads an address only when it belongs to the authenticated Company. */
  @Transactional(readOnly = true)
  public CompanyAddress getAddress(Long addressId) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return addressRepository.findByIdAndCompany_Id(addressId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company address not found"));
  }

  /** Creates an active Company Address and assigns any default role the Company lacks. */
  @Transactional
  public CompanyAddress createAddress(CompanyAddressForm form) {
    validateForm(form);
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = lockCompany(companyId);
    CompanyAddress address = new CompanyAddress();
    address.setCompany(company);
    applyForm(address, form);
    address.setActive(true);
    address.setDefaultBillTo(
        !addressRepository.existsByCompany_IdAndActiveTrueAndDefaultBillToTrue(companyId));
    address.setDefaultShipTo(
        !addressRepository.existsByCompany_IdAndActiveTrueAndDefaultShipToTrue(companyId));
    return addressRepository.save(address);
  }

  /** Updates address details while retaining the persisted Company and lifecycle flags. */
  @Transactional
  public CompanyAddress updateAddress(Long addressId, CompanyAddressForm form) {
    validateForm(form);
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    lockCompany(companyId);
    CompanyAddress address = addressRepository.findByIdAndCompany_Id(addressId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company address not found"));
    if (!address.isActive()) {
      throw new IllegalStateException("Inactive Company addresses cannot be edited");
    }
    applyForm(address, form);
    return addressRepository.save(address);
  }

  /** Moves the active default Bill To role to an address owned by the current Company. */
  @Transactional
  public CompanyAddress setDefaultBillTo(Long addressId) {
    return setDefault(addressId, true);
  }

  /** Moves the active default Ship To role to an address owned by the current Company. */
  @Transactional
  public CompanyAddress setDefaultShipTo(Long addressId) {
    return setDefault(addressId, false);
  }

  /** Deactivates a non-default address without deleting its historical record. */
  @Transactional
  public CompanyAddress deactivateAddress(Long addressId) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    lockCompany(companyId);
    CompanyAddress address = addressRepository.findByIdAndCompany_Id(addressId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company address not found"));
    if (address.isDefaultBillTo() || address.isDefaultShipTo()) {
      throw new IllegalStateException(
          "Choose replacement default address roles before deactivating this address");
    }
    if (!addressRepository.existsByCompany_IdAndActiveTrueAndDefaultBillToTrue(companyId)
        || !addressRepository.existsByCompany_IdAndActiveTrueAndDefaultShipToTrue(companyId)) {
      throw new IllegalStateException(
          "Company must have active Bill To and Ship To defaults before deactivation");
    }
    if (address.isActive()) {
      address.setActive(false);
      return addressRepository.save(address);
    }
    return address;
  }

  /** Reactivates an address while preserving its data and without assigning default roles. */
  @Transactional
  public CompanyAddress activateAddress(Long addressId) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    lockCompany(companyId);
    CompanyAddress address = addressRepository.findByIdAndCompany_Id(addressId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company address not found"));
    if (!address.isActive()) {
      address.setActive(true);
      return addressRepository.save(address);
    }
    return address;
  }

  /** Serializes default and lifecycle mutations by taking a write lock on the Company row. */
  private Company lockCompany(Long companyId) {
    return companyRepository.findByIdForUpdate(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
  }

  /** Replaces the requested default role after validating tenant ownership and active state. */
  private CompanyAddress setDefault(Long addressId, boolean billTo) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    lockCompany(companyId);
    CompanyAddress target = addressRepository.findByIdAndCompany_Id(addressId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Company address not found"));
    if (!target.isActive()) {
      throw new IllegalStateException("Inactive Company addresses cannot be made default");
    }

    if (billTo) {
      addressRepository.clearBillToDefaultForCompany(companyId);
      target.setDefaultBillTo(true);
    } else {
      addressRepository.clearShipToDefaultForCompany(companyId);
      target.setDefaultShipTo(true);
    }
    return addressRepository.save(target);
  }

  /** Copies validated form fields and trims optional text before persistence. */
  private void applyForm(CompanyAddress address, CompanyAddressForm form) {
    address.setNickname(form.getNickname().trim());
    address.setAddressLine1(form.getAddressLine1().trim());
    address.setAddressLine2(trimToNull(form.getAddressLine2()));
    address.setCity(form.getCity().trim());
    address.setState(form.getState());
    address.setPostalCode(form.getPostalCode().trim());
    address.setCountry(form.getCountry().trim());
    address.setGstin(trimToNull(form.getGstin()));
    address.setDrugLicenseNumber(trimToNull(form.getDrugLicenseNumber()));
    address.setContactPersonName(trimToNull(form.getContactPersonName()));
    address.setContactNumber(trimToNull(form.getContactNumber()));
    address.setContactEmail(trimToNull(form.getContactEmail()));
  }

  /** Trims optional values and converts empty text to null. */
  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Applies bean validation in the service layer so non-MVC callers cannot bypass field rules. */
  private void validateForm(CompanyAddressForm form) {
    if (form == null) {
      throw new IllegalArgumentException("Company address details are required");
    }
    String errors = validator.validate(form).stream().map(ConstraintViolation::getMessage)
        .distinct().reduce((first, next) -> first + "; " + next).orElse(null);
    if (errors != null) {
      throw new IllegalArgumentException(errors);
    }
  }
}
