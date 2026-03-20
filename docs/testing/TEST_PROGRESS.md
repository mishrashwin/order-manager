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

## Rule For Every Development Cycle
- Mandatory: each new or modified controller/service method must ship with dedicated automated tests.
- PR readiness check:
  1. Test added/updated for happy path
  2. Test added/updated for at least one failure/edge path
  3. `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` updated when behavior changes
  4. `docs/testing/TEST_PROGRESS.md` updated with what was completed in this session


