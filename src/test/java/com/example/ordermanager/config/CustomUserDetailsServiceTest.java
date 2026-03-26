package com.example.ordermanager.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private CompanyService companyService;
  @Mock
  private PasswordEncoder passwordEncoder;

  private CustomUserDetailsService customUserDetailsService;

  @BeforeEach
  void setUp() {
    customUserDetailsService = new CustomUserDetailsService(userRepository, companyService,
        passwordEncoder, "owner", "$2a$owner-hash", "owner@example.com");
  }

  @Test
  void loadUserByUsername_whenAccountInactive_throwsAdminContactMessage() {
    Company company = new Company();
    company.setApprovalStatus(CompanyApprovalStatus.APPROVED);
    company.setActive(true);

    User user = new User();
    user.setUsername("tenant-user");
    user.setPassword("encoded-pass");
    user.setCompany(company);
    user.setEnabled(true);
    user.setAccountActive(false);

    when(userRepository.findByUsername("tenant-user")).thenReturn(Optional.of(user));
    when(companyService.canUsersLogin(company)).thenReturn(true);

    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("tenant-user"))
        .isInstanceOf(DisabledException.class)
        .hasMessage("User is inactive. Contact Admin for account activation.");
  }

  @Test
  void loadUserByUsername_whenUnverifiedButActive_throwsVerificationMessage() {
    Company company = new Company();
    company.setApprovalStatus(CompanyApprovalStatus.APPROVED);
    company.setActive(true);

    User user = new User();
    user.setUsername("tenant-user");
    user.setPassword("encoded-pass");
    user.setCompany(company);
    user.setEnabled(false);
    user.setAccountActive(true);

    when(userRepository.findByUsername("tenant-user")).thenReturn(Optional.of(user));
    when(companyService.canUsersLogin(company)).thenReturn(true);

    assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("tenant-user"))
        .isInstanceOf(DisabledException.class).hasMessage("User is not verified.");
  }

  @Test
  void loadUserByUsername_whenVerifiedAndActive_returnsUserDetails() {
    Company company = new Company();
    company.setApprovalStatus(CompanyApprovalStatus.APPROVED);
    company.setActive(true);

    User user = new User();
    user.setUsername("tenant-admin");
    user.setPassword("encoded-pass");
    user.setRole("ADMIN");
    user.setCompany(company);
    user.setEnabled(true);
    user.setAccountActive(true);

    when(userRepository.findByUsername("tenant-admin")).thenReturn(Optional.of(user));
    when(companyService.canUsersLogin(company)).thenReturn(true);

    UserDetails userDetails = customUserDetailsService.loadUserByUsername("tenant-admin");

    assertThat(userDetails.getUsername()).isEqualTo("tenant-admin");
    assertThat(userDetails.getPassword()).isEqualTo("encoded-pass");
    assertThat(userDetails.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
  }
}

