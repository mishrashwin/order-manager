package com.example.ordermanager.vendor.entity;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "vendor_purchase_order")
@Getter
@Setter
@ToString(exclude = {"items", "company", "vendor"})
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

  private Double totalAmount = 0.0;

  @Enumerated(EnumType.STRING)
  private VendorPoStatus status;

  private String createdBy;

  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "vendorPo", cascade = CascadeType.ALL, orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<VendorPoItem> items = new ArrayList<>();

  public void addItem(VendorPoItem item) {
    item.setVendorPo(this);
    this.items.add(item);
    recalcTotal();
  }

  public void recalcTotal() {
    java.math.BigDecimal subtotal = java.math.BigDecimal.ZERO;
    for (VendorPoItem it : items) {
      if (it.getQuantity() != null && it.getPreGstUnitPrice() != null) {
        // Calculate GST-inclusive unit price from pre-GST price
        java.math.BigDecimal gstPercent =
            it.getGstPercentage() != null ? it.getGstPercentage() : java.math.BigDecimal.ZERO;

        java.math.BigDecimal gstMultiplier =
            gstPercent.divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal gstInclusiveUnitPrice =
            it.getPreGstUnitPrice().multiply(java.math.BigDecimal.ONE.add(gstMultiplier))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        it.setGstInclusiveUnitPrice(gstInclusiveUnitPrice);

        // Calculate line total using GST-inclusive price
        java.math.BigDecimal lineTotal =
            java.math.BigDecimal.valueOf(it.getQuantity()).multiply(gstInclusiveUnitPrice);
        subtotal = subtotal.add(lineTotal);

        // Calculate per-item GST amount
        java.math.BigDecimal gstAmount = lineTotal
            .subtract(
                it.getPreGstUnitPrice().multiply(java.math.BigDecimal.valueOf(it.getQuantity())))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        it.setGstAmount(gstAmount);

        // Update legacy unitPrice field for backward compatibility
        it.setUnitPrice(gstInclusiveUnitPrice.doubleValue());
      } else if (it.getQuantity() != null && it.getUnitPrice() != null) {
        // Fallback for legacy data (when preGstUnitPrice is null)
        java.math.BigDecimal lineTotal = java.math.BigDecimal.valueOf(it.getQuantity())
            .multiply(java.math.BigDecimal.valueOf(it.getUnitPrice()));
        subtotal = subtotal.add(lineTotal);

        // Calculate per-item GST amount
        java.math.BigDecimal gstPercent =
            it.getGstPercentage() != null ? it.getGstPercentage() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal gstAmount = lineTotal.multiply(gstPercent)
            .divide(java.math.BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        it.setGstAmount(gstAmount);
      }
    }
    this.totalAmount = subtotal.doubleValue();
  }

  public String getDisplayProductSummary() {
    if (items != null && !items.isEmpty()) {
      String summary = items.stream().filter(item -> item.getProductName() != null).map(item -> {
        String product = item.getProductName().trim();
        Integer itemQty = item.getQuantity().intValue();
        return itemQty != null ? product + " [" + itemQty + "]" : product;
      }).filter(item -> !item.isBlank()).collect(Collectors.joining(", "));
      if (!summary.isBlank()) {
        return summary;
      }
    }
    return "-";
  }

}

