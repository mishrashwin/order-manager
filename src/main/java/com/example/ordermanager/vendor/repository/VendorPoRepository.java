package com.example.ordermanager.vendor.repository;

import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorPoRepository extends JpaRepository<VendorPo, Long> {

  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  List<VendorPo> findByCompany_Id(Long companyId);

  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  Page<VendorPo> findByCompany_Id(Long companyId, Pageable pageable);

  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  Page<VendorPo> findByCompany_IdAndPoNumberContainingIgnoreCase(Long companyId, String search,
      Pageable pageable);

  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  List<VendorPo> findByCompany_IdAndStatus(Long companyId, VendorPoStatus status);

  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  Optional<VendorPo> findByIdAndCompany_Id(Long id, Long companyId);

  /** Locks one tenant-owned PO row while creating a new revision or issuing its current draft. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VendorPo> findLockedByIdAndCompany_Id(Long id, Long companyId);

  /** Lists all revisions for a logical PO inside the authenticated Company. */
  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  List<VendorPo> findByRootPo_IdAndCompany_IdOrderByRevisionNumberDesc(Long rootId, Long companyId);

  /** Returns the current revision for each Company PO list row. */
  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  Page<VendorPo> findByCompany_IdAndCurrentRevisionTrue(Long companyId, Pageable pageable);

  /** Searches only current revisions in the authenticated Company. */
  /** Searches current tenant-owned POs by PO number, vendor snapshot, or item product name. */
  @EntityGraph(attributePaths = {"company", "vendor", "items", "orderOwner"})
  @Query("select po from VendorPo po left join po.vendor vendor "
      + "where po.company.id = :companyId and po.currentRevision = true and ("
      + "lower(coalesce(po.poNumber, '')) like concat('%', :search, '%') or "
      + "lower(coalesce(po.vendorName, '')) like concat('%', :search, '%') or "
      + "lower(coalesce(vendor.companyName, '')) like concat('%', :search, '%') or "
      + "exists (select item.id from VendorPoItem item where item.vendorPo = po "
      + "and lower(coalesce(item.productName, '')) like concat('%', :search, '%')))")
  Page<VendorPo> searchCurrentRevisionsByCompanyAndText(@Param("companyId") Long companyId,
      @Param("search") String search, Pageable pageable);

  long countByCompany_Id(Long companyId);

  /** Counts only original PO numbers, excluding amendment rows. */
  long countByCompany_IdAndRevisionNumber(Long companyId, int revisionNumber);

}

