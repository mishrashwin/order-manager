package com.example.ordermanager.vendor.service;

import com.example.ordermanager.utils.IndianState;
import com.example.ordermanager.utils.Helper;
import com.example.ordermanager.utils.PhoneNumberUtils;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.repository.VendorRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

@Service
public class VendorBulkUploadService {

  private static final Logger LOGGER = LoggerFactory.getLogger(VendorBulkUploadService.class);

  private static final String[] VENDOR_HEADERS =
      {"Company Name*", "Contact Person*", "Email*", "Phone*", "GSTN*", "State*", "Address*"};

  private final VendorRepository vendorRepository;
  private final VendorService vendorService;
  private final VendorBulkUploadRowRegistrationService rowRegistrationService;
  private final Helper helper;
  private final int maxVendorDataRows;

  /**
   * Creates the upload service with tenant-scoped duplicate lookups and VendorService delegation.
   */
  @Autowired
  public VendorBulkUploadService(VendorRepository vendorRepository, VendorService vendorService,
      Helper helper, @Value("${app.vendor.bulk-upload.max-data-rows:1000}") int maxVendorDataRows,
      VendorBulkUploadRowRegistrationService rowRegistrationService) {
    this.vendorRepository = vendorRepository;
    this.vendorService = vendorService;
    this.rowRegistrationService = rowRegistrationService;
    this.helper = helper;
    if (maxVendorDataRows < 1) {
      throw new IllegalArgumentException("Maximum vendor upload row count must be positive.");
    }
    this.maxVendorDataRows = maxVendorDataRows;
  }

  /** Keeps direct unit-test construction aligned with the configured default row limit. */
  public VendorBulkUploadService(VendorRepository vendorRepository, VendorService vendorService) {
    this.vendorRepository = vendorRepository;
    this.vendorService = vendorService;
    this.helper = new Helper();
    this.maxVendorDataRows = 1000;
    this.rowRegistrationService = null;
  }

  /** Keeps direct row-limit test construction aligned with the production normalization helper. */
  public VendorBulkUploadService(VendorRepository vendorRepository, VendorService vendorService,
      int maxVendorDataRows) {
    this.vendorRepository = vendorRepository;
    this.vendorService = vendorService;
    this.helper = new Helper();
    if (maxVendorDataRows < 1) {
      throw new IllegalArgumentException("Maximum vendor upload row count must be positive.");
    }
    this.maxVendorDataRows = maxVendorDataRows;
    this.rowRegistrationService = null;
  }

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
  private static final Pattern GSTIN_PATTERN =
      Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

  /** Validates an OOXML workbook and parses populated Vendor Data rows with source row numbers. */
  public List<VendorBulkUploadRowDto> parseVendorRows(InputStream inputStream) throws IOException {
    if (inputStream == null) {
      throw new VendorBulkUploadValidationException("The uploaded workbook could not be read.");
    }
    List<VendorBulkUploadRowDto> parsedRows = new ArrayList<>();
    try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
      Sheet sheet = workbook.getSheet("Vendor Data");
      if (sheet == null) {
        throw new VendorBulkUploadValidationException(
            "Workbook is missing the required Vendor Data worksheet.");
      }
      validateVendorSheetStructure(sheet);
      DataFormatter formatter = new DataFormatter();
      for (Row row : sheet) {
        if (row.getRowNum() == 0) {
          continue;
        }
        String[] values = new String[7];
        boolean hasValue = false;
        for (int columnIndex = 0; columnIndex < values.length; columnIndex++) {
          Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
          String value = cell == null ? ""
              : formatCellValue(cell, formatter, columnIndex, row.getRowNum() + 1);
          values[columnIndex] = normalizeImportedField(columnIndex, value);
          hasValue |= !values[columnIndex].isEmpty();
        }
        validateNoUnexpectedDataColumns(row);
        if (hasValue && !isTemplateSampleRow(values)) {
          if (parsedRows.size() >= maxVendorDataRows) {
            throw new VendorBulkUploadValidationException(
                "Workbook exceeds the maximum of " + maxVendorDataRows + " vendor data rows.");
          }
          parsedRows.add(new VendorBulkUploadRowDto(row.getRowNum() + 1, values[0], values[1],
              values[2], values[3], values[4], values[5], values[6]));
        }
      }
      if (parsedRows.isEmpty()) {
        throw new VendorBulkUploadValidationException("Workbook contains no vendor data rows.");
      }
    } catch (VendorBulkUploadValidationException e) {
      throw e;
    } catch (IOException | RuntimeException e) {
      throw new VendorBulkUploadValidationException(
          "The uploaded file is not a valid or readable .xlsx workbook.", e);
    }
    return parsedRows;
  }

  /** Checks required template headers and rejects merged vendor fields before reading data rows. */
  private void validateVendorSheetStructure(Sheet sheet)
      throws VendorBulkUploadValidationException {
    Row header = sheet.getRow(0);
    if (header == null) {
      throw new VendorBulkUploadValidationException("Vendor Data is missing its header row.");
    }
    Set<String> foundHeaders = new HashSet<>();
    int lastHeaderCell = Math.max(header.getLastCellNum(), VENDOR_HEADERS.length);
    for (int column = 0; column < lastHeaderCell; column++) {
      Cell cell = header.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      String value = cell == null ? "" : readHeaderCell(cell, column);
      String normalized = normalizeHeader(value);
      if (normalized.isBlank()) {
        if (column < VENDOR_HEADERS.length) {
          throw new VendorBulkUploadValidationException(
              "Required header '" + VENDOR_HEADERS[column] + "' is blank or missing.");
        }
        continue;
      }
      if (!foundHeaders.add(normalized)) {
        throw new VendorBulkUploadValidationException(
            "Duplicate header '" + value.trim() + "' appears more than once.");
      }
      if (column >= VENDOR_HEADERS.length) {
        throw new VendorBulkUploadValidationException(
            "Unexpected header '" + value.trim() + "' in Vendor Data.");
      }
    }
    for (String required : VENDOR_HEADERS) {
      if (!foundHeaders.contains(normalizeHeader(required))) {
        throw new VendorBulkUploadValidationException(
            "Required header '" + required + "' is missing from Vendor Data.");
      }
    }
    for (int column = 0; column < VENDOR_HEADERS.length; column++) {
      Cell cell = header.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (!normalizeHeader(cell.getStringCellValue())
          .equals(normalizeHeader(VENDOR_HEADERS[column]))) {
        throw new VendorBulkUploadValidationException(
            "Vendor Data headers must follow the template column order.");
      }
    }
    for (int index = 0; index < sheet.getNumMergedRegions(); index++) {
      var range = sheet.getMergedRegion(index);
      if (range.getFirstColumn() < VENDOR_HEADERS.length && range.getLastColumn() >= 0) {
        throw new VendorBulkUploadValidationException(
            "Merged cells are not allowed in the Vendor Data worksheet.");
      }
    }
  }

  /** Reads a text-only header cell and reports unexpected header cell types safely. */
  private String readHeaderCell(Cell cell, int column) throws VendorBulkUploadValidationException {
    if (cell.getCellType() != CellType.STRING) {
      throw new VendorBulkUploadValidationException(
          "Header in column " + (column + 1) + " must contain text.");
    }
    return cell.getStringCellValue();
  }

  /** Rejects non-empty values in columns outside the seven supported vendor fields. */
  private void validateNoUnexpectedDataColumns(Row row) throws VendorBulkUploadValidationException {
    for (int column = VENDOR_HEADERS.length; column < row.getLastCellNum(); column++) {
      Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      if (cell != null
          && (cell.getCellType() != CellType.STRING ? cell.getCellType() != CellType.BLANK
              : !cell.getStringCellValue().isBlank())) {
        throw new VendorBulkUploadValidationException("Unexpected data in column " + (column + 1)
            + " at Excel row " + (row.getRowNum() + 1) + ".");
      }
    }
  }

  /** Normalizes header text for case-insensitive comparisons while preserving template labels. */
  private String normalizeHeader(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  /** Applies the same bulk-import normalization before preview validation and registration. */
  private List<VendorBulkUploadRowDto> normalizeVendorRows(List<VendorBulkUploadRowDto> rows) {
    return rows.stream()
        .map(row -> new VendorBulkUploadRowDto(row.excelRowNumber(),
            normalizeImportedField(0, row.companyName()),
            normalizeImportedField(1, row.contactPerson()), normalizeImportedField(2, row.email()),
            normalizeImportedField(3, row.phone()), normalizeImportedField(4, row.gstn()),
            normalizeImportedField(5, row.state()), normalizeImportedField(6, row.address())))
        .toList();
  }

  /** Normalizes one vendor field using established naming, phone, and state utilities. */
  private String normalizeImportedField(int columnIndex, String value) {
    String normalized = collapseWhitespace(value);
    if (normalized.isEmpty()) {
      return normalized;
    }
    return switch (columnIndex) {
      case 0 -> normalized.toUpperCase(Locale.ROOT);
      case 1 -> helper.toTitleCase(normalized);
      case 2 -> normalized.toLowerCase(Locale.ROOT);
      case 3 -> normalizePhoneValue(normalized);
      case 4 -> normalized.toUpperCase(Locale.ROOT);
      case 5 -> normalizeState(normalized);
      case 6 -> normalizeAddress(value);
      default -> normalized;
    };
  }

  /** Trims and collapses whitespace for single-line vendor fields. */
  private String collapseWhitespace(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", " ");
  }

  /** Canonicalizes valid international phones and retains invalid input for row-level errors. */
  private String normalizePhoneValue(String value) {
    try {
      return PhoneNumberUtils.normalizeRequiredInternational(value, "Vendor Phone No");
    } catch (IllegalArgumentException e) {
      return value;
    }
  }

  /** Resolves an IndianState display name or existing state-code alias to its display name. */
  private String normalizeState(String value) {
    IndianState state = IndianState.fromDisplayName(value);
    if (state == null) {
      state = IndianState.fromCode(value);
    }
    return state == null ? value : state.getDisplayName();
  }

  /** Collapses horizontal address whitespace while retaining meaningful line breaks. */
  private String normalizeAddress(String value) {
    if (value == null) {
      return "";
    }
    String[] lines = value.replace("\r\n", "\n").replace('\r', '\n').trim().split("\\n", -1);
    return java.util.Arrays.stream(lines)
        .map(line -> line.replaceAll("[\\t\\x0B\\f ]+", " ").trim())
        .collect(java.util.stream.Collectors.joining("\n"));
  }

  /**
   * Validates required vendor fields and returns every field error for each parsed worksheet row.
   */
  public List<VendorBulkUploadValidationResult> validateVendorRows(
      List<VendorBulkUploadRowDto> rows) {
    return normalizeVendorRows(rows).stream().map(this::validateVendorRow).toList();
  }

  /**
   * Adds file and company-scoped database duplicate errors to existing field validation results.
   */
  public List<VendorBulkUploadValidationResult> validateVendorRows(
      List<VendorBulkUploadRowDto> rows, Long companyId) {
    List<VendorBulkUploadRowDto> normalizedRows = normalizeVendorRows(rows);
    List<VendorBulkUploadValidationResult> validatedRows =
        normalizedRows.stream().map(this::validateVendorRow).toList();
    Set<String> gstins =
        collectKeys(normalizedRows, VendorBulkUploadRowDto::gstn, this::normalizeGstn);
    Set<String> emails =
        collectKeys(normalizedRows, VendorBulkUploadRowDto::email, this::normalizeEmail);
    Set<String> phones =
        collectKeys(normalizedRows, VendorBulkUploadRowDto::phone, this::normalizePhone);
    Set<String> existingGstins = new HashSet<>();
    Set<String> existingEmails = new HashSet<>();
    Set<String> existingPhones = new HashSet<>();
    if (!gstins.isEmpty()) {
      vendorRepository.findByCompanyIdAndGstnInIgnoreCase(companyId, gstins)
          .forEach(vendor -> existingGstins.add(normalizeGstn(vendor.getGstn())));
    }
    if (!emails.isEmpty()) {
      vendorRepository.findByCompanyIdAndEmailInIgnoreCase(companyId, emails)
          .forEach(vendor -> existingEmails.add(normalizeEmail(vendor.getEmail())));
    }
    if (!phones.isEmpty()) {
      vendorRepository.findByCompanyIdAndPhoneIn(companyId, phones)
          .forEach(vendor -> existingPhones.add(normalizePhone(vendor.getPhone())));
    }
    return addDuplicateErrors(validatedRows, existingGstins, existingEmails, existingPhones);
  }

  /** Registers valid results independently through VendorService and captures each row outcome. */
  public VendorBulkRegistrationResult registerValidVendors(
      List<VendorBulkUploadValidationResult> validationResults, Long companyId) {
    return registerValidVendors(validationResults, companyId, null);
  }

  /** Registers valid rows with a durable request reference for report and recovery correlation. */
  public VendorBulkRegistrationResult registerValidVendors(
      List<VendorBulkUploadValidationResult> validationResults, Long companyId, Long requestId) {
    int validCount = (int) validationResults.stream()
        .filter(row -> row.status() == VendorBulkUploadValidationResult.Status.VALID).count();
    int invalidCount = validationResults.size() - validCount;
    int registered = 0;
    int failures = 0;
    int skipped = 0;
    Set<Integer> processedRows = new HashSet<>();
    List<VendorBulkRegistrationResult.RowResult> outcomes = new ArrayList<>();
    for (VendorBulkUploadValidationResult result : validationResults) {
      VendorBulkUploadRowDto row = result.vendorData();
      if (result.status() != VendorBulkUploadValidationResult.Status.VALID) {
        skipped++;
        outcomes.add(new VendorBulkRegistrationResult.RowResult(result.excelRowNumber(),
            row.companyName(), VendorBulkRegistrationResult.Status.SKIPPED, null, result.errors()));
        continue;
      }
      if (!processedRows.add(result.excelRowNumber())) {
        skipped++;
        outcomes.add(new VendorBulkRegistrationResult.RowResult(result.excelRowNumber(),
            row.companyName(), VendorBulkRegistrationResult.Status.SKIPPED,
            "This Excel row was already processed in the current request.", result.errors()));
        continue;
      }
      try {
        Vendor vendor = toVendor(row);
        if (requestId == null || rowRegistrationService == null) {
          vendorService.saveVendorWithCompany(vendor, companyId);
        } else {
          rowRegistrationService.register(vendor, companyId, requestId, result.excelRowNumber());
        }
        registered++;
        outcomes.add(
            new VendorBulkRegistrationResult.RowResult(result.excelRowNumber(), row.companyName(),
                VendorBulkRegistrationResult.Status.REGISTERED, null, result.errors()));
      } catch (IllegalArgumentException e) {
        LOGGER.warn("Vendor bulk row {} failed business validation during persistence: {}",
            result.excelRowNumber(), e.getMessage());
        failures++;
        outcomes.add(
            new VendorBulkRegistrationResult.RowResult(result.excelRowNumber(), row.companyName(),
                VendorBulkRegistrationResult.Status.FAILED, failureReason(e), result.errors()));
      } catch (DataIntegrityViolationException e) {
        LOGGER.error("Vendor bulk row {} failed a database integrity constraint.",
            result.excelRowNumber(), e);
        failures++;
        outcomes.add(new VendorBulkRegistrationResult.RowResult(result.excelRowNumber(),
            row.companyName(), VendorBulkRegistrationResult.Status.FAILED,
            "Vendor conflicts with existing company data or violates a data constraint.",
            result.errors()));
      } catch (RuntimeException e) {
        LOGGER.error("Unexpected vendor bulk persistence failure at Excel row {}.",
            result.excelRowNumber(), e);
        throw e;
      }
    }
    return new VendorBulkRegistrationResult(validationResults.size(), validCount, invalidCount,
        registered, failures, skipped, outcomes);
  }

  /** Maps validated uploaded data into the existing Vendor entity before delegated persistence. */
  private Vendor toVendor(VendorBulkUploadRowDto row) {
    Vendor vendor = new Vendor();
    vendor.setCompanyName(row.companyName());
    vendor.setContactPerson(row.contactPerson());
    vendor.setEmail(row.email());
    vendor.setPhone(row.phone());
    vendor.setGstn(row.gstn());
    vendor.setState(IndianState.fromDisplayName(row.state()));
    vendor.setAddress(row.address());
    return vendor;
  }

  /** Supplies a useful row-level message when a registration exception has no message. */
  private String failureReason(RuntimeException exception) {
    return exception.getMessage() == null || exception.getMessage().isBlank()
        ? "Registration failed: " + exception.getClass().getSimpleName()
        : exception.getMessage();
  }

  /** Adds company and within-file duplicate errors while preserving earlier field errors. */
  private List<VendorBulkUploadValidationResult> addDuplicateErrors(
      List<VendorBulkUploadValidationResult> rows, Set<String> existingGstins,
      Set<String> existingEmails, Set<String> existingPhones) {
    Map<String, List<Integer>> gstinRows =
        groupRows(rows, VendorBulkUploadRowDto::gstn, this::normalizeGstn);
    Map<String, List<Integer>> emailRows =
        groupRows(rows, VendorBulkUploadRowDto::email, this::normalizeEmail);
    Map<String, List<Integer>> phoneRows =
        groupRows(rows, VendorBulkUploadRowDto::phone, this::normalizePhone);
    List<VendorBulkUploadValidationResult> results = new ArrayList<>();
    for (VendorBulkUploadValidationResult result : rows) {
      List<String> errors = new ArrayList<>(result.errors());
      VendorBulkUploadRowDto vendor = result.vendorData();
      addFileDuplicateError(errors, "gstn", "GSTIN", normalizeGstn(vendor.gstn()), gstinRows);
      addFileDuplicateError(errors, "email", "Email", normalizeEmail(vendor.email()), emailRows);
      addFileDuplicateError(errors, "phone", "Phone", normalizePhone(vendor.phone()), phoneRows);
      addExistingDuplicateError(errors, "gstn", "GSTIN", normalizeGstn(vendor.gstn()),
          existingGstins);
      addExistingDuplicateError(errors, "email", "Email", normalizeEmail(vendor.email()),
          existingEmails);
      addExistingDuplicateError(errors, "phone", "Phone", normalizePhone(vendor.phone()),
          existingPhones);
      var status = errors.isEmpty() ? VendorBulkUploadValidationResult.Status.VALID
          : VendorBulkUploadValidationResult.Status.INVALID;
      results.add(
          new VendorBulkUploadValidationResult(result.excelRowNumber(), vendor, status, errors));
    }
    return results;
  }

  /** Groups uploaded row numbers by normalized nonblank business identifier. */
  private Map<String, List<Integer>> groupRows(List<VendorBulkUploadValidationResult> rows,
      java.util.function.Function<VendorBulkUploadRowDto, String> getter,
      java.util.function.Function<String, String> normalizer) {
    Map<String, List<Integer>> groups = new HashMap<>();
    for (VendorBulkUploadValidationResult result : rows) {
      String key = normalizer.apply(getter.apply(result.vendorData()));
      if (key != null && !key.isBlank()) {
        groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(result.excelRowNumber());
      }
    }
    return groups;
  }

  /** Adds a duplicate identifier message to every row participating in its uploaded-file group. */
  private void addFileDuplicateError(List<String> errors, String field, String label, String key,
      Map<String, List<Integer>> rowGroups) {
    List<Integer> group = key == null ? null : rowGroups.get(key);
    if (group != null && group.size() > 1) {
      errors.add(field + ": Duplicate " + label + " found in Excel rows "
          + String.join(", ", group.stream().map(String::valueOf).toList()) + ".");
    }
  }

  /** Adds a duplicate identifier message when the current tenant already owns the value. */
  private void addExistingDuplicateError(List<String> errors, String field, String label,
      String value, Set<String> existingValues) {
    if (value != null && !value.isBlank() && existingValues.contains(value)) {
      errors.add(field + ": " + label + " already exists for this company.");
    }
  }

  /**
   * Collects normalized, nonblank identifiers from the uploaded rows for bulk repository lookups.
   */
  private Set<String> collectKeys(List<VendorBulkUploadRowDto> rows,
      java.util.function.Function<VendorBulkUploadRowDto, String> getter,
      java.util.function.Function<String, String> normalizer) {
    Set<String> keys = new HashSet<>();
    rows.stream().map(getter).map(normalizer).filter(value -> value != null && !value.isBlank())
        .forEach(keys::add);
    return keys;
  }

  /** Normalizes GSTIN values for case-insensitive duplicate matching. */
  private String normalizeGstn(String gstn) {
    return gstn == null ? null : gstn.trim().toUpperCase(Locale.ROOT);
  }

  /** Normalizes email values for case-insensitive duplicate matching. */
  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  /** Normalizes phone values to the same digits-only format used by vendor registration. */
  private String normalizePhone(String phone) {
    if (phone == null || phone.isBlank()) {
      return phone == null ? null : "";
    }
    try {
      return PhoneNumberUtils.normalizeRequiredInternational(phone, "Vendor Phone No");
    } catch (IllegalArgumentException e) {
      return phone.trim().replaceAll("[\\s()-]", "").toLowerCase(Locale.ROOT);
    }
  }

  /** Builds a row result containing all detected field errors without persisting vendor data. */
  private VendorBulkUploadValidationResult validateVendorRow(VendorBulkUploadRowDto row) {
    List<String> errors = new ArrayList<>();
    addRequiredError(errors, row.companyName(), "companyName", "Company Name");
    addRequiredError(errors, row.contactPerson(), "contactPerson", "Contact Person");
    addRequiredError(errors, row.email(), "email", "Email");
    if (hasText(row.email()) && !EMAIL_PATTERN.matcher(row.email().trim()).matches()) {
      errors.add("email: Email format is invalid.");
    }
    addRequiredError(errors, row.phone(), "phone", "Phone");
    if (hasText(row.phone())) {
      try {
        PhoneNumberUtils.normalizeRequiredInternational(row.phone(), "Vendor Phone No");
      } catch (IllegalArgumentException e) {
        errors.add("phone: " + e.getMessage());
      }
    }
    addRequiredError(errors, row.gstn(), "gstn", "GSTN");
    if (hasText(row.gstn())
        && !GSTIN_PATTERN.matcher(row.gstn().trim().toUpperCase(Locale.ROOT)).matches()) {
      errors.add("gstn: GSTIN format is invalid.");
    }
    addRequiredError(errors, row.state(), "state", "State");
    if (hasText(row.state()) && IndianState.fromDisplayName(row.state().trim()) == null) {
      errors.add("state: State must match a supported Indian state.");
    }
    addRequiredError(errors, row.address(), "address", "Address");
    var status = errors.isEmpty() ? VendorBulkUploadValidationResult.Status.VALID
        : VendorBulkUploadValidationResult.Status.INVALID;
    return new VendorBulkUploadValidationResult(row.excelRowNumber(), row, status, errors);
  }

  /** Adds a field-specific required error when a parsed value is blank. */
  private void addRequiredError(List<String> errors, String value, String field, String label) {
    if (!hasText(value)) {
      errors.add(field + ": " + label + " is required.");
    }
  }

  /** Checks whether a parsed field contains non-whitespace content. */
  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  /** Identifies only the complete, unchanged example row shipped in the downloadable template. */
  private boolean isTemplateSampleRow(String[] values) {
    return List.of("ACME SUPPLIES LTD", "John Doe", "john.doe@acme.com", "919876543210",
        "27ABCDE1234F1Z5", "Maharashtra", "123 Industrial Area, Mumbai").equals(List.of(values));
  }

  /** Reads supported cell types without evaluating formulas or coercing invalid values. */
  private String formatCellValue(Cell cell, DataFormatter formatter, int columnIndex,
      int excelRowNumber) throws VendorBulkUploadValidationException {
    return switch (cell.getCellType()) {
      case STRING -> cell.getStringCellValue();
      case BLANK -> "";
      case NUMERIC -> {
        if (columnIndex != 3 || DateUtil.isCellDateFormatted(cell)) {
          throw new VendorBulkUploadValidationException("Unexpected numeric value in "
              + VENDOR_HEADERS[columnIndex] + " at Excel row " + excelRowNumber + ".");
        }
        String formatted = formatter.formatCellValue(cell);
        yield formatted.matches(".*[Ee][+-]?[0-9]+$")
            ? BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
            : formatted;
      }
      case FORMULA -> throw new VendorBulkUploadValidationException(
          "Formula cells are not allowed in Vendor Data (Excel row " + excelRowNumber + ").");
      default -> throw new VendorBulkUploadValidationException("Unsupported cell value in "
          + VENDOR_HEADERS[columnIndex] + " at Excel row " + excelRowNumber + ".");
    };
  }

  // Generate Excel template with vendor data sheet and instructions sheet
  public byte[] generateBulkUploadTemplate() throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      createInstructionsSheet(workbook);
      createVendorDataSheet(workbook);
      createHiddenStateSheet(workbook);
      workbook.setSheetOrder("Instructions", 0);
      workbook.setSheetOrder("Vendor Data", 1);
      workbook.setSheetOrder("StateList", 2);
      return writeWorkbookToBytes(workbook);
    }
  }

  /**
   * Creates a complete or error-only workbook from a persisted tenant-owned registration result.
   */
  public byte[] generateRegistrationReport(VendorBulkUploadIdempotencyService.ReportData reportData,
      boolean errorsOnly) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setWrapText(true);
      CellStyle wrapStyle = workbook.createCellStyle();
      wrapStyle.setWrapText(true);
      createRegistrationSummarySheet(workbook, reportData, errorsOnly, headerStyle, wrapStyle);
      createRegistrationRowsSheet(workbook, reportData, errorsOnly, headerStyle, wrapStyle);
      workbook.write(output);
      return output.toByteArray();
    }
  }

  /** Writes request metadata and finalized registration counts to the summary worksheet. */
  private void createRegistrationSummarySheet(Workbook workbook,
      VendorBulkUploadIdempotencyService.ReportData data, boolean errorsOnly, CellStyle headerStyle,
      CellStyle wrapStyle) {
    var request = data.request();
    Sheet sheet = workbook.createSheet("Registration Summary");
    String[][] fields = {{"Registration Reference", String.valueOf(request.getId())},
        {"Company", request.getCompany().getName()},
        {"Workbook Fingerprint", maskFingerprint(request.getWorkbookFingerprint())},
        {"Uploaded Filename", request.getUploadedFilename()},
        {"Uploaded By", request.getInitiatedBy()},
        {"Upload Timestamp", formatTimestamp(request.getCreatedAt())},
        {"Registration Completion Timestamp", formatTimestamp(request.getCompletedAt())},
        {"Total Parsed Rows", valueOrZero(request.getTotalParsedRows())},
        {"Valid Rows", valueOrZero(request.getValidRowCount())},
        {"Registered Rows", valueOrZero(request.getRegisteredRowCount())},
        {"Failed Rows", valueOrZero(request.getFailedRowCount())},
        {"Skipped Rows", valueOrZero(request.getSkippedRowCount())},
        {"Overall Registration Status", request.getStatus().name()},
        {"Report Type", errorsOnly ? "Errors only" : "Complete registration"}};
    for (int index = 0; index < fields.length; index++) {
      Row row = sheet.createRow(index);
      writeTextCell(row.createCell(0), fields[index][0], headerStyle);
      writeTextCell(row.createCell(1), fields[index][1], wrapStyle);
    }
    sheet.setColumnWidth(0, 38 * 256);
    sheet.setColumnWidth(1, 58 * 256);
  }

  /** Writes row outcomes, retaining all rows or only failed and skipped rows as requested. */
  private void createRegistrationRowsSheet(Workbook workbook,
      VendorBulkUploadIdempotencyService.ReportData data, boolean errorsOnly, CellStyle headerStyle,
      CellStyle wrapStyle) {
    Sheet sheet = workbook.createSheet("Row Results");
    String[] headers = {"Excel Row Number", "Vendor Company Name", "Registration Status",
        "Failure Reason", "Validation Errors", "Processed At"};
    Row header = sheet.createRow(0);
    for (int index = 0; index < headers.length; index++) {
      writeTextCell(header.createCell(index), headers[index], headerStyle);
    }
    int rowIndex = 1;
    for (var result : data.rows()) {
      if (errorsOnly && result.status() == VendorBulkRegistrationResult.Status.REGISTERED) {
        continue;
      }
      Row row = sheet.createRow(rowIndex++);
      writeTextCell(row.createCell(0), String.valueOf(result.excelRowNumber()), wrapStyle);
      writeTextCell(row.createCell(1), result.vendorCompanyName(), wrapStyle);
      writeTextCell(row.createCell(2), result.status().name(), wrapStyle);
      writeTextCell(row.createCell(3), result.failureReason(), wrapStyle);
      writeTextCell(row.createCell(4), String.join("\n", result.validationErrors()), wrapStyle);
      LocalDateTime processedAt = data.processedAt().get(result.excelRowNumber());
      writeTextCell(row.createCell(5), formatTimestamp(processedAt), wrapStyle);
      applyStatusStyle(workbook, row, result.status());
    }
    sheet.createFreezePane(0, 1);
    if (rowIndex > 1) {
      sheet.setAutoFilter(new CellRangeAddress(0, rowIndex - 1, 0, headers.length - 1));
    }
    int[] widths = {18, 34, 22, 48, 68, 25};
    for (int index = 0; index < widths.length; index++) {
      sheet.setColumnWidth(index, widths[index] * 256);
    }
  }

  /** Applies a readable row fill for each final registration outcome. */
  private void applyStatusStyle(Workbook workbook, Row row,
      VendorBulkRegistrationResult.Status status) {
    short color = status == VendorBulkRegistrationResult.Status.REGISTERED
        ? IndexedColors.LIGHT_GREEN.getIndex()
        : status == VendorBulkRegistrationResult.Status.FAILED ? IndexedColors.ROSE.getIndex()
            : IndexedColors.LIGHT_YELLOW.getIndex();
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(color);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setWrapText(true);
    for (int column = 0; column < 6; column++) {
      row.getCell(column).setCellStyle(style);
    }
  }

  /** Writes untrusted report text explicitly as a string cell, never as a formula. */
  private void writeTextCell(Cell cell, String value, CellStyle style) {
    cell.setCellValue(value == null ? "" : value);
    cell.setCellStyle(style);
  }

  /** Formats available timestamps without exposing internal date conversion details. */
  private String formatTimestamp(LocalDateTime value) {
    return value == null ? "" : value.toString();
  }

  /**
   * Returns persisted counts as readable summary values when the request predates count metadata.
   */
  private String valueOrZero(Integer value) {
    return String.valueOf(value == null ? 0 : value);
  }

  /** Masks a stored SHA-256 fingerprint before including it in a downloadable report. */
  private String maskFingerprint(String fingerprint) {
    return fingerprint == null ? "" : fingerprint.substring(0, Math.min(12, fingerprint.length()));
  }

  // Create hidden sheet with state names for dropdown validation reference
  void createHiddenStateSheet(Workbook workbook) {
    Sheet sheet = workbook.createSheet("StateList");
    String[] stateNames = getStateDisplayNames();
    for (int i = 0; i < stateNames.length; i++) {
      Row row = sheet.createRow(i);
      row.createCell(0).setCellValue(stateNames[i]);
    }
    sheet.protectSheet(""); // Protect sheet from accidental modification
    workbook.setSheetHidden(workbook.getSheetIndex("StateList"), true);
  }

  // Create the main vendor data sheet with headers, sample row, and state dropdown
  private void createVendorDataSheet(Workbook workbook) {
    Sheet sheet = workbook.createSheet("Vendor Data");
    setColumnWidths(sheet);

    Row headerRow = sheet.createRow(0);
    String[] headers =
        {"Company Name*", "Contact Person*", "Email*", "Phone*", "GSTN*", "State*", "Address*"};
    for (int i = 0; i < headers.length; i++) {
      Cell cell = headerRow.createCell(i);
      cell.setCellValue(headers[i]);
      styleHeaderCell(workbook, cell);
    }

    Row sampleRow = sheet.createRow(1);
    sampleRow.createCell(0).setCellValue("ACME SUPPLIES LTD");
    sampleRow.createCell(1).setCellValue("John Doe");
    sampleRow.createCell(2).setCellValue("john.doe@acme.com");
    sampleRow.createCell(3).setCellValue("+919876543210");
    sampleRow.createCell(4).setCellValue("27ABCDE1234F1Z5");
    sampleRow.createCell(5).setCellValue("Maharashtra");
    sampleRow.createCell(6).setCellValue("123 Industrial Area, Mumbai");

    addStateDropdownValidation(workbook, sheet, 5);
  }

  // Set sensible column widths for better readability
  private void setColumnWidths(Sheet sheet) {
    sheet.setColumnWidth(0, 4000);
    sheet.setColumnWidth(1, 3500);
    sheet.setColumnWidth(2, 4500);
    sheet.setColumnWidth(3, 3500);
    sheet.setColumnWidth(4, 4000);
    sheet.setColumnWidth(5, 4000);
    sheet.setColumnWidth(6, 6000);
  }

  // Apply bold header styling with background color
  private void styleHeaderCell(Workbook workbook, Cell cell) {
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    headerStyle.setBorderBottom(BorderStyle.THIN);
    headerStyle.setBorderTop(BorderStyle.THIN);
    headerStyle.setBorderLeft(BorderStyle.THIN);
    headerStyle.setBorderRight(BorderStyle.THIN);
    cell.setCellStyle(headerStyle);
  }

  // Add dropdown validation for Indian states in the State column
  private void addStateDropdownValidation(Workbook workbook, Sheet sheet, int columnIndex) {
    DataValidationHelper validationHelper = sheet.getDataValidationHelper();
    DataValidationConstraint constraint =
        validationHelper.createFormulaListConstraint("StateList!$A$1:$A$37");
    DataValidation validation = validationHelper.createValidation(constraint,
        new CellRangeAddressList(1, 1000, columnIndex, columnIndex));
    validation.setShowErrorBox(true);
    sheet.addValidationData(validation);
  }

  // Get display names of all Indian states for dropdown
  private String[] getStateDisplayNames() {
    return new String[] {"Andaman and Nicobar Islands", "Andhra Pradesh", "Arunachal Pradesh",
        "Assam", "Bihar", "Chandigarh", "Chhattisgarh", "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi", "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jammu and Kashmir", "Jharkhand",
        "Karnataka", "Kerala", "Ladakh", "Lakshadweep", "Madhya Pradesh", "Maharashtra", "Manipur",
        "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Other Territory", "Puducherry", "Punjab",
        "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttarakhand", "Uttar Pradesh",
        "West Bengal"};
  }

  // Create instructions sheet with format descriptions
  private void createInstructionsSheet(Workbook workbook) {
    Sheet sheet = workbook.createSheet("Instructions");
    sheet.setColumnWidth(0, 8000);
    sheet.setColumnWidth(1, 12000);

    int rowNum = 0;
    createInstructionRow(sheet, rowNum++, "Field", "Format / Requirements");
    styleInstructionHeader(workbook, sheet.getRow(rowNum - 1));

    createInstructionRow(sheet, rowNum++, "Company Name*", "Uppercase, mandatory");
    createInstructionRow(sheet, rowNum++, "Contact Person*", "Title case, mandatory");
    createInstructionRow(sheet, rowNum++, "Email*", "Valid email format, mandatory");
    createInstructionRow(sheet, rowNum++, "Phone*",
        "International format with country code (e.g., +91...), mandatory");
    createInstructionRow(sheet, rowNum++, "GSTN*", "Valid 15-character GSTIN, mandatory");
    createInstructionRow(sheet, rowNum++, "State*", "Select from dropdown, mandatory");
    createInstructionRow(sheet, rowNum++, "Address*", "Full address, mandatory");
    rowNum++;
    createInstructionRow(sheet, rowNum++, "Notes", "Delete sample row before uploading your data");

    sheet.protectSheet(""); // Protect sheet from accidental modification
  }

  // Create a single instruction row with two columns
  private void createInstructionRow(Sheet sheet, int rowNum, String field, String description) {
    Row row = sheet.createRow(rowNum);
    row.createCell(0).setCellValue(field);
    row.createCell(1).setCellValue(description);
  }

  // Style the instruction header row
  private void styleInstructionHeader(Workbook workbook, Row row) {
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    row.getCell(0).setCellStyle(headerStyle);
    row.getCell(1).setCellStyle(headerStyle);
  }

  // Write workbook to byte array for download
  private byte[] writeWorkbookToBytes(Workbook workbook) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    return outputStream.toByteArray();
  }
}
