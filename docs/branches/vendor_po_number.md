# Branch: vendor_po_number

## Branch and objective
- **Branch name:** `vendor_po_number`
- **Feature / development objective:** Eliminate duplicate Vendor PO numbers caused by count-based allocation and concurrent creates.
- **Status / dates:** Implemented; 2026-11-26.

## Problem statement
`countByCompany_IdAndRevisionNumber + 1` reuses suffixes when rows have been deleted or suffixes contain gaps, and concurrent requests race. Production reported the LE013 collision for Company 4.

## Scope
- **Included:** Company/prefix-scoped PostgreSQL sequence, legacy suffix backfill, atomic transactional allocation, tests and documentation.
- **Excluded:** GST, financial calculations, PDF generation, and unrelated PO behavior.

## Features implemented
- [x] Atomic allocation of gap-safe original PO numbers; revisions preserve the original number.

## Technical implementation summary
`VendorPoNumberSequence` performs a PostgreSQL upsert with `RETURNING`. The current company initials remain the number prefix. Sequence rows are keyed by company and prefix to support company name changes without recycling old prefix values.

## Database and entity changes
V39 creates `vendor_po_number_sequence(company_id, prefix, last_value)` with a tenant foreign key and composite primary key. It backfills the maximum trailing numeric suffix for each existing Company/prefix.

## API, controller, and service changes
`VendorPoServiceImpl.createVendorPo` obtains the authenticated company ID as before and reserves a suffix before saving the PO. Allocation executes in the existing `@Transactional` method. Revision behavior is unchanged.

## UI, template, and static asset changes
None.

## Business rules and validations
The output remains current company initials followed by a minimum three-digit suffix. Gaps are not compacted; allocated values persist after PO deletion.

## Security and tenant isolation
Sequence company ID comes only from `SecurityContextHelper`; database key scopes counters to that company and prefix.

## Configuration changes
None.

## Migration details
`V39__vendor_po_number_sequences.sql` creates the counter table and seeds all existing PO values matching a trailing numeric suffix. PostgreSQL is required.

## Dependencies and prerequisites
Deploy with Flyway enabled against PostgreSQL while all old PO writers are stopped. Verify seeded counters before the new version accepts PO creation. Automatic Flyway startup does not itself drain old instances.

## Known limitations
- Historical deleted numbers cannot be reconstructed if no surviving PO or prior counter recorded them. After V39, allocations are retained in the counter table.
- PostgreSQL integration test requires `-DvendorPoSequenceTest=true` and configured `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

## Future enhancements
None.

## Related commits and PRs
- Not available yet.

## Documentation updates
- [x] Root README updated to mention gap-safe PostgreSQL numbering.
- [x] Backend, multi-tenant, and testing rules plus controller/service test records updated.
- [x] Paired testing guide: [vendor_po_number-TESTING.md](vendor_po_number-TESTING.md)

## Decisions and outcomes
- **Decision:** Keep counters per company and prefix because company initials are recalculated from mutable company names.
- **Transaction behavior:** `createVendorPo()` is `@Transactional`; Spring Boot's `JdbcTemplate` and JPA transaction manager use the same application DataSource. The sequence upsert is part of that transaction. If PO creation rolls back, its counter increment also rolls back and that uncommitted suffix may be allocated again; committed/deleted PO suffixes remain consumed.
- **Outcome:** Count-based allocation removed. Focused PostgreSQL sequence test (1) and VendorPoServiceTest (21) pass; full suite compilation is blocked by an unrelated duplicate test method. See test guide.
