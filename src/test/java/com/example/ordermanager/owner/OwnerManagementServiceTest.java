package com.example.ordermanager.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.owner.dto.OwnerDashboardMetrics;
import com.example.ordermanager.owner.service.OwnerManagementService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.repository.CompanyUserCount;
import com.example.ordermanager.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerManagementServiceTest {

  @Mock
  private CompanyRepository companyRepository;

  @Mock
  private UserRepository userRepository;

  private OwnerManagementService ownerManagementService;

  @BeforeEach
  void setUp() {
    ownerManagementService = new OwnerManagementService(companyRepository, userRepository);
  }

  @Test
  void getDashboardMetrics_calculatesSuspendedFromTotalMinusActive() {
    when(companyRepository.count()).thenReturn(10L);
    when(companyRepository.countByApprovalStatus(CompanyApprovalStatus.APPROVED)).thenReturn(5L);
    when(companyRepository.countByApprovalStatus(CompanyApprovalStatus.PENDING)).thenReturn(3L);
    when(companyRepository.countByApprovalStatus(CompanyApprovalStatus.REJECTED)).thenReturn(2L);
    when(companyRepository.countByActive(true)).thenReturn(7L);

    OwnerDashboardMetrics metrics = ownerManagementService.getDashboardMetrics();

    assertThat(metrics.totalCompanies()).isEqualTo(10L);
    assertThat(metrics.activeCompanies()).isEqualTo(7L);
    assertThat(metrics.suspendedCompanies()).isEqualTo(3L);
  }

  @Test
  void getAllCompanySummaries_mergesDuplicateUserCountRowsSafely() {
    Company first = new Company();
    first.setId(1L);
    first.setName("ALPHA");
    first.setApprovalStatus(CompanyApprovalStatus.APPROVED);
    first.setActive(true);
    first.setCreatedAt(LocalDateTime.now().minusDays(1));

    Company second = new Company();
    second.setId(2L);
    second.setName("BETA");
    second.setApprovalStatus(CompanyApprovalStatus.PENDING);
    second.setActive(true);
    second.setCreatedAt(LocalDateTime.now().minusDays(2));

    when(companyRepository.findAll()).thenReturn(List.of(first, second));
    when(userRepository.countByCompanyIdInAndRole(List.of(1L, 2L), "ADMIN"))
        .thenReturn(List.of(countRow(1L, 1L), countRow(2L, 1L)));
    when(userRepository.countByCompanyIdInAndRole(List.of(1L, 2L), "USER"))
        .thenReturn(List.of(countRow(1L, 4L), countRow(2L, 0L)));

    var summaries = ownerManagementService.getAllCompanySummaries();

    assertThat(summaries).hasSize(2);
    assertThat(summaries.get(0).companyId()).isEqualTo(1L);
    assertThat(summaries.get(0).adminCount()).isEqualTo(1L);
    assertThat(summaries.get(0).userCount()).isEqualTo(4L);
    assertThat(summaries.get(1).companyId()).isEqualTo(2L);
    assertThat(summaries.get(1).adminCount()).isEqualTo(1L);
    assertThat(summaries.get(1).userCount()).isEqualTo(0L);
  }

  @Test
  void getPendingCompanySummaries_includesFirstAdminContactDetails() {
    Company pending = new Company();
    pending.setId(11L);
    pending.setName("PENDINGCO");
    pending.setApprovalStatus(CompanyApprovalStatus.PENDING);
    pending.setActive(true);
    pending.setCreatedAt(LocalDateTime.now());

    User firstAdmin = new User();
    firstAdmin.setFirstName("Riya");
    firstAdmin.setLastName("Sharma");
    firstAdmin.setEmail("riya@pendingco.com");
    firstAdmin.setMobileNumber("+919876543210");

    when(companyRepository.findByApprovalStatusOrderByCreatedAtDesc(CompanyApprovalStatus.PENDING))
        .thenReturn(List.of(pending));
    when(userRepository.countByCompanyIdInAndRole(List.of(11L), "ADMIN"))
        .thenReturn(List.of(countRow(11L, 1L)));
    when(userRepository.countByCompanyIdInAndRole(List.of(11L), "USER"))
        .thenReturn(List.of(countRow(11L, 0L)));
    when(userRepository.findFirstByCompanyIdAndRoleOrderByIdAsc(11L, "ADMIN"))
        .thenReturn(Optional.of(firstAdmin));

    var summaries = ownerManagementService.getPendingCompanySummaries();

    assertThat(summaries).hasSize(1);
    assertThat(summaries.get(0).adminFirstName()).isEqualTo("Riya");
    assertThat(summaries.get(0).adminLastName()).isEqualTo("Sharma");
    assertThat(summaries.get(0).adminEmail()).isEqualTo("riya@pendingco.com");
    assertThat(summaries.get(0).adminMobileNumber()).isEqualTo("+919876543210");
  }

  @Test
  void getPendingCompanySummaries_withoutAdmin_setsContactFieldsNull() {
    Company pending = new Company();
    pending.setId(12L);
    pending.setName("NOADMINCO");
    pending.setApprovalStatus(CompanyApprovalStatus.PENDING);
    pending.setActive(true);
    pending.setCreatedAt(LocalDateTime.now());

    when(companyRepository.findByApprovalStatusOrderByCreatedAtDesc(CompanyApprovalStatus.PENDING))
        .thenReturn(List.of(pending));
    when(userRepository.countByCompanyIdInAndRole(List.of(12L), "ADMIN"))
        .thenReturn(List.of(countRow(12L, 0L)));
    when(userRepository.countByCompanyIdInAndRole(List.of(12L), "USER"))
        .thenReturn(List.of(countRow(12L, 0L)));
    when(userRepository.findFirstByCompanyIdAndRoleOrderByIdAsc(12L, "ADMIN"))
        .thenReturn(Optional.empty());

    var summaries = ownerManagementService.getPendingCompanySummaries();

    assertThat(summaries).hasSize(1);
    assertThat(summaries.get(0).adminEmail()).isNull();
    assertThat(summaries.get(0).adminFirstName()).isNull();
    assertThat(summaries.get(0).adminLastName()).isNull();
    assertThat(summaries.get(0).adminMobileNumber()).isNull();
  }

  private CompanyUserCount countRow(Long companyId, Long userCount) {
    return new CompanyUserCount() {
      @Override
      public Long getCompanyId() {
        return companyId;
      }

      @Override
      public Long getUserCount() {
        return userCount;
      }
    };
  }
}


