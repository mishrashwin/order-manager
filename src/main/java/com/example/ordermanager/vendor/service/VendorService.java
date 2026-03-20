package com.example.ordermanager.vendor.service;

import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

  private final VendorRepository vendorRepository;
  private final CompanyService companyService;
  private final Helper helper;

  public VendorService(VendorRepository vendorRepository, CompanyService companyService,
      Helper helper) {
    this.vendorRepository = vendorRepository;
    this.companyService = companyService;
    this.helper = helper;
  }

  public List<Vendor> getVendorsByCompanyId(Long companyId) {
    return vendorRepository.findByCompanyId(companyId);
  }

  public Vendor getVendorById(Long id) {
    return vendorRepository.findById(id).orElse(null);
  }

  public Vendor saveVendorWithCompany(Vendor vendor, Long companyId) {
    Company company =
        companyService.getCompanyById(companyId).orElseThrow(() -> new IllegalArgumentException(
            "Company not found. Cannot create vendor without a company."));

    vendor.setCompany(company);

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
