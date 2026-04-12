package com.example.ordermanager.order.entity;

import com.example.ordermanager.client.entity.Client;
import com.example.ordermanager.company.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Client relationship for data integrity
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "client_id")
  private Client client;

  // Compatibility-only field for transient binding/serialization; no longer persisted.
  @Transient
  private String customerName;

  private String productName;
  private Integer quantity;
  private Double totalAmount;
  @Column(length = 50)
  private String poOrderNo;
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OrderStatus status = OrderStatus.CREATED;
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate orderDate;
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private LocalDate deliveryDate;
  @Column(columnDefinition = "TEXT")
  private String orderNote;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<OrderItem> orderItems = new ArrayList<>();

  /**
   * Helper method to get the customer name from the client relationship. Falls back to the
   * customerName field if the client is not set.
   */
  public String getCustomerName() {
    if (client != null) {
      return client.getName();
    }
    return customerName;
  }

  /**
   * Helper method to set client and sync customerName
   */
  public void setClient(Client client) {
    this.client = client;
    if (client != null) {
      this.customerName = client.getName();
    }
  }

  // Constructors
  public Order() {}

  public Order(String customerName, String productName, Integer quantity, Double totalAmount,
      OrderStatus status, LocalDate orderDate, LocalDate deliveryDate) {
    this.customerName = customerName;
    this.productName = productName;
    this.quantity = quantity;
    this.totalAmount = totalAmount;
    this.status = status;
    this.orderDate = orderDate;
    this.deliveryDate = deliveryDate;
  }

  public Order(String customerName, String productName, Integer quantity, Double totalAmount,
      String poOrderNo, OrderStatus status, LocalDate orderDate, LocalDate deliveryDate,
      String orderNote) {
    this.customerName = customerName;
    this.productName = productName;
    this.quantity = quantity;
    this.totalAmount = totalAmount;
    this.poOrderNo = poOrderNo;
    this.status = status;
    this.orderDate = orderDate;
    this.deliveryDate = deliveryDate;
    this.orderNote = orderNote;
  }

  public String getDisplayProductSummary() {
    if (orderItems != null && !orderItems.isEmpty()) {
      String summary =
          orderItems.stream().filter(item -> item.getProductName() != null).map(item -> {
            String product = item.getProductName().trim();
            Integer itemQty = item.getQuantity();
            return itemQty != null ? product + " [" + itemQty + "]" : product;
          }).filter(item -> !item.isBlank()).collect(Collectors.joining(", "));
      if (!summary.isBlank()) {
        return summary;
      }
    }

    if (productName == null || productName.isBlank()) {
      return "-";
    }
    return quantity != null ? productName + " [" + quantity + "]" : productName;
  }

  public String getDisplayQuantitySummary() {
    if (orderItems != null && !orderItems.isEmpty()) {
      String summary = orderItems.stream().map(item -> {
        String product = item.getProductName() != null ? item.getProductName().trim() : "Product";
        Integer itemQty = item.getQuantity();
        return itemQty != null ? product + " [" + itemQty + "]" : product;
      }).filter(item -> !item.isBlank()).collect(Collectors.joining(", "));
      if (!summary.isBlank()) {
        return summary;
      }
    }

    if (quantity == null) {
      return "-";
    }
    return quantity.toString();
  }

}
