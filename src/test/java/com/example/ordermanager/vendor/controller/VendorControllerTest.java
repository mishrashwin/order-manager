package com.example.ordermanager.vendor.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.exception.GlobalExceptionHandler;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.service.VendorService;
import com.example.ordermanager.vendor.service.VendorBulkUploadService;
import com.example.ordermanager.vendor.service.VendorBulkUploadRowDto;
import com.example.ordermanager.vendor.service.VendorBulkUploadValidationResult;
import com.example.ordermanager.vendor.service.VendorBulkRegistrationResult;
import com.example.ordermanager.vendor.service.VendorBulkUploadIdempotencyService;
import com.example.ordermanager.vendor.service.VendorBulkUploadIdempotencyService.Claim;
import com.example.ordermanager.vendor.service.VendorBulkUploadIdempotencyService.ClaimState;
import com.example.ordermanager.vendor.entity.VendorBulkUploadRequest;
import com.example.ordermanager.vendor.repository.VendorRepository;
import java.util.List;
import java.util.Map;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VendorControllerTest {

  @Mock
  private VendorService vendorService;
  @Mock
  private VendorBulkUploadService vendorBulkUploadService;
  @Mock
  private CompanyService companyService;
  @Mock
  private SecurityContextHelper securityContextHelper;
  @Mock
  private PasswordVerificationService passwordVerificationService;
  @Mock
  private VendorBulkUploadIdempotencyService idempotencyService;

  private VendorController vendorController;

  /** Confirms parsed rows are returned as JSON data without invoking vendor persistence. */
  @Test
  void handleBulkUpload_returnsParsedRowsWithoutSavingVendors() throws Exception {
    var file = new MockMultipartFile("file", "vendors.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1});
    var parsedRows = List.of(
        new VendorBulkUploadRowDto(2, "Acme", "Jane Doe", "jane@example.com", "+919876543210",
            "27ABCDE1234F1Z5", "Maharashtra", "Mumbai"),
        new VendorBulkUploadRowDto(3, "", "Jane Doe", "invalid", "+919876543210", "27ABCDE1234F5Z1",
            "Maharashtra", "Mumbai"));
    var validationResults = List.of(
        new VendorBulkUploadValidationResult(2, parsedRows.get(0),
            VendorBulkUploadValidationResult.Status.VALID, List.of()),
        new VendorBulkUploadValidationResult(3, parsedRows.get(1),
            VendorBulkUploadValidationResult.Status.INVALID, List.of("companyName: required")));
    when(vendorBulkUploadService.parseVendorRows(org.mockito.ArgumentMatchers.any()))
        .thenReturn(parsedRows);
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    when(vendorBulkUploadService.validateVendorRows(parsedRows, 7L)).thenReturn(validationResults);

    var response = vendorController.handleBulkUpload(file);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().get("rows")).isEqualTo(validationResults);
    assertThat(response.getBody().get("totalParsedRows")).isEqualTo(2);
    assertThat(response.getBody().get("validRowCount")).isEqualTo(1L);
    assertThat(response.getBody().get("invalidRowCount")).isEqualTo(1L);
    verify(vendorService, never()).saveVendorWithCompany(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  /** Returns clear upload errors for null, empty, unsupported-extension, and oversized files. */
  @Test
  void handleBulkUpload_rejectsInvalidFileInputsBeforeParsing() throws Exception {
    var empty =
        new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);
    var legacy =
        new MockMultipartFile("file", "vendors.xls", "application/vnd.ms-excel", new byte[] {1});
    var oversized = new MockMultipartFile("file", "large.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        new byte[5 * 1024 * 1024 + 1]);

    assertThat(vendorController.handleBulkUpload(null).getBody()).containsEntry("success", false)
        .containsEntry("message", "File is empty. Please select a valid .xlsx workbook.");
    assertThat(vendorController.handleBulkUpload(empty).getBody()).containsEntry("success", false)
        .containsEntry("message", "File is empty. Please select a valid .xlsx workbook.");
    assertThat(vendorController.handleBulkUpload(legacy).getBody()).containsEntry("success", false)
        .containsEntry("message", "Invalid file type. Only .xlsx files are allowed.");
    assertThat(vendorController.handleBulkUpload(oversized).getBody())
        .containsEntry("success", false)
        .containsEntry("message", "File size exceeds the configured 5.0 MB limit.");
    verify(vendorBulkUploadService, never()).parseVendorRows(org.mockito.ArgumentMatchers.any());
  }

  /** Confirms a spoofed Excel MIME type cannot bypass OOXML workbook parsing. */
  @Test
  void handleBulkUpload_rejectsCsvContentWithExcelMimeType() {
    VendorBulkUploadService parser = new VendorBulkUploadService(
        org.mockito.Mockito.mock(VendorRepository.class), vendorService);
    VendorController controller = new VendorController(vendorService, parser, companyService,
        securityContextHelper, passwordVerificationService, idempotencyService);
    var spoofed = new MockMultipartFile("file", "vendors.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "name,email\nAcme,acme@example.com".getBytes());

    var response = controller.handleBulkUpload(spoofed);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).containsEntry("success", false).containsEntry("message",
        "The uploaded file is not a valid or readable .xlsx workbook.");
  }

  /** Returns the bulk-upload JSON error when servlet multipart limits reject an oversized file. */
  @Test
  void maxUploadSizeExceeded_returnsControlledBulkUploadError() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/vendors/bulk-upload");

    var response = new GlobalExceptionHandler()
        .handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(5L * 1024 * 1024), request);

    assertThat(response.getStatusCode().value()).isEqualTo(413);
    assertThat(response.getBody()).containsEntry("success", false).containsEntry("message",
        "File size exceeds the configured 5.0 MB limit.");
  }

  /**
   * Uses the authenticated tenant context and returns structured outcomes for explicit
   * registration.
   */
  @Test
  void registerBulkUpload_usesAuthenticatedCompanyAndReturnsRegistrationCounts() throws Exception {
    var file = new MockMultipartFile("file", "vendors.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1});
    var row = new VendorBulkUploadRowDto(2, "Acme", "Jane Doe", "jane@example.com", "+919876543210",
        "27ABCDE1234F1Z5", "Maharashtra", "Mumbai");
    var validations = List.of(new VendorBulkUploadValidationResult(2, row,
        VendorBulkUploadValidationResult.Status.VALID, List.of()));
    var outcomes = List.of(new VendorBulkRegistrationResult.RowResult(2, "Acme",
        VendorBulkRegistrationResult.Status.REGISTERED, null, List.of()));
    var registrationResult = new VendorBulkRegistrationResult(1, 1, 0, 1, 0, 0, outcomes);
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("admin");
    when(idempotencyService.claim(org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.eq("request-1"), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new Claim(ClaimState.CLAIMED, 44L, null));
    when(vendorBulkUploadService.parseVendorRows(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(row));
    when(vendorBulkUploadService.validateVendorRows(List.of(row), 7L)).thenReturn(validations);
    when(vendorBulkUploadService.registerValidVendors(validations, 7L, 44L))
        .thenReturn(registrationResult);

    var response = vendorController.registerBulkUpload(file, "request-1");

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).containsEntry("registeredRowCount", 1);
    assertThat(response.getBody()).containsEntry("registrationReference", 44L);
    assertThat(response.getBody()).containsEntry("rowResults", outcomes);
    verify(vendorBulkUploadService).registerValidVendors(validations, 7L, 44L);
    verify(idempotencyService).complete(org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.eq("request-1"), org.mockito.ArgumentMatchers.anyMap());
    verify(vendorService, never()).saveVendorWithCompany(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  /** Ignores a forged request company ID and uses only the authenticated tenant context. */
  @Test
  void registerBulkUpload_ignoresClientSuppliedCompanyId() throws Exception {
    var file = new MockMultipartFile("file", "vendors.xlsx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1});
    var row = new VendorBulkUploadRowDto(2, "Acme", "Jane Doe", "jane@example.com", "+919876543210",
        "27ABCDE1234F1Z5", "Maharashtra", "Mumbai");
    var validations = List.of(new VendorBulkUploadValidationResult(2, row,
        VendorBulkUploadValidationResult.Status.VALID, List.of()));
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("admin");
    when(idempotencyService.claim(org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.eq("request-1"), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new Claim(ClaimState.CLAIMED, 44L, null));
    when(vendorBulkUploadService.parseVendorRows(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(row));
    when(vendorBulkUploadService.validateVendorRows(List.of(row), 7L)).thenReturn(validations);
    when(vendorBulkUploadService.registerValidVendors(validations, 7L, 44L))
        .thenReturn(new VendorBulkRegistrationResult(1, 1, 0, 1, 0, 0, List.of()));
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(vendorController).build();

    mockMvc.perform(multipart("/vendors/bulk-upload/register").file(file).param("companyId", "42")
        .header("Idempotency-Key", "request-1")).andExpect(status().isOk());

    verify(securityContextHelper).getCompanyIdFromContext();
    verify(vendorBulkUploadService).validateVendorRows(List.of(row), 7L);
    verify(vendorBulkUploadService).registerValidVendors(validations, 7L, 44L);
  }

  /**
   * Re-parses and revalidates a changed workbook instead of trusting the earlier preview result.
   */
  @Test
  void registerBulkUpload_revalidatesChangedWorkbookAfterPreview() throws Exception {
    var previewFile =
        new MockMultipartFile("file", "vendors.xlsx", "application/octet-stream", new byte[] {1});
    var changedFile =
        new MockMultipartFile("file", "vendors.xlsx", "application/octet-stream", new byte[] {2});
    var previewRow = new VendorBulkUploadRowDto(2, "Acme", "Jane Doe", "jane@example.com",
        "+919876543210", "27ABCDE1234F1Z5", "Maharashtra", "Mumbai");
    var changedRow = new VendorBulkUploadRowDto(2, "Acme", "Jane Doe", "bad-email", "+919876543210",
        "27ABCDE1234F1Z5", "Maharashtra", "Mumbai");
    var previewValidation = List.of(new VendorBulkUploadValidationResult(2, previewRow,
        VendorBulkUploadValidationResult.Status.VALID, List.of()));
    var changedValidation = List.of(new VendorBulkUploadValidationResult(2, changedRow,
        VendorBulkUploadValidationResult.Status.INVALID,
        List.of("email: Email format is invalid.")));
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("admin");
    when(idempotencyService.claim(org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.eq("request-1"), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new Claim(ClaimState.CLAIMED, 44L, null));
    when(vendorBulkUploadService.parseVendorRows(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(previewRow), List.of(changedRow));
    when(vendorBulkUploadService.validateVendorRows(List.of(previewRow), 7L))
        .thenReturn(previewValidation);
    when(vendorBulkUploadService.validateVendorRows(List.of(changedRow), 7L))
        .thenReturn(changedValidation);
    var skipped = new VendorBulkRegistrationResult(1, 0, 1, 0, 0, 1,
        List.of(new VendorBulkRegistrationResult.RowResult(2, "Acme",
            VendorBulkRegistrationResult.Status.SKIPPED, null,
            List.of("email: Email format is invalid."))));
    when(vendorBulkUploadService.registerValidVendors(changedValidation, 7L, 44L))
        .thenReturn(skipped);

    var previewResponse = vendorController.handleBulkUpload(previewFile);
    var registrationResponse = vendorController.registerBulkUpload(changedFile, "request-1");

    assertThat(previewResponse.getBody().get("rows")).isEqualTo(previewValidation);
    assertThat(registrationResponse.getBody()).containsEntry("skippedRowCount", 1);
    verify(vendorBulkUploadService, org.mockito.Mockito.times(2))
        .parseVendorRows(org.mockito.ArgumentMatchers.any());
    verify(vendorBulkUploadService).registerValidVendors(changedValidation, 7L, 44L);
    verify(vendorBulkUploadService, never()).registerValidVendors(previewValidation, 7L, 44L);
    verify(vendorService, never()).saveVendorWithCompany(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any());
  }

  /** Returns the stored API payload for a completed retry without parsing or saving again. */
  @Test
  void registerBulkUpload_completedKeyReplaysStoredResponse() throws Exception {
    var file = new MockMultipartFile("file", "vendors.xlsx", "application/octet-stream",
        new byte[] {1, 2, 3});
    Map<String, Object> previous = Map.of("success", true, "registeredRowCount", 2);
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("admin");
    when(idempotencyService.claim(org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.eq("replay-key"), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new Claim(ClaimState.REPLAY, 4L, previous));

    var response = vendorController.registerBulkUpload(file, "replay-key");

    assertThat(response.getBody()).isEqualTo(previous);
    verify(vendorBulkUploadService, never()).parseVendorRows(org.mockito.ArgumentMatchers.any());
    verify(vendorBulkUploadService, never()).registerValidVendors(
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  /** Returns conflict for an active or abandoned claim without starting a second registration. */
  @Test
  void registerBulkUpload_inProgressKeyReturnsConflict() throws Exception {
    var file = new MockMultipartFile("file", "vendors.xlsx", "application/octet-stream",
        new byte[] {1, 2, 3});
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    when(securityContextHelper.getCurrentUsername()).thenReturn("admin");
    when(idempotencyService.claim(org.mockito.ArgumentMatchers.eq(7L),
        org.mockito.ArgumentMatchers.eq("active-key"), org.mockito.ArgumentMatchers.anyString(),
        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new Claim(ClaimState.IN_PROGRESS, 4L, null));

    var response = vendorController.registerBulkUpload(file, "active-key");

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).containsEntry("success", false);
    verify(vendorBulkUploadService, never()).parseVendorRows(org.mockito.ArgumentMatchers.any());
  }

  /** Returns an XLSX response with a safe reference-based filename for tenant-owned reports. */
  @Test
  void downloadBulkUploadReport_returnsExcelContentAndSafeFilename() throws Exception {
    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    var request = new VendorBulkUploadRequest(new com.example.ordermanager.company.entity.Company("Tenant"),
        "key", "0123456789abcdef", "vendors.xlsx", "admin");
    request.complete("{}");
    var reportData = new VendorBulkUploadIdempotencyService.ReportData(request, List.of(), Map.of());
    when(idempotencyService.completedReportData(7L, 44L)).thenReturn(reportData);
    when(vendorBulkUploadService.generateRegistrationReport(reportData, false))
        .thenReturn(new byte[] {1, 2, 3});

    var response = vendorController.downloadBulkUploadReport(44L, "complete");

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getHeaders().getContentType().toString())
        .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    assertThat(response.getHeaders().getFirst("Content-Disposition"))
        .contains("vendor-registration-44-complete.xlsx");
    assertThat((byte[]) response.getBody()).containsExactly(1, 2, 3);
  }

  /** Builds the controller with mocks for tenant context and durable idempotency state. */
  @BeforeEach
  void setUp() {
    vendorController = new VendorController(vendorService, vendorBulkUploadService, companyService,
        securityContextHelper, passwordVerificationService, idempotencyService);
  }

  /** Returns the dedicated vendor upload management view without invoking upload business logic. */
  @Test
  void vendorUploadManagementPage_returnsDedicatedTemplate() {
    assertThat(vendorController.vendorUploadManagementPage())
        .isEqualTo("vendors/bulk-upload-management");
  }

  @Test
  void saveVendor_addSuccess_redirectsToVendorListWithMessage() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Vendor added successfully.");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_updateSuccess_redirectsToVendorListWithMessage() {
    Vendor vendor = new Vendor();
    vendor.setId(11L);
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, null, model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Vendor updated successfully.");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_phoneValidationFailure_returnsFormWithInlinePhoneError() {
    Vendor vendor = new Vendor();
    vendor.setPhone("+9777894561230");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);
    doThrow(new IllegalArgumentException(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210)."))
        .when(vendorService).saveVendorWithCompany(vendor, 7L);

    String view = vendorController.saveVendor(vendor, "/products/new", model, redirectAttributes);

    assertThat(view).isEqualTo("vendors/form");
    assertThat(model.getAttribute("error")).isEqualTo(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("phoneError")).isEqualTo(
        "Vendor Phone No is invalid. Use country code + mobile number (for example +919876543210).");
    assertThat(model.getAttribute("vendor")).isEqualTo(vendor);
    assertThat(model.getAttribute("returnTo")).isEqualTo("/products/new");
  }

  @Test
  void newVendorForm_withReturnTo_loadsFormWithReturnContext() {
    Model model = new ConcurrentModel();

    String view = vendorController.newVendorForm("/products/new?returnTo=%2Forders%2Fnew", model);

    assertThat(view).isEqualTo("vendors/form");
    assertThat(model.getAttribute("vendor")).isNotNull();
    assertThat(model.getAttribute("returnTo")).isEqualTo("/products/new?returnTo=/orders/new");
  }

  @Test
  void saveVendor_withAllowedReturnTo_redirectsToProductFlow() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, "/products/new?returnTo=%2Forders%2Fnew",
        model, redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products/new?returnTo=/orders/new");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_withDuplicateCommaJoinedReturnTo_redirectsToSingleProductPath() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor,
        "/products/new?returnTo=%2Forders%2Fnew,/products/new?returnTo=%2Forders%2Fnew", model,
        redirectAttributes);

    assertThat(view).isEqualTo("redirect:/products/new?returnTo=/orders/new");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void saveVendor_withInvalidReturnTo_fallsBackToVendorsList() {
    Vendor vendor = new Vendor();
    vendor.setCompanyName("Acme Supplies");
    Model model = new ConcurrentModel();
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

    when(securityContextHelper.getCompanyIdFromContext()).thenReturn(7L);

    String view = vendorController.saveVendor(vendor, "https://evil.example/redirect", model,
        redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    verify(vendorService).saveVendorWithCompany(vendor, 7L);
  }

  @Test
  void deleteVendor_success_redirectsToVendorListWithMessage() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("correct")).thenReturn(true);

    String view = vendorController.deleteVendor(21L, "correct", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("message"))
        .isEqualTo("Vendor deleted successfully.");
    verify(vendorService).deleteVendor(21L);
  }

  @Test
  void deleteVendor_wrongPassword_doesNotDeleteAndReturnsError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("wrong")).thenReturn(false);

    String view = vendorController.deleteVendor(999L, "wrong", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("Incorrect password. Vendor was not deleted.");
  }

  @Test
  void deleteVendor_correctPasswordButServiceFails_redirectsWithError() {
    RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
    when(passwordVerificationService.verifyCurrentUserPassword("correct")).thenReturn(true);
    doThrow(new IllegalArgumentException("Vendor not found")).when(vendorService)
        .deleteVendor(999L);

    String view = vendorController.deleteVendor(999L, "correct", redirectAttributes);

    assertThat(view).isEqualTo("redirect:/vendors");
    assertThat(redirectAttributes.getFlashAttributes().get("error"))
        .isEqualTo("Error deleting vendor: Vendor not found");
    verify(vendorService).deleteVendor(999L);
  }
}

