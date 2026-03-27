# Test Progress Log

Use this file as the session-to-session handoff log for test implementation.

## Current Baseline (2026-03-20)
- Existing automated tests before this update:
  - `src/test/java/com/example/ordermanager/platform/aspect/MethodLoggingAspectTest.java`
  - `src/test/java/com/example/ordermanager/platform/config/GlobalModelAttributesTest.java`
  - `src/test/java/com/example/ordermanager/platform/config/SecurityConfigCsrfTest.java`
  - `src/test/java/com/example/ordermanager/dashboard/view/DashboardUrgentNotificationSanitizationTest.java`
- Added end-to-end lifecycle automation baseline:
  - `src/test/java/com/example/ordermanager/company/workflow/CompanyLifecycleWorkflowTest.java`
- Added service/controller coverage batch:
  - `src/test/java/com/example/ordermanager/company/service/CompanyServiceTest.java`
  - `src/test/java/com/example/ordermanager/user/service/UserServiceTest.java`
  - `src/test/java/com/example/ordermanager/client/service/ClientServiceTest.java`
  - `src/test/java/com/example/ordermanager/vendor/service/VendorServiceTest.java`
  - `src/test/java/com/example/ordermanager/user/controller/AuthControllerTest.java`
- Added complete test-case inventory:
  - `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md`

## What Is Covered Now
- Automated workflow chain: company registration -> email verification -> owner approval -> admin user creation -> client creation -> vendor creation -> order create/edit.
- Field formatting assertions included:
  - client name uppercase
  - vendor company uppercase
  - title-case contact names/product names
- Approval/login gate assertions included via `CompanyService.canUsersLogin`.

## Remaining Execution Work (Implementation Checklist)
- [ ] Add controller tests for all mappings listed in `CONTROLLER_SERVICE_TEST_CASES.md`
- [ ] Add unit tests for each service edge-case ID in `CONTROLLER_SERVICE_TEST_CASES.md` (CompanyService/UserService baseline started)
- [ ] Add REST error mapping tests for `/api/orders/{id}` not-found and invalid payload handling
- [ ] Add password-reset security edge tests (`TooManyAttemptsException`, token expiry) (TooManyAttempts controller path covered)
- [ ] Add owner-company access toggle edge tests (`APPROVED` gate for reactivation)

## Batch Completed (2026-03-20)
- Implemented `CompanyServiceTest` with happy + edge coverage for:
  - `registerCompanyWithAdmin`
  - `deactivateCompany`
  - `activateCompany` missing-company failure
  - `canUsersLogin`
- Implemented `UserServiceTest` with happy + edge coverage for:
  - `createUserByAdmin`
  - duplicate username validation
  - `updateUserByAdmin` email-change re-verification path
  - blank-email validation failure
- Implemented `AuthControllerTest` with happy + edge coverage for:
  - `/signup` redirect policy
  - `/verify` success/invalid token
  - forgot-password unknown user
  - verify-reset-code `TooManyAttemptsException`
  - verify-reset-code success session handoff
  - reset-password session enforcement and success flow
- Implemented international mobile normalization coverage:
  - `CompanyServiceTest` now verifies owner mobile normalization during registration
  - `UserServiceTest` now verifies normalized mobile persistence and invalid-mobile rejection
  - Added `ClientServiceTest` for phone normalization and invalid phone rejection
  - Added `VendorServiceTest` for phone normalization and invalid phone rejection

## Batch Completed (2026-03-23)
- Added `src/test/java/com/example/ordermanager/client/controller/ClientControllerTest.java` with:
  - happy path for `saveClient` redirect success
  - failure path for phone-validation exception mapping to `error` and `phoneError` model attributes
- Updated `clients/form.html` to show backend `error` alert and inline phone error under the phone widget.
- Added `src/test/java/com/example/ordermanager/vendor/controller/VendorControllerTest.java` with:
  - happy path for `saveVendor` redirect success
  - failure path for phone-validation exception mapping to `error` and `phoneError` model attributes
- Updated `vendors/form.html` and `VendorController` to show backend `error` alert and inline phone error under the vendor phone widget for both create and edit submissions.
- Added `src/test/java/com/example/ordermanager/admin/controller/AdminControllerTest.java` with:
  - happy path for `addUser` redirect success
  - failure path for mobile-validation exception mapping to `error` and `mobileError` model attributes in add form
  - happy path for `updateUser` redirect success
  - failure path for mobile-validation exception mapping to `error` and `mobileError` model attributes with refreshed user data in edit form
- Updated `AdminController.addUser()` and `updateUser()` to return form view on `IllegalArgumentException` (instead of redirect) with inline `mobileError` attribute for UX consistency with ClientController and VendorController
- Updated `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` with new test case IDs for mobile validation error handling (ADM-04, ADM-12)


## Rule For Every Development Cycle
- Mandatory: each new or modified controller/service method must ship with dedicated automated tests.
- PR readiness check:
  1. Test added/updated for happy path
  2. Test added/updated for at least one failure/edge path
  3. `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` updated when behavior changes
  4. `docs/testing/TEST_PROGRESS.md` updated with what was completed in this session

## Batch Completed (2026-03-24)
- Updated `AdminController.addUser()` success path to use `RedirectAttributes.addFlashAttribute("message", ...)` so success feedback survives redirect to `admin/users/list`.
- Confirmed `AdminController.updateUser()` success path uses flash message only (not model) to avoid lost messages after redirect.
- Updated `admin/users/list.html` to render flash-backed `message` and `error` alerts.
- Updated `admin/users/form.html` and `admin/users/form-edit.html` to render generic `error` alerts for non-mobile validation failures.
- Extended `AdminControllerTest` to assert success flash messages for both add and update flows.
- Updated `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` to reflect flash-message expectations for ADM-03 and ADM-11.
- Updated `VendorController.saveVendor()` to set flash `message` on redirect with separate text for add vs update success.
- Updated `vendors/list.html` to render flash-backed `message` and `error` alerts so vendor add/update feedback is visible.
- Extended `VendorControllerTest` with add/update success flash assertions and updated method signatures to include `RedirectAttributes`.
- Updated `VendorController.deleteVendor()` to set flash `message` on success and flash `error` on failure.
- Extended `VendorControllerTest` with vendor delete happy/failure path assertions for redirect flash messages.
- Updated `ClientController.saveClient()` to set flash `success` on redirect with separate text for add vs update success.
- Extended `ClientControllerTest` with add/update success flash assertions and updated method signature to include `RedirectAttributes`.
- Updated `OrderController.saveOrder()`, `updateOrder()`, and `deleteOrder()` to set flash `message` on redirect for create/update/delete success.
- Updated `orders/list.html` to render flash-backed `message` and `error` alerts so create/update/delete feedback is visible.
- Updated `orders/form.html` to render `error` alerts for same-request validation failures.
- Updated `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` to reflect order flash-message expectations for ORDC-03, ORDC-06, ORDC-07.
- Added `src/test/java/com/example/ordermanager/order/controller/OrderControllerTest.java` with comprehensive coverage for:
  - happy path for `saveOrder` (create) redirect success with flash `message`
  - failure path for `saveOrder` exception handling with error attribute and form reload
  - unauthenticated state (`IllegalStateException`) redirect to login
  - happy path for `updateOrder` redirect success with flash `message`
  - happy path for `deleteOrder` redirect success with flash `message`
  - edit/duplicate order happy and not-found paths

## Batch Completed (2026-03-26)
- Added `account_active` separation from email verification:
  - `enabled` now remains verification-only
  - admin activate/deactivate flow now updates `accountActive`
- Updated login gating in `CustomUserDetailsService` to block inactive users with:
  - `User is inactive. Contact Admin for account activation.`
- Updated admin users list toggle in `admin/users/list.html` to use `user.accountActive` for Active/Inactive action state.
- Added migration `src/main/resources/db/migration/V11__add_user_account_active_flag.sql` with safe backfill + not-null default.
- Updated tests:
  - `src/test/java/com/example/ordermanager/user/service/UserServiceTest.java` (`setUserActive` + defaults)
  - `src/test/java/com/example/ordermanager/admin/controller/AdminControllerTest.java` (toggle status uses accountActive; fixed premature class-closing brace)
  - `src/test/java/com/example/ordermanager/company/workflow/CompanyLifecycleWorkflowTest.java` (new users default active)
  - `src/test/java/com/example/ordermanager/config/CustomUserDetailsServiceTest.java` (inactive vs unverified vs success paths)

## Batch Completed (2026-03-26 — Overdue Delivery Alerts)
- Fixed dashboard urgent-order alerts to continue showing for overdue undelivered orders (delivery date in the past) not just orders due in the next 7 days.
- **`OrderRepository`**: added `findByCompanyIdAndDeliveryDateLessThanEqual` to fetch all orders with delivery date ≤ a given cutoff (includes overdue + upcoming up to today+7).
- **`OrderService.getUrgentOrdersByCompanyId`**: replaced `findByCompanyIdAndDeliveryDateBetween(today, today+7)` with `findByCompanyIdAndDeliveryDateLessThanEqual(today+7)`; overdue orders now remain in the alert set until their status becomes final.
- **`dashboard.html`**: updated `daysText` calculation to display `"X days overdue"` for past-due orders instead of the broken `"In -X days"`.
- Added `ORS-08` through `ORS-11` test cases in `OrderServiceTest` covering: overdue non-final included, final-status excluded, sort order (overdue-first), and beyond-7-day boundary.
- Updated `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` with ORS-07 through ORS-11.

## Batch Completed (2026-03-26 — Dashboard Alert Range Alignment)
- Updated `DashboardController.dashboard()` so urgent alert cards are shown only when the order's `orderDate` falls within the currently selected `startDate/endDate` filter.
- Added guard logic to exclude urgent-alert candidates with null `orderDate`.
- Added `src/test/java/com/example/ordermanager/dashboard/controller/DashboardControllerTest.java` with happy + edge coverage for date-range-aligned urgent notifications.
- Updated `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` with `DASH-05` and `DASH-06`.

## Batch Completed (2026-03-27 — Add User Form Field Layout)
- Updated `src/main/resources/templates/admin/users/form.html` layout to show:
  - row 1: `username` + `password`
  - row 2: `mobileNumber` + `email`
- Added `src/test/java/com/example/ordermanager/admin/controller/AdminUserFormTemplateTest.java` with:
  - happy-path assertion for source field order (`username -> password -> mobileNumber -> email`)
  - edge assertion that phone widget ids/init call remain unchanged



## Batch Completed (2026-03-27 — Product Feature + Multi-Product Orders)
- Added `Product` entity (`product/entity/Product.java`) with fields: name, similarName, description, brand, category, price, vendor (ManyToOne), company (ManyToOne).
- Added `OrderItem` entity (`order/entity/OrderItem.java`) for multi-product orders: order, product (nullable), productName, quantity, unitPrice.
- Added `Order.orderItems` (`@OneToMany(cascade=ALL, orphanRemoval=true)`).
- Added `ProductRepository` (`product/repository/ProductRepository.java`) and `OrderItemRepository` (`order/repository/OrderItemRepository.java`).
- Added `ProductService` with company-scoped CRUD, title-case formatting, vendor resolution.
- Added `ProductController` with list (+ orderUsageMap), new/edit/save/delete endpoints.
- Added `ProductOrderRef` DTO for order link display in product list.
- Modified `OrderController`: added `ProductService` dependency; `showCreateForm`, `showEditForm`, `duplicateOrder` now pass products; `saveOrder` and `updateOrder` accept `itemProductIds[]`, `itemQuantities[]`, `itemUnitPrices[]` arrays and call `buildOrderItems()`.
- Modified `OrderService.createOrderWithCompany()` and `patchOrder()` to call `syncLegacyFieldsFromItems()` which syncs `productName` and `quantity` legacy fields from orderItems for dashboard/list display.
- Added Flyway migration `V13__add_products_and_order_items.sql`.
- Added `products/list.html` with search bar, vendor column, order usage link column, delete modal.
- Added `products/form.html` with name, similarName, description, brand, category, price, and vendor dropdown.
- Updated `orders/form.html`: replaced product text field with dynamic multi-product rows (dropdown + qty + unit price), JS for add/remove rows and price auto-fill.
- Updated `dashboard.html`: truncate product name on card to 8 chars via `#strings.abbreviate`; full product name still shown in double-click modal.
- Updated `fragments/unified-navbar.html`: added Products nav link.
- Updated `OrderControllerTest` to pass `ProductService` mock and new `saveOrder`/`updateOrder` method signatures.
- Added `ProductControllerTest` with 7 test cases covering list, add/update success, error path, and delete happy/failure paths.
- Updated `CONTROLLER_SERVICE_TEST_CASES.md` with PRD-01 through PRD-06.

## Batch Completed (2026-03-27 — Nested Create Return Flow + Draft Resume)
- Updated `orders/form.html` with session-based draft persistence (`sessionStorage`) so in-progress order fields and dynamic product rows are restored after navigating away.
- Updated order form Add Product action to save draft and navigate to `/products/new?returnTo=...`.
- Updated `ProductController` (`newProductForm`, `editProduct`, `saveProduct`) to accept `returnTo`, validate allowed internal prefixes, and redirect back to order/product context on successful save.
- Updated `products/form.html` with hidden `returnTo`, session-based draft persistence, and Add Vendor action that navigates to `/vendors/new?returnTo=...` while preserving product draft.
- Updated `VendorController` (`newVendorForm`, `editVendor`, `saveVendor`) to accept `returnTo`, validate allowed internal prefixes, and redirect back to product context on successful save.
- Updated `vendors/form.html` with hidden `returnTo` and context-aware Back/Cancel links.
- Extended tests:
  - `src/test/java/com/example/ordermanager/product/controller/ProductControllerTest.java` (returnTo happy + invalid-edge redirects, model propagation)
  - `src/test/java/com/example/ordermanager/vendor/controller/VendorControllerTest.java` (returnTo happy + invalid-edge redirects, model propagation)
  - `src/test/java/com/example/ordermanager/order/controller/OrderFormTemplateTest.java` (draft + Add Product link hooks)
  - `src/test/java/com/example/ordermanager/product/controller/ProductFormTemplateTest.java` (returnTo hidden field + Add Vendor hook)
  - `src/test/java/com/example/ordermanager/vendor/controller/VendorFormTemplateTest.java` (returnTo hidden field + context link expression)

## Batch Completed (2026-03-27 — Follow-up ReturnTo Regression Fix)
- Fixed product-save redirect reliability by hardening `ProductController.sanitizeReturnTo` to normalize URL-encoded `returnTo` values (for example `%2Forders%2Fedit%2F42`).
- Updated `products/form.html` to include `returnTo` in form action query params in addition to hidden field, so context survives even if hidden payload is missed.
- Updated `orders/form.html` so both New Product links are server-rendered with `returnTo` context; removed the old target-blank path that could bypass context.
- Kept JS draft-save behavior but now navigates using rendered link URL to avoid rebuilding/losing context.
- Added regression coverage:
  - `ProductControllerTest.saveProduct_withEncodedReturnTo_redirectsToDecodedOrderPath`
  - `OrderFormTemplateTest` assertion for returnTo-aware New Product links
  - `ProductFormTemplateTest` assertion for returnTo-aware form action

## Batch Completed (2026-03-27 — Hotfix for Comma-Joined ReturnTo Values)
- Fixed live 500 path issue (`/orders/edit/{id},/orders/edit/{id}`) caused by duplicate `returnTo` values being merged into a comma-joined string by request binding.
- Updated `ProductController` and `VendorController` sanitization to decode and normalize `returnTo`, then collapse comma-joined duplicates to a single canonical path before allowlist checks.
- Added/updated regression tests:
  - `ProductControllerTest.saveProduct_withDuplicateCommaJoinedReturnTo_redirectsToSingleOrderPath`
  - `VendorControllerTest.saveVendor_withDuplicateCommaJoinedReturnTo_redirectsToSingleProductPath`
  - Updated vendor returnTo expectations to normalized decoded values.

## Batch Completed (2026-03-27 — Derived Order Totals + Edit Draft Fix)
- Updated `OrderService.createOrderWithCompany()` and `patchOrder()` to derive persisted order-level `quantity` and `totalAmount` from `orderItems` instead of trusting manual form entry.
- Added transient `Order` display helpers for item-based summaries so views can render `Product A [2], Product B [5]` without duplicating logic.
- Updated `orders/form.html` to remove manual order-level Qty/Total inputs, add live calculated summary fields, and replace fragile `innerHTML` draft persistence with structured row data.
- Fixed the edit-after-save blank product/qty bug by clearing order draft state on submit and restoring row values from structured data rather than stale HTML.
- Updated `orders/list.html` and `admin/order-statistics.html` to remove the separate Qty column and show per-product quantities inline in the product summary.
- Updated `dashboard.html` and `DashboardController` so dashboard cards/urgent payloads use item-based product summaries with quantities in brackets.
- Added/updated regression coverage:
  - `OrderServiceTest` for create/update derived totals (`ORS-12`, `ORS-13`)
  - `DashboardControllerTest` for item-summary urgent payload
  - `DashboardUrgentNotificationSanitizationTest` for summary-only notification text
  - `OrderFormTemplateTest` for calculated summary UI + structured draft persistence
- Verified with targeted Maven test suite: `OrderServiceTest`, `OrderControllerTest`, `OrderFormTemplateTest`, `DashboardControllerTest`, `DashboardUrgentNotificationSanitizationTest`, `ProductControllerTest`, `VendorControllerTest`, `ProductFormTemplateTest`, `VendorFormTemplateTest` (56 tests passing).

## Batch Completed (2026-03-27 — Order Date Validation Guard)
- Added service-level validation in `OrderService` so `deliveryDate` cannot be before `orderDate` for both create and update flows.
- Updated `OrderController` create/update error handling to surface validation errors back on `orders/form` and populate `dateError` for inline date feedback.
- Updated `orders/form.html` to show `dateError` directly below the Delivery Date input with invalid styling.
- Added/updated regression coverage:
  - `OrderServiceTest` happy + failure cases for valid/invalid order-vs-delivery date combinations.
  - `OrderControllerTest` validation failure path asserting both top-level error and inline `dateError` model attribute.
- Updated `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` with `ORDC-10` and `ORS-14`.

## Batch Completed (2026-03-27 — Client-Side Date Guard UX)
- Updated `orders/form.html` with client-side `validateOrderDatesOnClient()` pre-submit/date-change validation so delivery date cannot be earlier than order date.
- Added inline client feedback container `dateClientError` adjacent to the Delivery Date input, aligned with backend validation message text.
- Extended `OrderFormTemplateTest` assertions for client-side date-validation hook presence (`dateClientError`, validation function, and error copy).
- Refined `ORDC-10` in `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` to explicitly cover both client-side and server-side validation paths.

## Batch Completed (2026-03-27 — Order Statistics Total Value Summaries)
- Updated `AdminController.showOrderStatistics()` to add:
  - `totalOrderValue` (sum across grouped client stats)
  - `selectedClientTotalValue` (sum for drill-down order list)
- Updated `admin/order-statistics.html` to render total value summaries in both places:
  - top summary pills for grouped "Orders by Client"
  - drill-down header for selected client orders list
- Extended `AdminControllerTest` coverage with total-value assertions for default, grouped, and drill-down cases (including null order amounts).
- Added `src/test/java/com/example/ordermanager/admin/controller/AdminOrderStatisticsTemplateTest.java` to lock template bindings for `totalOrderValue` and `selectedClientTotalValue`.
- Updated `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` with `ADM-19` and `ADM-20`.

