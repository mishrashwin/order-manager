package com.example.ordermanager.vendor.entity;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.vendor.entity.Vendor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "vendor_purchase_order")
@Getter
@Setter
@ToString(exclude = {"items", "company", "vendor", "rootPo", "previousRevision"})
public class VendorPo {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "vendor_id")
  private Vendor vendor;

  private String vendorName;

  private String vendorGstn;

  @Column(updatable = false)
  private String poNumber;

  private LocalDate deliveryDate;

  @Column(columnDefinition = "TEXT")
  private String linkedOrderPoNos;

  @Column(columnDefinition = "TEXT")
  private String emails; // comma-separated

  @Column(columnDefinition = "TEXT")
  private String note;

  @Column(name = "total_amount", precision = 14, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "po_date")
  private LocalDate poDate;

  @Column(name = "revision_number", nullable = false)
  private int revisionNumber;

  @Column(name = "current_revision", nullable = false)
  private boolean currentRevision = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "root_po_id")
  private VendorPo rootPo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "previous_revision_id")
  private VendorPo previousRevision;

  @Column(name = "revision_created_at")
  private OffsetDateTime revisionCreatedAt;

  @Column(name = "revision_created_by", length = 128)
  private String revisionCreatedBy;

  @Column(name = "snapshot_origin", length = 32)
  private String snapshotOrigin;

  @Column(name = "issued_pdf")
  private byte[] issuedPdf;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "bill_to_address_id")
  private com.example.ordermanager.company.entity.CompanyAddress billToAddress;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ship_to_address_id")
  private com.example.ordermanager.company.entity.CompanyAddress shipToAddress;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "nickname",
          column = @Column(name = "bill_to_nickname", length = 120)),
      @AttributeOverride(name = "addressLine1",
          column = @Column(name = "bill_to_address_line1", columnDefinition = "TEXT")),
      @AttributeOverride(name = "addressLine2",
          column = @Column(name = "bill_to_address_line2", columnDefinition = "TEXT")),
      @AttributeOverride(name = "city", column = @Column(name = "bill_to_city", length = 120)),
      @AttributeOverride(name = "state", column = @Column(name = "bill_to_state", length = 100)),
      @AttributeOverride(name = "stateCode",
          column = @Column(name = "bill_to_state_code", length = 2)),
      @AttributeOverride(name = "postalCode",
          column = @Column(name = "bill_to_postal_code", length = 20)),
      @AttributeOverride(name = "country",
          column = @Column(name = "bill_to_country", length = 100)),
      @AttributeOverride(name = "gstin", column = @Column(name = "bill_to_gstin", length = 50)),
      @AttributeOverride(name = "drugLicenseNumber",
          column = @Column(name = "bill_to_drug_license_number", length = 100)),
      @AttributeOverride(name = "contactPersonName",
          column = @Column(name = "bill_to_contact_person_name", length = 120)),
      @AttributeOverride(name = "contactNumber",
          column = @Column(name = "bill_to_contact_number", length = 20)),
      @AttributeOverride(name = "contactEmail",
          column = @Column(name = "bill_to_contact_email", length = 255))})
  private VendorPoAddressSnapshot billToSnapshot = new VendorPoAddressSnapshot();

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "nickname",
          column = @Column(name = "ship_to_nickname", length = 120)),
      @AttributeOverride(name = "addressLine1",
          column = @Column(name = "ship_to_address_line1", columnDefinition = "TEXT")),
      @AttributeOverride(name = "addressLine2",
          column = @Column(name = "ship_to_address_line2", columnDefinition = "TEXT")),
      @AttributeOverride(name = "city", column = @Column(name = "ship_to_city", length = 120)),
      @AttributeOverride(name = "state", column = @Column(name = "ship_to_state", length = 100)),
      @AttributeOverride(name = "stateCode",
          column = @Column(name = "ship_to_state_code", length = 2)),
      @AttributeOverride(name = "postalCode",
          column = @Column(name = "ship_to_postal_code", length = 20)),
      @AttributeOverride(name = "country",
          column = @Column(name = "ship_to_country", length = 100)),
      @AttributeOverride(name = "gstin", column = @Column(name = "ship_to_gstin", length = 50)),
      @AttributeOverride(name = "drugLicenseNumber",
          column = @Column(name = "ship_to_drug_license_number", length = 100)),
      @AttributeOverride(name = "contactPersonName",
          column = @Column(name = "ship_to_contact_person_name", length = 120)),
      @AttributeOverride(name = "contactNumber",
          column = @Column(name = "ship_to_contact_number", length = 20)),
      @AttributeOverride(name = "contactEmail",
          column = @Column(name = "ship_to_contact_email", length = 255))})
  private VendorPoAddressSnapshot shipToSnapshot = new VendorPoAddressSnapshot();

  @Column(name = "buyer_company_name", length = 255)
  private String buyerCompanyName;

  @Column(name = "buyer_gstin", length = 50)
  private String buyerGstin;

  @Column(name = "buyer_logo_data")
  private byte[] buyerLogoData;

  @Column(name = "buyer_logo_content_type", length = 100)
  private String buyerLogoContentType;

  @Column(name = "buyer_signature_data")
  private byte[] buyerSignatureData;

  @Column(name = "buyer_signature_content_type", length = 100)
  private String buyerSignatureContentType;

  @Column(name = "vendor_contact_person", length = 255)
  private String vendorContactPerson;

  @Column(name = "vendor_address", columnDefinition = "TEXT")
  private String vendorAddress;

  @Column(name = "vendor_email", length = 255)
  private String vendorEmail;

  @Column(name = "vendor_phone", length = 50)
  private String vendorPhone;

  @Column(name = "vendor_state", length = 100)
  private String vendorState;

  @Column(name = "vendor_state_code", length = 2)
  private String vendorStateCode;

  @Column(name = "payment_terms", columnDefinition = "TEXT")
  private String paymentTerms;

  @Column(name = "delivery_terms", columnDefinition = "TEXT")
  private String deliveryTerms;

  @Column(name = "delivery_term_id")
  private Long deliveryTermId;

  @Column(name = "freight_included")
  private Boolean freightIncluded;

  @Column(name = "freight_amount", precision = 14, scale = 2)
  private BigDecimal freightAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "freight_taxable")
  private Boolean freightTaxable;

  @Column(name = "freight_gst_percentage", precision = 5, scale = 2)
  private BigDecimal freightGstPercentage;

  @Column(name = "packing_included")
  private Boolean packingIncluded;

  @Column(name = "packing_amount", precision = 14, scale = 2)
  private BigDecimal packingAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "packing_taxable")
  private Boolean packingTaxable;

  @Column(name = "packing_gst_percentage", precision = 5, scale = 2)
  private BigDecimal packingGstPercentage;

  @Column(name = "penalty_clause", columnDefinition = "TEXT")
  private String penaltyClause;

  @Column(name = "terms_template_id")
  private Long termsTemplateId;

  @Column(name = "terms_conditions_snapshot", columnDefinition = "TEXT")
  private String termsConditionsSnapshot;

  @Transient
  private String termsEditorText;

  @Column(name = "terms_conditions_enabled", nullable = false)
  private boolean termsConditionsEnabled = true;

  @Column(name = "subtotal_before_tax", precision = 14, scale = 2)
  private BigDecimal subtotalBeforeTax = BigDecimal.ZERO.setScale(2);

  @Column(name = "total_tax", precision = 14, scale = 2)
  private BigDecimal totalTax = BigDecimal.ZERO.setScale(2);

  @Column(name = "cgst_amount", precision = 14, scale = 2)
  private BigDecimal cgstAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "sgst_amount", precision = 14, scale = 2)
  private BigDecimal sgstAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "igst_amount", precision = 14, scale = 2)
  private BigDecimal igstAmount = BigDecimal.ZERO.setScale(2);

  @Enumerated(EnumType.STRING)
  private VendorPoStatus status;

  private String createdBy;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "order_owner_id")
  private User orderOwner;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "vendorPo", cascade = CascadeType.ALL, orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<VendorPoItem> items = new ArrayList<>();

  /** Attaches a PO item to this revision without performing financial calculations. */
  public void addItem(VendorPoItem item) {
    item.setVendorPo(this);
    this.items.add(item);
  }

  /** Converts legacy callers to the BigDecimal Vendor PO total representation. */
  public void setTotalAmount(Double value) {
    this.totalAmount = value == null ? null
        : BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  /** Stores PO totals at the monetary scale used for all calculations. */
  public void setTotalAmount(BigDecimal value) {
    this.totalAmount = value;
  }

  /** Returns a compact summary of product names and quantities for PO list rows. */
  public String getDisplayProductSummary() {
    if (items != null && !items.isEmpty()) {
      String summary = items.stream().filter(item -> item.getProductName() != null).map(item -> {
        String product = item.getProductName().trim();
        Integer itemQty = item.getQuantity() == null ? null : item.getQuantity().intValue();
        return itemQty != null ? product + " [" + itemQty + "]" : product;
      }).filter(item -> !item.isBlank()).collect(Collectors.joining(", "));
      if (!summary.isBlank()) {
        return summary;
      }
    }
    return "-";
  }

}

