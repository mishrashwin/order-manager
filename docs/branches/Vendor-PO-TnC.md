# Branch: Vendor-PO-TnC

## Intent
Implement configurable Terms & Conditions page for Vendor PO PDF with company-level template storage, toggle control, and automatic inclusion in PDF generation.

## Detailed Change Summary

### Lazy-Loaded Order Owner Fix
- The Vendor PO list displays `po.orderOwner.username`. Entity graphs previously fetched company, vendor, and items but omitted `orderOwner`, so Thymeleaf could dereference an uninitialized Hibernate proxy after the repository session closed.
- Added `orderOwner` to all Vendor PO repository entity graphs (company list, paged list, search, status list, and company-scoped ID lookup). Tenant filtering remains unchanged.
- Added a backend rule: include associations needed after a repository call explicitly in an `@EntityGraph`, because a fetch graph may leave omitted relationships unavailable outside the persistence context.

### Company-Level Configuration
- Added `vendor_po_terms_conditions` (`TEXT`) to store a JSON array of term strings and `vendor_po_terms_conditions_enabled` (`BOOLEAN`, default `true`) to the `companies` table in migration V29.
- Added matching fields to `Company`. The settings are shared across the company's Vendor POs, rather than stored independently on each PO.
- `GET /vendor/pos/terms-conditions` uses the authenticated company ID from `SecurityContextHelper`, parses and returns `{ terms, enabled }`, and falls back to an empty terms list if stored JSON cannot be parsed.
- `POST /vendor/pos/terms-conditions` filters blank terms, serializes the remaining strings as JSON, and saves the enable toggle for the authenticated company. A missing toggle parameter safely becomes OFF; only the string `true` enables it.

### Modal and Browser Behavior
- Added a Terms & Conditions launch button and configuration modal to the Vendor PO list fragment. The modal supports loading saved terms, adding/removing rows, renumbering, toggling PDF inclusion, and saving by AJAX.
- The PO list fragment is inserted into the parent page with `innerHTML`, so scripts inside that fragment do not execute. Modal handlers therefore live in `vendors/list.html` and use delegated events that survive list reloads.
- The form sends the toggle explicitly, including OFF, and excludes blank term inputs from the request.
- Constrained the modal to `90vh`; the modal body is a bounded flex column and `#termsList` is the only scrolling region, capped at `45vh`. Header, toggle, Add Term, and footer actions remain visible. Term inputs can shrink to the available width to avoid horizontal overflow. The existing `modal-lg` width is unchanged.

### PDF Rendering and Parser Hardening
- `VendorPdfServiceImpl` loads the company's terms JSON, defaults a null toggle to enabled for legacy rows, and passes both values to the Thymeleaf PDF template.
- When enabled and the list is non-empty, the PDF template appends a separate Terms & Conditions page with a numbered list. When disabled or empty, that page is omitted. The existing Notes section remains separate.
- Escaped the static heading ampersand and used Thymeleaf's `and` operator in the condition so the template remains valid XML.
- Added a final rendered-markup normalization step before OpenHTMLtoPDF: it preserves predefined XML entities and numeric references, and escapes any remaining bare ampersands. This covers raw ampersands in rendered content that would otherwise make the XML parser fail.

### Automated Coverage and Recorded Verification
- Added controller coverage for saving enabled terms, defaulting a missing toggle to OFF, and preserving an explicit OFF value; each case reloads the settings and checks persistence.
- Updated the Vendor PO service test's constructor mocks and PDF delegation assertion to match its current dependencies.
- Added test case IDs VPO-TNC-01 through VPO-TNC-03 to the controller/service test inventory.
- The test progress log records focused controller tests passing (3/3) and the existing unrelated suite issues: 8 stale failures in the full controller test class, stale repository mock errors in VendorPoServiceTest, and database URL configuration errors in the PDF integration context.
- Separately checked the modal's layout in headless Edge at 5, 12, and 35 terms. At all counts the modal remained within 90vh, the header/footer stayed visible, and there was no horizontal overflow; the list overflowed vertically at 12 and 35 terms and remained unscrollable at 5.

## Work Done
- [2026-10-28] Constrained the Terms & Conditions modal to 90vh and made only the terms list scroll, keeping its controls and actions visible for long lists
- [2026-10-28] Escaped the ampersand in the PDF Terms & Conditions heading so the generated XHTML remains XML-well-formed for OpenHTMLtoPDF
- [2026-10-28] Replaced raw `&&` in the Thymeleaf `th:if` attribute with Thymeleaf's `and` operator; raw ampersands in XML attribute values break parsing before expression evaluation
- [2026-10-28] Added a final rendered-XHTML normalization step that preserves valid XML entities and escapes any remaining bare ampersands before OpenHTMLtoPDF parses the document
- [2026-10-28] Fixed Terms & Conditions toggle submission so OFF is posted explicitly and a missing parameter safely defaults to OFF
- [2026-10-28] Moved modal event handling to the parent page because the PO list fragment is inserted with innerHTML and its scripts do not execute
- [2026-10-28] Added delegated Add/Remove/renumber handlers and backend controller tests for enabled and missing-toggle saves
- [2026-10-28] Added database migration V29__add_vendor_po_terms_conditions.sql to add vendor_po_terms_conditions and vendor_po_terms_conditions_enabled columns to companies table
- [2026-10-28] Updated Company entity with vendorPoTermsConditions (TEXT, JSON array) and vendorPoTermsConditionsEnabled (Boolean, default true) fields
- [2026-10-28] Added GET /vendor/pos/terms-conditions endpoint to load company-level Terms & Conditions as JSON
- [2026-10-28] Added POST /vendor/pos/terms-conditions endpoint to save company-level Terms & Conditions from form data
- [2026-10-28] Added Terms & Conditions button and toggle to Vendor PO list page (pos-list-ajax.html)
- [2026-10-28] Created modal for editing Terms & Conditions with dynamic add/remove functionality
- [2026-10-28] Updated VendorPdfServiceImpl to parse company-level Terms & Conditions and pass to PDF template
- [2026-10-28] Updated vendor_po.html PDF template to render Terms & Conditions as separate page when toggle is ON
- [2026-10-28] Added CSS styling for Terms & Conditions page with justified text alignment and page break

## Key Decisions
- **Storage Level**: Company-level (not per PO) - Terms & Conditions are configured once per company and apply to all Vendor POs
- **Data Format**: JSON array stored in TEXT column for flexibility and easy parsing
- **Default State**: Toggle defaults to ON (true) for new companies
- **PDF Rendering**: Terms & Conditions appear as a separate new page at the end of the PDF using page-break-before CSS
- **UI Pattern**: Modal with dynamic add/remove inputs, numbered list display, AJAX-based save/load
- **Tenant Isolation**: Terms & Conditions are company-scoped via SecurityContextHelper, following existing multi-tenant patterns

## Technical Details
- **Database Migration**: PostgreSQL-compatible ALTER TABLE with COMMENT ON COLUMN for documentation
- **Entity Fields**: 
  - `vendorPoTermsConditions`: TEXT column storing JSON array of strings
  - `vendorPoTermsConditionsEnabled`: Boolean column with default true
- **Controller Endpoints**:
  - GET returns JSON: `{ "terms": [...], "enabled": true/false }`
  - POST accepts form data with terms array and enabled toggle
- **PDF Template**: Uses Thymeleaf conditionals to only render Terms & Conditions page when enabled and non-empty
- **CSS**: `.tnc-page` class with `page-break-before: always` for separate page rendering

## Files Modified
1. `src/main/resources/db/migration/V29__add_vendor_po_terms_conditions.sql` - New company settings columns
2. `src/main/java/com/example/ordermanager/company/entity/Company.java` - Added settings fields
3. `src/main/java/com/example/ordermanager/vendor/controller/VendorPoController.java` - Added tenant-scoped GET/POST settings endpoints and persistence
4. `src/main/java/com/example/ordermanager/vendor/service/impl/VendorPdfServiceImpl.java` - Parse settings, pass them to the template, normalize rendered XML ampersands
5. `src/main/resources/templates/vendor/pos-list-ajax.html` - Added launch button and modal markup
6. `src/main/resources/templates/vendors/list.html` - Added delegated modal handlers and AJAX load/save behavior
7. `src/main/resources/static/css/style.css` - Added scoped modal sizing and list-only scrolling rules
8. `src/main/resources/templates/vendor/pdf/vendor_po.html` - Added the conditional standalone terms page and kept notes separate
9. `src/test/java/com/example/ordermanager/vendor/controller/VendorPoControllerTest.java` - Added settings save/load edge and happy-path coverage
10. `src/test/java/com/example/ordermanager/vendor/service/VendorPoServiceTest.java` - Updated constructor mocks and PDF delegation coverage
11. `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` - Added VPO-TNC test cases
12. `docs/testing/TEST_PROGRESS.md` - Recorded test outcomes and environment limitations

## Outcomes
- ✅ Terms modal remains at or below 90vh with 5, 12, and 35 terms; only the terms list overflows, and header/footer remain visible without horizontal overflow
- ✅ Vendor PO PDFs with Terms & Conditions enabled no longer fail XML parsing because of the heading ampersand
- ✅ The Terms & Conditions visibility condition is valid XML and can be evaluated by Thymeleaf
- ✅ Bare ampersands in rendered PDF content are normalized before the XML parser runs
- ✅ Terms & Conditions modal works after AJAX list loading and reloads; Add/Remove keeps numbering correct
- ✅ Toggle state is explicitly submitted and missing values cannot enable it
- ✅ Company-level Terms & Conditions configuration implemented
- ✅ Toggle control for PDF inclusion/exclusion
- ✅ Modal UI for add/edit/delete terms with numbered list
- ✅ PDF generation includes Terms & Conditions as separate page when toggle is ON
- ✅ Justified text alignment for terms in PDF
- ✅ Existing PO pages, totals, notes, signatures, and footer unchanged
- ✅ Tenant isolation maintained via company-scoped storage
- ✅ Default state is ON for new companies
- ✅ Existing records handled safely (NULL values treated as empty/default)

## Verification
- Vendor PO list rendering now fetches the assigned order owner through its entity graph, matching the `po.orderOwner.username` view access.
- Fetch-graph rule added to `ARCHITECTURE_BACKEND_RULES.md`: include associations consumed after the query in the explicit entity graph.
- Code follows existing architecture patterns (Controller → Service → Repository)
- Multi-tenant isolation enforced via SecurityContextHelper
- JSON serialization/deserialization handled with Jackson ObjectMapper
- PDF page break implemented via CSS page-break-before
- XSS protection via Thymeleaf #strings.escapeXml
- CSRF token included in modal form
- Modal layout checked in headless Edge at 5, 12, and 35 terms; list-only vertical overflow verified, modal cap respected, header/footer visible, and horizontal overflow absent
