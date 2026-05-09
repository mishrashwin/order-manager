package com.example.ordermanager.vendor.entity;

import com.example.ordermanager.product.entity.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
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

  private Double quantity = 0.0;

  private Double unitPrice = 0.0;

  // New GST calculation approach fields
  @Column(name = "pre_gst_unit_price", precision = 12, scale = 2)
  private java.math.BigDecimal preGstUnitPrice;

  @Column(name = "gst_inclusive_unit_price", precision = 12, scale = 2)
  private java.math.BigDecimal gstInclusiveUnitPrice;

  @Column(name = "line_total", insertable = false, updatable = false)
  private Double lineTotal;

  @Transient
  private BigDecimal taxableRate;

  @Transient
  private String formattedTaxableRate;

  @Transient
  private String formattedGstAmount;

  @Transient
  private String formattedLineTotal;

}

