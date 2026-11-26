# Branch: `vendor-po-redesign`

## Branch and objective
- **Branch name:** `vendor-po-redesign`
- **Feature / development objective:** Improve Vendor PO PDF monetary/quantity presentation, dynamic columns, header/value clarity, preserve selected line Units, and handle financial persistence boundaries safely.
- **Status / dates:** Initial formatting work completed 2026-11-19; quantity, overflow, and selected Unit follow-ups 2026-11-23 (automated validation not run in this session).

## Problem statement
Vendor PO PDF financial columns use a western grouping formatter and constrained widths, making larger values hard to read and potentially causing adjacent columns to collide.

## Scope
- **Included:** Centralized BigDecimal Indian grouping for PDF money, item and summary layout, Indian amount-in-words correctness, focused PDF/formatter tests, and documentation.
- **Included:** Validate Vendor PO financial values against persisted numeric precision before saving; show safe actionable feedback if persistence still rejects a value.
- **Included:** Preserve the PO line Unit selected by the user across product resolution, persistence, draft reopening, and PDF rendering.
- **Excluded:** Changes to GST business formulas, persisted snapshot semantics, schema changes, and unrelated Vendor PO behavior.

## Features implemented
- [x] Centralized BigDecimal Indian monetary formatting with two decimals for item and summary money.
- [x] Reserved explicit PDF table widths for financial columns, right-aligned numeric cells, wrapped product descriptions, and separated financial summary rows.
- [x] Sized item columns from rendered content lengths while preserving a minimum Product width and visible separators between HSN, Qty, Unit, and financial columns.
- [x] Preserved Indian lakh/crore amount-in-words conversion and added negative amount support.
- [x] Rendered PO quantities without insignificant trailing zeros and placed Unit immediately after Qty in the PDF item table.
- [x] Labeled the row number `Sr No.`, centered all item headers, grouped quantity values using Indian separators, and added `%` to displayed GST percentages.
- [x] Added an inline, non-wrapping `₹ amount` presentation to subtotal, freight, packing, applicable GST summary rows, and total amounts; amount-in-words begins with `Indian Rupees`.
- [x] Reject financial values that exceed database numeric limits before persistence and show a safe PO form error for residual constraint failures.
- [x] Preserve explicitly selected Vendor PO line Units; use Product Unit metadata only as a default, and prefill the Unit select when a Product is changed.

## Technical implementation summary
`IndianMoneyFormatter` formats monetary display values and `IndianQuantityFormatter` applies Indian digit grouping without insignificant quantity zeros while retaining meaningful fractional digits. `VendorPdfServiceImpl` passes formatted item and persisted PO snapshot totals to the existing Thymeleaf PDF template. `NumberToWordsUtil` continues to consume the saved BigDecimal total. GST stays in `VendorPoCalculationService`.

## Database and entity changes
None.

## API, controller, and service changes
`VendorPdfServiceImpl` uses the shared formatter for item rate/GST/amount, subtotal, freight, packing, CGST, SGST, IGST, and Grand Total. `VendorPoServiceImpl.resolveItemProduct` now fills Unit from Product only when the PO item Unit is blank, preserving the selected line Unit. The edit/create form's product-change handler targets the Unit select. `VendorPoCalculationService` checks item inputs, calculated item amounts, and PO totals against matching database precision limits before save. Create/edit controller handlers translate expected database integrity failures into a form message; `GlobalExceptionHandler` provides a redirect fallback for PO create/update requests and never reflects raw exception text or request paths.

## UI, template, and static asset changes
The Vendor PO form retains explicit line Unit selections through editing, and the PDF renders the saved line Unit. `VendorPoPdfColumnWidths` sizes columns from each column's longest rendered value while preserving minimum widths and reserving at least 16% for Product. The PDF labels its row number `Sr No.`, center-aligns all item headers, orders Qty before Unit, groups quantities, includes `%` in GST values, separates all adjacent columns, keeps numeric cells right-aligned, wraps long product names, and keeps the `₹` symbol and summary amount together in a non-wrapping right-aligned cell using a rupee-capable font fallback. Amount-in-words identifies the currency as Indian Rupees.

## Business rules and validations
Preserve persisted financial values, GST inclusion/exclusion rules, and snapshot semantics. Formatting is display-only. Financial inputs and calculated amounts must fit persisted numeric columns; invalid values are rejected before persistence with item context.

## Security and tenant isolation
No change; PDF continues to render the PO passed through existing company-scoped service flows.

## Configuration changes
None. `application-prod.properties` inspected; no PDF-specific configuration applies.

## Migration details
None.

## Dependencies and prerequisites
Existing Java 17, Thymeleaf, OpenHTMLToPDF, Maven.

## Known limitations
The automated PDF sample was visually inspected as a PNG. No GUI/browser QA or full application test suite was run.
Previously saved PO rows may already contain the Product Unit that overwrote the user's selection; that lost selection cannot be inferred. Correct affected DRAFT rows manually and save, or create a revision for an issued PO.

## Future enhancements
None identified.

## Related commits and PRs
Not available yet.

## Documentation updates
- [x] README update not applicable: the changes refine Vendor PO PDF presentation and validation feedback without changing project setup, configuration, or feature discovery.
- [x] PDF/UI rule, test matrix, progress log, and paired testing guide update.
- [x] Paired testing guide: [`vendor-po-redesign-TESTING.md`](vendor-po-redesign-TESTING.md).

## Decisions and outcomes
- **Decision:** Use one BigDecimal-based display formatter; keep GST calculations inside `VendorPoCalculationService`.
- **Decision:** Keep the persisted quantity scale unchanged for compatibility; remove insignificant zeros only in PDF presentation and show the Unit directly after Qty.
- **Outcome:** Formatter and PDF tests passed; the sample fixture uses 18% GST with matching ₹180,000,000 tax on a ₹1,000,000,000 taxable base. Rendered page inspected with separated numeric columns and no clipping. Sample PDF and PNG are in `output/pdf/`.
- **Fixture correction:** After review identified a zero-rate sample, set the test item rate to 18% and asserted the rendered GST%/GST Amt pair. The corrected sample was regenerated and visually confirmed; its focused PDF test passed.
- **Follow-up outcome:** Added regression assertions for quantity `1000000.000` displaying as `10,00,000 PCS` and for the Qty/Unit order; formatter coverage confirms `10.000` displays as `10`. Tests were not run in this session; `git diff --check` passed.
- **Overflow follow-up:** Added pre-save bounds matching Vendor PO item and total column precision, safe MVC fallback handling, and calculation/controller regression cases. Automated tests were not run in this session.
- **Error response hardening:** Replaced keyword-based exception redaction with a fixed generic message and removed request paths from generic errors. Database failures on PO create/update now have a global redirect-and-flash fallback. Handler tests were added but not executed in this session.
- **Currency rendering correction:** Summary currency symbol and amount now form one no-wrap text value, with Segoe UI/Arial Unicode font fallbacks for the rupee glyph. Amount in words now starts with `Indian Rupees`. Focused regression assertions were updated; tests and PDF visual review were not run in this session.
- **Selected Unit correction:** Product resolution no longer replaces a nonblank PO line Unit. Product Unit remains a fallback and the form's product-change script now targets the select control. Service regression cases cover a product Unit of `1` while preserving a selected `EA` or `Box`. Tests were not run in this session.
- **Dynamic PDF columns:** Added content-based item-column sizing so long HSN, quantity, rate, and amount values receive proportional width, short Rate values do not keep excess space, and all adjacent cells are separated. Calculator tests were added; tests and visual render were not run in this session.
- **PDF label and number clarity:** Updated the row-number heading to `Sr No.`, centered the item headings, grouped quantities in Indian style, and appended `%` to GST values. Formatter tests and PDF text assertions were added; not run in this session.
- **Summary currency marker:** Added `₹` to subtotal, freight, packing, applicable GST row(s), and total. PDF text coverage asserts all six intra-state summary amounts carry the symbol; tests were not run in this session.
