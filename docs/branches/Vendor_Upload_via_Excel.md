# Branch: Vendor_Upload_via_Excel

## Intent
Implement Excel-based bulk vendor registration incrementally, preserving existing tenant isolation and vendor registration rules.

## Work Done
- [2026-11-13] Restored the Admin Dashboard tile layout and moved upload audit history/reconciliation to a dedicated admin-only page while reusing the existing tenant-scoped APIs and interactions.
- [2026-11-01] Phase 1: reviewed existing vendor registration, validation, normalization, and company-scoped persistence to establish the bulk-upload integration points.
- [2026-11-01] Phase 2: added the Apache POI Excel template with vendor fields, instructions, sample row, and Indian-state dropdown.
- [2026-11-01] Phase 3: added the template download endpoint and vendor-list download control, with controller tests.
- [2026-11-01] Phase 4: added the XLSX upload modal, client-side type/size checks, and multipart upload endpoint with server-side empty-file, type, and 5 MB checks. Preserved CSRF protection and tested the upload flow.
- [2026-11-01] Phase 5: added a row DTO and Apache POI parsing for populated rows in the Vendor Data worksheet; parsing returns values only and does not persist vendors.
- [2026-11-01] Phase 6A: added required-field, email, phone, GSTIN-format, and IndianState validation with row numbers and multiple field errors; upload JSON now includes validation results and counts.
- [2026-11-01] Phase 6B: added normalized in-file and company-scoped existing-vendor duplicate detection for GSTIN, email, and phone; results preserve Phase 6A errors and row counts.
- [2026-11-01] Phase 6B correction: every row in an uploaded-file duplicate group, including the first occurrence, is now invalid and reports all participating Excel row numbers.
- [2026-11-01] Phase 7: added an AJAX validation preview in the upload modal with counts, responsive row table, expandable errors, loading and empty states, and replacement behavior.
- [2026-11-01] Preview layout refinement: bounded the modal to viewport dimensions, fixed the modal controls, and added a two-axis scrolling table with sticky headers and readable column widths.
- [2026-11-01] Preview sizing refinement: aligned upload controls and compact stacked summary cards in a desktop 75:25 layout; modal height follows rendered content up to the 85vh cap.
- [2026-11-01] Tenant-isolation verification: added regression coverage for ignoring a client-supplied company ID, assigning the authenticated Company through the bulk persistence path, and avoiding cross-company duplicate matches.
- [2026-11-01] Production hardening phase 1: aligned multipart and controller file-size limits, validated OOXML content and Vendor Data headers, capped populated rows, ignored blank rows, rejected formulas and unsupported cell types, and returned controlled workbook errors.
- [2026-11-01] Production hardening phase 2: normalized imported vendor DTOs before validation and duplicate checks so preview and registration revalidation share casing, identifier, phone, state-code, and address normalization.
- [2026-11-01] Phase 8: added explicit registration that reparses and revalidates the uploaded workbook, delegates valid rows to `VendorService.saveVendorWithCompany()`, and reports per-row outcomes.
- [2026-11-01] Registration results layout: keeps every row in an independently scrollable table and labels invalid skips distinctly.
- [2026-11-01] Registration results correction: sized the results scroller to rendered rows and footer space, collapsed validation preview after registration, and changed totals to mutually exclusive outcomes.

## Key Decisions
- Use Apache POI `DataFormatter` without formula evaluation; reject formulas and unsupported cell types. Preserve number formats for numeric phone cells and avoid scientific notation.
- Require the `Vendor Data` sheet and its seven ordered template headers, ignore blank rows, reject files with no vendor rows, and enforce the configurable populated-row ceiling.
- Reuse `PhoneNumberUtils` and `Helper.toTitleCase`; resolve states through `IndianState.fromDisplayName` and `IndianState.fromCode`. Normalize one DTO representation for preview and registration validation; validation does not persist vendors.
- Duplicate queries are company-filtered and batched by identifier, with empty identifier sets skipped; duplicate rows remain present in the validation response.
- File duplicate groups are computed before applying errors so first and later occurrences receive consistent field errors.
- Dynamic preview values are rendered via DOM text nodes; upload CSRF header and same-origin credentials are preserved.
- Registration has no enclosing batch transaction; each existing vendor repository save runs independently so an individual failed row does not prevent later rows from being attempted.
- Recoverable business-validation and database-integrity exceptions are recorded per row after the repository transaction rolls back; unexpected runtime persistence failures abort the response instead of being mislabeled as recoverable row failures.
- Vendor identifiers have no company-scoped unique database constraints in the current migrations. Pre-insert duplicate checks remain tenant-scoped, but a concurrent duplicate race cannot be prevented until existing data is audited and the necessary constraints are safely introduced.
- Production hardening phase 4: added company/key and company/fingerprint unique claims bound to uploaded workbook SHA-256. Repeated workbook content is replayed even with a new key; a key used with changed content conflicts. Completed and failed responses replay, while in-progress claims return conflict and are never auto-expired or reclaimed.
- Idempotency recovery policy: a process crash can leave an IN_PROGRESS claim after independently committed vendor rows. Retrying that key is blocked for manual reconciliation; this prevents automatic duplicate creation but does not claim exactly-once recovery or reconstruct partial row outcomes.
- Keys are retained without automatic expiry; terminal record cleanup must be a deliberate retention operation, and unresolved IN_PROGRESS records must never be deleted automatically. FAILED records are terminal and are replayed; a new workbook fingerprint is required to start a new attempt.
- Production hardening phase 5: persisted upload metadata, minimal row manifests, per-vendor request/row correlation, report generation from stored outcomes, tenant-scoped result/report retrieval, recent registration history, and admin-only reconciliation. Correlation-based reconciliation never reruns registration and only completes when every planned valid row has a persisted request/row match.
- Admin UI refinement: moved Recent Vendor Uploads and Unresolved Uploads into the Admin Dashboard's Vendor Management tab, and placed its dashboard card after User Management and before Order Statistics. Stored results now load only after the modal is shown, preventing blank details dialogs caused by measuring a hidden modal.
- Admin UI correction: removed tab navigation and restored all six dashboard tiles directly under the heading. The Vendor Management tile links to `/vendors/bulk-upload/management`, which is admin-restricted and contains only upload history and unresolved reconciliation.
- Retention remains indefinite: no cleanup job is configured, preserving replay and audit history; unresolved requests are never removed. `RECONCILIATION_REQUIRED` entries remain visible until safe reconciliation or manual investigation.
- Added focused template coverage for the direct tile navigation, standalone page scope, auto-loaded history/reconciliation, details modal, and CSRF metadata, plus a controller view mapping test.
- Verification: `mvn "-Dtest=VendorBulkUploadPreviewTemplateTest,VendorControllerTest" test` passed (29 tests, 0 failures/errors); `mvn -DskipTests compile` passed.
- Follow-up UI refinement: added the Admin Dashboard return action to Vendor Management and switched the audit sections to unified cards/tables with responsive page-scoped styling.
- Verification: `mvn "-Dtest=VendorBulkUploadPreviewTemplateTest" test` passed (8 tests, 0 failures/errors).
- Fixed Vendor Management result text mojibake by using JavaScript Unicode escapes and static HTML entities; added regression assertions for the displayed summary separators and missing-value marker. Focused template tests passed (8 tests, 0 failures/errors).
- Removed the existing explicit skips from the Vendor PO controller and PDF integration tests. Fixed the mocked saved-PO fixture and PDF rendering for a null items collection, and configured required test-only mail/owner properties.
- Verification: all 313 tests pass with 0 failures, errors, or skips after `mvn test`.

## Outcomes
- Phase 5 parser and Phase 6 validation return populated row results as JSON; Phase 8 registers only server-revalidated valid rows and returns per-row outcomes.
- Focused tests and compilation results recorded in `docs/testing/TEST_PROGRESS.md`.
- Production hardening phase 3 verifies independent row commits and a failed row rollback against H2 with a company-scoped test uniqueness constraint; current schema-level concurrency enforcement remains an open limitation.
- Production hardening phase 4 adds persistent database claims, conflict/replay semantics, a migration syntax check in PostgreSQL-mode H2, concurrent-claim integration coverage, and client key reuse across uncertain retries.
- Production hardening phase 5 adds V31 audit/correlation schema, complete and errors-only XLSX reports, replayable result retrieval, audit summaries, and fenced admin reconciliation; PostgreSQL V30/V31 syntax was executed in a rolled-back transaction. Focused tests passed; full-suite failures are recorded in `docs/testing/TEST_PROGRESS.md`.
