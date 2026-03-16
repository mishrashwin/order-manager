package com.example.ordermanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

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

  // Legacy field - kept for backward compatibility
  // This is now auto-populated from client.name
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

  /**
   * Helper method to get customer name from client relationship Falls back to customerName field if
   * client is not set
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

}
