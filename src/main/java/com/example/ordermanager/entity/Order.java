package com.example.ordermanager.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String productName;
    private Integer quantity;
    private Double totalAmount;
    private String status;

    private LocalDate orderDate;
    private LocalDate deliveryDate;

    // Constructors
    public Order() {}

    public Order(String customerName, String productName, Integer quantity, Double totalAmount, String status, LocalDate orderDate, LocalDate deliveryDate) {
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.deliveryDate = deliveryDate;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }
}
