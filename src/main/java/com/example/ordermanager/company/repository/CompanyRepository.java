package com.example.ordermanager.company.repository;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Company c where c.id = :id")
  Optional<Company> findByIdForUpdate(@Param("id") Long id);

  Optional<Company> findByName(String name);

  List<Company> findByApprovalStatusOrderByCreatedAtDesc(CompanyApprovalStatus approvalStatus);

  long countByApprovalStatus(CompanyApprovalStatus approvalStatus);

  long countByActive(boolean active);
}

