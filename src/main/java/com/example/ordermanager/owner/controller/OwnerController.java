package com.example.ordermanager.owner.controller;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.entity.CompanyApprovalStatus;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.owner.service.OwnerManagementService;
import com.example.ordermanager.payment.entity.Payment;
import com.example.ordermanager.payment.service.PaymentService;
import com.example.ordermanager.utils.PasswordVerificationService;
import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/owner")
@PreAuthorize("hasRole('OWNER')")
public class OwnerController {

  private final OwnerManagementService ownerManagementService;
  private final CompanyService companyService;
  private final PasswordVerificationService passwordVerificationService;
  private final PaymentService paymentService;

  public OwnerController(OwnerManagementService ownerManagementService,
      CompanyService companyService, PasswordVerificationService passwordVerificationService,
      PaymentService paymentService) {
    this.ownerManagementService = ownerManagementService;
    this.companyService = companyService;
    this.passwordVerificationService = passwordVerificationService;
    this.paymentService = paymentService;
  }

  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    model.addAttribute("companyName", "Owner Console");
    model.addAttribute("page", "owner-dashboard");
    model.addAttribute("metrics", ownerManagementService.getDashboardMetrics());
    model.addAttribute("pendingCompanies", ownerManagementService.getPendingCompanySummaries());
    return "owner/dashboard";
  }

  @GetMapping("/companies")
  public String companies(Model model) {
    model.addAttribute("companyName", "Owner Console");
    model.addAttribute("page", "owner-companies");
    model.addAttribute("companies", ownerManagementService.getAllCompanySummaries());
    model.addAttribute("pendingCount", companyService.getPendingCompanyCount());
    return "owner/companies";
  }

  @GetMapping("/payments")
  public String payments(Model model) {
    model.addAttribute("companyName", "Owner Console");
    model.addAttribute("page", "owner-payments");
    model.addAttribute("payments", paymentService.getAllPayments());
    return "owner/payments";
  }

  @GetMapping("/payments/{id}/download")
  public ResponseEntity<byte[]> downloadPaymentScreenshot(@PathVariable Long id) {
    Payment payment = paymentService.getPaymentById(id);

    if (payment.getPaymentSsData() == null) {
      return ResponseEntity.notFound().build();
    }

    String contentType =
        payment.getPaymentSsContentType() != null ? payment.getPaymentSsContentType()
            : "application/octet-stream";
    String filename =
        payment.getPaymentSsFilename() != null ? payment.getPaymentSsFilename() : "screenshot";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType(contentType)).body(payment.getPaymentSsData());
  }

  @PostMapping("/companies/{id}/approve")
  public String approve(@PathVariable Long id, Principal principal,
      RedirectAttributes redirectAttributes) {
    try {
      companyService.approveCompany(id, principal.getName());
      redirectAttributes.addFlashAttribute("message", "Company approved successfully.");
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/owner/companies";
  }

  @PostMapping("/companies/{id}/reject")
  public String reject(@PathVariable Long id, Principal principal,
      RedirectAttributes redirectAttributes) {
    try {
      companyService.rejectCompany(id, principal.getName());
      redirectAttributes.addFlashAttribute("message", "Company rejected and suspended.");
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/owner/companies";
  }

  @PostMapping("/companies/{id}/toggle-access")
  public String toggleAccess(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
      Company company = companyService.getCompanyById(id)
          .orElseThrow(() -> new IllegalArgumentException("Company not found"));
      if (company.isActive()) {
        companyService.deactivateCompany(id);
        redirectAttributes.addFlashAttribute("message", "Company access suspended.");
      } else {
        if (!CompanyApprovalStatus.APPROVED.equals(company.getApprovalStatus())) {
          redirectAttributes.addFlashAttribute("error",
              "Approve the company before re-enabling access.");
          return "redirect:/owner/companies";
        }
        companyService.activateCompany(id);
        redirectAttributes.addFlashAttribute("message", "Company access restored.");
      }
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/owner/companies";
  }

  @PostMapping("/companies/{id}/delete")
  public String deleteCompany(@PathVariable Long id, @RequestParam String password,
      RedirectAttributes redirectAttributes) {
    if (!passwordVerificationService.verifyCurrentUserPassword(password)) {
      redirectAttributes.addFlashAttribute("error", "Incorrect password. Company was not deleted.");
      return "redirect:/owner/companies";
    }
    try {
      companyService.deleteCompany(id);
      redirectAttributes.addFlashAttribute("message", "Company deleted successfully.");
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/owner/companies";
  }

  @PostMapping("/companies/{id}/set-fee")
  public String setCompanyFee(@PathVariable Long id,
      @RequestParam(required = false) Double monthlyFee, RedirectAttributes redirectAttributes) {
    try {
      companyService.setMonthlyFee(id, monthlyFee);
      redirectAttributes.addFlashAttribute("message", "Monthly fee updated successfully.");
    } catch (IllegalArgumentException ex) {
      redirectAttributes.addFlashAttribute("error", ex.getMessage());
    }
    return "redirect:/owner/companies";
  }
}


