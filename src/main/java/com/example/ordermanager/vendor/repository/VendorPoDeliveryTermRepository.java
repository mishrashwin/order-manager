package com.example.ordermanager.vendor.repository;

import com.example.ordermanager.vendor.entity.VendorPoDeliveryTerm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tenant-scoped storage for reusable delivery-term options. */
public interface VendorPoDeliveryTermRepository extends JpaRepository<VendorPoDeliveryTerm, Long> {

  /** Lists active delivery-term options belonging to the authenticated Company. */
  List<VendorPoDeliveryTerm> findByCompany_IdAndActiveTrueOrderByNameAsc(Long companyId);

  /** Lists active and inactive options within the requested Company boundary. */
  List<VendorPoDeliveryTerm> findByCompany_IdOrderByNameAsc(Long companyId);

  /** Finds an option by ID only inside the authenticated Company. */
  Optional<VendorPoDeliveryTerm> findByIdAndCompany_Id(Long id, Long companyId);
}
