package com.example.ordermanager.utils;

import com.example.ordermanager.config.CustomUserDetailsService;
import com.example.ordermanager.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service to verify the current authenticated user's password before performing sensitive
 * operations such as deletion. Supports both tenant users (from DB) and the owner (from
 * properties).
 */
@Service
public class PasswordVerificationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final CustomUserDetailsService customUserDetailsService;

  public PasswordVerificationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      CustomUserDetailsService customUserDetailsService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.customUserDetailsService = customUserDetailsService;
  }

  /**
   * Verifies the supplied raw password against the current authenticated user's stored (encoded)
   * password. Works for both tenant users and the owner account.
   *
   * @param submittedPassword the raw password entered by the user
   * @return {@code true} if the password matches; {@code false} otherwise
   */
  public boolean verifyCurrentUserPassword(String submittedPassword) {
    if (submittedPassword == null || submittedPassword.isBlank()) {
      return false;
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }

    String username = auth.getName();

    String ownerUsername = customUserDetailsService.getOwnerUsername();
    if (ownerUsername != null && ownerUsername.equalsIgnoreCase(username)) {
      String ownerEncodedPassword = customUserDetailsService.getOwnerEncodedPassword();
      if (ownerEncodedPassword == null) {
        return false;
      }
      return passwordEncoder.matches(submittedPassword, ownerEncodedPassword);
    }

    return userRepository.findByUsername(username)
        .map(user -> passwordEncoder.matches(submittedPassword, user.getPassword())).orElse(false);
  }
}
