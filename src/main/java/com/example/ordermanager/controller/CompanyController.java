package com.example.ordermanager.controller;

import com.example.ordermanager.dto.CompanyRegistrationDTO;
import com.example.ordermanager.entity.Company;
import com.example.ordermanager.service.CompanyService;
import com.example.ordermanager.utils.SecurityContextHelper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for company management. Handles company registration and admin operations.
 */
@Controller
@RequestMapping("/company")
public class CompanyController {

  private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;

  public CompanyController(CompanyService companyService,
      SecurityContextHelper securityContextHelper) {
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
  }

  /**
   * Show company registration form This is typically accessed during initial setup
   */
  @GetMapping("/register")
  public String showRegistrationForm(Model model) {
    model.addAttribute("registrationData", new CompanyRegistrationDTO());
    return "company/register";
  }

  /**
   * Register a new company with first admin user Collects: Company name, Owner details, username,
   * password Creates company and admin user in single transaction
   *
   * @Valid annotation ensures all validation constraints are checked before processing
   */
  @PostMapping("/register")
  public String registerCompany(
      @Valid @ModelAttribute("registrationData") CompanyRegistrationDTO registrationDTO,
      BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

    // Check if there are validation errors
    if (bindingResult.hasErrors()) {
      model.addAttribute("errors", bindingResult.getAllErrors());
      model.addAttribute("registrationData", registrationDTO);
      return "company/register";
    }

    try {
      // Validate that DTO fields are not null
      if (registrationDTO.getCompanyName() == null
          || registrationDTO.getCompanyName().trim().isEmpty()) {
        model.addAttribute("error", "Company name cannot be empty");
        model.addAttribute("registrationData", registrationDTO);
        return "company/register";
      }

      // Create Company entity from DTO
      Company company = new Company();
      company.setName(registrationDTO.getCompanyName().trim());

      // Create admin user with validated details
      com.example.ordermanager.user.entity.User adminUser =
          new com.example.ordermanager.user.entity.User();
      adminUser.setFirstName(registrationDTO.getOwnerFirstName().trim());
      adminUser.setLastName(registrationDTO.getOwnerLastName().trim());
      adminUser.setEmail(registrationDTO.getOwnerEmail().trim());
      adminUser.setMobileNumber(registrationDTO.getOwnerMobile().trim());
      adminUser.setUsername(registrationDTO.getUsername().trim());
      adminUser.setPassword(registrationDTO.getPassword());

      // Register company with admin user in one transaction
      Company savedCompany = companyService.registerCompanyWithAdmin(company, adminUser);

      // Success: redirect to login
      redirectAttributes.addFlashAttribute("message",
          "Company '" + savedCompany.getName()
              + "' registered successfully! Verification email sent to "
              + registrationDTO.getOwnerEmail()
              + ". Please verify your email and wait for owner approval before logging in.");
      return "redirect:/login?registered=true";

    } catch (IllegalArgumentException e) {
      model.addAttribute("error", e.getMessage());
      model.addAttribute("registrationData", registrationDTO);
      return "company/register";
    } catch (DataIntegrityViolationException e) {
      log.warn("Duplicate/constraint violation during company registration", e);
      model.addAttribute("error", getFriendlyRegistrationError(e));
      model.addAttribute("registrationData", registrationDTO);
      return "company/register";
    } catch (Exception e) {
      log.error("Error during company registration", e);
      model.addAttribute("error",
          "We could not complete registration right now. Please verify your details and try again.");
      model.addAttribute("registrationData", registrationDTO);
      return "company/register";
    }
  }

  private String getFriendlyRegistrationError(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage()
        : ex.getMessage();
    if (message == null) {
      return "Duplicate value found. Please use different registration details.";
    }

    String lowered = message.toLowerCase();
    if (lowered.contains("mobile_number") || lowered.contains("users_mobile_number_key")) {
      return "Mobile number is already registered. Please use a different mobile number.";
    }
    if (lowered.contains("username") || lowered.contains("users_username_key")) {
      return "Username already exists. Please choose another username.";
    }
    if (lowered.contains("email") || lowered.contains("users_email_key")) {
      return "Email is already registered. Please use a different email.";
    }
    if (lowered.contains("companies_name_key") || lowered.contains("companies.name")) {
      return "Company name already exists. Please use a different company name.";
    }
    return "Duplicate value found. Please use different registration details.";
  }

  /**
   * Get current user's company details Authenticated users can view their company
   */
  @GetMapping
  public String getCompanyDetails(Model model) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      Company company = companyService.getCompanyById(companyId)
          .orElseThrow(() -> new RuntimeException("Company not found"));

      model.addAttribute("company", company);
      return "company/details";

    } catch (IllegalStateException e) {
      return "redirect:/login";
    }
  }

  /**
   * Admin only: View all companies Typically restricted to super-admin role
   */
  @GetMapping("/admin/all")
  @PreAuthorize("hasRole('OWNER')")
  public String getAllCompanies(Model model) {
    model.addAttribute("companies", companyService.getAllCompanies());
    return "company/all-companies";
  }

  /**
   * Admin only: Deactivate a company
   */
  @PostMapping("/{id}/deactivate")
  @PreAuthorize("hasRole('OWNER')")
  public String deactivateCompany(@PathVariable Long id) {
    try {
      companyService.deactivateCompany(id);
      return "redirect:/company/admin/all?message=Company deactivated";
    } catch (IllegalArgumentException e) {
      return "redirect:/company/admin/all?error=" + e.getMessage();
    }
  }

  /**
   * Admin only: Activate a company
   */
  @PostMapping("/{id}/activate")
  @PreAuthorize("hasRole('OWNER')")
  public String activateCompany(@PathVariable Long id) {
    try {
      companyService.activateCompany(id);
      return "redirect:/company/admin/all?message=Company activated";
    } catch (IllegalArgumentException e) {
      return "redirect:/company/admin/all?error=" + e.getMessage();
    }
  }
}

