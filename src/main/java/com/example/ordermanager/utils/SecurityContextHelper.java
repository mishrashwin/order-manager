package com.example.ordermanager.utils;

import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility to extract tenant (company) information from authenticated user context. CRITICAL: All
 * service methods should use this to ensure tenant isolation.
 */
@Component
public class SecurityContextHelper {

  private final UserRepository userRepository;

  public SecurityContextHelper(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Extract the company ID from the currently authenticated user. This is the ONLY safe way to
   * determine which company's data a user can access.
   *
   * @return Company ID of the authenticated user
   * @throws IllegalStateException if user is not authenticated or company is null
   */
  public Long getCompanyIdFromContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("User is not authenticated");
    }

    String username = authentication.getName();
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

    if (user.getCompany() == null) {
      throw new IllegalStateException("User has no associated company");
    }

    return user.getCompany().getId();
  }

  /**
   * Extract the authenticated user from context
   *
   * @return User entity of the authenticated user
   * @throws IllegalStateException if user is not authenticated
   */
  public User getUserFromContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("User is not authenticated");
    }

    String username = authentication.getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));
  }

  /**
   * Extract the current username from context
   *
   * @return Username of the authenticated user
   * @throws IllegalStateException if user is not authenticated
   */
  public String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("User is not authenticated");
    }
    return authentication.getName();
  }
}

