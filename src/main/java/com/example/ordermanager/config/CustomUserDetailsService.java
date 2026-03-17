package com.example.ordermanager.config;

import com.example.ordermanager.entity.CompanyApprovalStatus;
import com.example.ordermanager.service.CompanyService;
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
  private final PasswordEncoder passwordEncoder;
  private final String ownerUsername;
  private final String ownerPassword;
  private final UserDetails ownerUserDetails;

  public CustomUserDetailsService(UserRepository userRepository, CompanyService companyService,
      PasswordEncoder passwordEncoder, @Value("${app.owner.username}") String ownerUsername,
      @Value("${app.owner.password}") String ownerPassword) {
    this.userRepository = userRepository;
    this.companyService = companyService;
    this.passwordEncoder = passwordEncoder;
    this.ownerUsername = ownerUsername;
    this.ownerPassword = ownerPassword;

    String passwordForUserDetails;
    if (ownerPassword != null && ownerPassword.startsWith("{")) {
      // Assume already-encoded password in Spring's "{id}..." format.
      passwordForUserDetails = ownerPassword;
    } else {
      // Encode raw password once at startup.
      passwordForUserDetails = this.passwordEncoder.encode(ownerPassword);
    }

    this.ownerUserDetails = new org.springframework.security.core.userdetails.User(
        this.ownerUsername, passwordForUserDetails, true, true, true, true, List.of(
            new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OWNER")));
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    if (ownerUsername.equals(username)) {
      return ownerUserDetails;
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

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

    return new org.springframework.security.core.userdetails.User(user.getUsername(),
        user.getPassword(), user.isEnabled(), true, true, true,
        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
  }
}
