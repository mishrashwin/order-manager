package com.example.ordermanager.company.entity;

import com.example.ordermanager.utils.IndianState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A tenant-owned address and its roles as a Company Bill To and Ship To location. */
@Entity
@Table(name = "company_addresses")
@Getter
@Setter
@NoArgsConstructor
public class CompanyAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(nullable = false, length = 120)
  private String nickname;

  @Column(name = "address_line1", columnDefinition = "TEXT")
  private String addressLine1;

  @Column(name = "address_line2", columnDefinition = "TEXT")
  private String addressLine2;

  @Column(length = 120)
  private String city;

  @Column(length = 100)
  @Enumerated(EnumType.STRING)
  private IndianState state;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Column(nullable = false, length = 100)
  private String country = "India";

  @Column(name = "gstin", length = 50)
  private String gstin;

  @Column(name = "drug_license_number", length = 100)
  private String drugLicenseNumber;

  @Column(name = "contact_person_name", length = 120)
  private String contactPersonName;

  @Column(name = "contact_number", length = 20)
  private String contactNumber;

  @Column(name = "contact_email", length = 255)
  private String contactEmail;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "default_bill_to", nullable = false)
  private boolean defaultBillTo;

  @Column(name = "default_ship_to", nullable = false)
  private boolean defaultShipTo;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** Returns the GST state code derived from the selected Indian state. */
  @Transient
  public String getStateCode() {
    return state == null ? null : state.getCode();
  }
}
