# Branch Testing Guide: vendor_po_number

## Feature overview and testing objective
Verify Vendor PO numbers remain unique and monotonically increasing per Company/prefix, even with gaps, deletions, name-derived prefix changes, and concurrent requests.

## Preconditions and test environment
- **Application/build:** Current branch; run `mvn test`.
- **Environment/profile:** PostgreSQL.
- **Services/configuration:** PostgreSQL connection via `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for opt-in integration test.
- **Preconditions:** Flyway migration V39 applied before PO create traffic.

## Required test data
- Company 4 records LE001–LE011 and LE013; other Companies use prefixes APL and LIPL.

## User roles and permissions
| Role/account | Required permissions | Test purpose |
|---|---|---|
| Company ADMIN | Create Vendor POs in own Company | Tenant-scoped numbering |

## Test scenarios

### Positive test cases

#### VPO-NUM-01 — First PO and established formatting
1. Create the first PO for a company whose current initials are TC.
2. Inspect the persisted PO number.

**Expected result:** The number is `TC001`.

#### VPO-NUM-02 — Existing gaps
1. Configure Company 4 with LE001–LE011 and LE013.
2. Allocate its next sequence value.

**Expected result:** Suffix 14 is returned (`LE014` after formatting).

#### VPO-NUM-03 — Independent counters and deletion
1. Allocate values for Companies 2, 4, and 6.
2. Delete a PO row after its allocation, then allocate again.

**Expected result:** Company counters advance independently; deleted numbers are not reused.

#### VPO-NUM-04 — Concurrent requests
1. Execute 48 allocations for Company 4 concurrently against PostgreSQL.

**Expected result:** All 48 returned suffixes are distinct and contiguous.

#### VPO-NUM-05 — Revision
1. Create an amendment revision from an issued PO.

**Expected result:** Revision retains its original PO number and does not invoke sequence allocation.

### Negative validation scenarios

#### VPO-NUM-06 — Missing company
1. Create a PO when the authenticated Company no longer exists.

**Expected result:** Creation fails through existing service handling; no sequence allocation occurs.

### Authorization and tenant-isolation tests
1. Authenticate as a Company user and create a PO.

**Expected result:** The authenticated Company ID is used for allocation; request data cannot select another company's counter.

### Regression tests
- [ ] Existing PO create format remains prefix plus minimum three digits.
- [ ] Revisions retain their source PO number.
- [ ] Existing uniqueness constraint `ux_vendor_po_company_number_revision` remains in place.

### Database and migration verification
- [ ] Apply V39 in PostgreSQL and verify each existing prefix has `last_value` equal to its highest existing suffix.
- [ ] Verify Company 2 / APL = 1, Company 4 / LE = 13, and Company 6 / LIPL = 5 in the seeded sequence table.
- [ ] Verify matching revision rows do not change the seeded max and an existing counter value is advanced rather than reset.
- [ ] Verify a transaction rollback also rolls back the allocation; an uncommitted suffix may subsequently be allocated again.
- [ ] Run `mvn -DvendorPoSequenceTest=true -Dtest=VendorPoNumberSequencePostgresTest test`.

### PDF, export, and email checks
- Not applicable; PO number allocation does not change PDF, export, or email rendering.

## Test execution checklist
- [ ] Positive scenarios executed.
- [ ] Negative validation scenarios executed.
- [ ] Authorization and tenant-isolation scenarios executed.
- [ ] Regression and migration scenarios executed.

## Test results

| Scenario / area | Status (Pass / Fail / Blocked) | Tester / date | Evidence or defect reference |
|---|---|---|---|
| VendorPoServiceTest | Pass | Codex / 2026-11-26 | 21 tests passed using focused test-source compilation |
| PostgreSQL sequence migration/concurrency test | Pass | Codex / 2026-11-26 | 1 test passed; local PostgreSQL; 12 workers, 48 allocations checked against all suffixes 15–62; 55 allocator calls total (54 committed, 1 rolled back) |
| Full project test compilation | Blocked | Codex / 2026-11-26 | Existing duplicate method `formatsLargeSnapshotAmountsForPdf()` in `VendorPdfServiceSnapshotTest.java:97` |

**Overall status:** Pass for focused sequence/service tests; full suite remains blocked

**Blockers or notes:** The normal full test compile hits an unrelated preexisting duplicate method. Focused tests were run with `maven.compiler.testIncludes` to compile only the relevant test source.
