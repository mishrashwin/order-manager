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

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "vendor_id")
  private Vendor vendor;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

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
