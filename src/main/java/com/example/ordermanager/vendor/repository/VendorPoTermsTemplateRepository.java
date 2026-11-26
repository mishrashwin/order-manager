package com.example.ordermanager.vendor.repository;

import com.example.ordermanager.vendor.entity.VendorPoTermsTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tenant-scoped storage for reusable Vendor PO terms. */
public interface VendorPoTermsTemplateRepository
    extends JpaRepository<VendorPoTermsTemplate, Long> {

  /** Lists active terms templates belonging to the authenticated Company. */
  List<VendorPoTermsTemplate> findByCompany_IdAndActiveTrueOrderByNameAsc(Long companyId);

  /** Lists active and inactive templates within the requested Company boundary. */
  List<VendorPoTermsTemplate> findByCompany_IdOrderByNameAsc(Long companyId);

  /** Finds a template by ID only inside the authenticated Company. */
  Optional<VendorPoTermsTemplate> findByIdAndCompany_Id(Long id, Long companyId);
}
