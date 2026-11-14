# Branch Testing Guide: `vendor-po-redesign`

## Feature overview and testing objective
Verify Phase 0.1 Vendor PO immutability and Phase 1 Company Address management, legacy backfill, role defaults, validation, authorization, and tenant isolation.

Address correction coverage also verifies activation/deactivation preservation, explicit reassignment of each default role (including dual-default addresses), live Company Details Bill To/GSTIN rendering, Drug License persistence, and tenant isolation.

## Preconditions and test environment
- **Application/build:** Java 17, Maven, branch `vendor-po-redesign`.
- **Environment/profile:** Automated controller/service tests use MockMvc and Mockito. Migration test requires PostgreSQL and is opt-in.
- **Services/configuration:** Phases 2–8 exercise PDF rendering, email/status lifecycle, decimal calculations, and PostgreSQL migrations. Standard dev profile uses PostgreSQL; `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` may override its defaults.
- **Preconditions:** ADMIN account for two Companies for manual UI scenarios. Existing Company address rows are backfilled by V32.

## Required test data
- Company A and Company B, each with an ADMIN.
- For Company A: one dual-default address, one non-default address, and one active address to promote.
- Same GSTIN value on two addresses.
- Optional Drug License Number on an address, including a value retained through deactivate/reactivate.
- Legacy Company records with populated and null legacy address/GSTIN/state fields.
- Vendor POs with DRAFT and each non-DRAFT status for Phase 0.1 regression.

## User roles and permissions
| Role/account | Required permissions | Test purpose |
|---|---|---|
| Company A ADMIN | `/admin/**` access for Company A | Manage Company A addresses |
| Company B ADMIN | `/admin/**` access for Company B | Verify tenant boundary |
| Authenticated tenant user | Vendor PO access | Verify Phase 0.1 update behavior |

## Manual test scenarios

### Positive test cases

#### CA-01 — Create and edit an address
1. Log in as Company A ADMIN and open `/admin/company` → **Manage Addresses**.
2. Add a nickname, address lines, city, state, postal code, country, and optional GSTIN.
3. Edit the address and save.

**Expected result:** Address appears under Company A and can be edited without changing its Company owner. Required fields are shown in the address list.

#### CA-02 — Set and change default roles
1. Set an active address as default Bill To.
2. Set the same or another active address as default Ship To.
3. Move each role to another active address.

**Expected result:** Each role has exactly one active default; one address may hold both roles. Confirmation appears after each change.

#### CA-03 — Duplicate GSTIN and deactivate
1. Add two addresses with the same GSTIN.
2. Deactivate an address that has neither default role.

**Expected result:** Both GSTIN values are accepted. The selected address becomes Inactive and remains visible in the list.

#### CA-08 — Activate and preserve address data
1. Deactivate a non-default address containing GSTIN and Drug License Number.
2. Activate it from the inactive row.

**Expected result:** The address becomes Active with every stored field intact; it receives no Bill To or Ship To default automatically.

#### CA-09 — Company Details follows active Bill To
1. Set Bill To and Ship To to different active addresses.
2. Open Company Details, change Bill To, then refresh Company Details.

**Expected result:** Company Details shows the current Bill To address and its GSTIN. If that GSTIN is blank, it shows the legacy Company GSTIN. An inactive address is never shown as current.

#### CA-04 — Legacy backfill
1. Apply V32 to a database containing Companies with populated and missing legacy values.
2. Inspect `company_addresses` alongside `companies`.

**Expected result:** Every existing Company has a `Legacy Address` with active Bill To and Ship To defaults. Address line 1, GSTIN, and state match the Company’s legacy values, including nulls. Legacy Company columns are unchanged.

#### VPO-SAFE-01 — Update DRAFT PO
1. Submit an update for a DRAFT PO.

**Expected result:** Update succeeds without a status-reset operation.

### Negative validation scenarios

#### CA-05 — Required fields and inactive/default restrictions
1. Submit an address with blank nickname, address line 1, city, state, postal code, or country.
2. Attempt to make an inactive address a default.
3. Attempt to deactivate a default address before moving its role.
4. Attempt to deactivate a dual-default address after moving only one role.

**Expected result:** Form or service rejects the action, and the Company retains an active default for each role until both roles are explicitly reassigned.

#### VPO-SAFE-02 — Reject non-DRAFT PO states
1. Attempt to update SENT, APPROVED, REJECTED, and RECEIVED POs.

**Expected result:** Each update is rejected before persistence; status is unchanged.

### Authorization and tenant-isolation tests

#### CA-06 — Cross-company address operations
1. As Company A ADMIN, try to open, edit, set default, and deactivate an address ID belonging to Company B.

**Expected result:** Operations return a safe not-found/error response; Company B’s address remains unchanged.

#### CA-07 — Role protection
1. Open `/admin/company/addresses` as a USER or MANAGER.

**Expected result:** Access is denied by the `/admin/**` ADMIN security rule.

#### VPO-SAFE-03 — Cross-tenant PO update
1. Attempt to update a PO absent from the authenticated Company-scoped lookup.

**Expected result:** Service treats the PO as not found and does not save it.

## Regression tests
- [x] Existing Company profile edit/details and registration unit/workflow tests run.
- [x] New Company registration seeds active default Bill To and Ship To.
- [x] Vendor PO service/controller behavior remains covered.
- [ ] Manual browser workflows and responsive layout have not been tested.

## Database and migration verification
- V33 applied to configured PostgreSQL 18.4; current Flyway version 33, 1 Company, 2 addresses, no missing Bill To or Ship To defaults. Legacy comparison reports one address-line mismatch with current Company fields; GSTIN and state match.
- Automated isolated-schema V32/V33 migration test: `mvn '-DcompanyAddressMigrationTest=true' '-Dtest=CompanyAddressMigrationPostgresTest' test` — 1 passed.
- The migration test creates and drops a unique temporary schema; it does not alter the existing public schema.

## PDF, export, and email checks
PDF integration coverage verifies that saved snapshot values render and that PDF generation succeeds for legacy/empty item cases. Manual visual review is not run. Existing email tests remain applicable to send/status transitions.

## Defect recording guidance
Record scenario ID, environment/build, role and Company, exact steps, expected and actual result, evidence (screenshots/logs with sensitive data removed), severity, and reproducibility. Do not include credentials or secrets.

## Test execution checklist
- [x] Automated positive, negative, and tenant-scoped service cases executed.
- [x] MVC field validation, route authorization, create/edit/default/deactivate feedback executed.
- [x] PostgreSQL V32 migration and legacy data backfill verified.
- [x] Existing Company registration/profile and Phase 0.1 Vendor PO suites executed.
- [ ] Manual UI scenarios executed.
- [x] PDF and email automated checks executed where applicable.
- [x] Results below match actual execution.

## Test results

| Scenario / area | Status (Pass / Fail / Blocked) | Tester / date | Evidence or defect reference |
|---|---|---|---|
| Focused Company Address + Company + Vendor PO suites | Pass | Codex / 2026-11-13 | 69 tests, 0 failures/errors/skips |
| Focused address + Company Details suites | Pass | Codex / 2026-11-13 | 53 tests, 0 failures/errors/skips |
| Opt-in PostgreSQL V32/V33 migration test | Pass | Codex / 2026-11-13 | 1 test, isolated schema, backfill/default/column assertions passed |
| Full suite (`mvn test`) | Pass | Codex / 2026-11-13 | 351 tests, 0 failures/errors, 1 opt-in migration test skipped |
| Consolidated focused Vendor PO, PDF, address, admin suites | Pass | Codex / 2026-11-13 | 107 tests, 0 failures/errors/skips |
| Final full suite (`mvn test`) | Pass | Codex / 2026-11-13 | 363 tests, 0 failures/errors, 2 opt-in migration tests skipped |
| Isolated V34–V36 PostgreSQL migration test | Pass | Codex / 2026-11-13 | 1 test, legacy snapshot and decimal backfill assertions passed |
| PostgreSQL schema check | Pass | Codex / 2026-11-13 | Flyway V33, Drug License column present, 0 Companies missing either default; one existing address-line mismatch reported |
| Manual browser QA | Not run | Codex / 2026-11-13 | No interactive UI session used |

**Overall status:** Automated implementation checks passed; manual browser QA not run

**Blockers or notes:** Browser-based QA was not run. The configured PostgreSQL public schema remains V33; V34–V36 were checked on an isolated PostgreSQL schema. The configured Company legacy address-line mismatch is recorded above; GSTIN/state match.

## Consolidated Vendor PO phases 2–8

### Address snapshots and tenant boundaries
- Create a draft using default and explicitly selected active Bill To/Ship To addresses; verify complete address, state code, GSTIN, and Drug License Number are stored on the PO.
- Attempt cross-company and inactive address IDs; expect rejection without saving.
- Edit/deactivate/change defaults after creating a PO; verify saved snapshots and issued PDF remain unchanged. Issue a draft after its selected address becomes inactive; expect rejection.

### Commercial and financial values
- Set Vendor default payment terms, override them on a PO, and verify the Vendor default is unchanged.
- Select a saved delivery term and a custom term; set item delivery override and compare to the header date.
- Test intra-state CGST/SGST and inter-state IGST. Test included freight/packing (informational only) and excluded taxable/non-taxable values.
- Omit Included/Excluded, taxable selection, or taxable rate; expect validation failure. Verify decimals and amount in words retain paisa.

### Terms and revisions
- As Company ADMIN, use the Vendor Purchase Orders list Terms & Conditions control to create/edit Company-wide default terms; enter delivery terms directly on the PO form. Verify another Company cannot view or mutate tenant settings.
- Issue a PO and create an amendment. Verify the prior revision remains immutable and retains its PDF; the amendment is a new DRAFT with copied fields/items/taxes; deleting the amendment restores the prior current revision.

### PDF
- Render a draft and an issued revision. Verify Bill To/Ship To, GSTIN, Drug License, Vendor details, item HSN and single Unit column, persisted tax totals, freight/packing, payment/delivery terms, penalty, T&C and amount in words.
- Change Company/Vendor/address/default terms after issue; verify saved PDF bytes remain stable and contains no live values.

Manual browser QA remains unrun unless a dated execution is recorded below. V34–V36 passed on the isolated schema test; they must still be applied through Flyway to the target environment during deployment.
- Calculation rejects negative quantities, prices, GST rates, and freight/packing amounts before saving totals.

### Hibernate quantity mapping regression
- Validate `vendor_purchase_order_item.quantity` after V35 is PostgreSQL `numeric` and maps to a Java `BigDecimal` with scale 3.
- Submit fractional item quantities and verify calculation/PDF snapshots preserve them.
- Final check after correction: focused PO/PDF/settings suites 54 passed; `mvn test` 363 tests, 0 failures/errors, 2 opt-in migration tests skipped; isolated V34–V36 PostgreSQL migration 1 passed.


## Product table and PDF layout checks

### VPO-UI-01 — Labeled product entry table
1. Open the Vendor PO create form and add multiple products.
2. Confirm each field aligns under Product, HSN, Qty, Unit, Price per, Unit price, GST %, Amount, and Action.
3. Resize to a narrow viewport and confirm the table scrolls horizontally without losing headers.
4. Edit a draft PO and add a row; confirm saved rows and the new row share the same layout and values.

**Expected result:** Product rows remain aligned and labeled; Unit and Price per are visibly distinct; calculations and row actions continue to work.

### VPO-PDF-UI-01 — Unit and item delivery date
1. Generate a Vendor PO PDF with an item-specific delivery date.
2. Inspect the item table headings and product cell.

**Expected result:** The PDF contains a single Unit column and the item delivery date appears on the next line below the product name.

**Execution status:** Not run (2026-11-14); automated tests and manual browser/PDF inspection were not performed.


## VPO-TERMS-01 — Single default terms editor
1. Open the Vendor Purchase Orders list and open Terms & Conditions. Create or edit multiple default terms and save.
2. Open the PO create form and confirm it has no terms template selector or per-PO terms editor. Confirm the separate Delivery terms field remains.
3. Save a PO and render its PDF.
4. Confirm there is no separate Delivery Terms button/page beside the list-page Terms & Conditions control.

**Expected result:** Default terms are managed only from the Vendor Purchase Orders list control and are snapshotted into the PO/PDF. Delivery terms remain on the PO form, with no separate settings entry point.

**Execution status:** Not run (2026-11-14); automated tests and manual browser/PDF inspection were not performed.
