package com.example.ordermanager.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.EmailService;
import com.example.ordermanager.user.service.UserService;
import java.math.BigDecimal;
import java.util.Optional;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

  @Mock
  private CompanyRepository companyRepository;
  @Mock
  private UserService userService;
  @Mock
  private EmailService emailService;

  private CompanyService companyService;

  @BeforeEach
  void setUp() {
    companyService = new CompanyService(companyRepository, userService, emailService,
        "owner@example.com", "http://localhost:8080");
  }

  @Test
  void registerCompanyWithAdmin_setsAdminDefaultsAndSendsOwnerNotification() {
    Company company = new Company();
    company.setName("ACME");

    User admin = new User();
    admin.setUsername("acme-admin");
    admin.setEmail("admin@acme.com");
    admin.setMobileNumber("+91 99999 99999");
    admin.setPassword("PlainPass123");

    when(companyRepository.findByName("ACME")).thenReturn(Optional.empty());
    when(userService.findByUsername("acme-admin")).thenReturn(null);
    when(userService.findByEmail("admin@acme.com")).thenReturn(null);
    when(userService.findByMobileNumber("919999999999")).thenReturn(null);
    when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
      Company toSave = invocation.getArgument(0);
      toSave.setId(1L);
      return toSave;
    });
    when(userService.saveUserWithCompany(any(User.class))).thenAnswer(invocation -> {
      User toSave = invocation.getArgument(0);
      toSave.setId(10L);
      return toSave;
    });

    Company savedCompany = companyService.registerCompanyWithAdmin(company, admin);

    assertThat(savedCompany.getId()).isEqualTo(1L);
    assertThat(savedCompany.isActive()).isTrue();
    assertThat(savedCompany.getApprovalStatus()).isEqualTo(CompanyApprovalStatus.PENDING);

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userService).saveUserWithCompany(userCaptor.capture());

    User savedAdmin = userCaptor.getValue();
    assertThat(savedAdmin.getCompany().getId()).isEqualTo(1L);
    assertThat(savedAdmin.getMobileNumber()).isEqualTo("919999999999");
    assertThat(savedAdmin.getRole()).isEqualTo("ADMIN");
    assertThat(savedAdmin.isEnabled()).isFalse();
    assertThat(savedAdmin.getPassword()).isNotEqualTo("PlainPass123");

    verify(emailService).sendNewCompanyRegistrationNotification(eq("owner@example.com"),
        eq(savedCompany), any(User.class), eq("http://localhost:8080/owner/companies"));
  }

  @Test
  void deactivateCompany_whenAlreadyInactive_doesNotSendNotificationAgain() {
    Company company = new Company();
    company.setId(7L);
    company.setName("Dormant Co");
    company.setActive(false);

    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Company result = companyService.deactivateCompany(7L);

    assertThat(result.isActive()).isFalse();
    verify(userService, never()).findPrimaryAdminByCompanyId(7L);
    verify(emailService, never()).sendCompanyAccessRevokedEmail(any(), any(), any(), any());
  }

  @Test
  void activateCompany_throwsWhenCompanyMissing() {
    when(companyRepository.findById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> companyService.activateCompany(404L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Company with ID 404 not found");
  }

  @Test
  void canUsersLogin_allowsOnlyApprovedAndActiveCompanies() {
    Company approvedActive = new Company();
    approvedActive.setActive(true);
    approvedActive.setApprovalStatus(CompanyApprovalStatus.APPROVED);

    Company pending = new Company();
    pending.setActive(true);
    pending.setApprovalStatus(CompanyApprovalStatus.PENDING);

    Company inactive = new Company();
    inactive.setActive(false);
    inactive.setApprovalStatus(CompanyApprovalStatus.APPROVED);

    assertThat(companyService.canUsersLogin(approvedActive)).isTrue();
    assertThat(companyService.canUsersLogin(pending)).isFalse();
    assertThat(companyService.canUsersLogin(inactive)).isFalse();
    assertThat(companyService.canUsersLogin(null)).isFalse();
  }

  @Test
  void setMonthlyFee_updatesCompanyFee() {
    Company company = new Company();
    company.setId(5L);
    company.setName("FEETEST");

    when(companyRepository.findById(5L)).thenReturn(Optional.of(company));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Company updated = companyService.setMonthlyFee(5L, new BigDecimal("1500.00"));

    assertThat(updated.getMonthlyFee()).isEqualByComparingTo("1500.00");
  }

  @Test
  void setMonthlyFee_clearsFeeWhenNullPassed() {
    Company company = new Company();
    company.setId(5L);
    company.setName("FEETEST");
    company.setMonthlyFee(new BigDecimal("500.00"));

    when(companyRepository.findById(5L)).thenReturn(Optional.of(company));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Company updated = companyService.setMonthlyFee(5L, null);

    assertThat(updated.getMonthlyFee()).isNull();
  }

  @Test
  void setMonthlyFee_throwsWhenCompanyNotFound() {
    when(companyRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> companyService.setMonthlyFee(99L, new BigDecimal("500.00")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Company with ID 99 not found");
  }

  @Test
  void updateCompany_shouldUpdateAllFieldsSuccessfully() {
    Company existingCompany = new Company();
    existingCompany.setId(5L);
    existingCompany.setName("Old Company Name");
    existingCompany.setBio("Old bio");
    existingCompany.setGstn("OldGSTN");

    Company updatedCompany = new Company();
    updatedCompany.setName("New Company Name");
    updatedCompany.setBio("New bio description");
    updatedCompany.setGstn("27AAAPL1234C1ZV");

    when(companyRepository.findById(5L)).thenReturn(Optional.of(existingCompany));
    when(companyRepository.findByName("New Company Name")).thenReturn(Optional.empty());
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Company result = companyService.updateCompany(5L, updatedCompany);

    assertThat(result.getName()).isEqualTo("New Company Name");
    assertThat(result.getBio()).isEqualTo("New bio description");
    assertThat(result.getGstn()).isEqualTo("27AAAPL1234C1ZV");
    verify(companyRepository).save(existingCompany);
  }

  @Test
  void updateCompany_shouldUpdateBioAndGstnOnly() {
    Company existingCompany = new Company();
    existingCompany.setId(5L);
    existingCompany.setName("Company Name");
    existingCompany.setBio("Old bio");
    existingCompany.setGstn("OldGSTN");

    Company updatedCompany = new Company();
    updatedCompany.setName(null); // No name change
    updatedCompany.setBio("Updated bio only");
    updatedCompany.setGstn("27AAAPL1234C1ZV");

    when(companyRepository.findById(5L)).thenReturn(Optional.of(existingCompany));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Company result = companyService.updateCompany(5L, updatedCompany);

    assertThat(result.getName()).isEqualTo("Company Name"); // Unchanged
    assertThat(result.getBio()).isEqualTo("Updated bio only");
    assertThat(result.getGstn()).isEqualTo("27AAAPL1234C1ZV");
    verify(companyRepository).save(existingCompany);
  }

  @Test
  void updateCompany_shouldHandleNullBioAndGstn() {
    Company existingCompany = new Company();
    existingCompany.setId(5L);
    existingCompany.setName("Company Name");
    existingCompany.setBio("Old bio");
    existingCompany.setGstn("OldGSTN");

    Company updatedCompany = new Company();
    updatedCompany.setName("Company Name");
    updatedCompany.setBio(null);
    updatedCompany.setGstn(null);

    when(companyRepository.findById(5L)).thenReturn(Optional.of(existingCompany));
    when(companyRepository.findByName("Company Name")).thenReturn(Optional.of(existingCompany));
    when(companyRepository.save(any(Company.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Company result = companyService.updateCompany(5L, updatedCompany);

    assertThat(result.getName()).isEqualTo("Company Name");
    assertThat(result.getBio()).isNull();
    assertThat(result.getGstn()).isNull();
    verify(companyRepository).save(existingCompany);
  }

  @Test
  void updateCompany_shouldThrowWhenCompanyNotFound() {
    Company updatedCompany = new Company();
    updatedCompany.setName("New Company Name");
    updatedCompany.setBio("New bio");
    updatedCompany.setGstn("27AAAPL1234C1ZV");

    when(companyRepository.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> companyService.updateCompany(5L, updatedCompany))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Company with ID 5 not found");
  }
}

