package com.example.ordermanager.payment.entity;

import com.example.ordermanager.company.entity.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "payment_month", nullable = false)
  private int paymentMonth;

  @Column(name = "payment_year", nullable = false)
  private int paymentYear;

  @Column(name = "payment_msg", columnDefinition = "TEXT")
  private String paymentMsg;

  @Column(name = "payment_ss_filename")
  private String paymentSsFilename;

  @Column(name = "payment_ss_data", columnDefinition = "BYTEA")
  private byte[] paymentSsData;

  @Column(name = "payment_ss_content_type", length = 100)
  private String paymentSsContentType;

  @Column(name = "created_at", nullable = false, updatable = false)
  @CreationTimestamp
  private LocalDateTime createdAt;
}
