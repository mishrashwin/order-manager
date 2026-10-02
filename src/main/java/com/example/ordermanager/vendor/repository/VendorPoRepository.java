package com.example.ordermanager.vendor.repository;

import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorPoRepository extends JpaRepository<VendorPo, Long> {

  @EntityGraph(attributePaths = {"company", "vendor", "items"})
  List<VendorPo> findByCompany_Id(Long companyId);

  @EntityGraph(attributePaths = {"company", "vendor", "items"})
  Page<VendorPo> findByCompany_Id(Long companyId, Pageable pageable);

  @EntityGraph(attributePaths = {"company", "vendor", "items"})
  Page<VendorPo> findByCompany_IdAndPoNumberContainingIgnoreCase(Long companyId, String search,
      Pageable pageable);

  @EntityGraph(attributePaths = {"company", "vendor", "items"})
  List<VendorPo> findByCompany_IdAndStatus(Long companyId, VendorPoStatus status);

  @EntityGraph(attributePaths = {"company", "vendor", "items"})
  Optional<VendorPo> findByIdAndCompany_Id(Long id, Long companyId);

  long countByCompany_Id(Long companyId);

}

