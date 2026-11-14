package com.example.ordermanager.company.controller;

import com.example.ordermanager.company.dto.CompanyAddressForm;
import com.example.ordermanager.company.service.CompanyAddressService;
import com.example.ordermanager.utils.IndianState;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** ADMIN-only MVC endpoints for managing addresses owned by the authenticated Company. */
@Controller
@RequestMapping("/admin/company/addresses")
public class CompanyAddressController {

  private final CompanyAddressService addressService;

  /** Injects the tenant-scoped service used by address management routes. */
  public CompanyAddressController(CompanyAddressService addressService) {
    this.addressService = addressService;
  }

  /** Shows all active and inactive addresses for the authenticated Company. */
  @GetMapping
  public String list(Model model) {
    model.addAttribute("addresses", addressService.listAddresses());
    return "admin/company/addresses";
  }

  /** Shows a blank address form for creating an address. */
  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("addressForm", new CompanyAddressForm());
    model.addAttribute("states", IndianState.values());
    return "admin/company/address-form";
  }

  /** Validates and creates an active address under the authenticated Company. */
  @PostMapping
  public String create(@Valid @ModelAttribute("addressForm") CompanyAddressForm form,
      BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("states", IndianState.values());
      return "admin/company/address-form";
    }
    try {
      addressService.createAddress(form);
      redirectAttributes.addFlashAttribute("message", "Company address added successfully.");
      return "redirect:/admin/company/addresses";
    } catch (IllegalArgumentException | IllegalStateException exception) {
      model.addAttribute("states", IndianState.values());
      model.addAttribute("error", exception.getMessage());
      return "admin/company/address-form";
    }
  }

  /** Loads a Company-scoped active address into the edit form. */
  @GetMapping("/{addressId}/edit")
  public String editForm(@PathVariable Long addressId, Model model,
      RedirectAttributes redirectAttributes) {
    try {
      model.addAttribute("addressForm", toForm(addressService.getAddress(addressId)));
      model.addAttribute("addressId", addressId);
      model.addAttribute("states", IndianState.values());
      return "admin/company/address-form";
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("error", exception.getMessage());
      return "redirect:/admin/company/addresses";
    }
  }

  /** Validates and updates address details without accepting a browser-supplied Company ID. */
  @PostMapping("/{addressId}")
  public String update(@PathVariable Long addressId,
      @Valid @ModelAttribute("addressForm") CompanyAddressForm form, BindingResult bindingResult,
      Model model, RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("addressId", addressId);
      model.addAttribute("states", IndianState.values());
      return "admin/company/address-form";
    }
    try {
      addressService.updateAddress(addressId, form);
      redirectAttributes.addFlashAttribute("message", "Company address updated successfully.");
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/admin/company/addresses";
  }

  /** Changes the Bill To default role after service-level tenant and active checks. */
  @PostMapping("/{addressId}/default-bill-to")
  public String setDefaultBillTo(@PathVariable Long addressId,
      RedirectAttributes redirectAttributes) {
    return runAddressAction(addressId, "Default Bill To address updated.", redirectAttributes,
        () -> addressService.setDefaultBillTo(addressId));
  }

  /** Changes the Ship To default role after service-level tenant and active checks. */
  @PostMapping("/{addressId}/default-ship-to")
  public String setDefaultShipTo(@PathVariable Long addressId,
      RedirectAttributes redirectAttributes) {
    return runAddressAction(addressId, "Default Ship To address updated.", redirectAttributes,
        () -> addressService.setDefaultShipTo(addressId));
  }

  /** Deactivates an address after service-level default-role checks. */
  @PostMapping("/{addressId}/deactivate")
  public String deactivate(@PathVariable Long addressId, RedirectAttributes redirectAttributes) {
    return runAddressAction(addressId, "Company address deactivated.", redirectAttributes,
        () -> addressService.deactivateAddress(addressId));
  }

  /** Reactivates an address without changing its stored default roles. */
  @PostMapping("/{addressId}/activate")
  public String activate(@PathVariable Long addressId, RedirectAttributes redirectAttributes) {
    return runAddressAction(addressId, "Company address activated.", redirectAttributes,
        () -> addressService.activateAddress(addressId));
  }

  /** Applies one address action and reports its success or safe failure to the list page. */
  private String runAddressAction(Long addressId, String successMessage,
      RedirectAttributes redirectAttributes, Runnable action) {
    try {
      action.run();
      redirectAttributes.addFlashAttribute("message", successMessage);
    } catch (IllegalArgumentException | IllegalStateException exception) {
      redirectAttributes.addFlashAttribute("error", exception.getMessage());
    }
    return "redirect:/admin/company/addresses";
  }

  /** Copies persisted address fields into a form that cannot bind ownership or default flags. */
  private CompanyAddressForm toForm(
      com.example.ordermanager.company.entity.CompanyAddress address) {
    CompanyAddressForm form = new CompanyAddressForm();
    form.setNickname(address.getNickname());
    form.setAddressLine1(address.getAddressLine1());
    form.setAddressLine2(address.getAddressLine2());
    form.setCity(address.getCity());
    form.setState(address.getState());
    form.setPostalCode(address.getPostalCode());
    form.setCountry(address.getCountry());
    form.setGstin(address.getGstin());
    form.setDrugLicenseNumber(address.getDrugLicenseNumber());
    form.setContactPersonName(address.getContactPersonName());
    form.setContactNumber(address.getContactNumber());
    form.setContactEmail(address.getContactEmail());
    return form;
  }
}
