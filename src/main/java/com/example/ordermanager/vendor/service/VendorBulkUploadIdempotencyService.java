package com.example.ordermanager.vendor.service;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.vendor.entity.VendorBulkUploadRequest;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.repository.VendorBulkUploadRequestRepository;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Claims tenant-scoped upload keys and persists replayable responses for completed attempts. */
@Service
public class VendorBulkUploadIdempotencyService {

  private static final TypeReference<Map<String, Object>> RESPONSE_TYPE = new TypeReference<>() {};
  private static final TypeReference<Manifest> MANIFEST_TYPE = new TypeReference<>() {};
  private static final Logger LOGGER =
      LoggerFactory.getLogger(VendorBulkUploadIdempotencyService.class);
  private final VendorBulkUploadRequestRepository requestRepository;
  private final CompanyRepository companyRepository;
  private final ObjectMapper objectMapper;
  private final VendorRepository vendorRepository;
  private final long reconciliationAgeSeconds;

  /** Wires persistence and stale-request policy for production reporting and reconciliation. */
  @org.springframework.beans.factory.annotation.Autowired
  public VendorBulkUploadIdempotencyService(VendorBulkUploadRequestRepository requestRepository,
      CompanyRepository companyRepository, VendorRepository vendorRepository,
      ObjectMapper objectMapper,
      @Value("${app.vendor.bulk-upload.reconciliation-age-seconds:1800}") long ageSeconds) {
    this.requestRepository = requestRepository;
    this.companyRepository = companyRepository;
    this.vendorRepository = vendorRepository;
    this.objectMapper = objectMapper;
    this.reconciliationAgeSeconds = Math.max(ageSeconds, 60);
  }

  /** Atomically creates a claim or reports the state of an existing tenant-scoped key. */
  public Claim claim(Long companyId, String key, String fingerprint, String filename,
      String initiatedBy) {
    validateKey(key);
    Company company = companyRepository.findById(companyId)
        .orElseThrow(() -> new IllegalStateException("Authenticated company was not found."));
    try {
      VendorBulkUploadRequest created = requestRepository.saveAndFlush(
          new VendorBulkUploadRequest(company, key, fingerprint, filename, initiatedBy));
      LOGGER.info("Vendor bulk registration initiated request={} company={}", created.getId(),
          companyId);
      return new Claim(ClaimState.CLAIMED, created.getId(), null);
    } catch (DataIntegrityViolationException duplicateClaim) {
      Optional<VendorBulkUploadRequest> existingByKey =
          requestRepository.findByCompanyIdAndIdempotencyKey(companyId, key);
      if (existingByKey.isPresent()
          && !existingByKey.get().getWorkbookFingerprint().equals(fingerprint)) {
        return new Claim(ClaimState.KEY_CONFLICT, null, null);
      }
      VendorBulkUploadRequest existing = existingByKey.orElseGet(() -> requestRepository
          .findByCompanyIdAndWorkbookFingerprint(companyId, fingerprint).orElse(null));
      if (existing == null) {
        throw duplicateClaim;
      }
      if (existing.getStatus() == VendorBulkUploadRequest.Status.COMPLETED
          || existing.getStatus() == VendorBulkUploadRequest.Status.FAILED) {
        return new Claim(ClaimState.REPLAY, existing.getId(),
            readResponse(existing.getResponsePayload()));
      }
      return new Claim(ClaimState.IN_PROGRESS, existing.getId(), null);
    }
  }

  /** Marks a claimed request complete and stores the exact response used for future replays. */
  public void complete(Long companyId, String key, Map<String, Object> response) {
    updateTerminalState(companyId, key, response, true);
  }

  /** Stores a controlled terminal failure so retrying a key cannot repeat partial row commits. */
  public void fail(Long companyId, String key, Map<String, Object> response) {
    updateTerminalState(companyId, key, response, false);
  }

  /** Updates only an in-progress tenant claim and serializes its API response. */
  private void updateTerminalState(Long companyId, String key, Map<String, Object> response,
      boolean completed) {
    VendorBulkUploadRequest request =
        requestRepository.findByCompanyIdAndIdempotencyKey(companyId, key)
            .orElseThrow(() -> new IllegalStateException("Bulk upload claim was not found."));
    if (request.getStatus() != VendorBulkUploadRequest.Status.IN_PROGRESS) {
      return;
    }
    String payload = writeResponse(response);
    if (completed) {
      request.complete(payload);
      request.setCompletedCounts(number(response, "totalParsedRows"),
          number(response, "validRowCount"), number(response, "registeredRowCount"),
          number(response, "registrationFailureCount"), number(response, "skippedRowCount"));
      LOGGER.info(
          "Vendor bulk registration completed request={} company={} registered={} failed={} skipped={}",
          request.getId(), companyId, number(response, "registeredRowCount"),
          number(response, "registrationFailureCount"), number(response, "skippedRowCount"));
    } else {
      List<Vendor> persisted =
          vendorRepository.findByCompanyIdAndBulkUploadRequestId(companyId, request.getId());
      String details = String.valueOf(response.getOrDefault("message", "Registration failed."));
      if (persisted.isEmpty()) {
        request.fail(payload);
      } else {
        request.requireReconciliation(VendorBulkUploadRequest.ReconciliationState.INCONCLUSIVE,
            "Registration stopped after correlated rows committed. " + details);
      }
      request.setLastFailureDetails(details);
      LOGGER.warn("Vendor bulk registration stopped request={} company={} persistedRows={}",
          request.getId(), companyId, persisted.size());
    }
    requestRepository.saveAndFlush(request);
  }

  /**
   * Persists the parsed row manifest and trusted upload metadata before any vendor row is written.
   */
  public void saveManifest(Long companyId, String key, Manifest manifest) {
    VendorBulkUploadRequest request =
        requestRepository.findByCompanyIdAndIdempotencyKey(companyId, key)
            .orElseThrow(() -> new IllegalStateException("Bulk upload claim was not found."));
    if (request.getStatus() != VendorBulkUploadRequest.Status.IN_PROGRESS) {
      throw new IllegalStateException("Bulk upload claim is no longer active.");
    }
    request.setManifest(writeResponse(Map.of("totalParsedRows", manifest.totalParsedRows(),
        "validRowCount", manifest.validRowCount(), "invalidRowCount", manifest.invalidRowCount(),
        "rows", manifest.rows())), manifest.totalParsedRows(), manifest.validRowCount());
    requestRepository.saveAndFlush(request);
  }

  /**
   * Returns recent attempts for one authenticated tenant without exposing another company's data.
   */
  public List<Map<String, Object>> recentRequests(Long companyId) {
    return requestRepository.findTop20ByCompanyIdOrderByCreatedAtDesc(companyId).stream()
        .map(this::auditSummary).toList();
  }

  /** Returns old or explicitly unresolved requests for the company-admin reconciliation screen. */
  public List<Map<String, Object>> unresolvedRequests(Long companyId) {
    LocalDateTime cutoff = LocalDateTime.now().minusSeconds(reconciliationAgeSeconds);
    Map<Long, VendorBulkUploadRequest> unresolved = new java.util.LinkedHashMap<>();
    requestRepository
        .findByCompanyIdAndStatusInAndUpdatedAtBeforeOrderByCreatedAtAsc(companyId,
            List.of(VendorBulkUploadRequest.Status.IN_PROGRESS), cutoff)
        .forEach(request -> unresolved.put(request.getId(), request));
    requestRepository
        .findByCompanyIdAndStatusOrderByCreatedAtAsc(companyId,
            VendorBulkUploadRequest.Status.RECONCILIATION_REQUIRED)
        .forEach(request -> unresolved.put(request.getId(), request));
    return unresolved.values().stream().map(this::auditSummary).toList();
  }

  /** Retrieves a completed response only when its registration reference belongs to the tenant. */
  public Map<String, Object> completedResponse(Long companyId, Long requestId) {
    VendorBulkUploadRequest request = ownedRequest(companyId, requestId);
    if (request.getStatus() != VendorBulkUploadRequest.Status.COMPLETED) {
      throw new IllegalStateException("Registration results are not complete.");
    }
    LOGGER.info("Vendor bulk result retrieved request={} company={}", requestId, companyId);
    return readResponse(request.getResponsePayload());
  }

  /** Returns persisted report data only for completed tenant-owned registrations. */
  public ReportData completedReportData(Long companyId, Long requestId) {
    VendorBulkUploadRequest request = ownedRequest(companyId, requestId);
    if (request.getStatus() != VendorBulkUploadRequest.Status.COMPLETED) {
      throw new IllegalStateException("Reports are available only for completed registrations.");
    }
    Map<String, Object> response = readResponse(request.getResponsePayload());
    List<VendorBulkRegistrationResult.RowResult> rows =
        objectMapper.convertValue(response.get("rowResults"),
            new TypeReference<List<VendorBulkRegistrationResult.RowResult>>() {});
    List<Vendor> persisted =
        vendorRepository.findByCompanyIdAndBulkUploadRequestId(companyId, requestId);
    Map<Integer, LocalDateTime> processed = new java.util.HashMap<>();
    persisted.forEach(vendor -> processed.put(vendor.getBulkUploadRowNumber(),
        vendor.getBulkUploadProcessedAt()));
    LOGGER.info("Vendor bulk report downloaded request={} company={}", requestId, companyId);
    return new ReportData(request, rows, processed);
  }

  /** Reconciles a stale claim from upload-row correlations without rerunning registration. */
  @Transactional
  public ReconciliationResult reconcile(Long companyId, Long requestId) {
    VendorBulkUploadRequest request =
        requestRepository.findOwnedRequestForUpdate(requestId, companyId)
            .orElseThrow(() -> new IllegalArgumentException("Registration request was not found."));
    LocalDateTime cutoff = LocalDateTime.now().minusSeconds(reconciliationAgeSeconds);
    if (request.getStatus() == VendorBulkUploadRequest.Status.IN_PROGRESS
        && request.getUpdatedAt().isAfter(cutoff)) {
      throw new IllegalStateException("Registration request is not old enough to reconcile.");
    }
    if (request.getStatus() != VendorBulkUploadRequest.Status.IN_PROGRESS
        && request.getStatus() != VendorBulkUploadRequest.Status.RECONCILIATION_REQUIRED) {
      throw new IllegalStateException("Registration request is not unresolved.");
    }
    List<Vendor> committed =
        vendorRepository.findByCompanyIdAndBulkUploadRequestId(companyId, requestId);
    Manifest manifest = readManifest(request.getManifestPayload());
    ReconciliationResult result = inspectCorrelations(manifest, committed);
    LOGGER.warn("Vendor bulk reconciliation request={} company={} state={}", requestId, companyId,
        result.state());
    if (result.state() == VendorBulkUploadRequest.ReconciliationState.FULLY_COMMITTED) {
      Map<String, Object> response = reconciledResponse(requestId, manifest);
      request.completeFromReconciliation(writeResponse(response), manifest.validRowCount(), 0,
          manifest.invalidRowCount(), manifest.totalParsedRows());
      return new ReconciliationResult(requestId, result.state(), result.persistedRows(),
          "All planned valid rows have matching persisted upload correlations.");
    }
    request.requireReconciliation(result.state(), result.details());
    return result;
  }

  /** Enforces tenant ownership while resolving a request reference for reports and result views. */
  private VendorBulkUploadRequest ownedRequest(Long companyId, Long requestId) {
    return requestRepository.findByIdAndCompanyId(requestId, companyId)
        .orElseThrow(() -> new IllegalArgumentException("Registration request was not found."));
  }

  /** Builds a safe audit summary without including vendor contact identifiers or workbook data. */
  private Map<String, Object> auditSummary(VendorBulkUploadRequest request) {
    Map<String, Object> summary = new java.util.LinkedHashMap<>();
    summary.put("registrationReference", request.getId());
    summary.put("status", request.getStatus().name());
    summary.put("reconciliationState", request.getReconciliationState().name());
    summary.put("uploadedFilename", request.getUploadedFilename());
    summary.put("initiatedBy", request.getInitiatedBy());
    summary.put("workbookFingerprint", maskFingerprint(request.getWorkbookFingerprint()));
    summary.put("startedAt", request.getCreatedAt());
    summary.put("completedAt", request.getCompletedAt());
    summary.put("totalParsedRows", request.getTotalParsedRows());
    summary.put("registeredRowCount", request.getRegisteredRowCount());
    summary.put("registrationFailureCount", request.getFailedRowCount());
    summary.put("skippedRowCount", request.getSkippedRowCount());
    summary.put("lastFailureDetails", request.getLastFailureDetails());
    return summary;
  }

  /** Masks the raw digest while retaining enough characters to identify a report request. */
  private String maskFingerprint(String fingerprint) {
    return fingerprint == null ? "" : fingerprint.substring(0, Math.min(12, fingerprint.length()));
  }

  /** Reads the minimal row manifest needed to reconcile committed registration correlations. */
  private Manifest readManifest(String payload) {
    if (payload == null || payload.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(payload, MANIFEST_TYPE);
    } catch (Exception e) {
      LOGGER.error("Unable to read the vendor bulk upload reconciliation manifest.", e);
      return null;
    }
  }

  /** Classifies persisted request/row correlations without guessing about absent valid rows. */
  private ReconciliationResult inspectCorrelations(Manifest manifest, List<Vendor> committed) {
    if (manifest == null) {
      return new ReconciliationResult(null,
          VendorBulkUploadRequest.ReconciliationState.INCONCLUSIVE, committed.size(),
          "The row manifest is missing or unreadable.");
    }
    Set<Integer> expected = new HashSet<>();
    manifest.rows().stream().filter(row -> "VALID".equals(row.status()))
        .forEach(row -> expected.add(row.excelRowNumber()));
    Set<Integer> actual = new HashSet<>();
    committed.forEach(vendor -> actual.add(vendor.getBulkUploadRowNumber()));
    if (actual.size() != committed.size() || !expected.containsAll(actual)) {
      return new ReconciliationResult(null,
          VendorBulkUploadRequest.ReconciliationState.INCONCLUSIVE, committed.size(),
          "Persisted row correlations do not match the saved manifest.");
    }
    if (actual.equals(expected)) {
      return new ReconciliationResult(null,
          VendorBulkUploadRequest.ReconciliationState.FULLY_COMMITTED, committed.size(),
          "All planned valid rows have persisted correlations.");
    }
    if (actual.isEmpty()) {
      return new ReconciliationResult(null,
          VendorBulkUploadRequest.ReconciliationState.NO_RECORDS_COMMITTED, 0,
          "No vendor rows have a verified correlation; the request remains unresolved.");
    }
    return new ReconciliationResult(null,
        VendorBulkUploadRequest.ReconciliationState.PARTIALLY_COMMITTED, committed.size(),
        "Only some planned rows have verified correlations; missing rows are not classified.");
  }

  /**
   * Reconstructs a completed response only when every valid manifest row is correlated in storage.
   */
  private Map<String, Object> reconciledResponse(Long requestId, Manifest manifest) {
    List<Map<String, Object>> rowResults = new ArrayList<>();
    for (ManifestRow row : manifest.rows()) {
      Map<String, Object> result = new java.util.LinkedHashMap<>();
      result.put("excelRowNumber", row.excelRowNumber());
      result.put("vendorCompanyName", row.vendorCompanyName());
      result.put("status", "VALID".equals(row.status()) ? "REGISTERED" : "SKIPPED");
      result.put("failureReason", null);
      result.put("validationErrors", row.validationErrors());
      rowResults.add(result);
    }
    Map<String, Object> response = new java.util.LinkedHashMap<>();
    response.put("success", true);
    response.put("message", "Registration completed after verified reconciliation.");
    response.put("registrationReference", requestId);
    response.put("totalParsedRows", manifest.totalParsedRows());
    response.put("validRowCount", manifest.validRowCount());
    response.put("invalidRowCount", manifest.invalidRowCount());
    response.put("registeredRowCount", manifest.validRowCount());
    response.put("registrationFailureCount", 0);
    response.put("skippedRowCount", manifest.invalidRowCount());
    response.put("rowResults", rowResults);
    return response;
  }

  /** Reads a numeric count from the serialized API response without changing its public shape. */
  private int number(Map<String, Object> response, String key) {
    Object value = response.get(key);
    return value instanceof Number number ? number.intValue() : 0;
  }

  /** Validates bounded idempotency keys before storing them in the database. */
  private void validateKey(String key) {
    if (key == null || key.isBlank() || key.length() > 100) {
      throw new IllegalArgumentException("A valid Idempotency-Key header is required.");
    }
  }

  /** Serializes the response without storing uploaded workbook contents. */
  private String writeResponse(Map<String, Object> response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to store the bulk upload result.", e);
    }
  }

  /** Restores a previously finalized response, failing closed if stored data is unreadable. */
  private Map<String, Object> readResponse(String payload) {
    try {
      return objectMapper.readValue(payload, RESPONSE_TYPE);
    } catch (Exception e) {
      throw new IllegalStateException("Stored bulk upload result is unavailable.", e);
    }
  }

  public enum ClaimState {
    CLAIMED, IN_PROGRESS, REPLAY, KEY_CONFLICT
  }

  public record Claim(ClaimState state, Long requestId, Map<String, Object> response) {}

  public record Manifest(int totalParsedRows, int validRowCount, int invalidRowCount,
      List<ManifestRow> rows) {}

  public record ManifestRow(int excelRowNumber, String vendorCompanyName, String status,
      List<String> validationErrors) {}

  public record ReconciliationResult(Long registrationReference,
      VendorBulkUploadRequest.ReconciliationState state, int persistedRows, String details) {}

  public record ReportData(VendorBulkUploadRequest request,
      List<VendorBulkRegistrationResult.RowResult> rows, Map<Integer, LocalDateTime> processedAt) {}
}
