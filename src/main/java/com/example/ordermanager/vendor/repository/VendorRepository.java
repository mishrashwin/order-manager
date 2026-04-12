package com.example.ordermanager.vendor.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.vendor.entity.Vendor;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
  @EntityGraph(attributePaths = {"company"})
  List<Vendor> findByCompanyId(Long companyId);

  Page<Vendor> findByCompanyId(Long companyId, Pageable pageable);

  Page<Vendor> findByCompanyIdAndCompanyNameContainingIgnoreCase(Long companyId, String search, Pageable pageable);
}
