package com.example.ordermanager.company.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.EmailService;
import com.example.ordermanager.user.service.UserService;
import com.example.ordermanager.utils.PhoneNumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Service for managing companies (tenants) in the multi-tenant system.
 */
@Service
public class CompanyService {

  private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

  private final CompanyRepository companyRepository;
  private final UserService userService;
  private final EmailService emailService;
  private final BCryptPasswordEncoder passwordEncoder;
  private final String ownerEmail;
  private final String appBaseUrl;

  public CompanyService(CompanyRepository companyRepository, UserService userService,
      EmailService emailService, @Value("${app.owner.email}") String ownerEmail,
      @Value("${app.base.url}") String appBaseUrl) {
    this.companyRepository = companyRepository;
    this.userService = userService;
    this.emailService = emailService;
    this.ownerEmail = ownerEmail;
    this.appBaseUrl = appBaseUrl;
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
    company.setActive(true);
    company.setApprovalStatus(CompanyApprovalStatus.PENDING);
    company.setApprovedAt(null);
    company.setApprovedBy(null);

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
    String normalizedMobile = PhoneNumberUtils
        .normalizeRequiredInternational(adminUser.getMobileNumber(), "Owner mobile number");

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

    if (userService.findByMobileNumber(normalizedMobile) != null) {
      throw new IllegalArgumentException(
          "Mobile number '" + normalizedMobile + "' is already registered!");
    }

    // Step 3: Create and save company FIRST
    company.setActive(true);
    company.setApprovalStatus(CompanyApprovalStatus.PENDING);
    company.setApprovedAt(null);
    company.setApprovedBy(null);
    Company savedCompany = companyRepository.save(company);

    // Step 4: Prepare admin user
    adminUser.setCompany(savedCompany); // Assign to company (MUST be set BEFORE saving)
    adminUser.setMobileNumber(normalizedMobile);
    adminUser.setRole("ADMIN"); // First user is admin
    adminUser.setEnabled(false); // Requires email verification
    adminUser.setAccountActive(true);
    adminUser.setPassword(passwordEncoder.encode(adminUser.getPassword())); // Hash password

    // Step 5: Save user with company and send verification email
    // Uses saveUserWithCompany to avoid double-hashing password
    User savedAdminUser = userService.saveUserWithCompany(adminUser);

    notifyOwnerOfNewCompany(savedCompany, savedAdminUser);

    return savedCompany;
  }

  private void notifyOwnerOfNewCompany(Company company, User adminUser) {
    if (ownerEmail == null || ownerEmail.isBlank()) {
      log.warn("Owner email is not configured; skipping owner notification for company: {}",
          company.getName());
      return;
    }

    String reviewUrl = appBaseUrl + "/owner/companies";
    try {
      emailService.sendNewCompanyRegistrationNotification(ownerEmail, company, adminUser,
          reviewUrl);
    } catch (Exception ex) {
      log.warn("Company registration succeeded but owner notification failed for company: {}",
          company.getName(), ex);
    }
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

    boolean wasActive = company.isActive();
    company.setActive(false);
    Company savedCompany = companyRepository.save(company);
    if (wasActive) {
      notifyCompanyAccessRevoked(savedCompany);
    }
    return savedCompany;
  }

  /**
   * Activate a company
   */
  public Company activateCompany(Long companyId) {
    Company company = companyRepository.findById(companyId).orElseThrow(
        () -> new IllegalArgumentException("Company with ID " + companyId + " not found"));

    boolean wasActive = company.isActive();
    company.setActive(true);
    Company savedCompany = companyRepository.save(company);
    if (!wasActive) {
      notifyCompanyAccessRestored(savedCompany);
    }
    return savedCompany;
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

  public List<Company> getPendingCompanies() {
    return companyRepository
        .findByApprovalStatusOrderByCreatedAtDesc(CompanyApprovalStatus.PENDING);
  }

  public long getPendingCompanyCount() {
    return companyRepository.countByApprovalStatus(CompanyApprovalStatus.PENDING);
  }

  public Company approveCompany(Long companyId, String ownerUsername) {
    Company company = companyRepository.findById(companyId).orElseThrow(
        () -> new IllegalArgumentException("Company with ID " + companyId + " not found"));
    company.setApprovalStatus(CompanyApprovalStatus.APPROVED);
    company.setApprovedAt(LocalDateTime.now());
    company.setApprovedBy(ownerUsername);
    Company approvedCompany = companyRepository.save(company);
    notifyCompanyApproval(approvedCompany);
    return approvedCompany;
  }

  public Company rejectCompany(Long companyId, String ownerUsername) {
    Company company = companyRepository.findById(companyId).orElseThrow(
        () -> new IllegalArgumentException("Company with ID " + companyId + " not found"));
    company.setApprovalStatus(CompanyApprovalStatus.REJECTED);
    company.setApprovedAt(LocalDateTime.now());
    company.setApprovedBy(ownerUsername);
    company.setActive(false);
    Company rejectedCompany = companyRepository.save(company);
    notifyCompanyRejection(rejectedCompany);
    return rejectedCompany;
  }

  public boolean canUsersLogin(Company company) {
    return company != null && company.isActive()
        && CompanyApprovalStatus.APPROVED.equals(company.getApprovalStatus());
  }

  private void notifyCompanyApproval(Company company) {
    User primaryAdmin = resolvePrimaryAdmin(company, "approved");
    if (primaryAdmin == null) {
      return;
    }

    String loginUrl = appBaseUrl + "/login";
    try {
      emailService.sendCompanyApprovedWelcomeEmail(primaryAdmin.getEmail(), company, primaryAdmin,
          loginUrl);
    } catch (Exception ex) {
      log.warn("Company approved but welcome email failed for company: {}", company.getName(), ex);
    }
  }

  private void notifyCompanyRejection(Company company) {
    User primaryAdmin = resolvePrimaryAdmin(company, "rejected");
    if (primaryAdmin == null) {
      return;
    }

    try {
      emailService.sendCompanyRejectedEmail(primaryAdmin.getEmail(), company, primaryAdmin);
    } catch (Exception ex) {
      log.warn("Company rejected but notification email failed for company: {}", company.getName(),
          ex);
    }
  }

  private void notifyCompanyAccessRevoked(Company company) {
    User primaryAdmin = resolvePrimaryAdmin(company, "suspended");
    if (primaryAdmin == null) {
      return;
    }

    String loginUrl = appBaseUrl + "/login";
    try {
      emailService.sendCompanyAccessRevokedEmail(primaryAdmin.getEmail(), company, primaryAdmin,
          loginUrl);
    } catch (Exception ex) {
      log.warn("Company suspended but notification email failed for company: {}", company.getName(),
          ex);
    }
  }

  private void notifyCompanyAccessRestored(Company company) {
    User primaryAdmin = resolvePrimaryAdmin(company, "restored");
    if (primaryAdmin == null) {
      return;
    }

    String loginUrl = appBaseUrl + "/login";
    try {
      emailService.sendCompanyAccessRestoredEmail(primaryAdmin.getEmail(), company, primaryAdmin,
          loginUrl);
    } catch (Exception ex) {
      log.warn("Company restored but notification email failed for company: {}", company.getName(),
          ex);
    }
  }

  private User resolvePrimaryAdmin(Company company, String action) {
    User primaryAdmin = userService.findPrimaryAdminByCompanyId(company.getId());
    if (primaryAdmin == null || primaryAdmin.getEmail() == null
        || primaryAdmin.getEmail().isBlank()) {
      log.warn("No primary admin email found for {} company: {}", action, company.getName());
      return null;
    }
    return primaryAdmin;
  }
}
