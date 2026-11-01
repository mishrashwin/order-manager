package com.example.ordermanager.vendor.controller;

import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.service.VendorService;
import com.example.ordermanager.vendor.service.VendorBulkUploadService;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/vendors")
public class VendorController {

  private final VendorService vendorService;
  private final VendorBulkUploadService vendorBulkUploadService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;

  public VendorController(VendorService vendorService,
      VendorBulkUploadService vendorBulkUploadService, CompanyService companyService,
      SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService) {
    this.vendorService = vendorService;
    this.vendorBulkUploadService = vendorBulkUploadService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
    this.passwordVerificationService = passwordVerificationService;
  }

  @GetMapping
  public String listVendors(@RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int size, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Pageable pageable = PageRequest.of(page, size);
    Page<Vendor> vendorsPage = vendorService.searchVendors(companyId, search, pageable);
    model.addAttribute("vendors", vendorsPage.getContent());
    model.addAttribute("currentPage", page + 1);
    model.addAttribute("totalPages", vendorsPage.getTotalPages());
    model.addAttribute("totalElements", vendorsPage.getTotalElements());
    model.addAttribute("search", search);
    return "vendors/list";
  }

  @GetMapping("/new")
  public String newVendorForm(@RequestParam(required = false) String returnTo, Model model) {
    model.addAttribute("vendor", new Vendor());
    model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
    return "vendors/form";
  }

  @PostMapping
  public String saveVendor(@ModelAttribute Vendor vendor,
      @RequestParam(required = false) String returnTo, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      boolean isUpdate = vendor.getId() != null;
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      vendorService.saveVendorWithCompany(vendor, companyId);
      redirectAttributes.addFlashAttribute("message",
          isUpdate ? "Vendor updated successfully." : "Vendor added successfully.");
      String redirectPath = sanitizeReturnTo(returnTo, "/vendors", "/products");
      return "redirect:" + redirectPath;
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      model.addAttribute("error", message);
      if (isPhoneValidationError(message)) {
        model.addAttribute("phoneError", message);
      }
      model.addAttribute("vendor", vendor);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
      return "vendors/form";
    } catch (IllegalStateException e) {
      model.addAttribute("error", "You must be logged in to create a vendor");
      model.addAttribute("vendor", vendor);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
      return "vendors/form";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving vendor: " + e.getMessage());
      model.addAttribute("vendor", vendor);
      model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
      return "vendors/form";
    }
  }

  private boolean isPhoneValidationError(String message) {
    return message != null && message.toLowerCase().contains("phone");
  }

  @GetMapping("/edit/{id}")
  public String editVendor(@PathVariable Long id, @RequestParam(required = false) String returnTo,
      Model model) {
    model.addAttribute("vendor", vendorService.getVendorById(id));
    model.addAttribute("returnTo", sanitizeReturnTo(returnTo, "/vendors", "/products"));
    return "vendors/form";
  }

  private String sanitizeReturnTo(String returnTo, String fallback, String... allowedPrefixes) {
    if (returnTo == null || returnTo.isBlank()) {
      return fallback;
    }

    String normalized = normalizeReturnToCandidate(returnTo.trim());
    if (!normalized.startsWith("/") || normalized.startsWith("//") || normalized.contains("\r")
        || normalized.contains("\n")) {
      return fallback;
    }

    boolean allowed = Arrays.stream(allowedPrefixes).anyMatch(normalized::startsWith);
    return allowed ? normalized : fallback;
  }

  private String normalizeReturnToCandidate(String value) {
    String normalized = value;
    try {
      normalized = URLDecoder.decode(value, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ignored) {
      // Keep original value when URL decoding fails.
    }

    int delimiter = normalized.indexOf(',');
    if (delimiter >= 0) {
      normalized = normalized.substring(0, delimiter).trim();
    }
    return normalized;
  }

  @PostMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public String deleteVendor(@PathVariable Long id, @RequestParam String password,
      RedirectAttributes redirectAttributes) {
    if (!passwordVerificationService.verifyCurrentUserPassword(password)) {
      redirectAttributes.addFlashAttribute("error", "Incorrect password. Vendor was not deleted.");
      return "redirect:/vendors";
    }
    try {
      vendorService.deleteVendor(id);
      redirectAttributes.addFlashAttribute("message", "Vendor deleted successfully.");
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "Error deleting vendor: " + e.getMessage());
    }
    return "redirect:/vendors";
  }

  // Download Excel template for bulk vendor upload
  @GetMapping("/bulk-upload/template")
  public ResponseEntity<byte[]> downloadBulkUploadTemplate() {
    try {
      byte[] templateBytes = vendorBulkUploadService.generateBulkUploadTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.setContentDispositionFormData("attachment", "vendor_bulk_upload_template.xlsx");
      return ResponseEntity.ok().headers(headers).body(templateBytes);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  // Handle bulk upload file submission (accepts file but does not parse yet)
  @PostMapping("/bulk-upload")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> handleBulkUpload(
      @RequestParam("file") MultipartFile file) {
    Map<String, Object> response = new HashMap<>();

    if (file.isEmpty()) {
      response.put("success", false);
      response.put("message", "File is empty. Please select a valid file.");
      return ResponseEntity.badRequest().body(response);
    }

    if (!file.getOriginalFilename().endsWith(".xlsx")) {
      response.put("success", false);
      response.put("message", "Invalid file type. Only .xlsx files are allowed.");
      return ResponseEntity.badRequest().body(response);
    }

    if (file.getSize() > 5 * 1024 * 1024) {
      response.put("success", false);
      response.put("message", "File size exceeds 5MB limit.");
      return ResponseEntity.badRequest().body(response);
    }

    // File accepted - parsing and vendor registration will be implemented in next phase
    response.put("success", true);
    response.put("message",
        "File uploaded successfully. Vendor registration will be implemented in the next phase.");
    return ResponseEntity.ok().body(response);
  }
}
