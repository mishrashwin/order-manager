package com.example.ordermanager.vendor.controller;

import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.vendor.service.VendorService;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.utils.SecurityContextHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vendors")
public class VendorController {

  private final VendorService vendorService;
  private final CompanyService companyService;
  private final SecurityContextHelper securityContextHelper;

  public VendorController(VendorService vendorService, CompanyService companyService,
      SecurityContextHelper securityContextHelper) {
    this.vendorService = vendorService;
    this.companyService = companyService;
    this.securityContextHelper = securityContextHelper;
  }

  @GetMapping
  public String listVendors(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    model.addAttribute("vendors", vendorService.getVendorsByCompanyId(companyId));
    return "vendors/list";
  }

  @GetMapping("/new")
  public String newVendorForm(Model model) {
    model.addAttribute("vendor", new Vendor());
    return "vendors/form";
  }

  @PostMapping
  public String saveVendor(@ModelAttribute Vendor vendor, Model model) {
    try {
      Long companyId = securityContextHelper.getCompanyIdFromContext();
      vendorService.saveVendorWithCompany(vendor, companyId);
      return "redirect:/vendors";
    } catch (IllegalArgumentException e) {
      String message = e.getMessage();
      model.addAttribute("error", message);
      if (isPhoneValidationError(message)) {
        model.addAttribute("phoneError", message);
      }
      model.addAttribute("vendor", vendor);
      return "vendors/form";
    } catch (IllegalStateException e) {
      model.addAttribute("error", "You must be logged in to create a vendor");
      model.addAttribute("vendor", vendor);
      return "vendors/form";
    } catch (Exception e) {
      model.addAttribute("error", "Error saving vendor: " + e.getMessage());
      model.addAttribute("vendor", vendor);
      return "vendors/form";
    }
  }

  private boolean isPhoneValidationError(String message) {
    return message != null && message.toLowerCase().contains("phone");
  }

  @GetMapping("/edit/{id}")
  public String editVendor(@PathVariable Long id, Model model) {
    model.addAttribute("vendor", vendorService.getVendorById(id));
    return "vendors/form";
  }

  @GetMapping("/delete/{id}")
  public String deleteVendor(@PathVariable Long id) {
    vendorService.deleteVendor(id);
    return "redirect:/vendors";
  }
}
