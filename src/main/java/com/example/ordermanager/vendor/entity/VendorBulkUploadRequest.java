package com.example.ordermanager.vendor.entity;

import com.example.ordermanager.company.entity.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;

/** Stores a tenant-scoped bulk-upload idempotency claim and its terminal API response. */
@Entity
@Getter
@Table(name = "vendor_bulk_upload_requests",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vendor_bulk_upload_company_key",
            columnNames = {"company_id", "idempotency_key"}),
        @UniqueConstraint(name = "uk_vendor_bulk_upload_company_fingerprint",
            columnNames = {"company_id", "workbook_fingerprint"})})
public class VendorBulkUploadRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @Column(name = "idempotency_key", nullable = false, length = 100)
  private String idempotencyKey;

  @Column(name = "workbook_fingerprint", nullable = false, length = 64)
  private String workbookFingerprint;

  @Column(name = "uploaded_filename", nullable = false, length = 255)
  private String uploadedFilename;

  @Column(name = "initiated_by", nullable = false, length = 255)
  private String initiatedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(name = "response_payload", columnDefinition = "TEXT")
  private String responsePayload;

  @Column(name = "manifest_payload", columnDefinition = "TEXT")
  private String manifestPayload;

  @Column(name = "last_failure_details", columnDefinition = "TEXT")
  private String lastFailureDetails;

  @Column(name = "total_parsed_rows")
  private Integer totalParsedRows;

  @Column(name = "valid_row_count")
  private Integer validRowCount;

  @Column(name = "registered_row_count")
  private Integer registeredRowCount;

  @Column(name = "failed_row_count")
  private Integer failedRowCount;

  @Column(name = "skipped_row_count")
  private Integer skippedRowCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "reconciliation_state", nullable = false, length = 30)
  private ReconciliationState reconciliationState = ReconciliationState.NOT_REQUIRED;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "reconciled_at")
  private LocalDateTime reconciledAt;

  @Version
  private long version;

  /** Provides the no-argument constructor required by JPA entity hydration. */
  protected VendorBulkUploadRequest() {}

  /** Creates the initial durable claim before any workbook row can be persisted. */
  public VendorBulkUploadRequest(Company company, String idempotencyKey, String workbookFingerprint,
      String uploadedFilename, String initiatedBy) {
    this.company = company;
    this.idempotencyKey = idempotencyKey;
    this.workbookFingerprint = workbookFingerprint;
    this.uploadedFilename = uploadedFilename;
    this.initiatedBy = initiatedBy;
    this.status = Status.IN_PROGRESS;
  }

  /** Stores a completed response only after vendor row transactions have returned successfully. */
  public void complete(String responsePayload) {
    this.responsePayload = responsePayload;
    this.status = Status.COMPLETED;
    this.completedAt = LocalDateTime.now();
  }

  /** Stores normalized manifest and planned counts before any vendor row transaction begins. */
  public void setManifest(String manifestPayload, int totalRows, int validRows) {
    this.manifestPayload = manifestPayload;
    this.totalParsedRows = totalRows;
    this.validRowCount = validRows;
  }

  /** Updates the most recent safe terminal failure detail for admin review. */
  public void setLastFailureDetails(String details) {
    this.lastFailureDetails = details;
  }

  /** Keeps the request lease fresh as a correlated vendor row commits. */
  public void touchProgress() {
    this.updatedAt = LocalDateTime.now();
  }

  /** Marks an old request as stopped for manual review without claiming unverified row outcomes. */
  public void requireReconciliation(ReconciliationState state, String details) {
    this.status = Status.RECONCILIATION_REQUIRED;
    this.reconciliationState = state;
    this.lastFailureDetails = details;
    this.reconciledAt = LocalDateTime.now();
  }

  /**
   * Completes a request only when reconciliation verifies every planned valid row by request ID.
   */
  public void completeFromReconciliation(String responsePayload, int registeredRows, int failedRows,
      int skippedRows, int totalRows) {
    this.responsePayload = responsePayload;
    this.status = Status.COMPLETED;
    this.reconciliationState = ReconciliationState.FULLY_COMMITTED;
    this.registeredRowCount = registeredRows;
    this.failedRowCount = failedRows;
    this.skippedRowCount = skippedRows;
    this.totalParsedRows = totalRows;
    this.completedAt = LocalDateTime.now();
    this.reconciledAt = LocalDateTime.now();
  }

  /** Marks a terminal controlled failure so the same key cannot start an unsafe retry. */
  public void fail(String responsePayload) {
    this.responsePayload = responsePayload;
    this.status = Status.FAILED;
    this.completedAt = LocalDateTime.now();
  }

  /** Persists outcome counts alongside the immutable completed result payload. */
  public void setCompletedCounts(int totalRows, int validRows, int registeredRows, int failedRows,
      int skippedRows) {
    this.totalParsedRows = totalRows;
    this.validRowCount = validRows;
    this.registeredRowCount = registeredRows;
    this.failedRowCount = failedRows;
    this.skippedRowCount = skippedRows;
  }

  public enum Status {
    IN_PROGRESS, COMPLETED, FAILED, RECONCILIATION_REQUIRED
  }

  public enum ReconciliationState {
    NOT_REQUIRED, FULLY_COMMITTED, PARTIALLY_COMMITTED, NO_RECORDS_COMMITTED, INCONCLUSIVE
  }
}
