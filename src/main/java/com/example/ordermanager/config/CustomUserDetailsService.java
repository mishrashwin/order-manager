package com.example.ordermanager.config;

import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final CompanyService companyService;
  private final String ownerUsername;
  private final String ownerEmail;
  // Store the encoded password as an immutable String instead of a mutable UserDetails.
  // Spring Security's User implements CredentialsContainer — eraseCredentials() sets
  // password = null after first successful auth, breaking all subsequent owner logins.
  private final String ownerEncodedPassword;

  public CustomUserDetailsService(UserRepository userRepository, CompanyService companyService,
      PasswordEncoder passwordEncoder, @Value("${app.owner.username}") String ownerUsername,
      @Value("${app.owner.password}") String ownerPassword,
      @Value("${app.owner.email:}") String ownerEmail) {
    this.userRepository = userRepository;
    this.companyService = companyService;
    this.ownerUsername = ownerUsername != null ? ownerUsername.trim() : null;
    this.ownerEmail = ownerEmail != null ? ownerEmail.trim() : null;

    if (this.ownerUsername != null && !this.ownerUsername.isBlank() && ownerPassword != null
        && !ownerPassword.isBlank()) {
      String rawOwnerPassword = ownerPassword.trim();

      if (rawOwnerPassword.startsWith("$2a$") || rawOwnerPassword.startsWith("$2b$")
          || rawOwnerPassword.startsWith("$2y$")) {
        // Already a BCrypt hash (without delegating prefix).
        this.ownerEncodedPassword = rawOwnerPassword;
      } else if (rawOwnerPassword.startsWith("{")) {
        int endIdx = rawOwnerPassword.indexOf('}');
        if (endIdx > 1) {
          String id = rawOwnerPassword.substring(1, endIdx);
          String value = rawOwnerPassword.substring(endIdx + 1).trim();
          if ("bcrypt".equalsIgnoreCase(id) && !value.isBlank()) {
            this.ownerEncodedPassword = value;
          } else if ("bcrypt".equalsIgnoreCase(id)) {
            // {bcrypt} with empty hash — invalid config, disable owner login.
            this.ownerEncodedPassword = null;
          } else {
            this.ownerEncodedPassword = passwordEncoder.encode(value);
          }
        } else {
          this.ownerEncodedPassword = passwordEncoder.encode(rawOwnerPassword);
        }
      } else {
        // Encode raw password once at startup.
        this.ownerEncodedPassword = passwordEncoder.encode(rawOwnerPassword);
      }
    } else {
      // Owner credentials not configured; disable owner login.
      this.ownerEncodedPassword = null;
    }
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    String normalizedUsername = username != null ? username.trim() : null;

    boolean ownerUsernameMatch = ownerUsername != null && normalizedUsername != null
        && ownerUsername.equalsIgnoreCase(normalizedUsername);
    boolean ownerEmailMatch = ownerEmail != null && !ownerEmail.isBlank()
        && normalizedUsername != null && ownerEmail.equalsIgnoreCase(normalizedUsername);

    if (ownerEncodedPassword != null && (ownerUsernameMatch || ownerEmailMatch)) {
      // Return a fresh UserDetails each time so eraseCredentials() after successful
      // authentication does not wipe the stored password for subsequent login attempts.
      return new org.springframework.security.core.userdetails.User(ownerUsername,
          ownerEncodedPassword, true, true, true, true,
          List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
    }

    User user = userRepository.findByUsername(normalizedUsername)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedUsername));

    if (user.getPassword() == null || user.getPassword().isBlank()) {
      throw new DisabledException(
          "Account credentials are unavailable. Please reset your password.");
    }

    if (user.getCompany() == null) {
      throw new DisabledException("Company assignment missing. Contact support.");
    }

    if (CompanyApprovalStatus.PENDING.equals(user.getCompany().getApprovalStatus())) {
      throw new DisabledException(
          "Your company registration is pending owner approval. Please wait for activation.");
    }

    if (CompanyApprovalStatus.REJECTED.equals(user.getCompany().getApprovalStatus())) {
      throw new DisabledException(
          "Your company registration was not approved. Contact support for assistance.");
    }

    if (!companyService.canUsersLogin(user.getCompany())) {
      throw new DisabledException(
          "Company access is currently paused due to account status. Please contact support.");
    }

    if (!user.isAccountActive()) {
      throw new DisabledException("User is inactive. Contact Admin for account activation.");
    }

    // Keep unverified-user message explicit so login page can show resend verification guidance.
    if (!user.isEnabled()) {
      throw new DisabledException("User is not verified.");
    }

    return new org.springframework.security.core.userdetails.User(user.getUsername(),
        user.getPassword(), true, true, true, true,
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
  }
}
