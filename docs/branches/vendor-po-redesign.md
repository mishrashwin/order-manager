# Branch: `vendor-po-redesign`

## Branch and objective
- **Branch name:** `vendor-po-redesign`
- **Feature / development objective:** Vendor PO roadmap Phases 0.1–8, including Company Address lifecycle, commercial snapshots, calculations, revisions, and PDF output.
- **Status / dates:** Implementation and automated verification passed; manual browser QA remains unrun; 2026-11-13.

## Address correction scope (2026-11-13)
- Add explicit address activation while retaining inactive rows and preserving all address data.
- Require explicit Bill To and/or Ship To reassignment before a default address can be deactivated; retain active defaults without automatic replacement.
- Render Company Details from the current active Bill To address, with its GSTIN and Company GSTIN compatibility fallback.
- Add optional Drug License Number through persistence, forms, validation, management display, and PO PDF snapshots.
- Documentation updates include README, backend/domain/architecture/testing rules, test cases/progress, and this paired branch guide.

## Consolidated remaining-phase plan (2026-11-13)
- **Requested scope:** implement roadmap Phases 2–8, continuing from the verified Phase 1/address-correction tree. The supplied consolidated prompt requests multiple phases in one work session; this supersedes the implementation brief’s default one-phase-at-a-time instruction.
- **Source reconciliation:** the old audit describes a pre-Phase-1 state. The working tree has CompanyAddress, V32/V33, the Phase 0.1 guard, and address correction behavior. The branch testing guide said browser QA was not performed, so the consolidated prompt's assertion that it was complete was unverified.
- **Buyer GSTIN:** use selected Bill To GSTIN when present; otherwise use the Company GSTIN. This follows the consolidated prompt's proposed compatibility rule and the Company Details rule already implemented.
- **Payment terms:** free text, copied from Vendor default on new PO and persisted on the PO/revision; a PO override never writes back to Vendor.
- **Delivery terms:** Company-scoped active option records plus custom free text stored on the PO.
- **Freight/packing tax:** require explicit include/exclude and taxable/rate selections. Included charges are informational and are not added twice; excluded charges add to taxable or non-taxable totals as explicitly selected. Missing GST states follow the existing PDF rule (inter-state).
- **Revision model:** one immutable Vendor PO row per issued revision; a revision action clones the latest immutable row as a new DRAFT row, increments revision number, and leaves the prior row/PDF unchanged. Issuing is DRAFT → SENT through the existing send/status conventions; other existing statuses remain in use.
- **Legacy snapshot policy:** V34 stores the best available migration-time Company default address and live Vendor values for old POs and marks those rows as legacy backfills. Those values cannot be represented as the original historical issue-time data.
- **Dates:** retain LocalDate PO/delivery dates, initializing PO date from current local date; revision audit timestamps use OffsetDateTime.
- **Documentation:** README, branch pair/index, backend/domain/UI/testing rules, testing case matrix/progress, and architecture overview were updated.

## Problem statement
Vendor POs could be mutated after issuance and reset to DRAFT. Company profiles supported only a single free-text address, with no tenant-managed multiple locations or default Bill To/Ship To roles.

## Phase 0 audit reconciliation
- The supplied audit ZIP lacked build files, while the current checkout contains `pom.xml`; Maven confirms Java 17 and Spring Boot 3.3.0.
- Audit migration baseline V29 differs from the checkout’s V31.
- The reported PO status set and unsafe update path were confirmed; Phase 0.1 now guards it.
- Existing Company state is `IndianState`, whose GST state code is derived from its enum value.

## Scope
- **Included:** DRAFT-only PO update protection, Company Address lifecycle, PO address snapshots, commercial fields, explicit GST/charge calculations, Company-wide default terms, delivery options, revisions, immutable issued PDFs, tenant checks, and their migrations/tests/docs.
- **Excluded:** Vendor PAN capture because Vendor has no PAN source field; Input Size, Customization, Minimum Packing Qty, Approved Manufacturer, and Indent/PR.

## Phase 1 design decisions
- Company Address is a Company-owned entity with nickname, address lines, city, `IndianState`, derived GST state code, postal code, country, independent address GSTIN, active state, and two role flags.
- V32 copies every existing Company’s legacy address/GSTIN/state into an active `Legacy Address` with both defaults. Legacy columns remain unchanged.
- New Company registration creates a matching dual-default placeholder. Incomplete legacy/registration placeholders can be completed in the address form.
- Partial unique indexes guarantee at most one active default per role; migration and service workflows ensure at least one. Default changes and active-state changes lock the Company row. A default address must be reassigned before deactivation.
- PO Buyer GSTIN uses the selected Bill To address GSTIN and falls back to Company GSTIN when the address field is blank.
- Activation preserves address data and does not assign defaults. Every role on a default address must be explicitly moved before deactivation.
- Company Details reads the active Bill To address through a tenant-scoped query; its GSTIN is shown with the legacy Company GSTIN as a display fallback.
- Optional Drug License Number is snapshotted and displayed in PO PDFs for Bill To/Ship To.
- A single address can carry both defaults. Duplicate GSTIN values are allowed.

## Features implemented
- [x] Non-DRAFT Vendor POs cannot be edited; controller no longer resets status.
- [x] Company admins can create/edit addresses and set default Bill To/Ship To locations.
- [x] Address records are tenant-scoped; default and deactivation changes are serialized.
- [x] Addresses can be activated/deactivated without losing data; inactive rows remain visible.
- [x] All default roles must be explicitly reassigned before deactivation.
- [x] Company Details reflects the active Bill To address and GSTIN.
- [x] Optional Drug License Number is validated, persisted, editable, and displayed.
- [x] New and existing Companies receive active dual-default address rows.

## Technical implementation summary
`CompanyAddress` is related to `Company`; `CompanyAddressRepository` exposes Company-scoped lookups and role-clearing updates. `CompanyAddressService` derives tenant context from `SecurityContextHelper`, validates forms, locks the Company row, and enforces active/default invariants. `CompanyAddressController` serves ADMIN-only routes under `/admin/company/addresses`. Company registration seeds the initial address. `spring-boot-starter-validation` was added because the project had the validation API but no provider, so `@Valid` constraints were not executed.

## Database and entity changes
- Added `company_addresses` with Company FK, address fields, active/default flags, and timestamps.
- Added additive V33 nullable `drug_license_number` column; V32 remains unchanged.
- Added `Company.addresses` lazy relationship.
- Added company/active index and PostgreSQL partial unique indexes for active default roles.
- No Company Address hard-delete operation is exposed.

## API, controller, and service changes
- ADMIN MVC endpoints: list, new form, create, edit form, update, set default Bill To, set default Ship To, activate, and deactivate.
- No Company ID or ownership/default flags are accepted from forms.
- Address lookups and mutations use the authenticated Company ID at service/repository boundaries.
- Existing Company profile management and legacy columns remain available.

## UI, template, and static asset changes
- Added Company Address list and address form templates.
- Added “Manage Addresses” link to Company Details.
- List shows nickname, address summary, GSTIN, Drug License Number, state code, active state, and default roles; role/lifecycle actions provide feedback and CSRF protection.
- Company Details shows the active default Bill To address and displays its GSTIN, falling back to the Company GSTIN when the address GSTIN is empty.

## Business rules and validations
- Nickname, address line 1, city, state, postal code, and country are required for form submissions.
- Address GSTIN is optional and may be duplicated.
- Drug License Number is optional and limited to 100 characters.
- Inactive addresses cannot be made defaults or edited. Activation preserves all fields and leaves defaults unchanged.
- Active defaults cannot be deactivated until each held role has been explicitly reassigned to another active address.
- Exactly one active default Bill To and Ship To is maintained per Company.

## Security and tenant isolation
`/admin/**` remains ADMIN-protected by `SecurityConfig`. Services derive Company ownership from `SecurityContextHelper`; address ID operations use `findByIdAndCompany_Id`. Default/create/deactivate methods lock the Company row before changing state.

## Configuration changes
Added `spring-boot-starter-validation` to activate Bean Validation at MVC and service boundaries. No environment variables or profiles changed.

## Migration details
V32/V33 remain unchanged. Additive V34 adds address/vendor/commercial snapshots, Company-scoped terms options, and revision metadata; V35 converts PO/item money columns to decimal and backfills legacy financial values; V36 persists calculated taxable unit rates. The isolated V34–V36 migration test passed against a disposable PostgreSQL schema. The configured application database remains at V33; new migrations were not applied to its public schema.

## Dependencies and prerequisites
Java 17, Maven, and PostgreSQL for migration execution. The standard full test suite does not require PostgreSQL; run the opt-in migration test with `-DcompanyAddressMigrationTest=true` and configured DB connection variables.

## Known limitations
- Legacy and registration placeholder addresses can be incomplete until an ADMIN edits them.
- Phase 2 Buyer GSTIN precedence is selected Bill To address GSTIN with Company GSTIN fallback; PO snapshots consume Company Address records.
- Browser/manual QA was not performed.

## Future enhancements
Run manual browser QA for the PO settings, address selection, revision history, and rendered PDF workflows.

## Related commits and PRs
Not available yet.

## Documentation updates
- [x] README updated for Company Address lifecycle actions, Drug License Number, Company Details display, and the Phase 0.1 PO rule.
- [x] Backend, multi-tenant, architecture overview, testing rules, test cases, and test progress updated.
- [x] Branch index and paired testing guide updated.

## Decisions and outcomes
- **Phase 0.1:** Reject all non-DRAFT PO edits until revision workflow exists.
- **Phase 1:** Keep Company GSTIN and legacy address fields for compatibility; store address GSTIN independently.
- **Focused tests:** 69 passed, 0 failures/errors/skips.
- **PostgreSQL migration test:** 1 passed using an isolated temporary schema.
- **Full suite:** `mvn test` passed, 344 tests, 0 failures/errors, 1 opt-in migration test skipped.
- **Schema validation:** Application started against PostgreSQL V32 with Hibernate validation enabled.

### Address correction outcomes
- **Focused address + Company Details suites:** 53 passed, 0 failures/errors/skips.
- **V32/V33 isolated PostgreSQL migration test:** 1 passed; configured PostgreSQL schema is at V33 and contains the nullable license column.
- **Full suite:** `mvn test` passed, 351 tests, 0 failures/errors, 1 opt-in migration test skipped.
- **Manual browser QA:** Not run; configured database legacy address-line mismatch is documented above.

## Consolidated Phases 2–8 implementation (2026-11-13)
- V34 adds PO snapshots, commercial fields, Company-scoped template/delivery tables, and revision records with `LEGACY_BACKFILL` values; V35 converts PO and line money to decimal columns; V36 persists calculated taxable unit rates for PDF output. Prior V32/V33 remain unchanged.
- New drafts snapshot tenant-owned active Bill To/Ship To, Vendor, logo/signature, terms and tax details. Buyer GSTIN uses selected Bill To GSTIN, falling back to Company GSTIN. Tax state comparison uses Bill To state code and Vendor state code; missing codes use IGST.
- Freight/Packing require explicit Included/Excluded and Taxable/Non-taxable selections, plus a rate when taxable. Included amount is shown for information only; excluded charges are added once to pre-tax totals.
- The Vendor Purchase Orders list manages Company-wide default terms; delivery terms are handled in the PO form. Vendor default payment terms can be overridden per PO. PO revisions preserve prior rows and issued PDF bytes; a new amendment is a DRAFT revision, and deleting it restores the prior current revision.
- The PDF uses the persisted PO/revision fields and totals. PAN capture was not added because Vendor has no PAN source field. Input Size, Customization, Minimum Packing Qty, Approved Manufacturer, and Indent/PR remain excluded.
- Browser QA was not run during this work session. The older branch record identified browser QA as unrun; the consolidated prompt's claim of completion had no supporting record.
- Final automated test, migration, and diff results are recorded below and in the paired testing guide.
- `mvn test`: passed; 363 tests, 0 failures/errors, 2 opt-in migration tests skipped.
- Focused Phase 2–8, address, PDF, and service/controller suites: 107 tests passed, 0 failures/errors/skips.
- Opt-in `VendorPoMigrationPostgresTest`: 1 passed on a temporary schema applying V34–V36. Configured public schema remains at V33.
- `git diff --check`: passed with Git line-ending conversion warnings only.

## Runtime schema-validation correction (2026-11-13)
- Resolved Hibernate's V35 schema mismatch: the migration stores `vendor_purchase_order_item.quantity` as `NUMERIC(12,3)`, so `VendorPoItem.quantity` and the form binding now use `BigDecimal` at scale 3. Legacy Java callers retain a documented Double conversion setter.
- The isolated V34–V36 PostgreSQL migration test now asserts PostgreSQL reports the quantity column as `numeric`; PO focused tests and full Maven suite pass after the mapping correction.


## Product table and PDF presentation (2026-11-14)
- **Objective:** Make product entry scannable and align Vendor PO PDF item details with the single-Unit-column layout.
- **Implementation:** `vendor/pos-form-new.html` now renders labeled table columns for product, HSN, quantity, Unit, pricing basis, unit price, GST, amount, and row action. Server-rendered edit rows, the empty create row, and JavaScript-created rows share the same tabular structure. Product delivery date is collected below the product selector.
- **PDF:** `vendor/pdf/vendor_po.html` now has one Unit column; the delivery date is explicitly separated from the product name with a line break. Pricing-basis data/calculation remains available but is not repeated as a PDF column.
- **Styles/docs:** Added responsive horizontal scrolling and header nowrap styling scoped to the product table. Updated UI design rules, test cases, progress log, and this branch record. README capability/setup documentation does not apply to this presentation-only refinement.
- **Database/API/security:** None.
- **Validation:** Automated tests and browser/PDF visual verification were not run.


## Vendor PO terms editor consolidation (2026-11-14)
- **Problem:** Company-wide default Terms & Conditions were editable from the PO list while a second reusable-template selector/editor appeared on PO create/edit forms and in settings.
- **Resolution:** The PO list Terms & Conditions modal is the single visible place to create/edit default PO terms. Removed per-PO term-template editing from create/edit forms and removed term-template CRUD from the settings page. Delivery terms remain on the PO form; the separate management entry point has been removed. New and edited POs use the Company-wide default and save a terms snapshot for PDF rendering.
- **Backend/data changes:** None; existing default Company terms storage and existing PO snapshots remain in use. Legacy template rows and snapshot fields are retained for compatibility.
- **Documentation:** UI rules, testing case matrix/progress, and paired branch testing guide updated. README capability/setup update is not applicable.
- **Validation:** Automated tests and manual browser/PDF verification were not run.

## Remove separate Delivery Terms entry point (2026-11-14)
- Removed the extra Delivery Terms button beside the list-page Terms & Conditions control and removed the standalone delivery-term settings page/routes.
- Kept the delivery terms field in the PO create/edit form, including its custom text input and existing tenant-scoped option resolution.
- Updated UI and QA documentation. No database or migration changes.
- Automated tests and browser verification were not run.
