package com.example.ordermanager.owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.owner.dto.OwnerDashboardMetrics;
import com.example.ordermanager.owner.service.OwnerManagementService;
import com.example.ordermanager.user.repository.CompanyUserCount;
import com.example.ordermanager.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
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


