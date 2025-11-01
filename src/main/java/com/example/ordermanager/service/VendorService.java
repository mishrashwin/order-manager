package com.example.ordermanager.service;

import com.example.ordermanager.entity.Vendor;
import com.example.ordermanager.repository.VendorRepository;
import com.example.ordermanager.utils.Helper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;

    private Helper helper;

    public VendorService(VendorRepository vendorRepository , Helper helper) {
        this.vendorRepository = vendorRepository;
        this.helper = helper;
    }

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
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

    public void deleteVendor(Long id) {
        vendorRepository.deleteById(id);
    }
}
