# Controller + Service Test Cases

This file is the living test inventory for controller and service methods.

## Scope
- Controllers under `src/main/java/com/example/ordermanager/**/controller` (for example `company/controller`, `order/controller`, `admin/controller`, `dashboard/controller`, `error/controller`, `user/controller`)
- Services under `src/main/java/com/example/ordermanager/**/service` (for example `company/service`, `order/service`, `client/service`, `vendor/service`, `owner/service`, `user/service`)
- Includes happy path, tenant/auth boundaries, and edge-case scenarios discoverable from current code.

## Workflow Automation Chain (End-to-End)
- `WF-01` Company registration creates company + first admin in one transaction (`CompanyService.registerCompanyWithAdmin`)
- `WF-02` Verification token generation + email send (`RegistrationService.sendVerificationEmail`)
- `WF-03` Verification token consumption enables admin (`RegistrationService.verifyToken`)
- `WF-04` Owner approval enables login eligibility (`CompanyService.approveCompany`, `CompanyService.canUsersLogin`)
- `WF-05` Admin creates additional user (`UserService.createUserByAdmin`)
- `WF-06` Tenant client creation with formatting and company assignment (`ClientService.saveClientWithCompany`)
- `WF-07` Tenant vendor creation with formatting and company assignment (`VendorService.saveVendorWithCompany`)
- `WF-08` Order creation with client/customer synchronization (`OrderService.createOrderWithCompany`)
- `WF-09` Order patch/edit updates selected fields only (`OrderService.patchOrder`)
- Automated baseline for this chain: `src/test/java/com/example/ordermanager/company/workflow/CompanyLifecycleWorkflowTest.java`

## Controller Test Cases

### `AdminController`
- `ADM-01` `GET /admin/dashboard` returns `admin/dashboard` and model contains tenant `companyId`
- `ADM-02` `GET /admin/users` includes users list, admin count, and current username from security context
- `ADM-03` `POST /admin/users/add` success redirects `/admin/users` with flash `message`
- `ADM-04` `POST /admin/users/add` mobile validation failure returns form with `mobileError` attribute and `is-invalid` class
- `ADM-05` `POST /admin/users/add` generic `IllegalArgumentException` sets error attribute and returns form
- `ADM-06` `GET /admin/users/{id}/edit` success returns `admin/users/form-edit` with role options
- `ADM-07` `GET /admin/users/{id}/edit` invalid id/company mismatch redirects with query error
- `ADM-08` `POST /admin/users/{id}/role` enforces service rule failures via flash error
- `ADM-09` `POST /admin/users/{id}/delete` blocks self-delete and last-admin self-delete with exact message branches
- `ADM-10` `POST /admin/users/{id}/delete` non-self delete success message includes deleted username
- `ADM-11` `POST /admin/users/{id}/update` success redirects `/admin/users` with flash `message`
- `ADM-12` `POST /admin/users/{id}/update` mobile validation failure returns edit form with `mobileError` and refreshed user data
- `ADM-13` `POST /admin/users/{id}/update` generic validation failure sets error attribute and returns edit form
- `ADM-14` company view/edit/update paths load current tenant company and handle missing company
- `ADM-15` `POST /admin/users/{id}/toggle-status` activates inactive user by setting `accountActive=true` and sets flash `message` with "activated"
- `ADM-16` `POST /admin/users/{id}/toggle-status` deactivates active user by setting `accountActive=false` and sets flash `message` with "deactivated"
- `ADM-17` `POST /admin/users/{id}/toggle-status` blocks self-toggle with flash `error`
- `ADM-18` `POST /admin/users/{id}/toggle-status` user not in company sets flash `error`
- `ADM-19` `GET /admin/order-statistics` computes and exposes overall total order value for the selected date/status filters
- `ADM-20` drill-down (`clientId`/`clientName`) computes selected-client total order value and ignores null order amounts

### `CompanyController`
- `COM-01` `GET /company/register` initializes `registrationData`
- `COM-02` `POST /company/register` binding errors return `company/register` and preserve DTO
- `COM-03` empty company name is rejected before service call
- `COM-04` happy path redirects `redirect:/login?registered=true` with onboarding message
- `COM-05` `DataIntegrityViolationException` maps to user-friendly duplicate messages by key name (`mobile_number`, `username`, `email`, `companies_name_key`)
- `COM-06` generic exception returns safe fallback error message
- `COM-07` `GET /company` redirects to login on missing tenant context (`IllegalStateException`)
- `COM-08` owner endpoints (`/company/admin/all`, `/company/{id}/activate`, `/company/{id}/deactivate`) map success/error query params

### `DashboardController`
- `DASH-01` no `startDate/endDate` defaults to `now().minusMonths(1)` and `now()`
- `DASH-02` valid date params are parsed and passed to `getOrdersByCompanyIdAndDateRange`
- `DASH-03` urgent order projection includes only expected fields (`id`, `customerName`, `productName`, `quantity`, `deliveryDate`)
- `DASH-04` includes all enum statuses and selected company name fallback
- `DASH-05` urgent alerts include only orders whose `orderDate` falls within the selected dashboard date range
- `DASH-06` urgent alerts exclude entries with null `orderDate` to prevent out-of-range notification leakage
- `DASH-07` dashboard urgent payload and cards use item-based product summaries with per-product quantities in brackets

### `OrderController`
- `ORDC-01` list endpoint uses tenant id and date range defaults and shows flash `message`/`error` alerts
- `ORDC-02` `GET /orders/new` loads clients by company and enum statuses
- `ORDC-03` `POST /orders` sets default status `CREATED` when missing; success redirects `/orders` with flash `message`
- `ORDC-04` `POST /orders` handles unauthenticated state (`IllegalStateException`) by redirecting `/login`; error path returns form with error attribute
- `ORDC-05` `POST /orders` generic failure returns form with clients/statuses restored
- `ORDC-06` edit/duplicate for missing order redirects to `/orders`; `POST /orders/update/{id}` success redirects with flash `message`
- `ORDC-07` `GET /orders/delete/{id}` success redirects with flash `message`
- `ORDC-07` duplicate order copies fields and resets `orderDate=now`, `status=CREATED`
- `ORDC-08` order form template includes draft-resume hooks and Add Product link wiring to preserve in-progress edits before navigation
- `ORDC-09` order form uses line-item qty/unit price as source of truth, shows calculated total/qty summary, and clears stale draft state on submit
- `ORDC-10` order date validation blocks `deliveryDate < orderDate` (client-side pre-submit + server-side `POST /orders` fallback) and shows inline warning

### `OrderRestController`
- `ORDA-01` `POST /api/orders` delegates create and returns created payload
- `ORDA-02` `PATCH /api/orders/{id}` updates only supplied fields and propagates `OrderNotFoundException`
- `ORDA-03` `DELETE /api/orders/{id}` returns `204 No Content`
- `ORDA-04` `GET /api/orders/api/order-statuses` includes enum `name`, `displayName`, `isFinal`

### `ClientController`
- `CLI-01` list endpoint uses tenant-scoped `getClientsByCompanyId`
- `CLI-02` save success (create/update) redirects `/clients` with flash `success`
- `CLI-03` save unauthenticated path returns form with specific login-required error
- `CLI-04` `ClientHasActiveOrdersException` renders order count in flash error message
- `CLI-05` generic delete error returns fallback flash error

### `VendorController`
- `VEN-01` list endpoint uses tenant-scoped `getVendorsByCompanyId`
- `VEN-02` save success (create/update) redirects `/vendors` with flash `message`
- `VEN-03` save unauthenticated/general error returns form with error
- `VEN-04` edit route delegates by id; delete success/failure redirects `/vendors` with flash `message`/`error`
- `VEN-05` `GET /vendors/new` and `POST /vendors` preserve/validate `returnTo` so nested vendor creation can return to product form safely
- `VEN-06` vendor `returnTo` sanitization normalizes encoded/duplicate comma-joined values (for example query+form duplicates) to a single safe path

### `ProductController`
- `PRD-01` list endpoint uses tenant-scoped `getProductsByCompanyId` and populates `orderUsageMap`
- `PRD-02` save success (create/update) redirects `/products` with flash `message`
- `PRD-03` save `IllegalArgumentException` returns form with inline error and product retained
- `PRD-04` delete correct password succeeds and redirects with flash `message`
- `PRD-05` delete wrong password does not delete and returns flash `error`
- `PRD-06` delete correct password but service throws returns flash `error`
- `PRD-07` `GET /products/new|edit` and `POST /products` preserve/validate `returnTo` so product creation can return to order flow safely
- `PRD-08` product `returnTo` sanitization normalizes encoded/duplicate comma-joined values (for example query+form duplicates) to a single safe path

### `OwnerController`
- `OWN-01` dashboard model includes owner page markers and metrics/pending lists
- `OWN-02` companies page includes summaries + pending count
- `OWN-03` approve/reject success and `IllegalArgumentException` branches set flash messages
- `OWN-04` toggle-access deactivates active company
- `OWN-05` toggle-access blocks re-enable unless approval status is `APPROVED`

### `ErrorPageController`
- `ERR-01` no reason defaults to `forbidden` reason key and fallback message
- `ERR-02` each reason (`suspended`, `pending-approval`, `rejected`) resolves correct copy

### `AuthController`
- `AUTH-01` `/signup` always redirects to login with admin-managed registration message
- `AUTH-02` `/verify` success, invalid token, and exception branches set appropriate flash message
- `AUTH-03` resend verification by email vs username path; invalid user shows correct error prompt
- `AUTH-04` resend verification for already-enabled account redirects login with info message
- `AUTH-05` forgot-password denies unknown and non-enabled users
- `AUTH-06` verify-reset-code stores `RESET_VERIFIED_EMAIL` in session only on valid code
- `AUTH-07` verify-reset-code handles `TooManyAttemptsException` and redirects to forgot-password
- `AUTH-08` reset-password GET enforces prior code verification session
- `AUTH-09` reset-password POST validates empty/mismatch/min-length and session expiry
- `AUTH-10` reset-password success clears session key and redirects login

## Service Test Cases

### `CompanyService`
- `COS-01` `registerCompany` sets defaults (`active=true`, `PENDING`, null approval metadata)
- `COS-02` duplicate company name throws `IllegalArgumentException`
- `COS-03` `registerCompanyWithAdmin` validates username/email/mobile uniqueness before write
- `COS-04` `registerCompanyWithAdmin` hashes admin password, sets `ADMIN`, `enabled=false`
- `COS-05` owner notification skipped when `app.owner.email` missing/blank
- `COS-06` notification failures do not roll back successful registration/approval/rejection/access-toggle writes
- `COS-07` `updateCompany` enforces unique name except self and updates bio
- `COS-08` approve/reject populate `approvedAt`/`approvedBy`; reject also sets inactive
- `COS-09` deactivate/activate only send corresponding notification on real state transition
- `COS-10` `canUsersLogin` true only for active + `APPROVED`
- `COS-11` `registerCompanyWithAdmin` normalizes owner mobile to international canonical digits and validates country-aware format

### `OrderService`
- `ORS-01` `createOrder`/`createOrderWithCompany` title-case `productName`
- `ORS-02` `createOrderWithCompany` throws if company id not found
- `ORS-03` `createOrderWithCompany` syncs `customerName` from client relation
- `ORS-04` `patchOrder` updates only non-null fields and preserves unspecified fields
- `ORS-05` `patchOrder` supports legacy `customerName` update when client absent
- `ORS-06` `deleteOrder` throws `OrderNotFoundException` when id absent
- `ORS-07` urgent order query: upcoming non-final orders (delivery date ≤ today+7) included
- `ORS-08` urgent order query: overdue non-final orders (delivery date < today) included
- `ORS-09` urgent order query: final status orders excluded regardless of delivery date
- `ORS-10` urgent order query: sorted overdue-first then upcoming by delivery date ascending
- `ORS-11` urgent order query: orders with delivery date > today+7 not returned
- `ORS-12` create flow derives persisted `quantity` and `totalAmount` from order items instead of trusting manual order-level inputs
- `ORS-13` patch/update flow recalculates persisted `quantity` and `totalAmount` from order items when items are submitted
- `ORS-14` create/update date validation rejects orders where `deliveryDate` is before `orderDate`

### `ClientService`
- `CLS-01` save assigns company by id and enforces uppercase client name
- `CLS-02` save title-cases `contactPerson`
- `CLS-03` save fails with `IllegalArgumentException` when company id missing
- `CLS-04` delete throws `ClientHasActiveOrdersException` with `orderCount` when referenced by orders
- `CLS-05` save normalizes optional phone to international canonical digits (country code + mobile)
- `CLS-06` save rejects invalid international phone input

### `VendorService`
- `VDS-01` save assigns company by id and enforces uppercase `companyName`
- `VDS-02` save title-cases `contactPerson`
- `VDS-03` save fails with `IllegalArgumentException` when company id missing
- `VDS-04` save normalizes optional phone to international canonical digits (country code + mobile)
- `VDS-05` save rejects invalid international phone input

### `OwnerManagementService`
- `OMS-01` dashboard metrics compute inactive as `total-active`
- `OMS-02` summaries sorted by `createdAt` descending with nulls last
- `OMS-03` user count map handles empty company list and missing company counts as `0`

### `UserService`
- `USR-01` admin create hashes password, sets company, disables user, triggers verification email
- `USR-02` admin create rejects duplicate username/email
- `USR-03` `getUserByIdAndCompany` rejects cross-tenant access
- `USR-04` delete blocks deleting last admin
- `USR-05` role update blocks >2 admins and last-admin demotion
- `USR-06` `isLastAdminInCompany` false for non-admin users
- `USR-07` `updateUserByAdmin` requires non-blank email/username
- `USR-08` `updateUserByAdmin` rejects duplicate email/username when changed
- `USR-09` email change resets `enabled=false` and sends new verification; unchanged email skips resend
- `USR-10` create/update normalize mobile to international canonical digits and reject invalid input
- `USR-11` create/update reject duplicate mobile after normalization
- `USR-12` `setUserActive` activates user (`accountActive=true`) and persists change without altering verification state
- `USR-13` `setUserActive` deactivates user (`accountActive=false`) and persists change without altering verification state
- `USR-14` `setUserActive` throws when user does not belong to the given company

### `RegistrationService`
- `REG-01` existing unexpired token raises `EmailAlreadySentException`
- `REG-02` existing expired token is rotated (new UUID + expiry)
- `REG-03` verify token returns false for missing/expired token
- `REG-04` verify token success sets `enabled=true` only and deletes token (must not override admin activation state)

### `CustomUserDetailsService`
- `CUD-01` login rejects `accountActive=false` users with admin-contact message
- `CUD-02` login rejects unverified users with resend-verification message path
- `CUD-03` login succeeds only when company access is allowed and user is both verified and active

### `PasswordResetService`
- `PRS-01` existing unexpired unverified token raises `EmailAlreadySentException`
- `PRS-02` expired/verified token is regenerated and failed attempts reset to `0`
- `PRS-03` verify code returns null for missing/expired token
- `PRS-04` verify code increments failed attempts on mismatch and throws `TooManyAttemptsException` once max reached
- `PRS-05` successful code verification consumes token immediately
- `PRS-06` password reset returns false for unknown email, true for known email and removes lingering token

### `EmailService` and `BrevoEmailService`
- `EML-01` `EmailService` delegates each email type to `BrevoEmailService`
- `BVO-01` blank API key throws `BrevoEmailException`
- `BVO-02` request timeout maps to timeout-specific message
- `BVO-03` HTTP 4xx/5xx responses map to API rejection message
- `BVO-04` request payload escapes JSON/HTML-sensitive characters

## Minimum Rule For New Code
- For every new or changed controller/service method, add or update tests in the same PR.
- At minimum, include one happy-path test and one edge-case/failure-path test.

