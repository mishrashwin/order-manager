package com.example.ordermanager.order.entity;

import com.example.ordermanager.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "product_id")
  private Product product;

  // Stored product name: synced from product.name or entered manually
  private String productName;

  private Integer quantity;

  // Unit price captured at time of order (from product.price or entered manually)
  private Double unitPrice;

  // New GST calculation approach fields
  @Column(name = "pre_gst_unit_price", precision = 12, scale = 2)
  private java.math.BigDecimal preGstUnitPrice;

  @Column(name = "gst_inclusive_unit_price", precision = 12, scale = 2)
  private java.math.BigDecimal gstInclusiveUnitPrice;

  @Column(name = "gst_amount", precision = 12, scale = 2)
  private java.math.BigDecimal gstAmount;

  // GST calculation methods
  public void calculateGstInclusivePrice(java.math.BigDecimal gstPercentage) {
    if (preGstUnitPrice != null && gstPercentage != null
        && gstPercentage.compareTo(java.math.BigDecimal.ZERO) > 0) {
      java.math.BigDecimal gstMultiplier = gstPercentage.divide(java.math.BigDecimal.valueOf(100),
          2, java.math.RoundingMode.HALF_UP);
      gstInclusiveUnitPrice = preGstUnitPrice.multiply(java.math.BigDecimal.ONE.add(gstMultiplier))
          .setScale(2, java.math.RoundingMode.HALF_UP);
      gstAmount = gstInclusiveUnitPrice.subtract(preGstUnitPrice).setScale(2,
          java.math.RoundingMode.HALF_UP);
      // Update legacy unitPrice field for backward compatibility
      this.unitPrice = gstInclusiveUnitPrice.doubleValue();
    } else if (preGstUnitPrice != null) {
      gstInclusiveUnitPrice = preGstUnitPrice;
      gstAmount = java.math.BigDecimal.ZERO;
      this.unitPrice = preGstUnitPrice.doubleValue();
    }
  }

  public void setPreGstUnitPrice(java.math.BigDecimal preGstUnitPrice) {
    this.preGstUnitPrice = preGstUnitPrice;
    // Will need GST percentage to calculate inclusive price
  }

  public OrderItem() {}
}
