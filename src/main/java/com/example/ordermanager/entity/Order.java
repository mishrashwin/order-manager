package com.example.ordermanager.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String clientName;
    private String vendor;
    private String product;
    private String status;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
}
