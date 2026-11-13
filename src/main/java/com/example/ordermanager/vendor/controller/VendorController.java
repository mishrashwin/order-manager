package com.example.ordermanager.vendor.controller;

import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.service.VendorService;
import com.example.ordermanager.vendor.service.VendorBulkUploadService;
import com.example.ordermanager.vendor.service.VendorBulkRegistrationResult;
import com.example.ordermanager.vendor.service.VendorBulkUploadIdempotencyService;
import com.example.ordermanager.vendor.service.VendorBulkUploadIdempotencyService.ClaimState;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.net.URLDecoder;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.util.unit.DataSize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/vendors")
public class VendorController {

  private static final Logger LOGGER = LoggerFactory.getLogger(VendorController.class);

  private final VendorService vendorService;
  private final VendorBulkUploadService vendorBulkUploadService;
  private final VendorBulkUploadIdempotencyService idempotencyService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;
  @Value("${app.vendor.bulk-upload.max-file-size:5MB}")
  private DataSize maxBulkUploadFileSize = DataSize.ofMegabytes(5);
  private static final MediaType XLSX_MEDIA_TYPE =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  /** Wires vendor operations, authenticated tenant context, and durable upload idempotency. */
  public VendorController(VendorService vendorService,
      VendorBulkUploadService vendorBulkUploadService, CompanyService companyService,
      SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService,
      VendorBulkUploadIdempotencyService idempotencyService) {
    this.vendorService = vendorService;
    this.vendorBulkUploadService = vendorBulkUploadService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
    this.passwordVerificationService = passwordVerificationService;
    this.idempotencyService = idempotencyService;
  }

  /**
   * Loads the authenticated tenant's vendor page and exposes admin-only reconciliation controls.
   */
  @GetMapping
  public String listVendors(@RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int size, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Pageable pageable = PageRequest.of(page, size);
    Page<Vendor> vendorsPage = vendorService.searchVendors(companyId, search, pageable);
    model.addAttribute("vendors", vendorsPage.getContent());
    model.addAttribute("currentPage", page + 1);
    model.addAttribute("totalPages", vendorsPage.getTotalPages());
    model.addAttribute("totalElements", vendorsPage.getTotalElements());
    model.addAttribute("search", search);
    return "vendors/list";
  }

  /** Opens the admin-only upload history and unresolved registration reconciliation page. */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/bulk-upload/management")
  public String vendorUploadManagementPage() {
    return "vendors/bulk-upload-management";
  }

  /** Returns recent registration audit summaries filtered to the authenticated company. */
  @GetMapping("/bulk-upload/registrations")
  @ResponseBody
  public ResponseEntity<List<Map<String, Object>>> recentBulkUploadRegistrations() {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return ResponseEntity.ok(idempotencyService.recentRequests(companyId));
  }

  /**
   * Retrieves a stored completed result without requiring or reprocessing the original workbook.
   */
  @GetMapping("/bulk-upload/registrations/{requestId}")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> retrieveBulkUploadResult(
      @PathVariable Long requestId) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    try {
      return ResponseEntity.ok(idempotencyService.completedResponse(companyId, requestId));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(409)
          .body(Map.of("success", false, "message", "Registration results are not available yet."));
    }
  }

  /** Downloads a complete or error-only workbook generated from persisted registration outcomes. */
  @GetMapping("/bulk-upload/registrations/{requestId}/report")
  @ResponseBody
  public ResponseEntity<?> downloadBulkUploadReport(@PathVariable Long requestId,
      @RequestParam(defaultValue = "complete") String type) {
    if (!"complete".equalsIgnoreCase(type) && !"errors".equalsIgnoreCase(type)) {
      return ResponseEntity.badRequest()
          .body(Map.of("success", false, "message", "Report type must be 'complete' or 'errors'."));
    }
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    try {
      byte[] report = vendorBulkUploadService.generateRegistrationReport(
          idempotencyService.completedReportData(companyId, requestId),
          "errors".equalsIgnoreCase(type));
      String filename = "vendor-registration-" + requestId + "-" + type.toLowerCase() + ".xlsx";
      return ResponseEntity.ok().contentType(XLSX_MEDIA_TYPE)
          .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
              .filename(filename, StandardCharsets.UTF_8).build().toString())
          .body(report);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(409).body(Map.of("success", false, "message",
          "Reports are available only for completed registrations."));
    } catch (Exception e) {
      LOGGER.error("Unable to generate vendor bulk report request={} company={}", requestId,
          companyId, e);
      return ResponseEntity.internalServerError()
          .body(Map.of("success", false, "message", "Unable to generate the registration report."));
    }
  }

  /** Lists only stale unresolved registrations for company administrators. */
  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/bulk-upload/reconciliation")
  @ResponseBody
  public ResponseEntity<List<Map<String, Object>>> unresolvedBulkUploadRegistrations() {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    return ResponseEntity.ok(idempotencyService.unresolvedRequests(companyId));
  }

  /**
   * Reconciles a stale request from persisted vendor correlations without rerunning registration.
   */
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/bulk-upload/registrations/{requestId}/reconcile")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> reconcileBulkUpload(@PathVariable Long requestId) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    try {
      var result = idempotencyService.reconcile(companyId, requestId);
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("registrationReference", requestId);
      response.put("reconciliationState", result.state().name());
      response.put("persistedRows", result.persistedRows());
      response.put("details", result.details());
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
    }
  }

  @GetMapping("/new")
  public String newVendorForm(@RequestParam(required = false) String returnTo, Model model) {
    model.addAttribute("vendor", new Vendor());
    model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
    return "vendors/form";
  }

  @PostMapping
  public String saveVendor(@ModelAttribute Vendor vendor,
      @RequestParam(required = false) String returnTo, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      boolean isUpdate = vendor.getId() != null;
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      vendorService.saveVendorWithCompany(vendor, companyId);
      redirectAttributes.addFlashAttribute("message",
          isUpdate ? "Vendor updated successfully." : "Vendor added successfully.");
      String redirectPath = sanitizeReturnTo(returnTo, "/vendors", "/products");
      return "redirect:" + redirectPath;
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      model.addAttribute("error", message);
      if (isPhoneValidationError(message)) {
        model.addAttribute("phoneError", message);
      }
      model.addAttribute("vendor", vendor);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
      return "vendors/form";
    } catch (IllegalStateException e) {
      model.addAttribute("error", "You must be logged in to create a vendor");
      model.addAttribute("vendor", vendor);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
      return "vendors/form";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving vendor: " + e.getMessage());
      model.addAttribute("vendor", vendor);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
      return "vendors/form";
    }
  }

  private boolean isPhoneValidationError(String message) {
    return message != null && message.toLowerCase().contains("phone");
  }

  @GetMapping("/edit/{id}")
  public String editVendor(@PathVariable Long id, @RequestParam(required = false) String returnTo,
      Model model) {
    model.addAttribute("vendor", vendorService.getVendorById(id));
    model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
    return "vendors/form";
  }

  private String sanitizeReturnTo(String returnTo, String fallback, String... allowedPrefixes) {
    if (returnTo == null || returnTo.isBlank()) {
      return fallback;
    }

    String normalized = normalizeReturnToCandidate(returnTo.trim());
    if (!normalized.startsWith("/") || normalized.startsWith("//") || normalized.contains("\r")
        || normalized.contains("\n")) {
      return fallback;
    }

    boolean allowed = Arrays.stream(allowedPrefixes).anyMatch(normalized::startsWith);
    return allowed ? normalized : fallback;
  }

  private String normalizeReturnToCandidate(String value) {
    String normalized = value;
    try {
      normalized = URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ignored) {
      // Keep original value when URL decoding fails.
    }

    int delimiter = normalized.indexOf(',');
    if (delimiter >= 0) {
      normalized = normalized.substring(0, delimiter).trim();
    }
    return normalized;
  }

  @PostMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public String deleteVendor(@PathVariable Long id, @RequestParam String password,
      RedirectAttributes redirectAttributes) {
    if (!passwordVerificationService.verifyCurrentUserPassword(password)) {
      redirectAttributes.addFlashAttribute("error", "Incorrect password. Vendor was not deleted.");
      return "redirect:/vendors";
    }
    try {
      vendorService.deleteVendor(id);
      redirectAttributes.addFlashAttribute("message", "Vendor deleted successfully.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error deleting vendor: " + e.getMessage());
    }
    return "redirect:/vendors";
  }

  // Download Excel template for bulk vendor upload
  @GetMapping("/bulk-upload/template")
  public ResponseEntity<byte[]> downloadBulkUploadTemplate() {
    try {
      byte[] templateBytes = vendorBulkUploadService.generateBulkUploadTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentDispositionFormData("attachment", "vendor_bulk_upload_template.xlsx");
      return ResponseEntity.ok().headers(headers).body(templateBytes);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  // Parse and validate uploaded rows, returning row results without persisting vendor data.
  @PostMapping("/bulk-upload")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleBulkUpload(
      @RequestParam("file") MultipartFile file) {
    Map<String, Object> response = new HashMap<>();
    String fileError = validateBulkUploadFile(file);
    if (fileError != null) {
      response.put("success", false);
      response.put("message", fileError);
      return ResponseEntity.badRequest().body(response);
    }

    try {
      response.put("success", true);
      var parsedRows = vendorBulkUploadService.parseVendorRows(file.getInputStream());
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      var validationResults = vendorBulkUploadService.validateVendorRows(parsedRows, companyId);
      long validCount = validationResults.stream().filter(row -> row
          .status() == com.example.ordermanager.vendor.service.VendorBulkUploadValidationResult.Status.VALID)
          .count();
      response.put("message", "File parsed and validated successfully.");
      response.put("totalParsedRows", parsedRows.size());
      response.put("validRowCount", validCount);
      response.put("invalidRowCount", parsedRows.size() - validCount);
      response.put("rows", validationResults);
      return ResponseEntity.ok().body(response);
    } catch (com.example.ordermanager.vendor.service.VendorBulkUploadValidationException e) {
      response.put("success", false);
      response.put("message", e.getMessage());
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      response.put("success", false);
      response.put("message", "Unable to read the Excel file. Please use the provided template.");
      return ResponseEntity.badRequest().body(response);
    }
  }

  /** Revalidates the uploaded workbook and registers only currently valid rows for this tenant. */
  @PostMapping("/bulk-upload/register")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> registerBulkUpload(
      @RequestParam("file") MultipartFile file,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    Map<String, Object> response = new HashMap<>();
    String fileError = validateBulkUploadFile(file);
    if (fileError != null) {
      response.put("success", false);
      response.put("message", fileError);
      return ResponseEntity.badRequest().body(response);
    }
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    byte[] workbookBytes;
    try {
      workbookBytes = file.getBytes();
    } catch (Exception e) {
      response.put("success", false);
      response.put("message", "Unable to read the uploaded workbook.");
      return ResponseEntity.badRequest().body(response);
    }
    VendorBulkUploadIdempotencyService.Claim claim;
    try {
      claim = idempotencyService.claim(companyId, idempotencyKey, fingerprint(workbookBytes),
          safeUploadFilename(file.getOriginalFilename()),
          securityContextHelper.getCurrentUsername());
    } catch (IllegalArgumentException e) {
      response.put("success", false);
      response.put("message", e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
    if (claim.state() == ClaimState.REPLAY) {
      return ResponseEntity.ok(claim.response());
    }
    if (claim.state() == ClaimState.IN_PROGRESS) {
      response.put("success", false);
      response.put("message",
          "This registration request is already in progress or requires reconciliation.");
      return ResponseEntity.status(409).body(response);
    }
    if (claim.state() == ClaimState.KEY_CONFLICT) {
      response.put("success", false);
      response.put("message", "This Idempotency-Key was already used with a different workbook.");
      return ResponseEntity.status(409).body(response);
    }
    try {
      var parsedRows =
          vendorBulkUploadService.parseVendorRows(new ByteArrayInputStream(workbookBytes));
      var validations = vendorBulkUploadService.validateVendorRows(parsedRows, companyId);
      var manifestRows = validations.stream()
          .map(row -> new VendorBulkUploadIdempotencyService.ManifestRow(row.excelRowNumber(),
              row.vendorData().companyName(), row.status().name(), row.errors()))
          .toList();
      int validCount = (int) validations.stream().filter(row -> row
          .status() == com.example.ordermanager.vendor.service.VendorBulkUploadValidationResult.Status.VALID)
          .count();
      var manifest = new VendorBulkUploadIdempotencyService.Manifest(validations.size(), validCount,
          validations.size() - validCount, manifestRows);
      idempotencyService.saveManifest(companyId, idempotencyKey, manifest);
      VendorBulkRegistrationResult result =
          vendorBulkUploadService.registerValidVendors(validations, companyId, claim.requestId());
      response.put("success", true);
      response.put("message", "Bulk vendor registration completed.");
      response.put("registrationReference", claim.requestId());
      response.put("totalParsedRows", result.totalParsedRows());
      response.put("validRowCount", result.validRowCount());
      response.put("invalidRowCount", result.invalidRowCount());
      response.put("registeredRowCount", result.registeredRowCount());
      response.put("registrationFailureCount", result.registrationFailureCount());
      response.put("skippedRowCount", result.skippedRowCount());
      response.put("rowResults", result.rowResults());
      idempotencyService.complete(companyId, idempotencyKey, response);
      return ResponseEntity.ok(response);
    } catch (com.example.ordermanager.vendor.service.VendorBulkUploadValidationException e) {
      response.put("success", false);
      response.put("message", e.getMessage());
      markIdempotencyFailedSafely(companyId, idempotencyKey, response);
      return ResponseEntity.badRequest().body(response);
    } catch (Exception e) {
      response.put("success", false);
      response.put("message", "Unable to process the workbook for registration.");
      markIdempotencyFailedSafely(companyId, idempotencyKey, response);
      return ResponseEntity.internalServerError().body(response);
    }
  }

  /**
   * Persists a terminal response when possible and leaves failed claims blocked for reconciliation.
   */
  private void markIdempotencyFailedSafely(Long companyId, String key,
      Map<String, Object> response) {
    try {
      idempotencyService.fail(companyId, key, response);
    } catch (RuntimeException e) {
      LOGGER.error("Unable to persist terminal state for vendor bulk upload request {}.", key, e);
    }
  }

  /** Calculates a content-only SHA-256 fingerprint used to bind a key to one workbook. */
  private String fingerprint(byte[] workbookBytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(workbookBytes));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable.", e);
    }
  }

  /**
   * Removes path components and unsafe control characters before preserving the submitted filename.
   */
  private String safeUploadFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return "vendor-upload.xlsx";
    }
    String basename = filename.replace('\\', '/');
    basename = basename.substring(basename.lastIndexOf('/') + 1);
    String safe = basename.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
    return safe.isBlank() ? "vendor-upload.xlsx" : safe.substring(0, Math.min(255, safe.length()));
  }

  /**
   * Checks required multipart input, the configured size limit, and the supported filename suffix.
   */
  private String validateBulkUploadFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return "File is empty. Please select a valid .xlsx workbook.";
    }
    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
      return "Invalid file type. Only .xlsx files are allowed.";
    }
    if (file.getSize() > maxBulkUploadFileSize.toBytes()) {
      String configuredLimit = String.format(java.util.Locale.ROOT, "%.1f MB",
          maxBulkUploadFileSize.toBytes() / (1024.0 * 1024.0));
      return "File size exceeds the configured " + configuredLimit + " limit.";
    }
    return null;
  }
}
