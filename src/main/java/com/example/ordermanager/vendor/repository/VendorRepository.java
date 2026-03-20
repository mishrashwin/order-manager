package com.example.ordermanager.vendor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.vendor.entity.Vendor;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
  List<Vendor> findByCompanyId(Long companyId);
}
