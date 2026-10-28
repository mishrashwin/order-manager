package com.example.ordermanager.vendor.controller;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.product.entity.Product;
import com.example.ordermanager.product.service.ProductService;
import com.example.ordermanager.user.entity.User;
import com.example.ordermanager.user.service.BrevoEmailService;
import com.example.ordermanager.user.service.UserService;
import com.example.ordermanager.utils.PasswordVerificationService;
import com.example.ordermanager.utils.SecurityContextHelper;
import com.example.ordermanager.vendor.entity.VendorPo;
import com.example.ordermanager.vendor.entity.VendorPoItem;
import com.example.ordermanager.vendor.entity.VendorPoStatus;
import com.example.ordermanager.vendor.service.VendorPoService;
import com.example.ordermanager.vendor.service.VendorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/vendor/pos")
public class VendorPoController {

  private final VendorPoService vendorPoService;
  private final BrevoEmailService brevoEmailService;
  private final UserService userService;
  private final CompanyService companyService;
  private final CompanyRepository companyRepository;
  private final VendorService vendorService;
  private final ProductService productService;
  private final SecurityContextHelper securityContextHelper;
  private final PasswordVerificationService passwordVerificationService;

  public VendorPoController(VendorPoService vendorPoService, BrevoEmailService brevoEmailService,
      UserService userService, CompanyService companyService, CompanyRepository companyRepository,
      VendorService vendorService, ProductService productService,
      SecurityContextHelper securityContextHelper,
      PasswordVerificationService passwordVerificationService) {
    this.vendorPoService = vendorPoService;
    this.brevoEmailService = brevoEmailService;
    this.userService = userService;
    this.companyService = companyService;
    this.companyRepository = companyRepository;
    this.vendorService = vendorService;
    this.productService = productService;
    this.securityContextHelper = securityContextHelper;
    this.passwordVerificationService = passwordVerificationService;
  }

  // ── List (AJAX fragment) ────────────────────────────────────────────────────

  @GetMapping("/list-ajax")
  public String listAjax(@RequestParam(required = false) String search,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "10") int size, Model model) {
    Page<VendorPo> posPage =
        vendorPoService.searchVendorPos(null, null, search, PageRequest.of(page, size));
    model.addAttribute("pos", posPage);
    model.addAttribute("currentPage", page + 1);
    model.addAttribute("totalPages", posPage.getTotalPages());
    model.addAttribute("totalElements", posPage.getTotalElements());
    model.addAttribute("search", search);
    return "vendor/pos-list-ajax :: pos-table";
  }

  // ── Create ─────────────────────────────────────────────────────────────────

  @GetMapping("/new")
  public String createForm(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("vendorPo", new VendorPo());
    model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
    model.addAttribute("products", productService.getProductsByCompanyId(companyId));
    model.addAttribute("users", userService.getUsersByCompany(companyId));
    return "vendor/pos-form-new";
  }

  @PostMapping
  public String create(@ModelAttribute VendorPo vendorPo,
      @RequestParam(required = false) List<Long> itemProductIds,
      @RequestParam(required = false) List<Double> itemQuantities,
      @RequestParam(required = false) List<Double> itemPreGstUnitPrices,
      @RequestParam(required = false) List<String> itemHsnCodes,
      @RequestParam(required = false) List<String> itemUnits,
      @RequestParam(required = false) String send, RedirectAttributes ra) {

    buildVendorPoItems(vendorPo, itemProductIds, itemQuantities, itemPreGstUnitPrices, itemHsnCodes,
        itemUnits);
    VendorPo saved = vendorPoService.createVendorPo(vendorPo);

    // Send email if requested
    if ("true".equalsIgnoreCase(send) || "1".equals(send)) {
      trySendPoEmail(saved, ra);
    }

    ra.addFlashAttribute("poMessage", "Vendor PO created successfully");
    return "redirect:/vendors#vendor-po";
  }

  // ── Edit ───────────────────────────────────────────────────────────────────

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo po = vendorPoService.getVendorPoById(id);
    model.addAttribute("vendorPo", po);
    model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
    model.addAttribute("products", productService.getProductsByCompanyId(companyId));
    model.addAttribute("users", userService.getUsersByCompany(companyId));
    return "vendor/pos-form-new";
  }

  /**
   * FIX 1 & 2: Dedicated update endpoint (mirrors POST /orders/update/{id}). No longer shares the
   * create POST, and no longer force-resets status to DRAFT.
   */
  @PostMapping("/update/{id}")
  public String update(@PathVariable Long id, @ModelAttribute VendorPo vendorPo,
      @RequestParam(required = false) List<Long> itemProductIds,
      @RequestParam(required = false) List<Double> itemQuantities,
      @RequestParam(required = false) List<Double> itemPreGstUnitPrices,
      @RequestParam(required = false) List<String> itemHsnCodes,
      @RequestParam(required = false) List<String> itemUnits, Model model, RedirectAttributes ra) {

    Long companyId = securityContextHelper.getCompanyIdFromContext();
    VendorPo existing = vendorPoService.getVendorPoById(id);
    if (existing == null) {
      ra.addFlashAttribute("poError", "Vendor PO not found.");
      return "redirect:/vendors#vendor-po";
    }

    buildVendorPoItems(vendorPo, itemProductIds, itemQuantities, itemPreGstUnitPrices, itemHsnCodes,
        itemUnits);

    try {
      vendorPoService.updateVendorPo(id, vendorPo);
      vendorPoService.changeStatus(id, VendorPoStatus.DRAFT);
      ra.addFlashAttribute("poMessage", "Vendor PO updated successfully");
      return "redirect:/vendors#vendor-po";
    } catch (IllegalArgumentException e) {
      model.addAttribute("poError", e.getMessage());
      model.addAttribute("vendorPo", vendorPo);
      model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
      model.addAttribute("products", productService.getProductsByCompanyId(companyId));
      model.addAttribute("users", userService.getUsersByCompany(companyId));
      return "vendor/pos-form-new";
    }
  }

  // ── Download ───────────────────────────────────────────────────────────────

  @GetMapping("/{id}/download")
  public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
    VendorPo po = vendorPoService.getVendorPoById(id);
    byte[] pdf = vendorPoService.generatePdf(id);

    // Build a safe filename: VendorName_PoNumber.pdf
    String vendorPart =
        (po.getVendorName() != null && !po.getVendorName().isBlank()) ? po.getVendorName()
            : "Vendor";
    String poPart = (po.getPoNumber() != null && !po.getPoNumber().isBlank()) ? po.getPoNumber()
        : String.valueOf(id);
    String filename = (vendorPart + "_" + poPart).replaceAll("[^a-zA-Z0-9._\\-]", "_") + ".pdf";

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_PDF).body(pdf);
  }

  /**
   * Resend the PO PDF email for an existing saved PO.
   */
  @PostMapping("/{id}/resend")
  public String resendEmail(@PathVariable Long id, RedirectAttributes ra) {
    VendorPo po = vendorPoService.getVendorPoById(id);
    trySendPoEmail(po, ra);
    if (!ra.getFlashAttributes().containsKey("poError")) {
      ra.addFlashAttribute("poMessage", "PO email resent successfully to vendor.");
    }
    return "redirect:/vendors#vendor-po";
  }


  // ── Delete ─────────────────────────────────────────────────────────────────

  /**
   * FIX 4 & 5: Password-verified, role-restricted delete — mirrors OrderController.deleteOrder.
   */
  @PostMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
  public String deleteVendorPo(@PathVariable Long id, @RequestParam String password,
      RedirectAttributes ra) {

    if (!passwordVerificationService.verifyCurrentUserPassword(password)) {
      ra.addFlashAttribute("poError", "Incorrect password. Vendor PO was not deleted.");
      return "redirect:/vendors#vendor-po";
    }

    try {
      vendorPoService.deleteVendorPo(id);
      ra.addFlashAttribute("poMessage", "Vendor PO deleted successfully");
    } catch (Exception e) {
      ra.addFlashAttribute("poError", "Failed to delete PO: " + e.getMessage());
    }
    return "redirect:/vendors#vendor-po";
  }

  // ── Terms & Conditions ───────────────────────────────────────────────────────

  /**
   * GET endpoint to load company-level Terms & Conditions for Vendor PO PDFs. Returns JSON with
   * terms array and enabled toggle state.
   */
  @GetMapping("/terms-conditions")
  @ResponseBody
  public java.util.Map<String, Object> getTermsConditions() {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyService.getCompanyById(companyId)
        .orElseThrow(() -> new IllegalStateException("Company not found"));

    java.util.Map<String, Object> response = new java.util.HashMap<>();

    // Parse JSON terms array
    java.util.List<String> terms = new java.util.ArrayList<>();
    if (company.getVendorPoTermsConditions() != null
        && !company.getVendorPoTermsConditions().isBlank()) {
      try {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
        terms = mapper.readValue(company.getVendorPoTermsConditions(),
            new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
      } catch (Exception e) {
        // If parsing fails, treat as empty
        terms = new java.util.ArrayList<>();
      }
    }

    response.put("terms", terms);
    response.put("enabled",
        company.getVendorPoTermsConditionsEnabled() != null
            ? company.getVendorPoTermsConditionsEnabled()
            : true);
    return response;
  }

  /**
   * POST endpoint to save company-level Terms & Conditions for Vendor PO PDFs. Accepts form data
   * with terms array and enabled toggle.
   */
  @PostMapping("/terms-conditions")
  @ResponseBody
  public java.util.Map<String, Object> saveTermsConditions(
      @RequestParam(required = false, defaultValue = "false") String termsConditionsEnabled,
      @RequestParam(required = false) java.util.List<String> termsConditions) {

    Long companyId = securityContextHelper.getCompanyIdFromContext();
    Company company = companyService.getCompanyById(companyId)
        .orElseThrow(() -> new IllegalStateException("Company not found"));

    // Convert terms list to JSON
    String termsJson = "[]";
    if (termsConditions != null && !termsConditions.isEmpty()) {
      try {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
            new com.fasterxml.jackson.databind.ObjectMapper();
        // Filter out empty strings
        java.util.List<String> filteredTerms =
            termsConditions.stream().filter(t -> t != null && !t.trim().isBlank()).toList();
        termsJson = mapper.writeValueAsString(filteredTerms);
      } catch (Exception e) {
        throw new RuntimeException("Failed to serialize terms", e);
      }
    }

    company.setVendorPoTermsConditions(termsJson);
    company.setVendorPoTermsConditionsEnabled("true".equalsIgnoreCase(termsConditionsEnabled));

    companyRepository.save(company);

    java.util.Map<String, Object> response = new java.util.HashMap<>();
    response.put("success", true);
    return response;
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  /**
   * Builds VendorPoItem objects from form arrays and attaches them to the vendor PO.
   */
  private void buildVendorPoItems(VendorPo vendorPo, List<Long> itemProductIds,
      List<Double> itemQuantities, List<Double> itemPreGstUnitPrices, List<String> itemHsnCodes,
      List<String> itemUnits) {

    vendorPo.getItems().clear();

    if (itemProductIds == null || itemProductIds.isEmpty()) {
      return;
    }

    for (int i = 0; i < itemProductIds.size(); i++) {
      Long productId = itemProductIds.get(i);
      if (productId == null)
        continue;

      Double qty =
          (itemQuantities != null && i < itemQuantities.size()) ? itemQuantities.get(i) : null;
      Double preGstUnitPrice = (itemPreGstUnitPrices != null && i < itemPreGstUnitPrices.size())
          ? itemPreGstUnitPrices.get(i)
          : null;
      String hsnCode =
          (itemHsnCodes != null && i < itemHsnCodes.size()) ? itemHsnCodes.get(i) : null;
      String unit = (itemUnits != null && i < itemUnits.size()) ? itemUnits.get(i) : null;

      if (qty == null || preGstUnitPrice == null)
        continue;

      VendorPoItem item = new VendorPoItem();
      Product product = productService.getProductById(productId);
      if (product != null) {
        // Must set productId (the plain @Column) explicitly — the @ManyToOne product field
        // is insertable=false/updatable=false so setProduct() alone never writes product_id to DB.
        item.setProductId(product.getId());
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setHsnCode(product.getHsnCode());
        item.setUnit(product.getUnit());
      }
      // Allow form-overridden HSN/unit (form fields are editable)
      if (hsnCode != null && !hsnCode.isBlank())
        item.setHsnCode(hsnCode);
      if (unit != null && !unit.isBlank())
        item.setUnit(unit);
      item.setQuantity(qty);
      item.setPreGstUnitPrice(java.math.BigDecimal.valueOf(preGstUnitPrice));
      vendorPo.addItem(item);
    }
  }

  /**
   * Sends the PO PDF by email after creation. Mirrors the inline email logic from the original
   * createOrUpdate method, extracted for clarity.
   */
  private void trySendPoEmail(VendorPo saved, RedirectAttributes ra) {
    try {
      byte[] pdf = vendorPoService.generatePdf(saved.getId());

      List<String> toEmails = new ArrayList<>();
      if (saved.getVendor() != null && saved.getVendor().getEmail() != null
          && !saved.getVendor().getEmail().isBlank()) {
        toEmails.add(saved.getVendor().getEmail());
      }
      if (saved.getEmails() != null && !saved.getEmails().isBlank()) {
        for (String email : saved.getEmails().split(",")) {
          String trimmed = email.trim();
          if (!trimmed.isBlank() && !toEmails.contains(trimmed)) {
            toEmails.add(trimmed);
          }
        }
      }

      List<String> ccEmails = new ArrayList<>();
      try {
        User companyAdmin = userService.findPrimaryAdminByCompanyId(saved.getCompany().getId());
        if (companyAdmin != null && companyAdmin.getEmail() != null
            && !companyAdmin.getEmail().isBlank()) {
          ccEmails.add(companyAdmin.getEmail());
        }
      } catch (Exception e) {
        System.err.println("Admin lookup failed: " + e.getMessage());
      }

      if (!toEmails.isEmpty()) {
        Company company = companyService.getCompanyById(saved.getCompany().getId())
            .orElseThrow(() -> new RuntimeException("Company not found"));

        brevoEmailService.sendVendorPoEmail(saved, company, toEmails.toArray(new String[0]),
            ccEmails.toArray(new String[0]), pdf);

        vendorPoService.changeStatus(saved.getId(), VendorPoStatus.SENT);
      }
    } catch (Exception e) {
      ra.addFlashAttribute("poError", "PO saved but email failed: " + e.getMessage());
    }
  }
}
