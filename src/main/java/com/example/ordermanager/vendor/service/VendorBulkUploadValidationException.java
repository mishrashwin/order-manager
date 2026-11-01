package com.example.ordermanager.vendor.service;

import java.io.IOException;

/** Represents a safe, user-displayable file or workbook validation failure. */
public class VendorBulkUploadValidationException extends IOException {

  /** Creates a controlled upload error message without exposing parser internals. */
  public VendorBulkUploadValidationException(String message) {
    super(message);
  }

  /** Creates a controlled upload error while retaining the internal parser failure as its cause. */
  public VendorBulkUploadValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
