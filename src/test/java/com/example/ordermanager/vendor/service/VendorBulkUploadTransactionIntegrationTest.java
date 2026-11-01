package com.example.ordermanager.vendor.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.ordermanager.company.entity.Company;
import com.example.ordermanager.vendor.entity.VendorBulkUploadRequest;
import com.example.ordermanager.vendor.entity.Vendor;
import com.example.ordermanager.company.service.CompanyService;
import com.example.ordermanager.company.repository.CompanyRepository;
import com.example.ordermanager.utils.Helper;
import com.example.ordermanager.vendor.repository.VendorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Verifies independent vendor row commits and rollback using a real relational database. */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:vendor_bulk_tx;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({VendorBulkUploadService.class, VendorService.class,
    VendorBulkUploadIdempotencyService.class, VendorBulkUploadRowRegistrationService.class,
    Helper.class, VendorBulkUploadJsonTestConfiguration.class})
class VendorBulkUploadTransactionIntegrationTest {

  @Autowired
  private VendorBulkUploadService bulkUploadService;
  @Autowired
  private VendorRepository vendorRepository;
  @Autowired
  private CompanyRepository companyRepository;
  @Autowired
  private JdbcTemplate jdbcTemplate;
  @Autowired
  private VendorBulkUploadIdempotencyService idempotencyService;
  @Autowired
  private VendorBulkUploadRowRegistrationService rowRegistrationService;
  @MockBean
  private CompanyService companyService;

  /**
   * Commits independent valid rows, rolls back one constraint failure, and reports exact outcomes.
   */
  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void registerValidVendors_commitsSuccessfulRowsAroundConstraintFailure() {
    Company company = companyRepository.saveAndFlush(new Company("Bulk Transaction Tenant"));
    when(companyService.getCompanyById(company.getId())).thenReturn(Optional.of(company));
    jdbcTemplate
        .execute("CREATE UNIQUE INDEX test_vendor_company_email_uq ON vendors(company_id, email)");
    List<VendorBulkUploadValidationResult> rows =
        List.of(validRow(2, "first@example.com", "27ABCDE1234F1Z5", "+919876543210"),
            validRow(4, "first@example.com", "27ABCDE1234F1Z6", "+919876543211"),
            validRow(7, "last@example.com", "27ABCDE1234F1Z7", "+919876543212"));

    VendorBulkRegistrationResult result =
        bulkUploadService.registerValidVendors(rows, company.getId());

    assertThat(vendorRepository.findByCompanyId(company.getId())).hasSize(2);
    assertThat(result.totalParsedRows()).isEqualTo(3);
    assertThat(result.validRowCount()).isEqualTo(3);
    assertThat(result.invalidRowCount()).isZero();
    assertThat(result.registeredRowCount()).isEqualTo(2);
    assertThat(result.registrationFailureCount()).isEqualTo(1);
    assertThat(result.skippedRowCount()).isZero();
    assertThat(result.rowResults()).extracting(VendorBulkRegistrationResult.RowResult::status)
        .containsExactly(VendorBulkRegistrationResult.Status.REGISTERED,
            VendorBulkRegistrationResult.Status.FAILED,
            VendorBulkRegistrationResult.Status.REGISTERED);
    assertThat(result.rowResults().get(1).excelRowNumber()).isEqualTo(4);
    assertThat(result.rowResults().get(1).failureReason())
        .isEqualTo("Vendor conflicts with existing company data or violates a data constraint.");
  }

  /**
   * Verifies tenant-scoped claims, fingerprint conflicts, completed replay, and abandoned claims.
   */
  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void idempotencyClaims_areCompanyScopedAndReplayFinalResponses() {
    Company firstCompany = companyRepository.saveAndFlush(new Company("Bulk Idempotency A"));
    Company secondCompany = companyRepository.saveAndFlush(new Company("Bulk Idempotency B"));

    var claimed = idempotencyService.claim(firstCompany.getId(), "shared-key", "fingerprint-a",
        "upload.xlsx", "admin");
    var repeated = idempotencyService.claim(firstCompany.getId(), "shared-key", "fingerprint-a",
        "upload.xlsx", "admin");
    var conflict = idempotencyService.claim(firstCompany.getId(), "shared-key", "fingerprint-b",
        "upload.xlsx", "admin");
    assertThat(claimed.state()).isEqualTo(VendorBulkUploadIdempotencyService.ClaimState.CLAIMED);
    assertThat(repeated.state())
        .isEqualTo(VendorBulkUploadIdempotencyService.ClaimState.IN_PROGRESS);
    assertThat(conflict.state())
        .isEqualTo(VendorBulkUploadIdempotencyService.ClaimState.KEY_CONFLICT);

    idempotencyService.complete(firstCompany.getId(), "shared-key",
        java.util.Map.of("success", true, "registeredRowCount", 2));
    var replay = idempotencyService.claim(firstCompany.getId(), "shared-key", "fingerprint-a",
        "other-name.xlsx", "other-admin");
    var otherTenant = idempotencyService.claim(secondCompany.getId(), "shared-key", "fingerprint-a",
        "upload.xlsx", "admin");
    var newAttempt = idempotencyService.claim(firstCompany.getId(), "new-key", "fingerprint-b",
        "changed.xlsx", "admin");
    idempotencyService.fail(firstCompany.getId(), "new-key",
        java.util.Map.of("success", false, "message", "terminal failure"));
    var failedRetry = idempotencyService.claim(firstCompany.getId(), "retry-key", "fingerprint-b",
        "changed.xlsx", "admin");
    assertThat(replay.state()).isEqualTo(VendorBulkUploadIdempotencyService.ClaimState.REPLAY);
    assertThat(replay.response()).containsEntry("success", true).containsEntry("registeredRowCount",
        2);
    assertThat(otherTenant.state())
        .isEqualTo(VendorBulkUploadIdempotencyService.ClaimState.CLAIMED);
    assertThat(newAttempt.state()).isEqualTo(VendorBulkUploadIdempotencyService.ClaimState.CLAIMED);
    assertThat(failedRetry.state()).isEqualTo(VendorBulkUploadIdempotencyService.ClaimState.REPLAY);
    assertThat(failedRetry.response()).containsEntry("message", "terminal failure");
  }

  /**
   * Applies the Flyway DDL to an isolated PostgreSQL-mode database to validate its schema syntax.
   */
  @Test
  void idempotencyMigration_runsOnPostgresCompatibleDatabase() {
    var dataSource = new DriverManagerDataSource(
        "jdbc:h2:mem:vendor_bulk_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
    JdbcTemplate migrationJdbc = new JdbcTemplate(dataSource);
    migrationJdbc.execute("CREATE TABLE companies (id BIGINT PRIMARY KEY)");
    migrationJdbc
        .execute("CREATE TABLE vendors (id BIGSERIAL PRIMARY KEY, company_id BIGINT NOT NULL)");
    new ResourceDatabasePopulator(
        new ClassPathResource("db/migration/V30__add_vendor_bulk_upload_idempotency.sql"))
        .execute(dataSource);
    new ResourceDatabasePopulator(
        new ClassPathResource("db/migration/V31__add_vendor_bulk_upload_audit_and_correlation.sql"))
        .execute(dataSource);
    assertThat(new JdbcTemplate(dataSource).queryForObject(
        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?", Integer.class,
        "VENDOR_BULK_UPLOAD_REQUESTS")).isEqualTo(1);
  }

  /**
   * Verifies upload row correlation and completes only when every valid manifest row is persisted.
   */
  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void reconciliation_completesWhenAllPlannedRowsHaveDurableCorrelation() {
    Company company = companyRepository.saveAndFlush(new Company("Bulk Reconcile Complete"));
    Company otherCompany = companyRepository.saveAndFlush(new Company("Bulk Reconcile Other"));
    when(companyService.getCompanyById(company.getId())).thenReturn(Optional.of(company));
    var claim = idempotencyService.claim(company.getId(), "reconcile-key", "fingerprint",
        "safe.xlsx", "tenant-admin");
    var valid = validRow(2, "reconcile@example.com", "27ABCDE1234F1Z5", "+919876543210");
    var skippedRow = new VendorBulkUploadIdempotencyService.ManifestRow(5, "Skipped Vendor",
        "INVALID", List.of("email: Email format is invalid."));
    idempotencyService.saveManifest(company.getId(), "reconcile-key",
        new VendorBulkUploadIdempotencyService.Manifest(2, 1, 1,
            List.of(new VendorBulkUploadIdempotencyService.ManifestRow(2, "Reconcile Vendor",
                "VALID", List.of()), skippedRow)));

    var registered =
        bulkUploadService.registerValidVendors(List.of(valid), company.getId(), claim.requestId());
    assertThat(registered.registeredRowCount()).isEqualTo(1);
    var vendor = vendorRepository
        .findByCompanyIdAndBulkUploadRequestId(company.getId(), claim.requestId()).get(0);
    assertThat(vendor.getBulkUploadRowNumber()).isEqualTo(2);
    assertThat(vendor.getBulkUploadProcessedAt()).isNotNull();
    jdbcTemplate.update(
        "UPDATE vendor_bulk_upload_requests SET updated_at = CURRENT_TIMESTAMP - INTERVAL '2' HOUR WHERE id = ?",
        claim.requestId());

    var outcome = idempotencyService.reconcile(company.getId(), claim.requestId());

    assertThat(outcome.state())
        .isEqualTo(VendorBulkUploadRequest.ReconciliationState.FULLY_COMMITTED);
    var response = idempotencyService.completedResponse(company.getId(), claim.requestId());
    assertThat(response).containsEntry("registeredRowCount", 1).containsEntry("skippedRowCount", 1);
    assertThat(idempotencyService.completedReportData(company.getId(), claim.requestId()).rows())
        .hasSize(2);
    assertThatThrownBy(
        () -> idempotencyService.completedResponse(otherCompany.getId(), claim.requestId()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
        () -> idempotencyService.completedReportData(otherCompany.getId(), claim.requestId()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /**
   * Stops unverified continuation after partial reconciliation and preserves committed evidence.
   */
  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void reconciliation_partialCommitRequiresReviewAndCannotResumeWrites() {
    Company company = companyRepository.saveAndFlush(new Company("Bulk Reconcile Partial"));
    when(companyService.getCompanyById(company.getId())).thenReturn(Optional.of(company));
    var claim = idempotencyService.claim(company.getId(), "partial-key", "partial-fingerprint",
        "partial.xlsx", "tenant-admin");
    var first = validRow(2, "first-reconcile@example.com", "27ABCDE1234F1Z5", "+919876543210");
    var second = validRow(3, "second-reconcile@example.com", "27ABCDE1234F1Z6", "+919876543211");
    idempotencyService.saveManifest(company.getId(), "partial-key",
        new VendorBulkUploadIdempotencyService.Manifest(2, 2, 0, List.of(
            new VendorBulkUploadIdempotencyService.ManifestRow(2, "First", "VALID", List.of()),
            new VendorBulkUploadIdempotencyService.ManifestRow(3, "Second", "VALID", List.of()))));
    assertThat(idempotencyService.unresolvedRequests(company.getId())).isEmpty();
    bulkUploadService.registerValidVendors(List.of(first), company.getId(), claim.requestId());
    jdbcTemplate.update(
        "UPDATE vendor_bulk_upload_requests SET updated_at = CURRENT_TIMESTAMP - INTERVAL '2' HOUR WHERE id = ?",
        claim.requestId());

    var outcome = idempotencyService.reconcile(company.getId(), claim.requestId());

    assertThat(outcome.state())
        .isEqualTo(VendorBulkUploadRequest.ReconciliationState.PARTIALLY_COMMITTED);
    assertThat(idempotencyService.unresolvedRequests(company.getId()))
        .extracting(item -> item.get("registrationReference")).contains(claim.requestId());
    assertThatThrownBy(() -> rowRegistrationService.register(new Vendor(), company.getId(),
        claim.requestId(), second.excelRowNumber())).isInstanceOf(IllegalStateException.class);
    assertThat(
        vendorRepository.findByCompanyIdAndBulkUploadRequestId(company.getId(), claim.requestId()))
        .hasSize(1);
  }

  /**
   * Leaves a fresh claim alone, then flags a stale zero-commit request for manual investigation.
   */
  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void reconciliation_recentRequestIsProtectedAndStaleEmptyRequestRemainsUnresolved() {
    Company company = companyRepository.saveAndFlush(new Company("Bulk Reconcile Empty"));
    var claim = idempotencyService.claim(company.getId(), "empty-key", "empty-fingerprint",
        "empty.xlsx", "tenant-admin");
    idempotencyService.saveManifest(company.getId(), "empty-key",
        new VendorBulkUploadIdempotencyService.Manifest(1, 1, 0,
            List.of(new VendorBulkUploadIdempotencyService.ManifestRow(2, "Not Yet Saved", "VALID",
                List.of()))));

    assertThatThrownBy(() -> idempotencyService.reconcile(company.getId(), claim.requestId()))
        .isInstanceOf(IllegalStateException.class);
    jdbcTemplate.update(
        "UPDATE vendor_bulk_upload_requests SET updated_at = CURRENT_TIMESTAMP - INTERVAL '2' HOUR WHERE id = ?",
        claim.requestId());
    var result = idempotencyService.reconcile(company.getId(), claim.requestId());

    assertThat(result.state())
        .isEqualTo(VendorBulkUploadRequest.ReconciliationState.NO_RECORDS_COMMITTED);
    assertThat(idempotencyService.unresolvedRequests(company.getId()))
        .extracting(item -> item.get("registrationReference")).contains(claim.requestId());
    assertThat(
        vendorRepository.findByCompanyIdAndBulkUploadRequestId(company.getId(), claim.requestId()))
        .isEmpty();
  }

  /**
   * Resolves simultaneous key claims through the database unique constraint, not JVM coordination.
   */
  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void idempotencyClaims_concurrentSameTenantRequestsHaveOneWinner() throws Exception {
    Company company = companyRepository.saveAndFlush(new Company("Bulk Idempotency Concurrent"));
    var ready = new java.util.concurrent.CountDownLatch(2);
    var start = new java.util.concurrent.CountDownLatch(1);
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var claims = java.util.stream.IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
        ready.countDown();
        start.await();
        return idempotencyService.claim(company.getId(), "race-key-" + index, "same-fingerprint",
            "upload.xlsx", "admin");
      })).toList();
      ready.await();
      start.countDown();
      var states = List.of(claims.get(0).get().state(), claims.get(1).get().state());
      assertThat(states).containsExactlyInAnyOrder(
          VendorBulkUploadIdempotencyService.ClaimState.CLAIMED,
          VendorBulkUploadIdempotencyService.ClaimState.IN_PROGRESS);
    } finally {
      executor.shutdownNow();
    }
  }

  /** Creates a valid row result that the registration service is permitted to persist. */
  private VendorBulkUploadValidationResult validRow(int rowNumber, String email, String gstn,
      String phone) {
    VendorBulkUploadRowDto row = new VendorBulkUploadRowDto(rowNumber, "Tenant Vendor " + rowNumber,
        "Jane Doe", email, phone, gstn, "Maharashtra", "Mumbai");
    return new VendorBulkUploadValidationResult(rowNumber, row,
        VendorBulkUploadValidationResult.Status.VALID, List.of());
  }

}


/** Supplies JSON serialization for persisted replay payloads in the database-backed test slice. */
@Configuration
class VendorBulkUploadJsonTestConfiguration {
  /** Provides the serializer used to persist and replay idempotency response payloads. */
  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
