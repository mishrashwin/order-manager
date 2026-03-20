package com.example.ordermanager.user.entity;

import com.example.ordermanager.company.entity.Company;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "First name cannot be empty")
  @Column(nullable = false)
  private String firstName;

  @NotBlank(message = "Last name cannot be empty")
  @Column(nullable = false)
  private String lastName;

  @NotBlank(message = "Username cannot be empty")
  @Column(nullable = false, unique = true)
  private String username;

  @NotBlank(message = "Email cannot be empty")
  @Email(message = "Invalid email format")
  @Column(nullable = false, unique = true)
  private String email;

  @NotBlank(message = "Mobile number cannot be empty")
  @Pattern(regexp = "^\\+[1-9][0-9]{6,14}$",
      message = "Mobile number must be in international format (e.g. +919876543210)")
  @Column(nullable = false, unique = true, length = 20)
  private String mobileNumber;

  @NotBlank(message = "Password cannot be empty")
  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private String role = "USER";

  @Column(nullable = false)
  private boolean enabled = false;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;
}
