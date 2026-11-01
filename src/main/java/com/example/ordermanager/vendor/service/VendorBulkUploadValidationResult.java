package com.example.ordermanager.vendor.service;

import java.util.List;

/** Carries a parsed worksheet row with its validation outcome and field-specific errors. */
public record VendorBulkUploadValidationResult(int excelRowNumber,
    VendorBulkUploadRowDto vendorData, Status status, List<String> errors) {

  /** Identifies whether a parsed row satisfies the required vendor field rules. */
  public enum Status {
    VALID,
    INVALID
  }
}
