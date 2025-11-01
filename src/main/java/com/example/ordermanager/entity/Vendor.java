package com.example.ordermanager.entity;

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

    // Constructors
    public Vendor() {}

    public Vendor(String companyName, String contactPerson, String email, String phone, String address) {
        this.companyName = companyName;
        this.contactPerson = contactPerson;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

}
