# UI Modernization Continuation Note

Date: 2026-03-23

## Goal
Refresh all Thymeleaf pages (dashboard, client, vendor, admin, owner, auth/forms/lists) to look modern and polished while staying lightweight and fast.

## Agreed Direction
- Keep server-rendered Spring MVC + Thymeleaf (no SPA rewrite).
- Use Bootstrap 5.3 + Bootstrap Icons as the primary UI system.
- Minimize extra libraries; only keep page-specific JS where needed (for example `Sortable` + `axios` on dashboard).
- Consolidate repeated inline CSS into shared styles to reduce duplication and improve maintainability.

## Proposed Lightweight Stack
- Base: Bootstrap 5.3 (already used on key pages)
- Icons: Bootstrap Icons
- Optional (only if needed):
  - `Tom Select` for searchable selects on heavy forms
  - `Chart.js` only on pages that truly need charts
- Avoid heavy component frameworks and large JS bundles.

## Rollout Phases
1. Build shared design system tokens and utilities in `src/main/resources/static/css/style.css`.
2. Normalize shared fragments (`unified-navbar`, `footer`, common header/actions, cards/tables/forms).
3. Modernize list-heavy pages (dashboard, clients, vendors, orders) with consistent cards/tables/filters.
4. Modernize form pages (create/edit flows) with cleaner spacing, validation states, and action bars.
5. Modernize admin + owner + auth pages for visual consistency.
6. Run regression + performance pass and remove dead CSS/JS.

## Performance Guardrails
- Keep first render server-side and avoid runtime-heavy JS.
- Keep CSS lean: prefer Bootstrap utilities + shared component classes over per-page inline `<style>` blocks.
- Load JS only where needed; defer non-critical scripts.
- Keep payloads small and reuse CDN/browser cache effectively.

## Acceptance Criteria
- Consistent visual system across all pages (spacing, typography, buttons, forms, tables, alerts).
- Dashboard drag/drop and urgent notifications continue to work unchanged functionally.
- No tenant/owner auth behavior regression.
- Pages remain fast to render and interactive quickly on normal network conditions.

## Next Session Checklist
- [ ] Audit templates and list duplicated UI patterns.
- [ ] Define a token map (colors/spacing/radius/shadows) in shared CSS.
- [x] Refactor one pilot page (recommended: `dashboard.html`) to set the standard.
- [ ] Apply the same component patterns to clients/vendors/orders list + form pages.
- [ ] Update test docs if behavior or flows change: `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` and `docs/testing/TEST_PROGRESS.md`.

## Progress This Session
- Started the implementation with a dashboard pilot and shared-token groundwork.
- Moved the large inline `<style>` block out of `src/main/resources/templates/dashboard.html` into shared CSS.
- Added reusable design tokens (`:root` CSS variables) to `src/main/resources/static/css/style.css`.
- Scoped dashboard-specific styling under `.dashboard-page` to avoid side effects on other pages.
- Kept existing dashboard drag/drop and urgent notification JS behavior unchanged.
- Completed the next slice for list pages by removing inline `<style>` blocks from:
  - `src/main/resources/templates/clients/list.html`
  - `src/main/resources/templates/vendors/list.html`
  - `src/main/resources/templates/orders/list.html`
  - `src/main/resources/templates/admin/users/list.html`
- Added shared list UI classes in `src/main/resources/static/css/style.css` (`om-list-toolbar`, `om-list-toolbar-row`, `om-list-actions`, `om-list-search-input`, `om-sortable`, `om-action-group`, `om-date-range`, `om-status-pill*`).
- Preserved existing table sort/search/date filtering behavior and existing IDs used by `fragments/table-utils.html`.

## Files Updated This Session
- `src/main/resources/templates/dashboard.html`
- `src/main/resources/static/css/style.css`
- `src/main/resources/templates/clients/list.html`
- `src/main/resources/templates/vendors/list.html`
- `src/main/resources/templates/orders/list.html`
- `src/main/resources/templates/admin/users/list.html`

## Open Decisions
- CDN vs self-hosted Bootstrap assets in production.
- Final brand palette (keep existing blue-forward palette or refresh).
- Whether searchable selects are needed broadly enough to include `Tom Select`.

## Final Mobile Number Error Handling Implementation

✅ **AdminController** - Now follows exact ClientController pattern:
   - Both `addUser()` and `updateUser()` POST methods catch `IllegalArgumentException`
   - Both methods extract error message and check `isMobileValidationError(message)` 
   - If mobile error detected, sets `mobileError` attribute in model
   - Also sets `error` attribute for general error display
   - Returns the form view (not redirect) with populated model so user sees error inline
   - Helper method `isMobileValidationError()` checks if message contains "mobile"

✅ **UserService** - Already validates mobile numbers correctly:
   - `createUserByAdmin()` calls `PhoneNumberUtils.normalizeRequiredInternational()` which throws `IllegalArgumentException` with "Mobile number is invalid..." message
   - `updateUserByAdmin()` does the same validation
   - PhoneNumberUtils validates via Google's libphonenumber library and rejects invalid international formats

✅ **HTML Forms** - Both form.html and form-edit.html display errors correctly:
   - Mobile input field gets `is-invalid` CSS class when `mobileError` is present
   - Invalid input has red border styling from CSS
   - `<div class="invalid-feedback d-block">` displays the error message in red below the input
   - CSS styling handles both `.form-control-custom.is-invalid` and `.invalid-feedback` classes

✅ **CSS Styling** - Complete error state styling:
   - `.invalid-feedback` - red text color, proper font size and weight, visible with `d-block` utility
   - `.form-control-custom.is-invalid` - red border color
   - `.form-control-custom.is-invalid:focus` - red border with red-tinted focus ring

## Pattern Consistency Achieved
Now AdminController and ClientController both follow the EXACT same error handling pattern:
1. Try to save/update entity via service
2. Catch IllegalArgumentException
3. Extract error message
4. Check if it's a field-specific validation error (phone/mobile)
5. If field-specific, set field error attribute (`phoneError` or `mobileError`)
6. Set general `error` attribute too
7. Return form view with model (not redirect)
8. Form displays error inline with styling

## Latest UI Tweak (Login Alignment)
- Reinforced login page right-side placement to match company registration visual layout.
- Added a scoped class in `src/main/resources/static/css/style.css`:
  - `.auth-login-right { justify-content: flex-end !important; padding-right: 6% !important; }`
  - `.auth-login-right .auth-card { margin-left: auto; margin-right: 0; }`
- Kept mobile behavior unchanged by centering the same class under `@media (max-width: 768px)`.
- Ensured `src/main/resources/templates/auth/login.html` uses `body.auth-page.auth-page-split.auth-login-right`.
- Root-cause fix applied: `body` globally uses `flex-direction: column`, so split auth pages interpreted `justify-content: flex-end` as bottom alignment.
- Added `flex-direction: row` to `.auth-page` so right-side split layouts stay vertically centered (mid-right) on desktop.

