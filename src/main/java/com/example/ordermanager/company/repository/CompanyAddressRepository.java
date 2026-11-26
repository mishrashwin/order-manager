package com.example.ordermanager.company.repository;

import com.example.ordermanager.company.entity.CompanyAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Company-scoped persistence queries for Company Address records. */
@Repository
public interface CompanyAddressRepository extends JpaRepository<CompanyAddress, Long> {

  /** Returns every active and inactive address belonging to a Company. */
  List<CompanyAddress> findByCompany_IdOrderByIdAsc(Long companyId);

  /** Lists active addresses belonging to one Company for new PO selections. */
  List<CompanyAddress> findByCompany_IdAndActiveTrueOrderByIdAsc(Long companyId);

  /** Finds an address only inside the specified Company boundary. */
  Optional<CompanyAddress> findByIdAndCompany_Id(Long id, Long companyId);

  /** Finds the current active Bill To address for Company Details. */
  Optional<CompanyAddress> findByCompany_IdAndActiveTrueAndDefaultBillToTrue(Long companyId);

  /** Finds the current active Ship To default for Vendor PO snapshots. */
  Optional<CompanyAddress> findByCompany_IdAndActiveTrueAndDefaultShipToTrue(Long companyId);

  /** Checks whether the Company has an active Bill To default. */
  boolean existsByCompany_IdAndActiveTrueAndDefaultBillToTrue(Long companyId);

  /** Checks whether the Company has an active Ship To default. */
  boolean existsByCompany_IdAndActiveTrueAndDefaultShipToTrue(Long companyId);

  /** Clears the Bill To role before another address is promoted within the same transaction. */
  @Modifying(flushAutomatically = true)
  @Query("update CompanyAddress a set a.defaultBillTo = false "
      + "where a.company.id = :companyId and a.defaultBillTo = true")
  int clearBillToDefaultForCompany(@Param("companyId") Long companyId);

  /** Clears the Ship To role before another address is promoted within the same transaction. */
  @Modifying(flushAutomatically = true)
  @Query("update CompanyAddress a set a.defaultShipTo = false "
      + "where a.company.id = :companyId and a.defaultShipTo = true")
  int clearShipToDefaultForCompany(@Param("companyId") Long companyId);
}
