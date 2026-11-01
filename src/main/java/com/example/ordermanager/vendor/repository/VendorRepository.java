package com.example.ordermanager.vendor.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.ordermanager.vendor.entity.Vendor;

import java.util.List;
import java.util.Collection;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
  @EntityGraph(attributePaths = {"company"})
  List<Vendor> findByCompanyId(Long companyId);

  Page<Vendor> findByCompanyId(Long companyId, Pageable pageable);

  Page<Vendor> findByCompanyIdAndCompanyNameContainingIgnoreCase(Long companyId, String search,
      Pageable pageable);

  /** Fetches tenant vendors matching any supplied GSTIN in one case-insensitive query. */
  List<Vendor> findByCompanyIdAndGstnInIgnoreCase(Long companyId, Collection<String> gstns);

  /** Fetches tenant vendors matching any supplied email in one case-insensitive query. */
  List<Vendor> findByCompanyIdAndEmailInIgnoreCase(Long companyId, Collection<String> emails);

  /** Fetches tenant vendors matching any supplied normalized phone number. */
  List<Vendor> findByCompanyIdAndPhoneIn(Long companyId, Collection<String> phones);

  /** Fetches vendors linked to one upload request for reliable commit reconciliation. */
  List<Vendor> findByCompanyIdAndBulkUploadRequestId(Long companyId, Long bulkUploadRequestId);
}
