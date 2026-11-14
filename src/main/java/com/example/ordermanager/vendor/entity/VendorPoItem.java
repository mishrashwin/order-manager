package com.example.ordermanager.vendor.entity;

import com.example.ordermanager.product.entity.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "vendor_purchase_order_item")
@Getter
@Setter
@ToString(exclude = {"vendorPo", "product"})
public class VendorPoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vendor_po_id", nullable = false)
  private VendorPo vendorPo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", insertable = false, updatable = false)
  private Product product;

  @Column(name = "product_id")
  private Long productId;

  private String productName;

  private String hsnCode;

  private String unit;

  @Column(name = "gst_percentage", precision = 5, scale = 2)
  private java.math.BigDecimal gstPercentage;

  @Column(name = "gst_amount", precision = 12, scale = 2)
  private java.math.BigDecimal gstAmount;

  @Column(name = "quantity", precision = 12, scale = 3)
  private BigDecimal quantity = BigDecimal.ZERO.setScale(3);

  @Column(name = "unit_price", precision = 14, scale = 4)
  private BigDecimal unitPrice = BigDecimal.ZERO.setScale(4);

  // New GST calculation approach fields
  @Column(name = "pre_gst_unit_price", precision = 12, scale = 2)
  private java.math.BigDecimal preGstUnitPrice;

  @Column(name = "gst_inclusive_unit_price", precision = 12, scale = 2)
  private java.math.BigDecimal gstInclusiveUnitPrice;

  @Column(name = "line_total", precision = 14, scale = 2)
  private BigDecimal lineTotal = BigDecimal.ZERO.setScale(2);

  @Column(name = "pricing_unit", precision = 12, scale = 3, nullable = false)
  private BigDecimal pricingUnit = BigDecimal.ONE;

  @Column(name = "taxable_amount", precision = 14, scale = 2)
  private BigDecimal taxableAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "cgst_amount", precision = 14, scale = 2)
  private BigDecimal cgstAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "sgst_amount", precision = 14, scale = 2)
  private BigDecimal sgstAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "igst_amount", precision = 14, scale = 2)
  private BigDecimal igstAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "delivery_date")
  private LocalDate deliveryDate;

  @Column(name = "taxable_rate", precision = 14, scale = 4)
  private BigDecimal taxableRate;

  @Transient
  private String formattedTaxableRate;

  @Transient
  private String formattedGstAmount;

  @Transient
  private String formattedLineTotal;

  /** Converts legacy test and integration inputs into the BigDecimal money representation. */
  public void setUnitPrice(Double value) {
    this.unitPrice = value == null ? null
        : BigDecimal.valueOf(value).setScale(4, java.math.RoundingMode.HALF_UP);
  }

  /** Stores unit price using the persisted decimal precision. */
  public void setUnitPrice(BigDecimal value) {
    this.unitPrice = value;
  }

  /** Converts legacy test and PDF fixtures into the persisted decimal line-total representation. */
  public void setLineTotal(Double value) {
    this.lineTotal = value == null ? null
        : BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  /** Stores the calculated line total using the persisted decimal representation. */
  public void setLineTotal(BigDecimal value) {
    this.lineTotal = value;
  }

  /** Converts legacy floating-point callers to the persisted decimal quantity scale. */
  public void setQuantity(Double value) {
    this.quantity = value == null ? null
        : BigDecimal.valueOf(value).setScale(3, java.math.RoundingMode.HALF_UP);
  }

  /** Stores quantity directly using the decimal type declared by the database migration. */
  public void setQuantity(BigDecimal value) {
    this.quantity = value;
  }

}

