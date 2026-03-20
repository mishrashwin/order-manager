package com.example.ordermanager.company.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for company registration with admin user details. Includes validation constraints to prevent
 * data truncation errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRegistrationDTO {

  @NotBlank(message = "Company name is required")
  @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
  private String companyName;

  @NotBlank(message = "Owner first name is required")
  @Size(min = 1, max = 100, message = "First name must not exceed 100 characters")
  private String ownerFirstName;

  @NotBlank(message = "Owner last name is required")
  @Size(min = 1, max = 100, message = "Last name must not exceed 100 characters")
  private String ownerLastName;

  @NotBlank(message = "Owner email is required")
  @Email(message = "Invalid email format")
  @Size(min = 1, max = 100, message = "Email must not exceed 100 characters")
  private String ownerEmail;

  @NotBlank(message = "Owner mobile number is required")
  @Pattern(regexp = "^\\+[1-9][0-9]{6,14}$",
      message = "Mobile number must be in international format (e.g. +919876543210)")
  private String ownerMobile;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
  private String username;

  @NotBlank(message = "Password is required")
  @Size(min = 6, message = "Password must be at least 6 characters long")
  private String password;
}

