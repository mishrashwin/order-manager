package com.example.ordermanager.product.entity;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String similarName;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String brand;

  private String category;

  private Double price;

  // New GST calculation approach fields
  @Column(name = "pre_gst_price", precision = 12, scale = 2)
  private java.math.BigDecimal preGstPrice;

  @Column(name = "gst_inclusive_price", precision = 12, scale = 2)
  private java.math.BigDecimal gstInclusivePrice;

  // HSN code and unit required for Vendor PO generation
  private String hsnCode;
  private String unit;

  @Column(name = "gst_percentage", precision = 5, scale = 2)
  private java.math.BigDecimal gstPercentage;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "vendor_id")
  private Vendor vendor;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  // GST calculation methods
  public void calculateGstInclusivePrice() {
    if (preGstPrice != null && gstPercentage != null
        && gstPercentage.compareTo(java.math.BigDecimal.ZERO) > 0) {
      java.math.BigDecimal gstMultiplier = gstPercentage.divide(java.math.BigDecimal.valueOf(100),
          2, java.math.RoundingMode.HALF_UP);
      gstInclusivePrice = preGstPrice.multiply(java.math.BigDecimal.ONE.add(gstMultiplier))
          .setScale(2, java.math.RoundingMode.HALF_UP);
      // Update legacy price field for backward compatibility
      this.price = gstInclusivePrice.doubleValue();
    } else if (preGstPrice != null) {
      gstInclusivePrice = preGstPrice;
      this.price = preGstPrice.doubleValue();
    }
  }

  public void setPreGstPrice(java.math.BigDecimal preGstPrice) {
    this.preGstPrice = preGstPrice;
    calculateGstInclusivePrice();
  }

  public void setGstPercentage(java.math.BigDecimal gstPercentage) {
    this.gstPercentage = gstPercentage;
    calculateGstInclusivePrice();
  }

  // Constructors
  public Product() {}

  public Product(String name, String similarName, String description, String brand, String category,
      Double price) {
    this.name = name;
    this.similarName = similarName;
    this.description = description;
    this.brand = brand;
    this.category = category;
    this.price = price;
  }
}
