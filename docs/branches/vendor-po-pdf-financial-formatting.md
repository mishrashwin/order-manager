# Branch: `vendor-po-redesign`

## Branch and objective
- **Branch name:** `vendor-po-redesign`
- **Feature / development objective:** Improve Vendor PO PDF monetary formatting, financial column layout, and Indian amount-in-words rendering.
- **Status / dates:** Complete; 2026-11-19.

## Problem statement
Vendor PO PDF financial columns use a western grouping formatter and constrained widths, making larger values hard to read and potentially causing adjacent columns to collide.

## Scope
- **Included:** Centralized BigDecimal Indian grouping for PDF money, item and summary layout, Indian amount-in-words correctness, focused PDF/formatter tests, and documentation.
- **Excluded:** GST calculation changes, persisted snapshot changes, schema changes, and unrelated Vendor PO behavior.

## Features implemented
- [x] Centralized BigDecimal Indian monetary formatting with two decimals for item and summary money.
- [x] Reserved explicit PDF table widths for financial columns, right-aligned numeric cells, wrapped product descriptions, and separated financial summary rows.
- [x] Preserved Indian lakh/crore amount-in-words conversion and added negative amount support.

## Technical implementation summary
`IndianMoneyFormatter` formats monetary display values; `VendorPdfServiceImpl` passes the formatted item and persisted PO snapshot totals to the existing Thymeleaf PDF template. `NumberToWordsUtil` continues to consume the saved BigDecimal total. GST stays in `VendorPoCalculationService`.

## Database and entity changes
None.

## API, controller, and service changes
`VendorPdfServiceImpl` uses the shared formatter for item rate/GST/amount, subtotal, freight, packing, CGST, SGST, IGST, and Grand Total. Existing `VendorPoCalculationService` and snapshot behavior are unchanged.

## UI, template, and static asset changes
The Vendor PO PDF template now defines all item column widths explicitly, adds subtle separation before financial cells, keeps numeric cells right-aligned, wraps long product names, and gives the summary amounts a fixed right-aligned column with a prominent Grand Total row.

## Business rules and validations
Preserve persisted financial values, GST inclusion/exclusion rules, and snapshot semantics. Formatting is display-only.

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

## Future enhancements
None identified.

## Related commits and PRs
Not available yet.

## Documentation updates
- [x] README update not applicable: this is a PDF presentation and display-formatting change; no setup, capability, or configuration behavior changed.
- [x] PDF/testing rule, test matrix, progress log, and paired testing guide update.
- [x] Paired testing guide: [`vendor-po-redesign-TESTING.md`](vendor-po-redesign-TESTING.md).

## Decisions and outcomes
- **Decision:** Use one BigDecimal-based display formatter; keep GST calculations inside `VendorPoCalculationService`.
- **Outcome:** Formatter and PDF tests passed; the sample fixture uses 18% GST with matching ₹180,000,000 tax on a ₹1,000,000,000 taxable base. Rendered page inspected with separated numeric columns and no clipping. Sample PDF and PNG are in `output/pdf/`.
- **Fixture correction:** After review identified a zero-rate sample, set the test item rate to 18% and asserted the rendered GST%/GST Amt pair. The corrected sample was regenerated and visually confirmed; its focused PDF test passed.
