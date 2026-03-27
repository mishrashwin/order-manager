package com.example.ordermanager.vendor.controller;

import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.service.VendorService;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendors")
public class VendorController {

  private final VendorService vendorService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;

  public VendorController(VendorService vendorService, CompanyService companyService,
      SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService) {
    this.vendorService = vendorService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
    this.passwordVerificationService = passwordVerificationService;
  }

  @GetMapping
  public String listVendors(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
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
}
