package com.example.ordermanager.controller;

import com.example.ordermanager.entity.Vendor;
import com.example.ordermanager.service.VendorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    public String listVendors(Model model) {
        model.addAttribute("vendors", vendorService.getAllVendors());
        return "vendors/list";
    }

    @GetMapping("/new")
    public String newVendorForm(Model model) {
        model.addAttribute("vendor", new Vendor());
        return "vendors/form";
    }

    @PostMapping
    public String saveVendor(@ModelAttribute Vendor vendor) {
        vendorService.saveVendor(vendor);
        return "redirect:/vendors";
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
