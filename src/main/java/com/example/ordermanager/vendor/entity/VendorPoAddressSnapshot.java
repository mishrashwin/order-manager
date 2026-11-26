package com.example.ordermanager.vendor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable address values copied into a Vendor PO revision. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class VendorPoAddressSnapshot {

  @Column(length = 120)
  private String nickname;

  @Column(name = "address_line1", columnDefinition = "TEXT")
  private String addressLine1;

  @Column(name = "address_line2", columnDefinition = "TEXT")
  private String addressLine2;

  @Column(length = 120)
  private String city;

  @Column(length = 100)
  private String state;

  @Column(name = "state_code", length = 2)
  private String stateCode;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Column(length = 100)
  private String country;

  @Column(length = 50)
  private String gstin;

  @Column(name = "drug_license_number", length = 100)
  private String drugLicenseNumber;

  @Column(name = "contact_person_name", length = 120)
  private String contactPersonName;

  @Column(name = "contact_number", length = 20)
  private String contactNumber;

  @Column(name = "contact_email", length = 255)
  private String contactEmail;
}
