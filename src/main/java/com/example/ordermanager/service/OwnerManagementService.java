package com.example.ordermanager.service;

import com.example.ordermanager.dto.OwnerCompanySummary;
import com.example.ordermanager.dto.OwnerDashboardMetrics;
import com.example.ordermanager.entity.Company;
import com.example.ordermanager.entity.CompanyApprovalStatus;
import com.example.ordermanager.repository.CompanyRepository;
import com.example.ordermanager.user.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class OwnerManagementService {

  private final CompanyRepository companyRepository;
  private final UserRepository userRepository;

  @PersistenceContext
  private EntityManager entityManager;

  public OwnerManagementService(CompanyRepository companyRepository,
      UserRepository userRepository) {
    this.companyRepository = companyRepository;
    this.userRepository = userRepository;
  }

  public OwnerDashboardMetrics getDashboardMetrics() {
    long total = companyRepository.count();
    long approved = countCompaniesByApprovalStatus(CompanyApprovalStatus.APPROVED);
    long pending = countCompaniesByApprovalStatus(CompanyApprovalStatus.PENDING);
    long rejected = countCompaniesByApprovalStatus(CompanyApprovalStatus.REJECTED);
    long active = countCompaniesByActive(true);
    long inactive = total - active;
    return new OwnerDashboardMetrics(total, approved, pending, rejected, active, inactive);
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

  private long countCompaniesByApprovalStatus(CompanyApprovalStatus status) {
    String jpql = "SELECT COUNT(c) FROM Company c WHERE c.approvalStatus = :status";
    return entityManager.createQuery(jpql, Long.class).setParameter("status", status)
        .getSingleResult();
  }

  private long countCompaniesByActive(boolean active) {
    String jpql = "SELECT COUNT(c) FROM Company c WHERE c.active = :active";
    return entityManager.createQuery(jpql, Long.class).setParameter("active", active)
        .getSingleResult();
  }
}

