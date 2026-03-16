package com.example.ordermanager.repository;

import com.example.ordermanager.entity.Company;
import com.example.ordermanager.entity.CompanyApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
  Optional<Company> findByName(String name);

  List<Company> findByApprovalStatusOrderByCreatedAtDesc(CompanyApprovalStatus approvalStatus);

  long countByApprovalStatus(CompanyApprovalStatus approvalStatus);
}

