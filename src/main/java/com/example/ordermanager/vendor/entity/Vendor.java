package com.example.ordermanager.vendor.entity;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.utils.IndianState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@ToString
public class Vendor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String companyName;
  private String contactPerson;
  private String email;
  private String phone;
  private String address;
  private String gstn;

  @Enumerated(EnumType.STRING)
  @Column(length = 100)
  private IndianState state;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  // Constructors
  public Vendor() {}

  public Vendor(String companyName, String contactPerson, String email, String phone,
      String address, String gstn, IndianState state) {
    this.companyName = companyName;
    this.contactPerson = contactPerson;
    this.email = email;
    this.phone = phone;
    this.address = address;
    this.gstn = gstn;
    this.state = state;
  }

  // Legacy constructor for backward compatibility
  public Vendor(String companyName, String contactPerson, String email, String phone,
      String address, String gstn) {
    this.companyName = companyName;
    this.contactPerson = contactPerson;
    this.email = email;
    this.phone = phone;
    this.address = address;
    this.gstn = gstn;
  }

}
