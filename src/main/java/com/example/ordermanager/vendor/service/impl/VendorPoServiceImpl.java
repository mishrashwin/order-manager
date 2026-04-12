package com.example.ordermanager.vendor.service.impl;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.repository.ProductRepository;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import com.example.ordermanager.vendor.repository.VendorPoRepository;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.example.ordermanager.vendor.service.VendorPoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class VendorPoServiceImpl implements VendorPoService {

  private final VendorPoRepository vendorPoRepository;
  private final VendorRepository vendorRepository;
  private final CompanyRepository companyRepository;
  private final ProductRepository productRepository;
  private final SecurityContextHelper securityContextHelper;
  private final com.example.ordermanager.vendor.service.VendorPdfService vendorPdfService;
  private final com.example.ordermanager.utils.Helper helper;

  public VendorPoServiceImpl(VendorPoRepository vendorPoRepository,
      VendorRepository vendorRepository, CompanyRepository companyRepository,
      ProductRepository productRepository, SecurityContextHelper securityContextHelper,
      com.example.ordermanager.vendor.service.VendorPdfService vendorPdfService,
      com.example.ordermanager.utils.Helper helper) {
    this.vendorPoRepository = vendorPoRepository;
    this.vendorRepository = vendorRepository;
    this.companyRepository = companyRepository;
    this.productRepository = productRepository;
    this.securityContextHelper = securityContextHelper;
    this.vendorPdfService = vendorPdfService;
    this.helper = helper;
  }

  // ── Create ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public VendorPo createVendorPo(VendorPo po) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new IllegalStateException("Company not found"));
    po.setCompany(company);

    // Auto-generate PO number: company initials + sequence number (e.g., "ABC001")
    String companyInitials = helper.extractInitials(company.getName());
    long sequenceNumber = vendorPoRepository.countByCompany_Id(companyId) + 1;
    String poNumber = String.format("%s%03d", companyInitials, sequenceNumber);
    po.setPoNumber(poNumber);

    if (po.getVendor() != null && po.getVendor().getId() != null) {
      Vendor vendor = vendorRepository.findById(po.getVendor().getId())
          .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
      if (!vendor.getCompany().getId().equals(companyId)) {
        throw new IllegalStateException("Vendor does not belong to company");
      }
      po.setVendor(vendor);
      po.setVendorName(vendor.getCompanyName());
      po.setVendorGstn(vendor.getGstn());
    }

    resolveItemProducts(po);
    po.recalcTotal();

    po.setCreatedAt(OffsetDateTime.now());
    po.setUpdatedAt(OffsetDateTime.now());
    po.setCreatedBy(securityContextHelper.getCurrentUsername());
    if (po.getStatus() == null) {
      po.setStatus(VendorPoStatus.DRAFT);
    }

    return vendorPoRepository.save(po);
  }

  // ── Update ─────────────────────────────────────────────────────────────────

  /**
   * FIX 3: Full field patch — now also updates vendor and poNumber (mirrors
   * OrderService.applyPatch). Status is preserved as-is; it can only be changed via changeStatus()
   * or an explicit user action — NOT reset to DRAFT on every save.
   */
  @Override
  @Transactional
  public VendorPo updateVendorPo(Long id, VendorPo po) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo existing = vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));

    // Update vendor (with tenant validation) if provided
    if (po.getVendor() != null && po.getVendor().getId() != null) {
      Vendor vendor = vendorRepository.findById(po.getVendor().getId())
          .orElseThrow(() -> new IllegalArgumentException("Vendor not found"));
      if (!vendor.getCompany().getId().equals(companyId)) {
        throw new IllegalArgumentException("Vendor does not belong to this company");
      }
      existing.setVendor(vendor);
      existing.setVendorName(vendor.getCompanyName());
      existing.setVendorGstn(vendor.getGstn());
    }

    // PO number is auto-generated and non-amendable - skip update

    // Update remaining editable fields
    existing.setDeliveryDate(po.getDeliveryDate());
    existing.setNote(po.getNote());
    existing.setLinkedOrderPoNos(po.getLinkedOrderPoNos());
    existing.setEmails(po.getEmails());

    // Replace items
    existing.getItems().clear();
    if (po.getItems() != null) {
      for (VendorPoItem it : po.getItems()) {
        it.setVendorPo(existing);
        resolveItemProduct(it);
        existing.getItems().add(it);
      }
    }

    existing.recalcTotal();
    existing.setUpdatedAt(OffsetDateTime.now());
    return vendorPoRepository.save(existing);
  }

  // ── Read ───────────────────────────────────────────────────────────────────

  @Override
  public VendorPo getVendorPoById(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));
  }

  @Override
  public Page<VendorPo> listVendorPos(LocalDate startDate, LocalDate endDate, Pageable pageable) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return vendorPoRepository.findByCompany_Id(companyId, pageable);
  }

  @Override
  public Page<VendorPo> searchVendorPos(LocalDate startDate, LocalDate endDate, String search, Pageable pageable) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    if (search == null || search.trim().isEmpty()) {
      return vendorPoRepository.findByCompany_Id(companyId, pageable);
    }
    return vendorPoRepository.findByCompany_IdAndPoNumberContainingIgnoreCase(companyId, search, pageable);
  }

  // ── Status ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public VendorPo changeStatus(Long id, VendorPoStatus status) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo existing = vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));
    existing.setStatus(status);
    existing.setUpdatedAt(OffsetDateTime.now());
    return vendorPoRepository.save(existing);
  }

  // ── Delete ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public void deleteVendorPo(Long id) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo existing = vendorPoRepository.findByIdAndCompany_Id(id, companyId)
        .orElseThrow(() -> new IllegalStateException("Vendor PO not found"));
    vendorPoRepository.delete(existing);
  }

  // ── PDF ────────────────────────────────────────────────────────────────────

  @Override
  public byte[] generatePdf(Long id) {
    VendorPo po = getVendorPoById(id);
    return vendorPdfService.generatePdf(po);
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  /**
   * Resolves product references for all items on a VendorPo, populating productName, hsnCode, and
   * unit from the Product entity.
   */
  private void resolveItemProducts(VendorPo po) {
    if (po.getItems() == null)
      return;
    for (VendorPoItem it : po.getItems()) {
      it.setVendorPo(po);
      resolveItemProduct(it);
    }
  }

  /**
   * Resolves the product reference on a single VendorPoItem. A directly-set Product entity takes
   * priority; falls back to productId lookup.
   */
  private void resolveItemProduct(VendorPoItem it) {
    if (it.getProduct() != null) {
      it.setProductName(it.getProduct().getName());
      it.setHsnCode(it.getProduct().getHsnCode());
      it.setUnit(it.getProduct().getUnit());
      it.setGstPercentage(it.getProduct().getGstPercentage());
    } else if (it.getProductId() != null) {
      Product product = productRepository.findById(it.getProductId()).orElse(null);
      if (product != null) {
        it.setProductName(product.getName());
        it.setHsnCode(product.getHsnCode());
        it.setUnit(product.getUnit());
        it.setGstPercentage(product.getGstPercentage());
        it.setProduct(product);
      } else {
        it.setProductName(it.getProductName() != null ? it.getProductName() : "Unknown Product");
      }
    } else {
      it.setProductName(it.getProductName() != null ? it.getProductName() : "Unknown Product");
    }
  }
}
