package com.example.ordermanager.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private RegistrationService registrationService;
  @Mock
  private CompanyRepository companyRepository;

  private UserService userService;

  @BeforeEach
  void setUp() {
    userService = new UserService(userRepository, registrationService, companyRepository);
  }

  @Test
  void createUserByAdmin_setsCompanyHashesPasswordAndSendsVerification() {
    Company company = new Company();
    company.setId(11L);
    company.setName("ACME");

    User user = new User();
    user.setUsername("manager1");
    user.setEmail("manager1@acme.com");
    user.setPassword("RawPass123");

    when(companyRepository.findById(11L)).thenReturn(Optional.of(company));
    when(userRepository.findByUsername("manager1")).thenReturn(Optional.empty());
    when(userRepository.findByEmail("manager1@acme.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User toSave = invocation.getArgument(0);
      toSave.setId(100L);
      return toSave;
    });

    User created = userService.createUserByAdmin(user, 11L);

    assertThat(created.getId()).isEqualTo(100L);
    assertThat(created.getCompany().getId()).isEqualTo(11L);
    assertThat(created.isEnabled()).isFalse();
    assertThat(created.getPassword()).isNotEqualTo("RawPass123");
    verify(registrationService).sendVerificationEmail(created);
  }

  @Test
  void createUserByAdmin_rejectsDuplicateUsername() {
    Company company = new Company();
    company.setId(11L);

    User user = new User();
    user.setUsername("manager1");
    user.setEmail("manager1@acme.com");

    when(companyRepository.findById(11L)).thenReturn(Optional.of(company));
    when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(new User()));

    assertThatThrownBy(() -> userService.createUserByAdmin(user, 11L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Username already exists");

    verify(userRepository, never()).save(any(User.class));
    verify(registrationService, never()).sendVerificationEmail(any(User.class));
  }

  @Test
  void updateUserByAdmin_whenEmailChanges_disablesUserAndResendsVerification() {
    Company company = new Company();
    company.setId(11L);

    User existing = new User();
    existing.setId(101L);
    existing.setCompany(company);
    existing.setUsername("manager1");
    existing.setEmail("old@acme.com");
    existing.setFirstName("Old");
    existing.setEnabled(true);

    User update = new User();
    update.setUsername("manager1-new");
    update.setEmail("new@acme.com");
    update.setFirstName("New");
    update.setRole("MANAGER");

    when(userRepository.findById(101L)).thenReturn(Optional.of(existing));
    when(userRepository.findByEmail("new@acme.com")).thenReturn(Optional.empty());
    when(userRepository.findByUsername("manager1-new")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    User updated = userService.updateUserByAdmin(101L, update, 11L);

    assertThat(updated.getEmail()).isEqualTo("new@acme.com");
    assertThat(updated.getUsername()).isEqualTo("manager1-new");
    assertThat(updated.isEnabled()).isFalse();
    verify(registrationService).sendVerificationEmail(updated);
  }

  @Test
  void updateUserByAdmin_rejectsBlankEmail() {
    Company company = new Company();
    company.setId(11L);

    User existing = new User();
    existing.setId(101L);
    existing.setCompany(company);
    existing.setUsername("manager1");
    existing.setEmail("old@acme.com");

    User update = new User();
    update.setUsername("manager1");
    update.setEmail("   ");

    when(userRepository.findById(101L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> userService.updateUserByAdmin(101L, update, 11L))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Email is required");
  }
}

