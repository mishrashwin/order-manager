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

  public OrderItem() {}
}
