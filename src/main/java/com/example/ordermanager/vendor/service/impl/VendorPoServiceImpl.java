package com.example.ordermanager.vendor.service.impl;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyAddress;
import com.example.ordermanager.company.repository.CompanyAddressRepository;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.repository.ProductRepository;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.UserRepository;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import com.example.ordermanager.vendor.entity.VendorPoAddressSnapshot;
import com.example.ordermanager.vendor.entity.VendorPoTermsTemplate;
import com.example.ordermanager.vendor.repository.VendorPoDeliveryTermRepository;
import com.example.ordermanager.vendor.repository.VendorPoRepository;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.example.ordermanager.vendor.repository.VendorPoTermsTemplateRepository;
import com.example.ordermanager.vendor.service.VendorPoCalculationService;
import com.example.ordermanager.vendor.service.VendorPoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class VendorPoServiceImpl implements VendorPoService {

  private final VendorPoRepository vendorPoRepository;
  private final CompanyAddressRepository companyAddressRepository;
  private final VendorRepository vendorRepository;
  private final CompanyRepository companyRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final SecurityContextHelper securityContextHelper;
  private final com.example.ordermanager.vendor.service.VendorPdfService vendorPdfService;
  private final com.example.ordermanager.utils.Helper helper;
  private final VendorPoCalculationService calculationService;
  private final VendorPoTermsTemplateRepository termsTemplateRepository;
  private final VendorPoDeliveryTermRepository deliveryTermRepository;
  private final ObjectMapper objectMapper;

  /** Wires tenant-scoped repositories and PO snapshot/calculation collaborators. */
  public VendorPoServiceImpl(VendorPoRepository vendorPoRepository,
      CompanyAddressRepository companyAddressRepository, VendorRepository vendorRepository,
      CompanyRepository companyRepository, ProductRepository productRepository,
      UserRepository userRepository, SecurityContextHelper securityContextHelper,
      com.example.ordermanager.vendor.service.VendorPdfService vendorPdfService,
      com.example.ordermanager.utils.Helper helper, VendorPoCalculationService calculationService,
      VendorPoTermsTemplateRepository termsTemplateRepository,
      VendorPoDeliveryTermRepository deliveryTermRepository, ObjectMapper objectMapper) {
    this.vendorPoRepository = vendorPoRepository;
    this.companyAddressRepository = companyAddressRepository;
    this.vendorRepository = vendorRepository;
    this.companyRepository = companyRepository;
    this.productRepository = productRepository;
    this.userRepository = userRepository;
    this.securityContextHelper = securityContextHelper;
    this.vendorPdfService = vendorPdfService;
    this.helper = helper;
    this.calculationService = calculationService;
    this.termsTemplateRepository = termsTemplateRepository;
    this.deliveryTermRepository = deliveryTermRepository;
    this.objectMapper = objectMapper;
  }

  // ── Create ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  /** Creates a new Company-owned draft, snapshots its sources, and calculates persisted totals. */
  public VendorPo createVendorPo(VendorPo po) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new IllegalStateException("Company not found"));
    po.setCompany(company);

    // Auto-generate PO number: company initials + sequence number (e.g., "ABC001")
    String companyInitials = helper.extractInitials(company.getName());
    long sequenceNumber = vendorPoRepository.countByCompany_IdAndRevisionNumber(companyId, 0) + 1;
    String poNumber = String.format("%s%03d", companyInitials, sequenceNumber);
    po.setPoNumber(poNumber);

    if (po.getVendor() != null && po.getVendor().getId() != null) {
      Vendor vendor = vendorRepository.findByIdAndCompany_Id(po.getVendor().getId(), companyId)
          .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
      po.setVendor(vendor);
      po.setVendorName(vendor.getCompanyName());
      po.setVendorGstn(vendor.getGstn());
    }

    po.setPoDate(po.getPoDate() == null ? LocalDate.now() : po.getPoDate());
    populateBuyerSnapshot(po, company, companyId);
    populateVendorSnapshot(po);
    normalizeTermsSnapshot(po, companyId, company);
    resolveItemProducts(po, companyId);
    calculationService.calculate(po);

    po.setCreatedAt(OffsetDateTime.now());
    po.setUpdatedAt(OffsetDateTime.now());
    po.setCreatedBy(securityContextHelper.getCurrentUsername());

    // Set orderOwner if provided (with tenant validation)
    if (po.getOrderOwner() != null && po.getOrderOwner().getId() != null) {
      User orderOwner = userRepository.findById(po.getOrderOwner().getId())
          .orElseThrow(() -> new IllegalArgumentException("Order owner not found"));
      if (!orderOwner.getCompany().getId().equals(companyId)) {
        throw new IllegalStateException("Order owner does not belong to company");
      }
      po.setOrderOwner(orderOwner);
    }

    po.setStatus(VendorPoStatus.DRAFT);
    po.setRevisionNumber(0);
    po.setCurrentRevision(true);
    po.setSnapshotOrigin("CURRENT");
    VendorPo saved = vendorPoRepository.save(po);
    saved.setRootPo(saved);
    return vendorPoRepository.save(saved);
  }

  // ── Update ─────────────────────────────────────────────────────────────────

  /**
   * FIX 3: Full field patch — now also updates vendor and poNumber (mirrors
   * OrderService.applyPatch). Status is preserved as-is; it can only be changed via changeStatus()
   * or an explicit user action — NOT reset to DRAFT on every save.
   */
  @Override
  @Transactional
  /** Replaces editable draft fields after tenant and lifecycle checks. */
  public VendorPo updateVendorPo(Long id, VendorPo po) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo existing = vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));

    if (existing.getStatus() != VendorPoStatus.DRAFT) {
      throw new IllegalStateException("Only DRAFT Vendor POs can be updated");
    }

    // Update vendor (with tenant validation) if provided
    if (po.getVendor() != null && po.getVendor().getId() != null) {
      Vendor vendor = vendorRepository.findByIdAndCompany_Id(po.getVendor().getId(), companyId)
          .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
      existing.setVendor(vendor);
      existing.setVendorName(vendor.getCompanyName());
      existing.setVendorGstn(vendor.getGstn());
    }

    // PO number is auto-generated and non-amendable - skip update

    // Update orderOwner if provided (with tenant validation)
    if (po.getOrderOwner() != null && po.getOrderOwner().getId() != null) {
      User orderOwner = userRepository.findById(po.getOrderOwner().getId())
          .orElseThrow(() -> new IllegalArgumentException("Order owner not found"));
      if (!orderOwner.getCompany().getId().equals(companyId)) {
        throw new IllegalArgumentException("Order owner does not belong to this company");
      }
      existing.setOrderOwner(orderOwner);
    }

    // Update remaining editable fields
    existing.setDeliveryDate(po.getDeliveryDate());
    existing.setNote(po.getNote());
    existing.setLinkedOrderPoNos(po.getLinkedOrderPoNos());
    existing.setEmails(po.getEmails());
    existing.setPoDate(po.getPoDate() == null ? existing.getPoDate() : po.getPoDate());
    existing.setPaymentTerms(po.getPaymentTerms());
    existing.setDeliveryTerms(po.getDeliveryTerms());
    existing.setDeliveryTermId(po.getDeliveryTermId());
    existing.setFreightIncluded(po.getFreightIncluded());
    existing.setFreightAmount(po.getFreightAmount());
    existing.setFreightTaxable(po.getFreightTaxable());
    existing.setFreightGstPercentage(po.getFreightGstPercentage());
    existing.setPackingIncluded(po.getPackingIncluded());
    existing.setPackingAmount(po.getPackingAmount());
    existing.setPackingTaxable(po.getPackingTaxable());
    existing.setPackingGstPercentage(po.getPackingGstPercentage());
    existing.setPenaltyClause(po.getPenaltyClause());
    existing.setTermsTemplateId(po.getTermsTemplateId());
    existing.setTermsConditionsEnabled(po.isTermsConditionsEnabled());
    existing.setTermsConditionsSnapshot(po.getTermsConditionsSnapshot());
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new IllegalStateException("Company not found"));
    populateBuyerSnapshot(existing, company, companyId, po);
    populateVendorSnapshot(existing);
    normalizeTermsSnapshot(existing, companyId, company);

    // Replace items
    existing.getItems().clear();
    if (po.getItems() != null) {
      for (VendorPoItem it : po.getItems()) {
        it.setVendorPo(existing);
        resolveItemProduct(it, companyId);
        existing.getItems().add(it);
      }
    }

    calculationService.calculate(existing);
    existing.setUpdatedAt(OffsetDateTime.now());
    return vendorPoRepository.save(existing);
  }

  // ── Read ───────────────────────────────────────────────────────────────────

  @Override
  /** Loads a PO only when it belongs to the authenticated Company. */
  public VendorPo getVendorPoById(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));
  }

  @Override
  /** Lists only current PO revisions for the authenticated Company. */
  public Page<VendorPo> listVendorPos(LocalDate startDate, LocalDate endDate, Pageable pageable) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return vendorPoRepository.findByCompany_IdAndCurrentRevisionTrue(companyId, pageable);
  }

  @Override
  /** Searches current PO revisions within the authenticated Company. */
  public Page<VendorPo> searchVendorPos(LocalDate startDate, LocalDate endDate, String search,
      Pageable pageable) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    if (search == null || search.trim().isEmpty()) {
      return vendorPoRepository.findByCompany_IdAndCurrentRevisionTrue(companyId, pageable);
    }
    return vendorPoRepository.findByCompany_IdAndCurrentRevisionTrueAndPoNumberContainingIgnoreCase(
        companyId, search, pageable);
  }

  // ── Status ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  /** Applies permitted status changes and freezes the PDF when a DRAFT is issued. */
  public VendorPo changeStatus(Long id, VendorPoStatus status) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo existing = vendorPoRepository.findLockedByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));
    if (status == VendorPoStatus.SENT) {
      if (existing.getStatus() != VendorPoStatus.DRAFT) {
        throw new IllegalStateException("Only a DRAFT Vendor PO can be issued");
      }
      validateSelectedAddresses(existing, companyId);
      calculationService.calculate(existing);
      existing.setIssuedPdf(vendorPdfService.generatePdf(existing));
    } else if (existing.getStatus() == VendorPoStatus.DRAFT) {
      throw new IllegalStateException("A Vendor PO must be issued before its status can change");
    }
    existing.setStatus(status);
    existing.setUpdatedAt(OffsetDateTime.now());
    return vendorPoRepository.save(existing);
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  /** Deletes only the current draft and restores its prior revision when present. */
  public void deleteVendorPo(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo existing = vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));
    if (existing.getStatus() != VendorPoStatus.DRAFT || !existing.isCurrentRevision()) {
      throw new IllegalStateException("Only the current DRAFT Vendor PO can be deleted");
    }
    VendorPo previous = existing.getPreviousRevision();
    vendorPoRepository.delete(existing);
    if (previous != null) {
      previous.setCurrentRevision(true);
      vendorPoRepository.save(previous);
    }
  }

  // ── PDF ────────────────────────────────────────────────────────────────────

  @Override
  /** Returns the saved issued PDF or renders the current draft preview from snapshots. */
  public byte[] generatePdf(Long id) {
    VendorPo po = getVendorPoById(id);
    if (po.getStatus() != VendorPoStatus.DRAFT && po.getIssuedPdf() != null) {
      return po.getIssuedPdf().clone();
    }
    return vendorPdfService.generatePdf(po);
  }

  /** Refreshes the selected active address and issue-time Company snapshots before SENT. */
  private void validateSelectedAddresses(VendorPo po, Long companyId) {
    validateAddressStillActive(po.getBillToAddress(), companyId, "Bill To");
    validateAddressStillActive(po.getShipToAddress(), companyId, "Ship To");
  }

  /** Ensures a draft's selected address remains active and tenant-owned before issuance. */
  private void validateAddressStillActive(CompanyAddress address, Long companyId, String role) {
    if (address == null || address.getId() == null) {
      throw new IllegalArgumentException("An active " + role + " address is required");
    }
    CompanyAddress current =
        companyAddressRepository.findByIdAndCompany_Id(address.getId(), companyId).orElseThrow(
            () -> new IllegalArgumentException("Selected " + role + " address not found"));
    if (!current.isActive()) {
      throw new IllegalArgumentException("Selected " + role + " address is inactive");
    }
  }

  /**
   * Selects active same-Company addresses, defaults missing selections, and snapshots the buyer.
   */
  private void populateBuyerSnapshot(VendorPo po, Company company, Long companyId) {
    populateBuyerSnapshot(po, company, companyId, po);
  }

  /** Applies submitted address choices or existing choices, then copies their complete values. */
  private void populateBuyerSnapshot(VendorPo target, Company company, Long companyId,
      VendorPo submitted) {
    CompanyAddress billTo =
        selectAddress(submitted.getBillToAddress(), target.getBillToAddress(), companyId, true);
    CompanyAddress shipTo =
        selectAddress(submitted.getShipToAddress(), target.getShipToAddress(), companyId, false);
    boolean preserveBillSnapshot = target.getId() != null && target.getBillToAddress() != null
        && target.getBillToAddress().getId().equals(billTo.getId());
    boolean preserveShipSnapshot = target.getId() != null && target.getShipToAddress() != null
        && target.getShipToAddress().getId().equals(shipTo.getId());
    target.setBillToAddress(billTo);
    target.setShipToAddress(shipTo);
    if (!preserveBillSnapshot) {
      target.setBillToSnapshot(toSnapshot(billTo));
      target.setBuyerGstin(
          billTo.getGstin() == null || billTo.getGstin().isBlank() ? company.getGstn()
              : billTo.getGstin());
    }
    if (!preserveShipSnapshot) {
      target.setShipToSnapshot(toSnapshot(shipTo));
    }
    if (target.getId() == null) {
      target.setBuyerCompanyName(company.getName());
      target.setBuyerLogoData(copyBytes(company.getLogoData()));
      target.setBuyerLogoContentType(company.getLogoContentType());
      target.setBuyerSignatureData(copyBytes(company.getSignatureData()));
      target.setBuyerSignatureContentType(company.getSignatureContentType());
    }
  }

  /** Resolves an active address by tenant and defaults only a missing create selection. */
  private CompanyAddress selectAddress(CompanyAddress submitted, CompanyAddress existing,
      Long companyId, boolean billTo) {
    CompanyAddress choice = submitted != null && submitted.getId() != null ? submitted : existing;
    if (choice == null || choice.getId() == null) {
      choice = billTo
          ? companyAddressRepository.findByCompany_IdAndActiveTrueAndDefaultBillToTrue(companyId)
              .orElse(null)
          : companyAddressRepository.findByCompany_IdAndActiveTrueAndDefaultShipToTrue(companyId)
              .orElse(null);
    } else {
      choice = companyAddressRepository.findByIdAndCompany_Id(choice.getId(), companyId)
          .orElseThrow(() -> new IllegalArgumentException(
              "Selected " + (billTo ? "Bill To" : "Ship To") + " address not found"));
    }
    if (choice == null || !choice.isActive()) {
      throw new IllegalArgumentException(
          "An active " + (billTo ? "Bill To" : "Ship To") + " address is required");
    }
    return choice;
  }

  /** Copies the address fields into an independent immutable PO value object. */
  private VendorPoAddressSnapshot toSnapshot(CompanyAddress address) {
    VendorPoAddressSnapshot snapshot = new VendorPoAddressSnapshot();
    snapshot.setNickname(address.getNickname());
    snapshot.setAddressLine1(address.getAddressLine1());
    snapshot.setAddressLine2(address.getAddressLine2());
    snapshot.setCity(address.getCity());
    snapshot.setState(address.getState() == null ? null : address.getState().name());
    snapshot.setStateCode(address.getStateCode());
    snapshot.setPostalCode(address.getPostalCode());
    snapshot.setCountry(address.getCountry());
    snapshot.setGstin(address.getGstin());
    snapshot.setDrugLicenseNumber(address.getDrugLicenseNumber());
    snapshot.setContactPersonName(address.getContactPersonName());
    snapshot.setContactNumber(address.getContactNumber());
    snapshot.setContactEmail(address.getContactEmail());
    return snapshot;
  }

  /** Copies mutable logo and signature arrays so later Company changes cannot rewrite snapshots. */
  private byte[] copyBytes(byte[] source) {
    return source == null ? null : source.clone();
  }

  /** Copies Vendor identity values into the PO's tenant-independent display snapshot. */
  private void populateVendorSnapshot(VendorPo po) {
    Vendor vendor = po.getVendor();
    if (vendor == null) {
      return;
    }
    po.setVendorName(vendor.getCompanyName());
    po.setVendorGstn(vendor.getGstn());
    po.setVendorContactPerson(vendor.getContactPerson());
    po.setVendorAddress(vendor.getAddress());
    po.setVendorEmail(vendor.getEmail());
    po.setVendorPhone(vendor.getPhone());
    po.setVendorState(vendor.getState() == null ? null : vendor.getState().name());
    po.setVendorStateCode(vendor.getState() == null ? null : vendor.getState().getCode());
    if (po.getPaymentTerms() == null || po.getPaymentTerms().isBlank()) {
      po.setPaymentTerms(vendor.getDefaultPaymentTerms());
    }
  }

  /** Resolves tenant-owned delivery/T&C selections and freezes the final T&C text as JSON. */
  private void normalizeTermsSnapshot(VendorPo po, Long companyId, Company company) {
    if (po.getTermsConditionsSnapshot() != null
        && po.getTermsConditionsSnapshot().length() > 20000) {
      throw new IllegalArgumentException("Terms and Conditions must be 20,000 characters or fewer");
    }
    if (po.getDeliveryTermId() != null) {
      var deliveryTerm =
          deliveryTermRepository.findByIdAndCompany_Id(po.getDeliveryTermId(), companyId)
              .filter(com.example.ordermanager.vendor.entity.VendorPoDeliveryTerm::isActive)
              .orElseThrow(() -> new IllegalArgumentException("Delivery term not found"));
      po.setDeliveryTerms(deliveryTerm.getName());
    } else if (po.getDeliveryTerms() != null && po.getDeliveryTerms().length() > 500) {
      throw new IllegalArgumentException("Custom delivery terms must be 500 characters or fewer");
    }
    String submitted = po.getTermsConditionsSnapshot();
    if (po.getTermsTemplateId() != null) {
      VendorPoTermsTemplate template =
          termsTemplateRepository.findByIdAndCompany_Id(po.getTermsTemplateId(), companyId)
              .filter(VendorPoTermsTemplate::isActive)
              .orElseThrow(() -> new IllegalArgumentException("Terms template not found"));
      if (submitted == null || submitted.isBlank()) {
        submitted = template.getTermsJson();
      }
    } else {
      if (submitted == null || submitted.isBlank()) {
        submitted = company.getVendorPoTermsConditions();
      }
      po.setTermsConditionsEnabled(company.getVendorPoTermsConditionsEnabled() == null
          || company.getVendorPoTermsConditionsEnabled());
    }
    if (submitted == null || submitted.isBlank()) {
      submitted = "[]";
    }
    try {
      java.util.List<String> lines;
      try {
        lines = objectMapper.readValue(submitted, new TypeReference<java.util.List<String>>() {});
      } catch (Exception notJson) {
        lines = submitted.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
      }
      po.setTermsConditionsSnapshot(objectMapper.writeValueAsString(lines));
    } catch (Exception exception) {
      throw new IllegalArgumentException("Terms and Conditions could not be saved");
    }
  }

  /** Clones revision fields while leaving the source row and its issued PDF untouched. */
  private VendorPo copyRevision(VendorPo source) {
    VendorPo copy = new VendorPo();
    copy.setCompany(source.getCompany());
    copy.setVendor(source.getVendor());
    copy.setVendorName(source.getVendorName());
    copy.setVendorGstn(source.getVendorGstn());
    copy.setPoNumber(source.getPoNumber());
    copy.setPoDate(source.getPoDate());
    copy.setDeliveryDate(source.getDeliveryDate());
    copy.setLinkedOrderPoNos(source.getLinkedOrderPoNos());
    copy.setEmails(source.getEmails());
    copy.setNote(source.getNote());
    copy.setOrderOwner(source.getOrderOwner());
    copy.setBillToAddress(source.getBillToAddress());
    copy.setShipToAddress(source.getShipToAddress());
    copy.setBillToSnapshot(copyAddressSnapshot(source.getBillToSnapshot()));
    copy.setShipToSnapshot(copyAddressSnapshot(source.getShipToSnapshot()));
    copy.setBuyerCompanyName(source.getBuyerCompanyName());
    copy.setBuyerGstin(source.getBuyerGstin());
    copy.setBuyerLogoData(copyBytes(source.getBuyerLogoData()));
    copy.setBuyerLogoContentType(source.getBuyerLogoContentType());
    copy.setBuyerSignatureData(copyBytes(source.getBuyerSignatureData()));
    copy.setBuyerSignatureContentType(source.getBuyerSignatureContentType());
    copy.setVendorContactPerson(source.getVendorContactPerson());
    copy.setVendorAddress(source.getVendorAddress());
    copy.setVendorEmail(source.getVendorEmail());
    copy.setVendorPhone(source.getVendorPhone());
    copy.setVendorState(source.getVendorState());
    copy.setVendorStateCode(source.getVendorStateCode());
    copy.setPaymentTerms(source.getPaymentTerms());
    copy.setDeliveryTerms(source.getDeliveryTerms());
    copy.setDeliveryTermId(source.getDeliveryTermId());
    copy.setFreightIncluded(source.getFreightIncluded());
    copy.setFreightAmount(source.getFreightAmount());
    copy.setFreightTaxable(source.getFreightTaxable());
    copy.setFreightGstPercentage(source.getFreightGstPercentage());
    copy.setPackingIncluded(source.getPackingIncluded());
    copy.setPackingAmount(source.getPackingAmount());
    copy.setPackingTaxable(source.getPackingTaxable());
    copy.setPackingGstPercentage(source.getPackingGstPercentage());
    copy.setPenaltyClause(source.getPenaltyClause());
    copy.setTermsTemplateId(source.getTermsTemplateId());
    copy.setTermsConditionsSnapshot(source.getTermsConditionsSnapshot());
    copy.setTermsConditionsEnabled(source.isTermsConditionsEnabled());
    copy.setSubtotalBeforeTax(source.getSubtotalBeforeTax());
    copy.setTotalTax(source.getTotalTax());
    copy.setCgstAmount(source.getCgstAmount());
    copy.setSgstAmount(source.getSgstAmount());
    copy.setIgstAmount(source.getIgstAmount());
    copy.setTotalAmount(source.getTotalAmount());
    if (source.getItems() != null) {
      source.getItems().forEach(item -> copy.addItem(copyItem(item)));
    }
    return copy;
  }

  /** Copies embedded address values so revisions have independent snapshot instances. */
  private VendorPoAddressSnapshot copyAddressSnapshot(VendorPoAddressSnapshot source) {
    VendorPoAddressSnapshot copy = new VendorPoAddressSnapshot();
    if (source != null) {
      copy.setNickname(source.getNickname());
      copy.setAddressLine1(source.getAddressLine1());
      copy.setAddressLine2(source.getAddressLine2());
      copy.setCity(source.getCity());
      copy.setState(source.getState());
      copy.setStateCode(source.getStateCode());
      copy.setPostalCode(source.getPostalCode());
      copy.setCountry(source.getCountry());
      copy.setGstin(source.getGstin());
      copy.setDrugLicenseNumber(source.getDrugLicenseNumber());
    }
    return copy;
  }

  /** Copies a PO item into a new revision without carrying its database identifier. */
  private VendorPoItem copyItem(VendorPoItem source) {
    VendorPoItem copy = new VendorPoItem();
    copy.setProduct(source.getProduct());
    copy.setProductId(source.getProductId());
    copy.setProductName(source.getProductName());
    copy.setHsnCode(source.getHsnCode());
    copy.setUnit(source.getUnit());
    copy.setGstPercentage(source.getGstPercentage());
    copy.setGstAmount(source.getGstAmount());
    copy.setQuantity(source.getQuantity());
    copy.setUnitPrice(source.getUnitPrice());
    copy.setPreGstUnitPrice(source.getPreGstUnitPrice());
    copy.setGstInclusiveUnitPrice(source.getGstInclusiveUnitPrice());
    copy.setLineTotal(source.getLineTotal());
    copy.setPricingUnit(source.getPricingUnit());
    copy.setTaxableAmount(source.getTaxableAmount());
    copy.setTaxableRate(source.getTaxableRate());
    copy.setCgstAmount(source.getCgstAmount());
    copy.setSgstAmount(source.getSgstAmount());
    copy.setIgstAmount(source.getIgstAmount());
    copy.setDeliveryDate(source.getDeliveryDate());
    return copy;
  }

  /** Creates the next editable revision while preserving the existing immutable PO version. */
  @Override
  @Transactional
  public VendorPo createRevision(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo source = vendorPoRepository.findLockedByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Vendor PO not found"));
    if (!source.isCurrentRevision() || source.getStatus() == VendorPoStatus.DRAFT) {
      throw new IllegalStateException("Only the current issued Vendor PO can be revised");
    }
    VendorPo root = source.getRootPo() == null ? source : source.getRootPo();
    source.setCurrentRevision(false);
    vendorPoRepository.save(source);

    VendorPo revision = copyRevision(source);
    revision.setRootPo(root);
    revision.setPreviousRevision(source);
    revision.setRevisionNumber(source.getRevisionNumber() + 1);
    revision.setRevisionCreatedAt(OffsetDateTime.now());
    revision.setRevisionCreatedBy(securityContextHelper.getCurrentUsername());
    revision.setCurrentRevision(true);
    revision.setIssuedPdf(null);
    revision.setStatus(VendorPoStatus.DRAFT);
    revision.setCreatedAt(OffsetDateTime.now());
    revision.setUpdatedAt(OffsetDateTime.now());
    revision.setCreatedBy(securityContextHelper.getCurrentUsername());
    return vendorPoRepository.save(revision);
  }

  /** Returns every revision row after resolving the requested PO inside the tenant. */
  @Override
  @Transactional(readOnly = true)
  public java.util.List<VendorPo> getRevisionHistory(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo requested = vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Vendor PO not found"));
    VendorPo root = requested.getRootPo() == null ? requested : requested.getRootPo();
    return vendorPoRepository.findByRootPo_IdAndCompany_IdOrderByRevisionNumberDesc(root.getId(),
        companyId);
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  /**
   * Resolves product references for all items on a VendorPo, populating productName, hsnCode, and
   * unit from the Product entity.
   */
  private void resolveItemProducts(VendorPo po, Long companyId) {
    if (po.getItems() == null)
      return;
    for (VendorPoItem it : po.getItems()) {
      it.setVendorPo(po);
      resolveItemProduct(it, companyId);
    }
  }

  /**
   * Resolves the product reference on a single VendorPoItem. A directly-set Product entity takes
   * priority; falls back to productId lookup.
   */
  private void resolveItemProduct(VendorPoItem it, Long companyId) {
    if (it.getProduct() != null) {
      if (it.getProduct().getCompany() == null
          || !companyId.equals(it.getProduct().getCompany().getId())) {
        throw new IllegalArgumentException("Product does not belong to this company");
      }
      it.setProductName(it.getProduct().getName());
      it.setHsnCode(it.getProduct().getHsnCode());
      it.setUnit(it.getProduct().getUnit());
      it.setGstPercentage(it.getProduct().getGstPercentage());

      // Handle pre-GST price calculation
      if (it.getPreGstUnitPrice() == null && it.getUnitPrice() != null
          && it.getProduct().getGstPercentage() != null) {
        // Convert legacy unitPrice to pre-GST price for backward compatibility
        java.math.BigDecimal gstMultiplier = it.getProduct().getGstPercentage()
            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal preGstPrice = it.getUnitPrice()
            .divide(java.math.BigDecimal.ONE.add(gstMultiplier), 2, java.math.RoundingMode.HALF_UP);
        it.setPreGstUnitPrice(preGstPrice);
      }
    } else if (it.getProductId() != null) {
      Product product = productRepository.findByIdAndCompany_Id(it.getProductId(), companyId)
          .orElseThrow(() -> new IllegalArgumentException("Product not found for this company"));
      it.setProductName(product.getName());
      it.setHsnCode(product.getHsnCode());
      it.setUnit(product.getUnit());
      it.setGstPercentage(product.getGstPercentage());
      it.setProduct(product);

      if (it.getPreGstUnitPrice() == null && it.getUnitPrice() != null
          && product.getGstPercentage() != null) {
        java.math.BigDecimal gstMultiplier = product.getGstPercentage()
            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal preGstPrice = it.getUnitPrice()
            .divide(java.math.BigDecimal.ONE.add(gstMultiplier), 2, java.math.RoundingMode.HALF_UP);
        it.setPreGstUnitPrice(preGstPrice);
      }
    } else {
      it.setProductName(it.getProductName() != null ? it.getProductName() : "Unknown Product");
    }
  }
}
