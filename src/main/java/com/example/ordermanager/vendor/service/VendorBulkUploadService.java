package com.example.ordermanager.vendor.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class VendorBulkUploadService {

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
