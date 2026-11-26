package com.example.ordermanager.vendor.service;

import com.example.ordermanager.company.entity.CompanyAddress;
import com.example.ordermanager.company.repository.CompanyAddressRepository;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.VendorPoDeliveryTerm;
import com.example.ordermanager.vendor.entity.VendorPoTermsTemplate;
import com.example.ordermanager.vendor.repository.VendorPoDeliveryTermRepository;
import com.example.ordermanager.vendor.repository.VendorPoTermsTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides tenant-scoped PO form choices and manages reusable terms settings. */
@Service
public class VendorPoSettingsService {

  private final SecurityContextHelper securityContextHelper;
  private final CompanyAddressRepository addressRepository;
  private final CompanyRepository companyRepository;
  private final VendorPoTermsTemplateRepository termsRepository;
  private final VendorPoDeliveryTermRepository deliveryRepository;
  private final ObjectMapper objectMapper;

  /** Wires tenant context and repositories for Company-scoped Vendor PO settings. */
  public VendorPoSettingsService(SecurityContextHelper securityContextHelper,
      CompanyAddressRepository addressRepository, CompanyRepository companyRepository,
      VendorPoTermsTemplateRepository termsRepository,
      VendorPoDeliveryTermRepository deliveryRepository, ObjectMapper objectMapper) {
    this.securityContextHelper = securityContextHelper;
    this.addressRepository = addressRepository;
    this.companyRepository = companyRepository;
    this.termsRepository = termsRepository;
    this.deliveryRepository = deliveryRepository;
    this.objectMapper = objectMapper;
  }

  /** Lists active same-Company addresses available for new or revised POs. */
  @Transactional(readOnly = true)
  public List<CompanyAddress> getActiveAddresses() {
    return addressRepository
        .findByCompany_IdAndActiveTrueOrderByIdAsc(securityContextHelper.getCompanyIdFromContext());
  }

  /** Lists active reusable terms templates for PO creation. */
  @Transactional(readOnly = true)
  public List<VendorPoTermsTemplate> getActiveTermsTemplates() {
    return termsRepository.findByCompany_IdAndActiveTrueOrderByNameAsc(
        securityContextHelper.getCompanyIdFromContext());
  }

  /** Lists all tenant templates for the ADMIN settings page. */
  @Transactional(readOnly = true)
  public List<VendorPoTermsTemplate> getAllTermsTemplates() {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return termsRepository.findByCompany_IdOrderByNameAsc(companyId);
  }

  /** Lists active delivery-term options for PO creation. */
  @Transactional(readOnly = true)
  public List<VendorPoDeliveryTerm> getActiveDeliveryTerms() {
    return deliveryRepository.findByCompany_IdAndActiveTrueOrderByNameAsc(
        securityContextHelper.getCompanyIdFromContext());
  }

  /** Lists all Company delivery options for the ADMIN settings page. */
  @Transactional(readOnly = true)
  public List<VendorPoDeliveryTerm> getAllDeliveryTerms() {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return deliveryRepository.findByCompany_IdOrderByNameAsc(companyId);
  }

  /** Creates or updates a tenant-owned terms template and stores its lines as JSON. */
  @Transactional
  public VendorPoTermsTemplate saveTermsTemplate(Long id, String name, String text) {
    String normalizedName = requireName(name, "Template name");
    if (text == null || text.isBlank() || text.length() > 20000) {
      throw new IllegalArgumentException("Enter terms with 20,000 characters or fewer");
    }
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPoTermsTemplate template = id == null ? new VendorPoTermsTemplate()
        : termsRepository.findByIdAndCompany_Id(id, companyId)
            .orElseThrow(() -> new IllegalArgumentException("Terms template not found"));
    template.setName(normalizedName);
    template.setTermsJson(serializeLines(text));
    if (id == null) {
      template.setCompany(companyRepository.findById(companyId)
          .orElseThrow(() -> new IllegalArgumentException("Company not found")));
    }
    return termsRepository.save(template);
  }

  /** Activates or deactivates a tenant-owned template without changing saved PO snapshots. */
  @Transactional
  public VendorPoTermsTemplate toggleTermsTemplate(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPoTermsTemplate template = termsRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Terms template not found"));
    template.setActive(!template.isActive());
    return termsRepository.save(template);
  }

  /** Creates or updates a tenant-owned delivery-term option. */
  @Transactional
  public VendorPoDeliveryTerm saveDeliveryTerm(Long id, String name) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPoDeliveryTerm term = id == null ? new VendorPoDeliveryTerm()
        : deliveryRepository.findByIdAndCompany_Id(id, companyId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery term not found"));
    term.setName(requireName(name, "Delivery term"));
    if (id == null) {
      term.setCompany(companyRepository.findById(companyId)
          .orElseThrow(() -> new IllegalArgumentException("Company not found")));
    }
    return deliveryRepository.save(term);
  }

  /** Activates or deactivates a tenant-owned delivery option without altering PO snapshots. */
  @Transactional
  public VendorPoDeliveryTerm toggleDeliveryTerm(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPoDeliveryTerm term = deliveryRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Delivery term not found"));
    term.setActive(!term.isActive());
    return deliveryRepository.save(term);
  }

  /** Trims and validates a short configuration name. */
  private String requireName(String value, String field) {
    if (value == null || value.isBlank() || value.trim().length() > 160) {
      throw new IllegalArgumentException(
          field + " is required and must be 160 characters or fewer");
    }
    return value.trim();
  }

  /** Converts editor lines into the JSON array format consumed by saved PO snapshots. */
  private String serializeLines(String text) {
    try {
      List<String> lines;
      try {
        lines =
            objectMapper.readValue(text, new com.fasterxml.jackson.core.type.TypeReference<>() {});
      } catch (Exception notJson) {
        lines = text.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
      }
      return objectMapper.writeValueAsString(lines);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Terms could not be saved");
    }
  }
}
