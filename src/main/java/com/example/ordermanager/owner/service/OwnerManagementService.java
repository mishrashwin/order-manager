package com.example.ordermanager.owner.service;

import com.example.ordermanager.owner.dto.OwnerCompanySummary;
import com.example.ordermanager.owner.dto.OwnerDashboardMetrics;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.user.repository.CompanyUserCount;
import com.example.ordermanager.user.repository.UserRepository;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OwnerManagementService {

  private final CompanyRepository companyRepository;
  private final UserRepository userRepository;

  public OwnerManagementService(CompanyRepository companyRepository,
      UserRepository userRepository) {
    this.companyRepository = companyRepository;
    this.userRepository = userRepository;
  }

  public OwnerDashboardMetrics getDashboardMetrics() {
    long total = companyRepository.count();
    long approved = companyRepository.countByApprovalStatus(CompanyApprovalStatus.APPROVED);
    long pending = companyRepository.countByApprovalStatus(CompanyApprovalStatus.PENDING);
    long rejected = companyRepository.countByApprovalStatus(CompanyApprovalStatus.REJECTED);
    long active = companyRepository.countByActive(true);
    long inactive = total - active;
    return new OwnerDashboardMetrics(total, approved, pending, rejected, active, inactive);
  }

  public List<OwnerCompanySummary> getAllCompanySummaries() {
    List<Company> companies =
        companyRepository.findAll().stream().sorted(Comparator.comparing(Company::getCreatedAt,
            Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    Map<Long, Long> adminCounts = fetchRoleCountMap(companies, "ADMIN");
    Map<Long, Long> userCounts = fetchRoleCountMap(companies, "USER");
    return companies.stream().map(c -> toSummary(c, adminCounts.getOrDefault(c.getId(), 0L),
        userCounts.getOrDefault(c.getId(), 0L))).toList();
  }

  public List<OwnerCompanySummary> getPendingCompanySummaries() {
    List<Company> companies =
        companyRepository.findByApprovalStatusOrderByCreatedAtDesc(CompanyApprovalStatus.PENDING);
    Map<Long, Long> adminCounts = fetchRoleCountMap(companies, "ADMIN");
    Map<Long, Long> userCounts = fetchRoleCountMap(companies, "USER");
    return companies.stream().map(c -> toSummary(c, adminCounts.getOrDefault(c.getId(), 0L),
        userCounts.getOrDefault(c.getId(), 0L))).toList();
  }

  private Map<Long, Long> fetchRoleCountMap(List<Company> companies, String role) {
    if (companies.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> ids = companies.stream().map(Company::getId).toList();
    return userRepository.countByCompanyIdInAndRole(ids, role).stream()
        .filter(row -> row.getCompanyId() != null && row.getUserCount() != null).collect(Collectors
            .toMap(CompanyUserCount::getCompanyId, CompanyUserCount::getUserCount, Long::sum));
  }

  private OwnerCompanySummary toSummary(Company company, long adminCount, long userCount) {
    return new OwnerCompanySummary(company.getId(), company.getName(), company.getApprovalStatus(),
        company.isActive(), adminCount, userCount, company.getCreatedAt());
  }
}
