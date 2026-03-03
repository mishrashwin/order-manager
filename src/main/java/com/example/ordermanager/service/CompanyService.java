package com.example.ordermanager.service;

import com.example.ordermanager.entity.Company;
import com.example.ordermanager.repository.CompanyRepository;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.RegistrationService;
import com.example.ordermanager.user.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing companies (tenants) in the multi-tenant system.
 */
@Service
public class CompanyService {

  private final CompanyRepository companyRepository;
  private final UserService userService;
  private final RegistrationService registrationService;
  private final BCryptPasswordEncoder passwordEncoder;

  public CompanyService(CompanyRepository companyRepository, UserService userService,
      RegistrationService registrationService) {
    this.companyRepository = companyRepository;
    this.userService = userService;
    this.registrationService = registrationService;
    this.passwordEncoder = new BCryptPasswordEncoder();
  }

  /**
   * Register a new company (tenant). This creates the company entry in the system. The first admin
   * user will then be assigned to this company.
   *
   * @param company Company object with at least name field
   * @return Created Company entity
   */
  public Company registerCompany(Company company) {
    // Validate company name is unique
    if (companyRepository.findByName(company.getName()).isPresent()) {
      throw new IllegalArgumentException(
          "Company with name '" + company.getName() + "' already exists");
    }

    // Set default values
    if (company.isActive() == false) {
      company.setActive(true);
    }

    return companyRepository.save(company);
  }

  /**
   * Register new company with first admin user (owner) in a single transaction. Creates company and
   * automatically creates admin user with hashed password. Admin user receives verification email.
   *
   * @param company Company with name
   * @param adminUser First admin user (username, email, password will be hashed)
   * @return Registered company
   */
  @Transactional
  public Company registerCompanyWithAdmin(Company company, User adminUser) {
    // Step 1: Validate company name is unique
    if (companyRepository.findByName(company.getName()).isPresent()) {
      throw new IllegalArgumentException(
          "Company with name '" + company.getName() + "' already exists");
    }

    // Step 2: Validate admin user details
    if (userService.findByUsername(adminUser.getUsername()) != null) {
      throw new IllegalArgumentException(
          "Username '" + adminUser.getUsername() + "' already exists!");
    }

    if (userService.findByEmail(adminUser.getEmail()) != null) {
      throw new IllegalArgumentException(
          "Email '" + adminUser.getEmail() + "' already registered!");
    }

    // Step 3: Create and save company FIRST
    company.setActive(true);
    Company savedCompany = companyRepository.save(company);

    // Step 4: Prepare admin user
    adminUser.setCompany(savedCompany); // Assign to company (MUST be set BEFORE saving)
    adminUser.setRole("ADMIN"); // First user is admin
    adminUser.setEnabled(false); // Requires email verification
    adminUser.setPassword(passwordEncoder.encode(adminUser.getPassword())); // Hash password

    // Step 5: Save user with company and send verification email
    // Uses saveUserWithCompany to avoid double-hashing password
    userService.saveUserWithCompany(adminUser);

    return savedCompany;
  }

  /**
   * Get company by ID
   */
  public Optional<Company> getCompanyById(Long id) {
    return companyRepository.findById(id);
  }

  /**
   * Get company by name
   */
  public Optional<Company> getCompanyByName(String name) {
    return companyRepository.findByName(name);
  }

  /**
   * Get all companies (ADMIN only)
   */
  public List<Company> getAllCompanies() {
    return companyRepository.findAll();
  }

  /**
   * Deactivate a company (soft delete) This prevents new logins but preserves data
   */
  public Company deactivateCompany(Long companyId) {
    Company company = companyRepository.findById(companyId).orElseThrow(
        () -> new IllegalArgumentException("Company with ID " + companyId + " not found"));

    company.setActive(false);
    return companyRepository.save(company);
  }

  /**
   * Activate a company
   */
  public Company activateCompany(Long companyId) {
    Company company = companyRepository.findById(companyId).orElseThrow(
        () -> new IllegalArgumentException("Company with ID " + companyId + " not found"));

    company.setActive(true);
    return companyRepository.save(company);
  }

  /**
   * Update company details
   */
  public Company updateCompany(Long companyId, Company updatedCompany) {
    Company company = companyRepository.findById(companyId).orElseThrow(
        () -> new IllegalArgumentException("Company with ID " + companyId + " not found"));

    if (updatedCompany.getName() != null && !updatedCompany.getName().isEmpty()) {
      // Check if new name is already taken
      Optional<Company> existingCompany = companyRepository.findByName(updatedCompany.getName());
      if (existingCompany.isPresent() && !existingCompany.get().getId().equals(companyId)) {
        throw new IllegalArgumentException(
            "Company with name '" + updatedCompany.getName() + "' already exists");
      }
      company.setName(updatedCompany.getName());
    }

    // Update bio (can be null or empty)
    company.setBio(updatedCompany.getBio());

    return companyRepository.save(company);
  }
}
