package com.example.ordermanager.vendor.repository;

import com.example.ordermanager.vendor.entity.VendorBulkUploadRequest;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Provides company-scoped access to durable bulk-upload idempotency requests. */
public interface VendorBulkUploadRequestRepository
    extends JpaRepository<VendorBulkUploadRequest, Long> {

  /** Finds the request claim using both tenant identity and the caller's idempotency key. */
  Optional<VendorBulkUploadRequest> findByCompanyIdAndIdempotencyKey(Long companyId,
      String idempotencyKey);

  /** Finds an already claimed identical workbook to block retries arriving with a new key. */
  Optional<VendorBulkUploadRequest> findByCompanyIdAndWorkbookFingerprint(Long companyId,
      String workbookFingerprint);

  /** Loads one tenant-owned request with its company metadata for result and report rendering. */
  @EntityGraph(attributePaths = "company")
  Optional<VendorBulkUploadRequest> findByIdAndCompanyId(Long id, Long companyId);

  /** Lists recent tenant-owned attempts for the registration audit panel. */
  List<VendorBulkUploadRequest> findTop20ByCompanyIdOrderByCreatedAtDesc(Long companyId);

  /** Finds stale or manually flagged attempts without exposing another tenant's request rows. */
  List<VendorBulkUploadRequest> findByCompanyIdAndStatusInAndUpdatedAtBeforeOrderByCreatedAtAsc(
      Long companyId, Collection<VendorBulkUploadRequest.Status> statuses, LocalDateTime cutoff);

  /** Lists all requests explicitly marked for review, regardless of when the flag was set. */
  List<VendorBulkUploadRequest> findByCompanyIdAndStatusOrderByCreatedAtAsc(Long companyId,
      VendorBulkUploadRequest.Status status);

  /** Locks the tenant-owned request while reconciling it against committed vendor correlations. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select request from VendorBulkUploadRequest request where request.id = :id "
      + "and request.company.id = :companyId")
  Optional<VendorBulkUploadRequest> findOwnedRequestForUpdate(@Param("id") Long id,
      @Param("companyId") Long companyId);
}
