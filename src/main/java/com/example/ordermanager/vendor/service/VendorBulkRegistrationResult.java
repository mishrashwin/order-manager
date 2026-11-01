package com.example.ordermanager.vendor.service;

import java.util.List;

/** Summarizes a bulk registration attempt and the outcome for every parsed row. */
public record VendorBulkRegistrationResult(int totalParsedRows, int validRowCount,
    int invalidRowCount, int registeredRowCount, int registrationFailureCount, int skippedRowCount,
    List<RowResult> rowResults) {

  /** Describes registration outcome while retaining validation errors for rows that were skipped. */
  public record RowResult(int excelRowNumber, String vendorCompanyName, Status status,
      String failureReason, List<String> validationErrors) {}

  /** Identifies whether the row was saved, failed while saving, or skipped as invalid/duplicate. */
  public enum Status {
    REGISTERED,
    FAILED,
    SKIPPED
  }
}
