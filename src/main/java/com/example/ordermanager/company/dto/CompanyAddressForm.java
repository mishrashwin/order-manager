package com.example.ordermanager.company.dto;

import com.example.ordermanager.utils.IndianState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Form data for creating or editing a Company Address without binding tenant or default flags. */
@Getter
@Setter
@NoArgsConstructor
public class CompanyAddressForm {

  @NotBlank(message = "Address nickname is required")
  @Size(max = 120, message = "Nickname must be 120 characters or fewer")
  private String nickname;

  @NotBlank(message = "Address line 1 is required")
  @Size(max = 1000, message = "Address line 1 must be 1000 characters or fewer")
  private String addressLine1;

  @Size(max = 1000, message = "Address line 2 must be 1000 characters or fewer")
  private String addressLine2;

  @NotBlank(message = "City is required")
  @Size(max = 120, message = "City must be 120 characters or fewer")
  private String city;

  @NotNull(message = "State is required")
  private IndianState state;

  @NotBlank(message = "PIN/postal code is required")
  @Pattern(regexp = "[A-Za-z0-9 -]{3,20}", message = "Enter a valid PIN/postal code")
  private String postalCode;

  @NotBlank(message = "Country is required")
  @Size(max = 100, message = "Country must be 100 characters or fewer")
  private String country = "India";

  @Size(max = 50, message = "GSTIN must be 50 characters or fewer")
  private String gstin;

  @Size(max = 100, message = "Drug License Number must be 100 characters or fewer")
  private String drugLicenseNumber;

  @Size(max = 120, message = "Contact person name must be 120 characters or fewer")
  private String contactPersonName;

  @Size(max = 20, message = "Contact number must be 20 characters or fewer")
  private String contactNumber;

  @Size(max = 255, message = "Contact email must be 255 characters or fewer")
  private String contactEmail;
}
