package com.example.ordermanager.vendor.service;

import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.example.ordermanager.utils.Helper;
import com.example.ordermanager.utils.PhoneNumberUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  public Page<Vendor> getVendorsByCompanyId(Long companyId, Pageable pageable) {
    return vendorRepository.findByCompanyId(companyId, pageable);
  }

  public Page<Vendor> searchVendors(Long companyId, String search, Pageable pageable) {
    if (search == null || search.trim().isEmpty()) {
      return vendorRepository.findByCompanyId(companyId, pageable);
    }
    return vendorRepository.findByCompanyIdAndCompanyNameContainingIgnoreCase(companyId, search,
        pageable);
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

    vendor.setPhone(
        PhoneNumberUtils.normalizeOptionalInternational(vendor.getPhone(), "Vendor Phone No"));

    return vendorRepository.save(vendor);
  }

  public void deleteVendor(Long id) {
    vendorRepository.deleteById(id);
  }
}
