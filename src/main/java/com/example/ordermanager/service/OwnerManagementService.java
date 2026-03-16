package com.example.ordermanager.service;

import com.example.ordermanager.dto.OwnerCompanySummary;
import com.example.ordermanager.dto.OwnerDashboardMetrics;
import com.example.ordermanager.entity.Company;
import com.example.ordermanager.entity.CompanyApprovalStatus;
import com.example.ordermanager.repository.CompanyRepository;
import com.example.ordermanager.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
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
    List<Company> companies = companyRepository.findAll();
    long total = companies.size();
    long approved = companies.stream()
        .filter(c -> CompanyApprovalStatus.APPROVED.equals(c.getApprovalStatus())).count();
    long pending = companies.stream()
        .filter(c -> CompanyApprovalStatus.PENDING.equals(c.getApprovalStatus())).count();
    long rejected = companies.stream()
        .filter(c -> CompanyApprovalStatus.REJECTED.equals(c.getApprovalStatus())).count();
    long active = companies.stream().filter(Company::isActive).count();
    return new OwnerDashboardMetrics(total, approved, pending, rejected, active, total - active);
  }

  public List<OwnerCompanySummary> getAllCompanySummaries() {
    return companyRepository.findAll().stream().sorted(Comparator.comparing(Company::getCreatedAt,
        Comparator.nullsLast(Comparator.reverseOrder()))).map(this::toSummary).toList();
  }

  public List<OwnerCompanySummary> getPendingCompanySummaries() {
    return companyRepository.findByApprovalStatusOrderByCreatedAtDesc(CompanyApprovalStatus.PENDING)
        .stream().map(this::toSummary).toList();
  }

  private OwnerCompanySummary toSummary(Company company) {
    long usersCount = userRepository.countByCompanyId(company.getId());
    return new OwnerCompanySummary(company.getId(), company.getName(), company.getApprovalStatus(),
        company.isActive(), usersCount, company.getCreatedAt(), company.getApprovedBy(),
        company.getApprovedAt());
  }
}

