package com.example.ordermanager.company.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Company Entity - Represents a tenant in the multi-tenant system. All business data must be
 * associated with a Company.
 */
@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Company {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Company name cannot be empty")
  @Column(nullable = false, unique = true)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String bio;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private boolean active = true;

  @Enumerated(EnumType.STRING)
  @Column(name = "approval_status", nullable = false)
  private CompanyApprovalStatus approvalStatus = CompanyApprovalStatus.PENDING;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Column(name = "approved_by", length = 100)
  private String approvedBy;

  @Column(name = "monthly_fee", precision = 10, scale = 2)
  private BigDecimal monthlyFee;

  // Constructor with name
  public Company(String name) {
    this.name = name;
    this.active = true;
    this.approvalStatus = CompanyApprovalStatus.PENDING;
  }
}

