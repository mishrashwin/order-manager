package com.example.ordermanager.exception;

import com.example.ordermanager.order.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final String SAFE_PO_SAVE_ERROR =
      "The PO could not be saved because one or more values are invalid or exceed supported limits. "
          + "Review quantities, rates, taxes, and charges.";

  @Value("${app.vendor.bulk-upload.max-file-size:5MB}")
  private DataSize maxBulkUploadFileSize = DataSize.ofMegabytes(5);

  /** Returns a controlled JSON size error when multipart resolution rejects a vendor workbook. */
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException ex, HttpServletRequest request) {
    String path = request.getRequestURI();
    if ("/vendors/bulk-upload".equals(path) || "/vendors/bulk-upload/register".equals(path)) {
      Map<String, Object> body = new LinkedHashMap<>();
      body.put("success", false);
      String configuredLimit = String.format(java.util.Locale.ROOT, "%.1f MB",
          maxBulkUploadFileSize.toBytes() / (1024.0 * 1024.0));
      body.put("message", "File size exceeds the configured " + configuredLimit + " limit.");
      return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
    body.put("error", "Payload Too Large");
    body.put("message", "Uploaded file exceeds the configured size limit.");
    body.put("path", path);
    return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
  }

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<Object> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.NOT_FOUND.value());
    body.put("error", "Order Not Found");
    body.put("message", ex.getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));
    return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
  }

  /** Returns PO form feedback for persistence failures without exposing database details. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public Object handleDataIntegrityViolation(DataIntegrityViolationException exception,
      HttpServletRequest request, RedirectAttributes redirectAttributes) {
    String path = request.getServletPath();
    String constraintDetails = extractConstraintDetails(exception);
    String errorMessage = SAFE_PO_SAVE_ERROR;
    if (constraintDetails != null) {
      errorMessage += " Issue: " + constraintDetails;
    }
    if ("/vendor/pos".equals(path)) {
      redirectAttributes.addFlashAttribute("poError", errorMessage);
      return "redirect:/vendor/pos/new";
    }
    if (path.matches("/vendor/pos/update/\\d+")) {
      String id = path.substring(path.lastIndexOf('/') + 1);
      redirectAttributes.addFlashAttribute("poError", errorMessage);
      return "redirect:/vendor/pos/" + id + "/edit";
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error");
    body.put("message",
        "The request could not be completed. Please review your input and try again.");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  /** Extracts constraint violation details from DataIntegrityViolationException for logging. */
  private String extractConstraintDetails(DataIntegrityViolationException exception) {
    Throwable rootCause = exception.getRootCause();
    if (rootCause != null) {
      String message = rootCause.getMessage();
      if (message != null) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
            .error("DataIntegrityViolationException: {}", message, exception);
        if (message.contains("foreign key constraint")) {
          if (message.contains("vendor"))
            return "Invalid vendor selected";
          if (message.contains("product"))
            return "Invalid product selected";
          if (message.contains("company"))
            return "Invalid company reference";
          if (message.contains("address"))
            return "Invalid address selected";
          if (message.contains("user"))
            return "Invalid user selected";
        }
        if (message.contains("unique constraint") || message.contains("duplicate key")) {
          if (message.contains("po_number"))
            return "Duplicate PO number";
          return "Duplicate entry detected";
        }
        if (message.contains("cannot be null")) {
          int nullIndex = message.indexOf("cannot be null");
          if (nullIndex > 0) {
            String context = message.substring(Math.max(0, nullIndex - 50), nullIndex);
            if (context.contains("'")) {
              int start = context.lastIndexOf("'");
              if (start >= 0) {
                String column = context.substring(start + 1).trim();
                return "Required field missing: " + column;
              }
            }
          }
          return "Required field is missing";
        }
        if (message.contains("value too long")) {
          int longIndex = message.indexOf("value too long");
          if (longIndex > 0) {
            String context = message.substring(Math.max(0, longIndex - 100), longIndex);
            if (context.contains("'")) {
              int start = context.lastIndexOf("'");
              if (start >= 0) {
                String column = context.substring(start + 1).trim();
                return "Field '" + column + "' exceeds maximum allowed length";
              }
            }
          }
          return "A field value exceeds the maximum allowed length";
        }
      }
    }
    return null;
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneric(Exception exception, WebRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error");
    body.put("message",
        "An error occurred while processing your request. Please contact support if the issue persists.");
    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
