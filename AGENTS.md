# AGENTS.md

## 📌 Engineering Rule Evolution (MANDATORY)

This repository follows a strict engineering playbook located under /docs.

RULE:
- If any new pattern, bug fix, or architectural improvement is identified:
    1. It MUST be documented in the relevant engineering file:
        - @Architecture → docs/architecture/ARCHITECTURE_BACKEND_RULES.md
        - Architecture Overview → docs/architecture/ARCHITECTURE_OVERVIEW.md
        - Multi-Tenant/Domain → docs/architecture/MULTI_TENANT_DOMAIN_RULES.md
        - Testing → docs/testing/TESTING_RULES.md
        - UI → docs/ui/UI_DESIGN_RULES.md
        - Project Overview → README.md
    2. It MUST NOT remain only in code.
    3. Documentation must be updated in the SAME PR.

Failure to update documentation = REJECT PR

Goal:
Engineering knowledge must evolve WITH the system, not after it.

---

## 📌 Branch-Based Documentation Strategy

### Branch Documentation Directory: `docs/branches/`

**Purpose**: Track work done per branch/feature for easy reference and knowledge sharing.

**Process**:
1. **Create branch-specific file**: When starting new work, create `docs/branches/{branch-name}.md`
2. **Document intent**: State what this branch aims to achieve
3. **Track progress**: Log work done, decisions made, and outcomes
4. **Reference in AGENTS.md**: This file is automatically consulted for context

**Benefits**:
- Clear separation of concerns between branches
- Historical record of what was done and why
- Easy knowledge transfer between developers
- Context preservation for future work

**Example Structure**:
```markdown
# Branch: vendor-po-enhancement

## Intent
Enhance Vendor PO feature with advanced GST calculations and multi-vendor support.

## Work Done
- [2026-04-15] Added IGST vs CGST/SGST logic based on state comparison
- [2026-04-16] Implemented multi-vendor PO bulk operations
- [2026-04-17] Added vendor performance analytics

## Key Decisions
- State-based GST calculation chosen over manual GST type selection
- Bulk operations use transactional approach for data consistency
- Analytics dashboard uses cached data for performance

## Outcomes
- ✅ GST calculation accuracy improved by 95%
- ✅ Bulk operations reduced processing time by 60%
- ✅ Vendor analytics dashboard adopted by 80% of users
```

**Usage**:
- Before starting work on a branch, check if `docs/branches/{current-branch}.md` exists
- Read the file to understand previous work and context
- Update the file as work progresses
- Reference key decisions when implementing related features

This ensures all developers have full context of what was done, why, and with what results.

## 📌 Mandatory Engineering Checklist (CRITICAL)

**For EVERY prompt/work session, you MUST follow this exact sequence**:

**Phase 1: Pre-Implementation Review**
1. **Read engineering rule files**:
   - [ ] `docs/architecture/ARCHITECTURE_BACKEND_RULES.md`
   - [ ] `docs/architecture/ARCHITECTURE_OVERVIEW.md`
   - [ ] `docs/architecture/MULTI_TENANT_DOMAIN_RULES.md`
   - [ ] `docs/testing/TESTING_RULES.md`
   - [ ] `docs/ui/UI_DESIGN_RULES.md`
2. **Get current branch**: `git branch --show-current`
3. **Check branch documentation**: `docs/branches/{branch-name}.md` exists?
4. **Read existing branch file** for context and previous decisions

**Phase 2: Documentation & Implementation**
5. **Create/update branch file** with current intent
6. **Implement changes** following documented patterns
7. **Update branch file** with work done, decisions, outcomes
8. **Reference branch decisions** for implementation guidance

**Phase 3: Completion**
9. **Update outcomes** and final decisions in branch file
10. **Reference specific rule sections** in commit messages when applicable

**Failure to follow this checklist = REJECT WORK**

## Scope + Existing AI Instructions
- This repo currently has one dedicated AI rule file (`AGENTS.md`) plus `README.md` from the requested glob scan; no other AI rule files were found.
- Treat `README.md`, `docs/architecture/ARCHITECTURE_OVERVIEW.md`, and `DEPLOYMENT.md` as context, but prefer code as source of truth when docs conflict.

## Big Picture Architecture
- Stack: Spring Boot 3.3, Java 17, Thymeleaf MVC + small REST surface (`OrderRestController`), JPA/Hibernate, Flyway, Spring Security.
- Core flow is `Controller -> Service -> Repository -> Entity/DB`, with server-rendered templates in `src/main/resources/templates/**`.
- Codebase layout is feature-first under `src/main/java/com/example/ordermanager/**` (for example `company/*`, `order/*`, `client/*`, `vendor/*`, `owner/*`, plus `admin/dashboard/error` controller modules and shared `user/*`, `config/*`, `utils/*`).
- Tenant model: each business row carries `company_id`; tenant context is derived from auth (`SecurityContextHelper`) rather than request params.
- Owner model is separate from tenant users: `CustomUserDetailsService` injects a virtual `ROLE_OWNER` user from `app.owner.*` properties.
- Company onboarding now includes owner approval (`Company.approvalStatus` + `/owner/**` endpoints) before tenant users can log in.
- Owner-wide company review runs through `OwnerController -> OwnerManagementService -> CompanyRepository/UserRepository` and renders `src/main/resources/templates/owner/{dashboard,companies}.html`; prefer that path over the legacy owner-only methods still present in `CompanyController`.
- `GlobalModelAttributes` (`@ControllerAdvice` in `config/`) injects `companyName` and `navbarGreetingName` into every view for authenticated users; do **not** re-set these attributes in new controllers.

## Multi-Tenant + Auth Rules (Critical)
- For tenant-scoped logic, always start with `securityContextHelper.getCompanyIdFromContext()` (see `DashboardController`, `OrderController`, `AdminController`).
- `SecurityContextHelper` also provides `getUserFromContext()` (full `User` entity) and `getCurrentUsername()` (string); `isOwnerContext()` is a safe boolean callable from any context including owner sessions.
- Owner sessions have no tenant context; `SecurityContextHelper` throws for owner-only contexts to prevent accidental cross-tenant access.
- `CustomUserDetailsService` blocks login when company is `PENDING`, `REJECTED`, or inactive (`CompanyService.canUsersLogin`).
- Public self-registration is disabled; `/signup` redirects to login. New users are created exclusively by admins via `UserService.createUserByAdmin(user, companyId)` → `POST /admin/users/add`. Admin-initiated email updates reset `enabled` to `false` and trigger re-verification (`UserService.updateUserByAdmin`).
- CSRF is enabled for MVC by default but explicitly ignored for `/api/**` in `SecurityConfig`; keep JSON/AJAX state updates on `/api/**` (for example dashboard drag-drop status PATCH) and keep non-API form posts CSRF-protected.
- Repository pattern is explicit: prefer `findByCompanyId(...)` methods; avoid unfiltered `findAll()` in tenant-facing paths.
- `OrderRestController` is API-oriented and not owner/tenant-context enforcing by itself; route new tenant-facing reads/writes through `SecurityContextHelper` and `...ByCompanyId(...)` service/repository methods.

## Domain Patterns You Should Mirror
- Company registration is transactional and creates first admin in one unit (`CompanyService.registerCompanyWithAdmin`).
- Admin user management enforces business rules in `UserService`: min 1 admin/company, max 2 admins/company.
- Orders keep both `client` relation and legacy `customerName`; setters sync these fields (`Order.setClient`, V5 migration).
- `OrderStatus` enum carries `isFinal()`, `isActive()`, `getDisplayName()`, and `getBadgeColor()` (Bootstrap badge class string); use `isFinal()` for status-gate logic instead of direct enum comparisons.
- Client deletion is guarded: `ClientService.deleteClient(id)` throws `ClientHasActiveOrdersException` (carries `orderCount`) when any orders reference the client; `ClientController` surfaces this as a flash error via `RedirectAttributes`.
- Input formatting conventions are in services (e.g., uppercase company/client/vendor names, title-case contact/product names via `utils/Helper.toTitleCase`).
- AOP method logging is globally applied (`MethodLoggingAspect`); exclude noisy/sensitive flows with `@SkipMethodLogging` — the annotation lives in `com.example.ordermanager.aspect` (the `annotation/` package is empty); apply at method or class level (e.g., `BrevoEmailService` uses class-level).
- `DashboardController` and `OrderController` both default the date range to the past 1 month when `startDate`/`endDate` query params are absent; prefer `OrderService.getOrdersByCompanyIdAndDateRange()` over `getOrdersByCompanyId()` in new list views.
- **Dashboard status updates**: Dashboard uses a drag-and-drop board (`Sortable` in `dashboard.html`) and persists status transitions via `axios.patch('/api/orders/{id}', { status: '<ENUM_NAME>' })`; payload status values must be enum names (`OrderStatus.name()`), and the UI reverts the card if the PATCH fails.

## Developer Workflows
- Local run: `mvn spring-boot:run` (or profile-specific: `-Dspring-boot.run.profiles=dev`).
- The checked-in `start-local.bat` assumes `.env` / `.env.example`, which are not committed in this repo, so prefer the Maven/profile commands above when automating local setup.
- Tests: `mvn test`.
- Test planning/progress docs now live in `docs/testing/CONTROLLER_SERVICE_TEST_CASES.md` and `docs/testing/TEST_PROGRESS.md`; keep both updated when behavior changes.
- Mandatory engineering rule: every new or changed controller/service method must include dedicated automated tests (at least one happy-path and one edge/failure-path case) in the same development cycle.
- Build/package: `mvn clean package` (Dockerfile uses `-DskipTests` during image build).
- Formatting runs automatically in Maven `process-sources` via `formatter-maven-plugin` using `eclipse-java-google-style.xml`.
- DB evolution: add Flyway scripts in `src/main/resources/db/migration/V{N}__description.sql`.

## Environment + Integrations
- Profiles: `application-dev.properties`, `application-qa.properties`, `application-prod.properties`.
- Current runtime direction is PostgreSQL in dev/prod profiles; root `application.properties` still contains MySQL-oriented defaults, so verify active profile before changing DB logic.
- `pom.xml` carries both PostgreSQL and MySQL drivers/Flyway modules, but `docker-compose.yml` is still a MySQL-only local path with Flyway disabled while the checked-in migrations include PostgreSQL-specific scripts (for example `V5__add_client_relationship_to_orders.sql` uses `DO $$ ... $$`).
- Email is Brevo API via `WebClient` (`BrevoEmailService`); key env vars include `BREVO_API_KEY`, `MAIL_FROM`, `APP_BASE_URL`. `brevo.request-timeout-ms` (default `5000`) controls the Brevo `WebClient` request timeout.
- Owner onboarding notifications and approval/rejection emails are sent from `CompanyService` through `EmailService`. Company suspension (`deactivateCompany`) and restoration (`activateCompany`) trigger two additional email events — `sendCompanyAccessRevokedEmail` / `sendCompanyAccessRestoredEmail` — delivered to the primary admin resolved via `UserService.findPrimaryAdminByCompanyId`.
- Owner console credentials come from `OWNER_EMAIL`, `OWNER_USERNAME`, and `OWNER_PASSWORD`; `CustomUserDetailsService` accepts `OWNER_PASSWORD` either raw or already encoded in Spring's `{id}...` format.
- `SecurityConfig` registers a context-aware `AccessDeniedHandler` that redirects to `/access-denied?reason=suspended|pending-approval|rejected|forbidden`; `ErrorPageController` resolves the `reason` param to user-facing messages. Successful login routes the owner to `/owner/dashboard` and all tenant roles to `/dashboard`.
- Keep-alive behavior is code-driven: `KeepAliveScheduler` is enabled by `app.keepalive.enabled`, uses `RestTemplate`, and pings `APP_BASE_URL + "/login"`; container health checks separately target `/actuator/health`.
- Deployment assets: `Dockerfile`, `docker-compose.yml`, and `render.yaml` (Render + Neon PostgreSQL assumptions).

## Recent Notes

- [2026-05-03] Vendor PO: edit-mode product-selection parity
  - Developers: when modifying Vendor PO UI/JS ensure both server-rendered edit rows and client-side ROW_TEMPLATE share identical
    column widths, name attributes and data-* attributes on option elements. The recommended pattern is to cache product option
    HTML at page load and to explicitly reset select.value for new rows. See `docs/branches/Vendor-PO.md` and
    `src/main/resources/templates/vendor/pos-form-new.html` for the canonical implementation details.

