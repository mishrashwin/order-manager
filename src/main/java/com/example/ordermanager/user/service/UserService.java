package com.example.ordermanager.user.service;

import com.example.ordermanager.entity.Company;
import com.example.ordermanager.repository.CompanyRepository;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;
  private final RegistrationService registrationService;
  private final CompanyRepository companyRepository;

  public UserService(UserRepository userRepository, RegistrationService registrationService,
      CompanyRepository companyRepository) {
    this.userRepository = userRepository;
    this.passwordEncoder = new BCryptPasswordEncoder();
    this.registrationService = registrationService;
    this.companyRepository = companyRepository;
  }

  /**
   * Register a new user with hashed password and send verification email Used during signup process
   *
   * @param user User to register
   * @return Registered user
   */
  public User register(User user) {
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    user.setEnabled(false); // not verified yet
    User saved = userRepository.save(user);

    registrationService.sendVerificationEmail(saved);
    return saved;
  }

  /**
   * ADMIN PANEL: Create user by admin for their company Company is derived from admin's company
   * context Password is automatically hashed Verification email is sent
   *
   * @param user User to create
   * @param companyId Admin's company ID
   * @return Created user
   */
  public User createUserByAdmin(User user, Long companyId) {
    // Verify company exists
    Company company = companyRepository.findById(companyId).orElseThrow(
        () -> new IllegalArgumentException("Company with ID " + companyId + " not found"));

    // Check if username already exists
    if (userRepository.findByUsername(user.getUsername()).isPresent()) {
      throw new IllegalArgumentException("Username already exists!");
    }

    // Check if email already exists
    if (userRepository.findByEmail(user.getEmail()).isPresent()) {
      throw new IllegalArgumentException("Email already exists!");
    }

    // Set company and default values
    user.setCompany(company);
    user.setEnabled(false); // Email verification required
    user.setPassword(passwordEncoder.encode(user.getPassword()));

    // Save user
    User savedUser = userRepository.save(user);

    // Send verification email
    registrationService.sendVerificationEmail(savedUser);

    return savedUser;
  }

  /**
   * Get all users for a company (admin view)
   *
   * @param companyId Company ID
   * @return List of users in company
   */
  public List<User> getUsersByCompany(Long companyId) {
    return userRepository.findByCompanyId(companyId);
  }

  /**
   * Get user by ID and verify they belong to company
   *
   * @param userId User ID
   * @param companyId Company ID
   * @return User if found and belongs to company
   */
  public User getUserByIdAndCompany(Long userId, Long companyId) {
    return userRepository.findById(userId)
        .filter(user -> user.getCompany() != null && user.getCompany().getId().equals(companyId))
        .orElseThrow(() -> new IllegalArgumentException(
            "User not found or does not belong to your company"));
  }

  /**
   * Delete user by admin (with company verification) Rule: Cannot delete the last admin in the
   * company
   *
   * @param userId User to delete
   * @param companyId Admin's company ID
   */
  public void deleteUserByAdmin(Long userId, Long companyId) {
    User user = getUserByIdAndCompany(userId, companyId);

    // Prevent deleting the last admin in the company
    if ("ADMIN".equals(user.getRole())) {
      long adminCount = userRepository.countByCompanyIdAndRole(companyId, "ADMIN");
      if (adminCount <= 1) {
        throw new IllegalArgumentException(
            "Cannot delete the last admin in the company. At least one admin must exist.");
      }
    }

    userRepository.delete(user);
  }

  /**
   * Update user role by admin Rule: Cannot demote the last admin to a lower role Rule: Cannot have
   * more than 2 admins per company
   *
   * @param userId User to update
   * @param role New role
   * @param companyId Admin's company ID
   */
  public void updateUserRole(Long userId, String role, Long companyId) {
    User user = getUserByIdAndCompany(userId, companyId);
    String currentRole = user.getRole();

    // If promoting to ADMIN, check max 2 admins rule
    if ("ADMIN".equals(role) && !"ADMIN".equals(currentRole)) {
      long adminCount = userRepository.countByCompanyIdAndRole(companyId, "ADMIN");

      if (adminCount >= 2) {
        throw new IllegalArgumentException(
            "Cannot create more than 2 admins per company. Current admins: " + adminCount);
      }
    }

    // Prevent demoting the last admin to another role
    if ("ADMIN".equals(currentRole) && !role.equals("ADMIN")) {
      long adminCount = userRepository.countByCompanyIdAndRole(companyId, "ADMIN");

      if (adminCount <= 1) {
        throw new IllegalArgumentException(
            "Cannot change the role of the last admin in the company. At least one admin must exist.");
      }
    }

    user.setRole(role);
    userRepository.save(user);
  }

  /**
   * Check if user is the last admin in their company
   *
   * @param userId User ID to check
   * @param companyId Company ID
   * @return true if user is the last admin, false otherwise
   */
  public boolean isLastAdminInCompany(Long userId, Long companyId) {
    User user = getUserByIdAndCompany(userId, companyId);

    if (!"ADMIN".equals(user.getRole())) {
      return false;
    }

    long adminCount = userRepository.countByCompanyIdAndRole(companyId, "ADMIN");

    return adminCount <= 1;
  }

  /**
   * Get count of admins in a company
   *
   * @param companyId Company ID
   * @return Number of admins
   */
  public long getAdminCountInCompany(Long companyId) {
    return userRepository.countByCompanyIdAndRole(companyId, "ADMIN");
  }

  public boolean isCurrentUser(Long userId, String currentUsername) {
    User user = userRepository.findById(userId).orElse(null);
    return user != null && user.getUsername().equals(currentUsername);
  }

  /**
   * Save user that already has hashed password and company assigned. Used by company registration
   * to avoid double-hashing. Sends verification email.
   *
   * @param user User with password already hashed and company assigned
   * @return Saved user
   */
  public User saveUserWithCompany(User user) {
    // Save user (password is already hashed, company already set)
    User savedUser = userRepository.save(user);

    // Send verification email
    registrationService.sendVerificationEmail(savedUser);

    return savedUser;
  }

  /**
   * Find user by username
   */
  public User findByUsername(String username) {
    return userRepository.findByUsername(username).orElse(null);
  }

  /**
   * Find user by email
   */
  public User findByEmail(String email) {
    return userRepository.findByEmail(email).orElse(null);
  }
}
