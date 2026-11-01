package com.example.ordermanager.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import com.example.ordermanager.vendor.entity.VendorBulkUploadRequest;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.Helper;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.repository.VendorRepository;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class VendorBulkUploadServiceTest {

  /** Generates and reopens complete/error-only reports, preserving rows and string-safe values. */
  @Test
  void generateRegistrationReport_containsCompleteAndErrorOnlyRowsWithoutFormulaCells()
      throws Exception {
    VendorBulkUploadService reportService =
        new VendorBulkUploadService(vendorRepository, vendorService);
    VendorBulkUploadRequest request = new VendorBulkUploadRequest(new Company("=1+1"), "key",
        "abcdef1234567890", "vendors.xlsx", "admin@example.test");
    request.complete("{}");
    request.setCompletedCounts(3, 2, 1, 1, 1);
    String longError = "validation error ".repeat(500);
    var rows = List.of(
        new VendorBulkRegistrationResult.RowResult(2, "=HYPERLINK(\"https://bad\")",
            VendorBulkRegistrationResult.Status.REGISTERED, null, List.of()),
        new VendorBulkRegistrationResult.RowResult(5, "Failed Vendor",
            VendorBulkRegistrationResult.Status.FAILED, longError, List.of("email: " + longError)),
        new VendorBulkRegistrationResult.RowResult(8, "Skipped Vendor",
            VendorBulkRegistrationResult.Status.SKIPPED, null, List.of("state: unsupported")));
    var data = new VendorBulkUploadIdempotencyService.ReportData(request, rows,
        Map.of(2, LocalDateTime.now()));

    byte[] complete = reportService.generateRegistrationReport(data, false);
    byte[] errors = reportService.generateRegistrationReport(data, true);
    try (XSSFWorkbook completeWorkbook = new XSSFWorkbook(new ByteArrayInputStream(complete));
        XSSFWorkbook errorsWorkbook = new XSSFWorkbook(new ByteArrayInputStream(errors))) {
      assertThat(
          completeWorkbook.getSheet("Registration Summary").getRow(0).getCell(1).getCellType())
          .isEqualTo(CellType.STRING);
      assertThat(completeWorkbook.getSheet("Row Results").getLastRowNum()).isEqualTo(3);
      assertThat(completeWorkbook.getSheet("Row Results").getRow(1).getCell(0).getStringCellValue())
          .isEqualTo("2");
      assertThat(completeWorkbook.getSheet("Row Results").getRow(1).getCell(1).getCellType())
          .isEqualTo(CellType.STRING);
      assertThat(completeWorkbook.getSheet("Row Results").getRow(2).getCell(4).getStringCellValue())
          .contains(longError);
      assertThat(errorsWorkbook.getSheet("Row Results").getLastRowNum()).isEqualTo(2);
      assertThat(errorsWorkbook.getSheet("Row Results").getRow(1).getCell(2).getStringCellValue())
          .isEqualTo("FAILED");
      assertThat(errorsWorkbook.getSheet("Row Results").getRow(2).getCell(2).getStringCellValue())
          .isEqualTo("SKIPPED");
      assertThat(completeWorkbook.getSheet("Row Results").getPaneInformation().isFreezePane())
          .isTrue();
      assertThat(completeWorkbook.getSheet("Row Results").getCTWorksheet().getAutoFilter())
          .isNotNull();
    }
  }

  private final VendorRepository vendorRepository = mock(VendorRepository.class);
  private final VendorService vendorService = mock(VendorService.class);
  private final VendorBulkUploadService service =
      new VendorBulkUploadService(vendorRepository, vendorService);

  /** Registers valid rows through VendorService and verifies tenant ID and mapped vendor data. */
  @Test
  void registerValidVendors_savesAllValidRowsThroughVendorService() {
    var first = validRow(2);
    var second = validRow(3, "27ABCDE1234F1Z3", "next@example.com", "+919876543213");
    when(vendorService.saveVendorWithCompany(any(Vendor.class), eq(73L)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.registerValidVendors(List.of(validResult(first), validResult(second)), 73L);

    assertThat(result.totalParsedRows()).isEqualTo(2);
    assertThat(result.validRowCount()).isEqualTo(2);
    assertThat(result.registeredRowCount()).isEqualTo(2);
    assertThat(result.registrationFailureCount()).isZero();
    assertThat(result.skippedRowCount()).isZero();
    assertThat(result.rowResults()).extracting(VendorBulkRegistrationResult.RowResult::status)
        .containsOnly(VendorBulkRegistrationResult.Status.REGISTERED);
    var vendorCaptor = org.mockito.ArgumentCaptor.forClass(Vendor.class);
    verify(vendorService, org.mockito.Mockito.times(2))
        .saveVendorWithCompany(vendorCaptor.capture(), eq(73L));
    assertThat(vendorCaptor.getAllValues().get(0).getState())
        .isEqualTo(com.example.ordermanager.utils.IndianState.MAHARASHTRA);
  }

  /** Verifies the complete bulk-to-vendor-service path assigns the authenticated tenant entity. */
  @Test
  void registerValidVendors_persistsVendorUnderAuthenticatedCompany() {
    Company authenticatedCompany = new Company();
    authenticatedCompany.setId(73L);
    CompanyService companyService = mock(CompanyService.class);
    VendorRepository persistenceRepository = mock(VendorRepository.class);
    when(companyService.getCompanyById(73L))
        .thenReturn(java.util.Optional.of(authenticatedCompany));
    when(persistenceRepository.save(any(Vendor.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    VendorService realVendorService =
        new VendorService(persistenceRepository, companyService, new Helper());
    VendorBulkUploadService bulkService =
        new VendorBulkUploadService(vendorRepository, realVendorService);

    var result = bulkService.registerValidVendors(List.of(validResult(validRow(2))), 73L);

    var savedVendor = org.mockito.ArgumentCaptor.forClass(Vendor.class);
    verify(persistenceRepository).save(savedVendor.capture());
    assertThat(result.registeredRowCount()).isEqualTo(1);
    assertThat(savedVendor.getValue().getCompany()).isSameAs(authenticatedCompany);
    assertThat(savedVendor.getValue().getCompany().getId()).isEqualTo(73L);
  }

  /** Ensures invalid rows are skipped and never passed to the persistence service. */
  @Test
  void registerValidVendors_invalidRowsAreNeverSaved() {
    var invalid = invalidResult(4, List.of("email: invalid format", "gstn: duplicate"));

    var result = service.registerValidVendors(List.of(invalid), 73L);

    assertThat(result.invalidRowCount()).isEqualTo(1);
    assertThat(result.skippedRowCount()).isEqualTo(1);
    assertThat(result.registeredRowCount()).isZero();
    assertThat(result.rowResults().get(0).status())
        .isEqualTo(VendorBulkRegistrationResult.Status.SKIPPED);
    assertThat(result.rowResults().get(0).validationErrors())
        .containsExactly("email: invalid format", "gstn: duplicate");
    verify(vendorService, never()).saveVendorWithCompany(any(Vendor.class), any());
  }

  /** Continues after a row failure while preserving counts, row numbers, and failure reasons. */
  @Test
  void registerValidVendors_mixedRowsIsolatesSingleRegistrationFailure() {
    var saved = validRow(2);
    var invalid = invalidResult(3, List.of("companyName: required"));
    var failed = validRow(4, "27ABCDE1234F1Z4", "fail@example.com", "+919876543214");
    when(vendorService.saveVendorWithCompany(any(Vendor.class), eq(73L))).thenAnswer(invocation -> {
      Vendor vendor = invocation.getArgument(0);
      if (vendor.getEmail().equals("fail@example.com")) {
        throw new DataIntegrityViolationException("private database constraint details");
      }
      return vendor;
    });

    var result = service
        .registerValidVendors(List.of(validResult(saved), invalid, validResult(failed)), 73L);

    assertThat(result.totalParsedRows()).isEqualTo(3);
    assertThat(result.validRowCount()).isEqualTo(2);
    assertThat(result.invalidRowCount()).isEqualTo(1);
    assertThat(result.registeredRowCount()).isEqualTo(1);
    assertThat(result.registrationFailureCount()).isEqualTo(1);
    assertThat(result.skippedRowCount()).isEqualTo(1);
    assertThat(result.rowResults().get(2).excelRowNumber()).isEqualTo(4);
    assertThat(result.rowResults().get(2).failureReason())
        .isEqualTo("Vendor conflicts with existing company data or violates a data constraint.");
    assertThat(result.rowResults().get(1).validationErrors())
        .containsExactly("companyName: required");
  }

  /**
   * Propagates unexpected failures as batch errors instead of misreporting them as row failures.
   */
  @Test
  void registerValidVendors_unexpectedFailurePropagates() {
    var first = validResult(validRow(2));
    var failed =
        validResult(validRow(3, "27ABCDE1234F1Z3", "unexpected@example.com", "+919876543213"));
    when(vendorService.saveVendorWithCompany(any(Vendor.class), eq(73L))).thenAnswer(invocation -> {
      Vendor vendor = invocation.getArgument(0);
      if (vendor.getEmail().equals("unexpected@example.com")) {
        throw new IllegalStateException("unexpected persistence failure");
      }
      return vendor;
    });

    assertThatThrownBy(() -> service.registerValidVendors(List.of(first, failed), 73L))
        .isInstanceOf(IllegalStateException.class).hasMessage("unexpected persistence failure");
    verify(vendorService, org.mockito.Mockito.times(2)).saveVendorWithCompany(any(Vendor.class),
        eq(73L));
  }

  /** Handles batches with no valid rows without calling VendorService. */
  @Test
  void registerValidVendors_emptyValidSet_returnsSkippedInvalidRows() {
    var result =
        service.registerValidVendors(List.of(invalidResult(2, List.of("state: unsupported"))), 73L);

    assertThat(result.registeredRowCount()).isZero();
    assertThat(result.registrationFailureCount()).isZero();
    assertThat(result.skippedRowCount()).isEqualTo(1);
    verify(vendorService, never()).saveVendorWithCompany(any(Vendor.class), any());
  }

  /** Prevents duplicate source row numbers from being registered more than once per request. */
  @Test
  void registerValidVendors_duplicateRowNumbers_registerOnlyFirstOccurrence() {
    var first = validRow(5);
    var repeated = validRow(5, "27ABCDE1234F1Z3", "repeat@example.com", "+919876543213");
    when(vendorService.saveVendorWithCompany(any(Vendor.class), eq(73L)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.registerValidVendors(List.of(validResult(first), validResult(repeated)), 73L);

    assertThat(result.registeredRowCount()).isEqualTo(1);
    assertThat(result.skippedRowCount()).isEqualTo(1);
    assertThat(result.rowResults().get(1).failureReason())
        .contains("already processed in the current request");
    verify(vendorService).saveVendorWithCompany(any(Vendor.class), eq(73L));
  }

  /** Ensures a complete vendor record is marked valid with no field errors. */
  @Test
  void validateVendorRows_allFieldsValid_returnsValidResult() {
    var result = service.validateVendorRows(List.of(validRow(2))).get(0);

    assertThat(result.excelRowNumber()).isEqualTo(2);
    assertThat(result.status()).isEqualTo(VendorBulkUploadValidationResult.Status.VALID);
    assertThat(result.errors()).isEmpty();
  }

  /** Ensures every mandatory field independently produces a field-specific required error. */
  @Test
  void validateVendorRows_eachMandatoryFieldMissing_reportsItsError() {
    List<VendorBulkUploadRowDto> missingRows = List.of(
        new VendorBulkUploadRowDto(2, "", "Contact", "a@example.com", "+919876543210",
            "27ABCDE1234F1Z5", "Maharashtra", "Address"),
        new VendorBulkUploadRowDto(3, "Company", "", "a@example.com", "+919876543210",
            "27ABCDE1234F1Z5", "Maharashtra", "Address"),
        new VendorBulkUploadRowDto(4, "Company", "Contact", "", "+919876543210", "27ABCDE1234F1Z5",
            "Maharashtra", "Address"),
        new VendorBulkUploadRowDto(5, "Company", "Contact", "a@example.com", "", "27ABCDE1234F1Z5",
            "Maharashtra", "Address"),
        new VendorBulkUploadRowDto(6, "Company", "Contact", "a@example.com", "+919876543210", "",
            "Maharashtra", "Address"),
        new VendorBulkUploadRowDto(7, "Company", "Contact", "a@example.com", "+919876543210",
            "27ABCDE1234F1Z5", "", "Address"),
        new VendorBulkUploadRowDto(8, "Company", "Contact", "a@example.com", "+919876543210",
            "27ABCDE1234F1Z5", "Maharashtra", ""));
    List<String> fields =
        List.of("companyName", "contactPerson", "email", "phone", "gstn", "state", "address");

    var results = service.validateVendorRows(missingRows);

    for (int index = 0; index < results.size(); index++) {
      int fieldIndex = index;
      assertThat(results.get(index).status())
          .isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
      assertThat(results.get(index).errors())
          .anyMatch(error -> error.startsWith(fields.get(fieldIndex)));
    }
  }

  /** Ensures malformed email, phone, GSTIN, and state values each receive a field error. */
  @Test
  void validateVendorRows_invalidFormats_reportsErrors() {
    var row = new VendorBulkUploadRowDto(9, "Company", "Contact", "not-an-email", "123", "invalid",
        "Not a state", "Address");

    var result = service.validateVendorRows(List.of(row)).get(0);

    assertThat(result.errors()).anyMatch(error -> error.startsWith("email:"));
    assertThat(result.errors()).anyMatch(error -> error.startsWith("phone:"));
    assertThat(result.errors()).anyMatch(error -> error.startsWith("gstn:"));
    assertThat(result.errors()).anyMatch(error -> error.startsWith("state:"));
  }

  /** Ensures a row retains multiple errors and valid/invalid rows are counted independently. */
  @Test
  void validateVendorRows_mixedRows_preservesMultipleErrorsAndStatuses() {
    var invalid = new VendorBulkUploadRowDto(2, "", "", "bad", "bad", "bad", "bad", "");

    var results = service.validateVendorRows(
        List.of(validRow(3, "27ABCDE1234F1Z3", "third@example.com", "+919876543213"), invalid));

    assertThat(results.get(0).status()).isEqualTo(VendorBulkUploadValidationResult.Status.VALID);
    assertThat(results.get(1).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(1).errors()).hasSizeGreaterThan(1);
  }

  /**
   * Normalizes workbook fields consistently while retaining address line breaks and phone zeros.
   */
  @Test
  void parseVendorRows_normalizesImportedFieldsForValidationAndPersistence() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      var row = sheet.createRow(1);
      String[] values = {"  acme   industrial ", "  jANE   O'NEIL  ", " USER@EXAMPLE.COM ",
          "+91 (98765) 43210", " 27abcde1234f1z5 ", " 27 ", " 12   Main St.\r\n  Floor  2  "};
      for (int column = 0; column < values.length; column++) {
        row.createCell(column).setCellValue(values[column]);
      }
    });

    var parsed = service.parseVendorRows(new ByteArrayInputStream(bytes));
    var result = service.validateVendorRows(parsed, 41L).get(0);
    var normalized = result.vendorData();

    assertThat(normalized).isEqualTo(
        new VendorBulkUploadRowDto(2, "ACME INDUSTRIAL", "Jane O'neil", "user@example.com",
            "919876543210", "27ABCDE1234F1Z5", "Maharashtra", "12 Main St.\nFloor 2"));
    assertThat(result.status()).isEqualTo(VendorBulkUploadValidationResult.Status.VALID);
    var leadingZeroPhone = new VendorBulkUploadRowDto(3, "Acme", "Jane Doe", "x@example.com",
        "+390212345678", "27ABCDE1234F1Z6", "Maharashtra", "Address");
    assertThat(service.validateVendorRows(List.of(leadingZeroPhone)).get(0).vendorData().phone())
        .isEqualTo("390212345678");
  }

  /**
   * Ensures repeat parsing and validation produce the same normalized preview and registration
   * rows.
   */
  @Test
  void validateVendorRows_previewAndRegistrationParsing_areConsistent() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      writeVendor(sheet.createRow(1), new VendorBulkUploadRowDto(2, " acme  ltd ", " JANE DOE ",
          " USER@EXAMPLE.COM ", "+91 98765 43210", "27abcde1234f1z5", "27", " One  Main Rd "));
    });

    var preview =
        service.validateVendorRows(service.parseVendorRows(new ByteArrayInputStream(bytes)), 41L);
    var registration =
        service.validateVendorRows(service.parseVendorRows(new ByteArrayInputStream(bytes)), 41L);

    assertThat(registration).isEqualTo(preview);
    assertThat(registration.get(0).status())
        .isEqualTo(VendorBulkUploadValidationResult.Status.VALID);
  }

  /** Detects in-file identifier duplicates despite case, spacing, and phone punctuation changes. */
  @Test
  void validateVendorRows_formattedIdentifiers_areDuplicatesAfterNormalization() {
    var first = new VendorBulkUploadRowDto(2, "ACME", "Jane Doe", "USER@EXAMPLE.COM",
        "+919876543210", "27ABCDE1234F1Z5", "Maharashtra", "Address");
    var second = new VendorBulkUploadRowDto(5, "Other", "John Doe", " user@example.com ",
        "+91 98765-43210", " 27abcde1234f1z5 ", "Maharashtra", "Address");

    var results = service.validateVendorRows(List.of(first, second), 41L);

    assertThat(results)
        .allMatch(result -> result.status() == VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(1).errors())
        .anyMatch(error -> error.startsWith("gstn: Duplicate GSTIN"));
    assertThat(results.get(1).errors())
        .anyMatch(error -> error.startsWith("email: Duplicate Email"));
    assertThat(results.get(1).errors())
        .anyMatch(error -> error.startsWith("phone: Duplicate Phone"));
  }

  /**
   * Verifies all occurrences across a generated 14-vendor workbook are invalid for each identifier.
   */
  @Test
  void validateVendorRows_fourteenRowWorkbook_marksEveryDuplicateOccurrenceInvalid()
      throws Exception {
    byte[] completedWorkbook;
    try (
        var workbook =
            new XSSFWorkbook(new ByteArrayInputStream(service.generateBulkUploadTemplate()));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.getSheet("Vendor Data");
      for (int index = 0; index < 14; index++) {
        var data = validRow(index + 3, "27ABCDE1234F1Z" + (char) ('A' + index),
            "vendor" + index + "@example.com", "+9198765" + String.format("%05d", index + 1));
        if (index == 5) {
          data = withIdentifiers(data, "27ABCDE1234F1ZA", data.email(), data.phone());
        } else if (index == 9) {
          data = withIdentifiers(data, data.gstn(), "vendor1@example.com", data.phone());
        } else if (index == 13) {
          data = withIdentifiers(data, data.gstn(), data.email(), "+919876500003");
        } else if (index == 2) {
          data = withIdentifiers(data, data.gstn(), data.email(), "+919876500003");
        }
        writeVendor(sheet.createRow(index + 2), data);
      }
      workbook.write(output);
      completedWorkbook = output.toByteArray();
    }

    var parsed = service.parseVendorRows(new ByteArrayInputStream(completedWorkbook));
    var results = service.validateVendorRows(parsed, 41L);

    assertThat(results).hasSize(14);
    assertThat(results.get(0).excelRowNumber()).isEqualTo(3);
    assertThat(results.get(5).excelRowNumber()).isEqualTo(8);
    assertThat(results.get(0).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(5).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(1).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(9).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(2).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(13).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(3).status()).isEqualTo(VendorBulkUploadValidationResult.Status.VALID);
    assertThat(results.get(0).errors()).anyMatch(error -> error.startsWith("gstn:"));
    assertThat(results.get(1).errors()).anyMatch(error -> error.startsWith("email:"));
    assertThat(results.get(2).errors()).anyMatch(error -> error.startsWith("phone:"));
  }

  /** Detects repeated GSTIN values in non-consecutive worksheet rows. */
  @Test
  void validateVendorRows_duplicateGstinAcrossRows_reportsBothRowNumbers() {
    var rows = List.of(validRow(2, "27ABCDE1234F1Z5", "first@example.com", "+919876543210"),
        validRow(3, "27ABCDE1234F1Z3", "middle@example.com", "+919876543213"),
        validRow(6, " 27abcde1234f1z5 ", "last@example.com", "+919876543216"));

    var results = service.validateVendorRows(rows, 41L);

    assertThat(results.get(2).errors()).contains("gstn: Duplicate GSTIN found in Excel rows 2, 6.");
    assertThat(results.get(0).errors()).contains("gstn: Duplicate GSTIN found in Excel rows 2, 6.");
    assertThat(results.get(0).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
    assertThat(results.get(2).status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
  }

  /** Detects case-insensitive email duplicates within the uploaded workbook. */
  @Test
  void validateVendorRows_duplicateEmailWithinFile_reportsFieldError() {
    var rows = List.of(validRow(2, "27ABCDE1234F1Z5", "same@example.com", "+919876543210"),
        validRow(3, "27ABCDE1234F3", " SAME@EXAMPLE.COM ", "+919876543213"));

    var results = service.validateVendorRows(rows, 41L);

    assertThat(results.get(1).errors())
        .anyMatch(error -> error.startsWith("email: Duplicate Email"));
    assertThat(results.get(0).errors())
        .anyMatch(error -> error.startsWith("email: Duplicate Email"));
    assertThat(results)
        .allMatch(row -> row.status() == VendorBulkUploadValidationResult.Status.INVALID);
  }

  /** Detects phone duplicates after applying the existing international phone normalization. */
  @Test
  void validateVendorRows_duplicatePhoneWithinFile_reportsFieldError() {
    var rows = List.of(validRow(2, "27ABCDE1234F1Z5", "first@example.com", "+91 98765-43210"),
        validRow(3, "27ABCDE1234F3", "second@example.com", "919876543210"));

    var results = service.validateVendorRows(rows, 41L);

    assertThat(results.get(1).errors())
        .anyMatch(error -> error.startsWith("phone: Duplicate Phone"));
    assertThat(results.get(0).errors())
        .anyMatch(error -> error.startsWith("phone: Duplicate Phone"));
    assertThat(results)
        .allMatch(row -> row.status() == VendorBulkUploadValidationResult.Status.INVALID);
  }

  /** Adds independent duplicate messages for every matching business identifier on a row. */
  @Test
  void validateVendorRows_multipleDuplicateIdentifiers_addsAllErrors() {
    var rows = List.of(validRow(2), validRow(3));

    var duplicate = service.validateVendorRows(rows, 41L).get(1);

    assertThat(duplicate.errors()).anyMatch(error -> error.startsWith("gstn: Duplicate GSTIN"));
    assertThat(duplicate.errors()).anyMatch(error -> error.startsWith("email: Duplicate Email"));
    assertThat(duplicate.errors()).anyMatch(error -> error.startsWith("phone: Duplicate Phone"));
  }

  /** Detects a tenant vendor matching the uploaded GSTIN using one company-scoped lookup. */
  @Test
  void validateVendorRows_existingGstinForCompany_isInvalid() {
    when(vendorRepository.findByCompanyIdAndGstnInIgnoreCase(41L,
        java.util.Set.of("27ABCDE1234F1Z5"))).thenReturn(List.of(existingVendor("27abcde1234f1z5",
            "other@example.com", "919999999999")));

    var uploaded = new VendorBulkUploadRowDto(2, " acme ", " Jane Doe ", " jane@example.com ",
        "+919876543210", " 27abcde1234f1z5 ", "Maharashtra", "Address");
    var result = service.validateVendorRows(List.of(uploaded), 41L).get(0);

    assertThat(result.errors()).contains("gstn: GSTIN already exists for this company.");
    verify(vendorRepository).findByCompanyIdAndGstnInIgnoreCase(41L,
        java.util.Set.of("27ABCDE1234F1Z5"));
  }

  /** Detects a tenant vendor matching the uploaded email using one company-scoped lookup. */
  @Test
  void validateVendorRows_existingEmailForCompany_isInvalid() {
    when(vendorRepository.findByCompanyIdAndEmailInIgnoreCase(41L,
        java.util.Set.of("jane@example.com"))).thenReturn(List.of(existingVendor("27ABCDE1234F3",
            "JANE@example.com", "919999999999")));

    var uploaded = new VendorBulkUploadRowDto(2, "Acme", "Jane", " JANE@EXAMPLE.COM ",
        "+919876543210", "27ABCDE1234F1Z5", "Maharashtra", "Address");
    var result = service.validateVendorRows(List.of(uploaded), 41L).get(0);

    assertThat(result.errors()).contains("email: Email already exists for this company.");
  }

  /** Detects a tenant vendor matching the uploaded normalized phone using a bulk lookup. */
  @Test
  void validateVendorRows_existingPhoneForCompany_isInvalid() {
    when(vendorRepository.findByCompanyIdAndPhoneIn(41L, java.util.Set.of("919876543210")))
        .thenReturn(List.of(existingVendor("27ABCDE1234F3", "other@example.com",
            "+91 98765-43210")));

    var uploaded = new VendorBulkUploadRowDto(2, "Acme", "Jane", "jane@example.com",
        "+91 98765 43210", "27ABCDE1234F1Z5", "Maharashtra", "Address");
    var result = service.validateVendorRows(List.of(uploaded), 41L).get(0);

    assertThat(result.errors()).contains("phone: Phone already exists for this company.");
  }

  /** Keeps an identifier owned by a different tenant from being considered a duplicate. */
  @Test
  void validateVendorRows_otherCompanyIdentifiers_areNotDuplicates() {
    Company otherCompany = new Company();
    otherCompany.setId(42L);
    Vendor otherTenantVendor =
        existingVendor("27ABCDE1234F1Z5", "jane@example.com", "919876543210");
    otherTenantVendor.setCompany(otherCompany);
    when(vendorRepository.findByCompanyIdAndGstnInIgnoreCase(42L,
        java.util.Set.of("27ABCDE1234F1Z5"))).thenReturn(List.of(otherTenantVendor));
    when(vendorRepository.findByCompanyIdAndEmailInIgnoreCase(42L,
        java.util.Set.of("jane@example.com"))).thenReturn(List.of(otherTenantVendor));
    when(vendorRepository.findByCompanyIdAndPhoneIn(42L, java.util.Set.of("919876543210")))
        .thenReturn(List.of(otherTenantVendor));

    var uploaded = new VendorBulkUploadRowDto(2, "Acme", "Jane", " JANE@EXAMPLE.COM ",
        "+91 98765-43210", " 27abcde1234f1z5 ", "Maharashtra", "Address");
    var result = service.validateVendorRows(List.of(uploaded), 41L).get(0);

    assertThat(result.status()).isEqualTo(VendorBulkUploadValidationResult.Status.VALID);
    verify(vendorRepository).findByCompanyIdAndGstnInIgnoreCase(41L,
        java.util.Set.of("27ABCDE1234F1Z5"));
    verify(vendorRepository, org.mockito.Mockito.never()).findByCompanyIdAndGstnInIgnoreCase(42L,
        java.util.Set.of("27ABCDE1234F1Z5"));
    verify(vendorRepository, org.mockito.Mockito.never()).findByCompanyIdAndEmailInIgnoreCase(42L,
        java.util.Set.of("jane@example.com"));
    verify(vendorRepository, org.mockito.Mockito.never()).findByCompanyIdAndPhoneIn(42L,
        java.util.Set.of("919876543210"));
  }

  /** Preserves required-field errors while adding an existing-company duplicate error. */
  @Test
  void validateVendorRows_duplicateAndFieldErrors_areBothRetained() {
    when(vendorRepository.findByCompanyIdAndEmailInIgnoreCase(41L,
        java.util.Set.of("jane@example.com"))).thenReturn(List.of(existingVendor("27ABCDE1234F3",
            "jane@example.com", "919999999999")));
    var invalid = new VendorBulkUploadRowDto(2, "", "Contact", "jane@example.com",
        "+919876543210", "27ABCDE1234F1Z5", "Maharashtra", "Address");

    var result = service.validateVendorRows(List.of(invalid), 41L).get(0);

    assertThat(result.errors()).contains("companyName: Company Name is required.",
        "email: Email already exists for this company.");
    assertThat(result.status()).isEqualTo(VendorBulkUploadValidationResult.Status.INVALID);
  }

  /** Creates a vendor fixture for repository duplicate-query results. */
  private Vendor existingVendor(String gstn, String email, String phone) {
    Vendor vendor = new Vendor();
    vendor.setGstn(gstn);
    vendor.setEmail(email);
    vendor.setPhone(phone);
    return vendor;
  }

  /** Creates a known-valid row fixture with the requested original worksheet row number. */
  private VendorBulkUploadRowDto validRow(int rowNumber) {
    return validRow(rowNumber, "27ABCDE1234F1Z5", "jane@example.com", "+919876543210");
  }

  /** Creates a valid row with distinct business identifiers for duplicate-detection cases. */
  private VendorBulkUploadRowDto validRow(int rowNumber, String gstin, String email, String phone) {
    return new VendorBulkUploadRowDto(rowNumber, "ACME " + rowNumber, "Jane Doe", email, phone,
        gstin, "Maharashtra", "Mumbai");
  }

  /** Wraps a known-valid parsed row as a validation result for registration tests. */
  private VendorBulkUploadValidationResult validResult(VendorBulkUploadRowDto row) {
    return new VendorBulkUploadValidationResult(row.excelRowNumber(), row,
        VendorBulkUploadValidationResult.Status.VALID, List.of());
  }

  /** Wraps a parsed row with supplied field errors to verify invalid-row skip behavior. */
  private VendorBulkUploadValidationResult invalidResult(int rowNumber, List<String> errors) {
    VendorBulkUploadRowDto row = validRow(rowNumber);
    return new VendorBulkUploadValidationResult(rowNumber, row,
        VendorBulkUploadValidationResult.Status.INVALID, errors);
  }

  /** Verifies the untouched template example is omitted while user-entered rows remain. */
  @Test
  void parseVendorRows_excludesTemplateSampleAndKeepsActualVendors() throws Exception {
    byte[] template = service.generateBulkUploadTemplate();
    byte[] completedWorkbook;
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(template));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.getSheet("Vendor Data");
      var firstVendor = sheet.createRow(2);
      writeVendor(firstVendor, "Example-independent Vendor", "vendor@example.com");
      sheet.createRow(3);
      var secondVendor = sheet.createRow(4);
      writeVendor(secondVendor, "Second Vendor", "second@example.com");
      workbook.write(output);
      completedWorkbook = output.toByteArray();
    }

    var parsed = service.parseVendorRows(new ByteArrayInputStream(completedWorkbook));

    assertThat(parsed).hasSize(2);
    assertThat(parsed).extracting(VendorBulkUploadRowDto::excelRowNumber).containsExactly(3, 5);
    assertThat(parsed).extracting(VendorBulkUploadRowDto::companyName)
        .containsExactly("EXAMPLE-INDEPENDENT VENDOR", "SECOND VENDOR");
  }

  /** Writes a complete vendor row using the template's seven-column layout. */
  private void writeVendor(org.apache.poi.ss.usermodel.Row row, String name, String email) {
    String[] values =
        {name, "Contact Person", email, "+919876543210", "GSTIN", "Maharashtra", "Vendor Address"};
    for (int column = 0; column < values.length; column++) {
      row.createCell(column).setCellValue(values[column]);
    }
  }

  /** Writes all seven fields from a parsed vendor row into the matching worksheet columns. */
  private void writeVendor(org.apache.poi.ss.usermodel.Row row, VendorBulkUploadRowDto vendor) {
    String[] values = {vendor.companyName(), vendor.contactPerson(), vendor.email(), vendor.phone(),
        vendor.gstn(), vendor.state(), vendor.address()};
    for (int column = 0; column < values.length; column++) {
      row.createCell(column).setCellValue(values[column]);
    }
  }

  /** Copies a row with selected identifiers replaced to construct duplicate workbook records. */
  private VendorBulkUploadRowDto withIdentifiers(VendorBulkUploadRowDto row, String gstin,
      String email, String phone) {
    return new VendorBulkUploadRowDto(row.excelRowNumber(), row.companyName(), row.contactPerson(),
        email, phone, gstin, row.state(), row.address());
  }

  /** Verifies parsing trims/collapses text and ignores blank worksheet rows. */
  @Test
  void parseVendorRows_mapsColumnsAndSkipsBlankRows() throws Exception {
    byte[] workbookBytes;
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Vendor Data");
      writeHeaders(sheet.createRow(0));
      var row = sheet.createRow(1);
      row.createCell(0).setCellValue("  acme   supplies ");
      row.createCell(1).setCellValue("Jane Doe");
      row.createCell(2).setCellValue("jane@example.com");
      row.createCell(3).setCellValue(919876543210d);
      row.createCell(4).setCellValue("GSTIN");
      row.createCell(5).setCellValue("Maharashtra");
      row.createCell(6).setCellValue("  Mumbai   India ");
      sheet.createRow(2);
      workbook.write(output);
      workbookBytes = output.toByteArray();
    }

    var rows = service.parseVendorRows(new ByteArrayInputStream(workbookBytes));

    assertThat(rows).containsExactly(new VendorBulkUploadRowDto(2, "ACME SUPPLIES", "Jane Doe",
        "jane@example.com", "919876543210", "GSTIN", "Maharashtra", "Mumbai India"));
  }

  /** Verifies workbooks without the expected data worksheet fail with a readable parse error. */
  @Test
  void parseVendorRows_missingDataSheet_throwsIOException() throws Exception {
    byte[] workbookBytes;
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      workbook.createSheet("Other");
      workbook.write(output);
      workbookBytes = output.toByteArray();
    }

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(workbookBytes)))
        .isInstanceOf(IOException.class).hasMessageContaining("Vendor Data");
  }

  /** Rejects content that is not OOXML even when callers label it as an XLSX upload. */
  @Test
  void parseVendorRows_nonOoxmlContent_rejectsSpoofedWorkbook() {
    byte[] csvContent = "Company Name*,Email*\nAcme,acme@example.com".getBytes();

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(csvContent)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("valid or readable .xlsx");
  }

  /**
   * Rejects arbitrary corrupt bytes with the same controlled workbook error as other bad content.
   */
  @Test
  void parseVendorRows_corruptXlsx_rejectsWorkbook() {
    assertThatThrownBy(
        () -> service.parseVendorRows(new ByteArrayInputStream(new byte[] {0, 1, 2})))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("valid or readable .xlsx");
  }

  /** Rejects workbooks without all seven required headers before reading vendor rows. */
  @Test
  void parseVendorRows_missingMandatoryHeader_rejectsWorkbook() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      sheet.getRow(0).getCell(6).setCellValue("");
      writeVendor(sheet.createRow(1), validRow(2));
    });

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class).hasMessageContaining("Address*")
        .hasMessageContaining("blank or missing");
  }

  /** Rejects renamed columns when a mandatory template header is absent. */
  @Test
  void parseVendorRows_renamedMandatoryHeader_rejectsWorkbook() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      sheet.getRow(0).getCell(6).setCellValue("Location");
    });

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("Required header 'Address*' is missing");
  }

  /** Rejects duplicate header names instead of assigning data to an ambiguous field. */
  @Test
  void parseVendorRows_duplicateHeaders_rejectsWorkbook() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      sheet.getRow(0).getCell(1).setCellValue("Company Name*");
    });

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("Duplicate header");
  }

  /** Accepts expected headers when case and surrounding whitespace differ from the template. */
  @Test
  void parseVendorRows_normalizesHeaderWhitespaceAndCase() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      for (int column = 0; column < 7; column++) {
        sheet.getRow(0).getCell(column).setCellValue(
            "  " + sheet.getRow(0).getCell(column).getStringCellValue().toLowerCase() + "  ");
      }
      writeVendor(sheet.createRow(1), validRow(2));
    });

    assertThat(service.parseVendorRows(new ByteArrayInputStream(bytes))).hasSize(1);
  }

  /** Rejects workbooks that contain headers but no populated vendor rows. */
  @Test
  void parseVendorRows_headersOnly_rejectsWorkbookWithoutVendorData() throws Exception {
    byte[] bytes = workbookBytes(sheet -> writeHeaders(sheet.createRow(0)));

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("no vendor data rows");
  }

  /** Rejects a valid but empty Vendor Data worksheet before row parsing. */
  @Test
  void parseVendorRows_emptyDataSheet_rejectsWorkbook() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
    });

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class).hasMessageContaining("header row");
  }

  /** Rejects merged vendor fields that could make values appear under the wrong column. */
  @Test
  void parseVendorRows_mergedVendorCells_rejectsWorkbook() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 1));
    });

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("Merged cells are not allowed");
  }

  /** Ignores completely blank data rows while rejecting formulas without evaluating them. */
  @Test
  void parseVendorRows_formulaCell_rejectsWithoutEvaluatingFormula() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      var row = sheet.createRow(1);
      writeVendor(row, validRow(2));
      row.getCell(0).setCellFormula("1+1");
      sheet.createRow(2);
    });

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("Formula cells are not allowed");
  }

  /** Rejects a populated cell whose type cannot safely represent a vendor text field. */
  @Test
  void parseVendorRows_booleanCell_rejectsUnexpectedCellType() throws Exception {
    byte[] bytes = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      var row = sheet.createRow(1);
      writeVendor(row, validRow(2));
      row.getCell(0).setCellValue(true);
    });

    assertThatThrownBy(() -> service.parseVendorRows(new ByteArrayInputStream(bytes)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("Unsupported cell value");
  }

  /** Enforces the configured row ceiling and retains formatted zero padding for phone cells. */
  @Test
  void parseVendorRows_respectsRowLimitAndFormatsNumericPhone() throws Exception {
    VendorBulkUploadService limitedService =
        new VendorBulkUploadService(vendorRepository, vendorService, 1);
    byte[] tooManyRows = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      writeVendor(sheet.createRow(1), validRow(2));
      writeVendor(sheet.createRow(2),
          validRow(3, "27ABCDE1234F1Z3", "b@example.com", "+919876543213"));
    });

    assertThatThrownBy(() -> limitedService.parseVendorRows(new ByteArrayInputStream(tooManyRows)))
        .isInstanceOf(VendorBulkUploadValidationException.class)
        .hasMessageContaining("maximum of 1 vendor data rows");

    byte[] paddedPhone = workbookBytes(sheet -> {
      writeHeaders(sheet.createRow(0));
      var row = sheet.createRow(1);
      writeVendor(row, validRow(2));
      row.getCell(3).setCellValue(12345);
      var phoneStyle = sheet.getWorkbook().createCellStyle();
      phoneStyle.setDataFormat(sheet.getWorkbook().createDataFormat().getFormat("00000000000"));
      row.getCell(3).setCellStyle(phoneStyle);
    });
    assertThat(service.parseVendorRows(new ByteArrayInputStream(paddedPhone)).get(0).phone())
        .isEqualTo("00000012345");
  }

  /** Creates an in-memory OOXML workbook for the supplied Vendor Data sheet setup. */
  private byte[] workbookBytes(java.util.function.Consumer<org.apache.poi.ss.usermodel.Sheet> setup)
      throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Vendor Data");
      setup.accept(sheet);
      workbook.write(output);
      return output.toByteArray();
    }
  }

  /** Writes template-compatible headers for generated workbook tests. */
  private void writeHeaders(org.apache.poi.ss.usermodel.Row row) {
    String[] headers =
        {"Company Name*", "Contact Person*", "Email*", "Phone*", "GSTN*", "State*", "Address*"};
    for (int column = 0; column < headers.length; column++) {
      row.createCell(column).setCellValue(headers[column]);
    }
  }
}
