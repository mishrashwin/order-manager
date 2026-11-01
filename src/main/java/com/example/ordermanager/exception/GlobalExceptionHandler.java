package com.example.ordermanager.exception;

import com.example.ordermanager.order.exception.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.unit.DataSize;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

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

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGeneric(Exception ex, WebRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error");
    body.put("message", ex.getMessage());
    body.put("path", request.getDescription(false).replace("uri=", ""));
    return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
