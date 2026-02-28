package com.example.ordermanager.service;

import com.example.ordermanager.entity.Vendor;
import com.example.ordermanager.entity.Company;
import com.example.ordermanager.repository.VendorRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

  private final VendorRepository vendorRepository;
  private final CompanyService companyService;
  private Helper helper;

  public VendorService(VendorRepository vendorRepository, CompanyService companyService,
      Helper helper) {
    this.vendorRepository = vendorRepository;
    this.companyService = companyService;
    this.helper = helper;
  }

  public List<Vendor> getAllVendors() {
    return vendorRepository.findAll();
  }

  /**
   * TENANT-AWARE: Get all vendors for a specific company
   *
   * @param companyId Company ID
   * @return List of vendors for the company
   */
  public List<Vendor> getVendorsByCompanyId(Long companyId) {
    return vendorRepository.findByCompanyId(companyId);
  }

  public Vendor getVendorById(Long id) {
    return vendorRepository.findById(id).orElse(null);
  }

  public Vendor saveVendor(Vendor vendor) {
    if (vendor.getCompanyName() != null) {
      vendor.setCompanyName(vendor.getCompanyName().toUpperCase());
    }
    if (vendor.getContactPerson() != null) {
      vendor.setContactPerson(helper.toTitleCase(vendor.getContactPerson()));
    }
    return vendorRepository.save(vendor);
  }

  /**
   * TENANT-AWARE: Save vendor with company association The vendor is automatically assigned to the
   * authenticated user's company
   *
   * @param vendor Vendor entity
   * @param companyId Company ID
   */
  public Vendor saveVendorWithCompany(Vendor vendor, Long companyId) {
    // Get company and assign to vendor
    Company company =
        companyService.getCompanyById(companyId).orElseThrow(() -> new IllegalArgumentException(
            "Company not found. Cannot create vendor without a company."));

    vendor.setCompany(company);

    // Apply formatting
    if (vendor.getCompanyName() != null) {
      vendor.setCompanyName(vendor.getCompanyName().toUpperCase());
    }
    if (vendor.getContactPerson() != null) {
      vendor.setContactPerson(helper.toTitleCase(vendor.getContactPerson()));
    }

    return vendorRepository.save(vendor);
  }

  public void deleteVendor(Long id) {
    vendorRepository.deleteById(id);
  }
}
