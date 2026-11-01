package com.example.ordermanager.vendor.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VendorBulkUploadPreviewTemplateTest {

  private static final Path TEMPLATE_PATH =
      Path.of("src", "main", "resources", "templates", "vendors", "list.html");
  private static final Path STYLE_PATH =
      Path.of("src", "main", "resources", "static", "css", "style.css");

  /** Locks preview counts, all vendor columns, status indicators, and expandable field errors. */
  @Test
  void vendorListTemplate_containsValidationPreviewAndErrorRendering() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("id=\"totalRecordCount\"", "id=\"validRecordCount\"",
        "id=\"invalidRecordCount\"", "Excel Row", "Company Name", "Contact Person", "Email",
        "Phone", "GSTIN", "State", "Address", "Validation Errors", "text-bg-success",
        "text-bg-danger", "function appendValidationErrors", "errors.forEach",
        "result.status === 'VALID'", "result.vendorData", "result.errors");
  }

  /**
   * Locks empty-result handling, preview replacement, upload feedback, and existing CSRF wiring.
   */
  @Test
  void vendorListTemplate_handlesEmptyReplacementErrorsAndCsrf() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("No vendor rows were found", "function clearVendorPreview",
        "previewRows.replaceChildren()", "function renderVendorPreview", "uploadProcessing",
        "data.totalParsedRows", "data.validRowCount", "data.invalidRowCount", "rows.length === 0",
        "rows.length !== 0", "Unable to upload and validate the workbook", "meta[name=\"_csrf\"]",
        "meta[name=\"_csrf_header\"]", "headers[csrfHeader] = csrfToken",
        "credentials: 'same-origin'");
    assertThat(template).doesNotContain("headers: {'Content-Type'");
  }

  /** Locks modal sizing, fixed controls, independent two-axis scrolling, and readable columns. */
  @Test
  void vendorListStyles_keepPreviewReadableAndScrollable() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);
    String styles = Files.readString(STYLE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("vendor-bulk-upload-dialog", "vendor-preview-scroll",
        "vendor-preview-col-errors", "modal-header", "modal-footer");
    assertThat(styles).contains("width: 95vw", "max-width: 1600px", "max-height: 85vh",
        "overflow: hidden", "overflow: auto", "min-width: 1950px", "position: sticky",
        ".vendor-preview-col-errors { width: 340px; }", "overflow-wrap: anywhere",
        "@media (max-width: 576px)");
    assertThat(template).contains("col-lg-9", "col-lg-3", "class=\"vendor-preview-summary d-none\"",
        "function sizeBulkUploadModal", "body.scrollHeight", "window.innerHeight * 0.85");
  }

  /**
   * Locks explicit registration confirmation, loading, result rendering, and CSRF-protected fetch.
   */
  @Test
  void vendorListTemplate_containsExplicitRegistrationFlow() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("Register Valid Vendors", "window.confirm('Register '",
        "data.validRowCount", "vendorPreview", "registerValidBtn.disabled = true", "Registering...",
        "registrationProcessing", "registeredRowCount", "registrationFailureCount",
        "totalParsedRows", "skippedRowCount", "registrationSkippedInvalidCount",
        "registrationTotalProcessedCount", "result.failureReason", "result.validationErrors",
        "'/vendors/bulk-upload/register'", "if (registrationCompleted) window.location.reload()");
    assertThat(template).doesNotContain("registrationInvalidCount", "registrationSkippedCount");
  }

  /**
   * Ensures registration rendering iterates every outcome and displays status, row, and error data.
   */
  @Test
  void vendorListTemplate_rendersAllRegistrationOutcomesAndStatuses() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);
    String styles = Files.readString(STYLE_PATH, StandardCharsets.UTF_8);
    int rendererStart = template.indexOf("function renderRegistrationResults(data)");
    int rendererEnd = template.indexOf("// Recalculates preview height", rendererStart);
    String renderer = template.substring(rendererStart, rendererEnd);

    assertThat(renderer)
        .contains("Array.isArray(data.rowResults) ? data.rowResults : []).forEach",
            "tbody.appendChild(tr)", "result.excelRowNumber", "result.failureReason",
            "result.validationErrors", "registrationStatusLabel(result)")
        .doesNotContain(".slice(", ".splice(");
    assertThat(template).contains("REGISTERED", "FAILED", "SKIPPED – INVALID",
        "vendor-registration-results-scroll", "registrationStatusLabel(result)",
        "result.status === 'SKIPPED' && result.validationErrors?.length");
    assertThat(styles).contains(".vendor-registration-results-scroll", "overflow-y: scroll",
        "scrollbar-gutter: stable", "min-height: 0", "flex: 0 1 auto",
        ".vendor-registration-results {", "flex: 1 1 auto;");
    assertThat(renderer).contains("registrationSkippedInvalidCount",
        "registrationTotalProcessedCount", "data.skippedRowCount", "data.totalParsedRows");
  }

  /**
   * Keeps upload audit sections in the admin tab and ensures persisted results load into a visible
   * modal.
   */
  @Test
  void vendorManagementAdminTab_containsAuditSectionsAndVisibleDetailsFlow() throws IOException {
    String vendorTemplate = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);
    String adminTemplate = Files.readString(
        Path.of("src", "main", "resources", "templates", "admin", "dashboard.html"),
        StandardCharsets.UTF_8);

    assertThat(vendorTemplate).doesNotContain("id=\"bulkUploadHistoryRows\"",
        "id=\"bulkUploadReconciliationRows\"");
    assertThat(adminTemplate).contains("id=\"vendor-management-tab\"", "Vendor Management",
        "Recent Vendor Uploads", "Unresolved Uploads", "id=\"bulkUploadHistoryRows\"",
        "id=\"bulkUploadReconciliationRows\"", "id=\"vendorRegistrationDetailsModal\"",
        "function showRegistrationDetails", "registrationFailureCount", "validationErrors",
        "id=\"openVendorManagementTab\"");
    assertThat(adminTemplate.indexOf("<h3>User Management</h3>"))
        .isLessThan(adminTemplate.indexOf("<h3>Vendor Management</h3>"));
    assertThat(adminTemplate.indexOf("<h3>Vendor Management</h3>"))
        .isLessThan(adminTemplate.indexOf("<h3>Order Statistics</h3>"));
    assertThat(
        vendorTemplate.indexOf("bulkUploadModal.addEventListener('shown.bs.modal', loadResult"))
        .isLessThan(vendorTemplate.indexOf("modalInstance.show()"));
  }

  /**
   * Confirms registration completion collapses the retained preview and exposes an accessible
   * toggle.
   */
  @Test
  void vendorListTemplate_collapsesValidationPreviewAfterRegistration() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("id=\"validationPreviewToggle\"", "aria-expanded=\"true\"",
        "aria-controls=\"validationPreviewContent\"", "function setValidationPreviewExpanded",
        "setValidationPreviewExpanded(false)", "setValidationPreviewExpanded(true)",
        "validationPreviewContent.classList.toggle('d-none', !expanded)",
        "vendorPreview.classList.toggle('is-collapsed', !expanded)");
    int previewClose = template.indexOf("</section>", template.indexOf("id=\"vendorPreview\""));
    int resultsStart = template.indexOf("<section id=\"registrationResults\"");
    int toggleStart = template.indexOf("function setValidationPreviewExpanded");
    int toggleEnd = template.indexOf("// Toggles the validation preview", toggleStart);
    String toggle = template.substring(toggleStart, toggleEnd);

    assertThat(resultsStart).isGreaterThan(previewClose);
    assertThat(toggle).contains("validationPreviewContent", "vendorPreview.classList")
        .doesNotContain("registrationResults.classList");
    assertThat(Files.readString(STYLE_PATH, StandardCharsets.UTF_8)).contains(
        ".vendor-upload-preview.d-none", ".vendor-registration-results.d-none",
        ".vendor-upload-preview.is-collapsed", "overflow-y: scroll");
  }

  /** Locks immediate button disabling and reuse of the request key across uncertain retries. */
  @Test
  void vendorListTemplate_reusesIdempotencyKeyAndPreventsRepeatedClicks() throws IOException {
    String template = Files.readString(TEMPLATE_PATH, StandardCharsets.UTF_8);

    assertThat(template).contains("function newRegistrationIdempotencyKey",
        "registrationIdempotencyKey = file ? newRegistrationIdempotencyKey() : null",
        "headers['Idempotency-Key'] = registrationIdempotencyKey",
        "registerValidBtn.disabled = true", "credentials: 'same-origin'");
  }
}
