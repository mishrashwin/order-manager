package com.example.ordermanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.entity.Vendor;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
}
