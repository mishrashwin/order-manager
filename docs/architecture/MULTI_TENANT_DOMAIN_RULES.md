# MULTI-TENANT & DOMAIN RULES

## PURPOSE
This document defines the **authoritative multi-tenant architecture rules** for the Order Manager system. It ensures complete data isolation between companies and prevents cross-tenant data leakage.

Violation of any rule → **DO NOT MERGE**

---

# 1. TENANT MODEL FUNDAMENTALS

## 1.1 Core Principle
- **Every business row MUST carry `company_id`**
- Tenant context is derived from authentication (`SecurityContextHelper`), **NOT** from request parameters
- Each company's data is completely isolated from others

## 1.2 Entity Requirements
All business entities MUST include:
- `company_id` (FK → companies table)
- Index on `company_id` for query performance

Examples:
- Client
- Vendor
- Order
- VendorPo
- Product

---

# 2. SECURITYCONTEXTHELPER (TRUST ANCHOR)

## 2.1 Purpose
`SecurityContextHelper` is the **ONLY** trusted source for tenant context. It extracts company information from the authenticated user's session.

## 2.2 Available Methods

### getCompanyIdFromContext()
- **Usage**: Extract current user's company ID
- **Returns**: Long (company_id)
- **When to use**: ALL tenant-scoped controller/service methods
- **Throws**: For owner-only contexts (no tenant context)

### getUserFromContext()
- **Usage**: Get full User entity for current authenticated user
- **Returns**: User entity
- **When to use**: When you need user details beyond just company_id

### getCurrentUsername()
- **Usage**: Get username string for current authenticated user
- **Returns**: String (username)
- **When to use**: Logging, audit trails, user-specific operations

### isOwnerContext()
- **Usage**: Check if current session is owner (not tenant user)
- **Returns**: Boolean
- **When to use**: Safe callable from any context to distinguish owner vs tenant

## 2.3 Critical Rules
- **NEVER** accept `company_id` from request parameters in tenant-facing endpoints
- **NEVER** manually construct tenant context
- **ALWAYS** use `SecurityContextHelper.getCompanyIdFromContext()` for tenant-scoped logic
- Owner sessions have NO tenant context - `SecurityContextHelper` throws for owner-only operations

---

# 3. OWNER VS TENANT USER SEPARATION

## 3.1 Owner Model
- Owner is **NOT** a tenant user
- Owner has virtual `ROLE_OWNER` injected by `CustomUserDetailsService` from `app.owner.*` properties
- Owner sessions have no `company_id` context
- Owner can review and approve/reject companies but cannot access tenant data

## 3.2 Tenant User Model
- Tenant users belong to a specific company
- Must have valid `company_id` in their user record
- Can only access data belonging to their company
- Login blocked when company status is `PENDING`, `REJECTED`, or inactive

## 3.3 Owner Console Credentials
Configured via:
- `OWNER_EMAIL`
- `OWNER_USERNAME`
- `OWNER_PASSWORD` (accepted raw or Spring-encoded `{id}...` format)

---

# 4. COMPANY ONBOARDING WORKFLOW

## 4.1 Registration Flow
1. Company registration creates first admin transactionally (`CompanyService.registerCompanyWithAdmin`)
2. Company status starts as `PENDING`
3. Owner reviews company via `/owner/**` endpoints
4. Owner approves/rejects via `OwnerController -> OwnerManagementService`

## 4.2 Company Status States
- `PENDING` - Awaiting owner approval (users cannot login)
- `APPROVED` - Active (users can login)
- `REJECTED` - Rejected by owner (users cannot login)
- `SUSPENDED` - Suspended by owner (users cannot login)

## 4.3 Login Blocking
`CustomUserDetailsService` blocks login when:
- Company status is `PENDING`
- Company status is `REJECTED`
- Company is inactive

Implemented via: `CompanyService.canUsersLogin`

---

# 5. REPOSITORY PATTERNS (CRITICAL)

## 5.1 Tenant-Aware Query Methods
**ALWAYS** use company-scoped repository methods:
- `findByCompanyId(Long companyId)`
- `findByIdAndCompanyId(Long id, Long companyId)`
- `findByCompanyIdAndDateRange(...)`

## 5.2 Forbidden Patterns
**NEVER** use unfiltered methods in tenant-facing paths:
- ❌ `findAll()` - returns all companies' data
- ❌ `findById(Long id)` - without company check

## 5.3 Repository Pattern Examples

### Good (Tenant-Safe)
```java
// Repository
List<Order> findByCompanyId(Long companyId);
Optional<Order> findByIdAndCompanyId(Long id, Long companyId);

// Service
Long companyId = securityContextHelper.getCompanyIdFromContext();
return orderRepository.findByCompanyId(companyId);
```

### Bad (Cross-Tenant Risk)
```java
// Repository
List<Order> findAll();

// Service
return orderRepository.findAll(); // LEAKS ALL COMPANIES' DATA
```

---

# 6. CONTROLLER PATTERNS

## 6.1 Tenant-Scoped Controllers
All tenant-facing controllers MUST:
1. Extract `companyId` via `SecurityContextHelper.getCompanyIdFromContext()`
2. Pass `companyId` to service methods
3. Never accept `company_id` from request body/params

### Template
```java
@GetMapping
public String listOrders(Model model) {
    Long companyId = securityContextHelper.getCompanyIdFromContext();
    List<Order> orders = orderService.getOrdersByCompanyId(companyId);
    model.addAttribute("orders", orders);
    return "order/list";
}
```

## 6.2 Owner-Only Controllers
Owner controllers (`/owner/**`) operate without tenant context:
- Do NOT call `SecurityContextHelper.getCompanyIdFromContext()`
- Work with all companies for review/approval
- Use `CompanyRepository` and `UserRepository` directly

## 6.3 API Controllers
`OrderRestController` is API-oriented and does NOT enforce tenant context by itself:
- Route new tenant-facing reads/writes through `SecurityContextHelper`
- Use `...ByCompanyId(...)` service/repository methods
- Keep JSON/AJAX state updates on `/api/**` (CSRF ignored)

---

# 7. SERVICE LAYER PATTERNS

## 7.1 Tenant-Aware Service Methods
All service methods that operate on business data MUST:
- Accept `companyId` as parameter (or derive internally via SecurityContextHelper)
- Pass `companyId` to repository methods
- Validate that operations are scoped to the correct company

### Template
```java
public List<Order> getOrdersByCompanyId(Long companyId) {
    return orderRepository.findByCompanyId(companyId);
}

public Order getOrderByIdAndCompanyId(Long id, Long companyId) {
    return orderRepository.findByIdAndCompanyId(id, companyId)
        .orElseThrow(() -> new OrderNotFoundException(id));
}
```

## 7.2 Transactional Operations
When creating entities with relationships:
- Ensure all related entities belong to the same `companyId`
- Validate `companyId` consistency before persisting

---

# 8. DOMAIN-SPECIFIC RULES

## 8.1 Client Deletion Guard
- `ClientService.deleteClient(id)` throws `ClientHasActiveOrdersException` when orders reference the client
- Exception carries `orderCount` for user feedback
- `ClientController` surfaces this as flash error via `RedirectAttributes`

## 8.2 Order Status Management
- `OrderStatus` enum carries `isFinal()`, `isActive()`, `getDisplayName()`, `getBadgeColor()`
- Use `isFinal()` for status-gate logic instead of direct enum comparisons
- Dashboard status updates via `axios.patch('/api/orders/{id}', { status: '<ENUM_NAME>' })`
- Payload status values must be enum names (`OrderStatus.name()`)

## 8.3 Input Formatting Conventions
- Uppercase: company names, client names, vendor names
- Title-case: contact names, product names (via `utils/Helper.toTitleCase`)
- Applied in service layer, not controller

## 8.4 Date Range Defaults
- `DashboardController` and `OrderController` default to past 1 month when `startDate`/`endDate` absent
- Prefer `OrderService.getOrdersByCompanyIdAndDateRange()` over `getOrdersByCompanyId()`

---

# 9. SECURITY RULES

## 9.1 CSRF Configuration
- CSRF enabled for MVC by default
- Explicitly ignored for `/api/**` in `SecurityConfig`
- Keep JSON/AJAX state updates on `/api/**`
- Keep non-API form posts CSRF-protected

## 9.2 Access Denied Handling
- `SecurityConfig` registers context-aware `AccessDeniedHandler`
- Redirects to `/access-denied?reason=suspended|pending-approval|rejected|forbidden`
- `ErrorPageController` resolves `reason` param to user-facing messages
- Successful login routes: owner → `/owner/dashboard`, tenant → `/dashboard`

## 9.3 User Management
- Public self-registration disabled; `/signup` redirects to login
- New users created exclusively by admins via `UserService.createUserByAdmin(user, companyId)`
- Admin-initiated email updates reset `enabled` to `false` and trigger re-verification
- Admin user management enforces: min 1 admin/company, max 2 admins/company

---
# 10. DATA CONSISTENCY RULES

## MUST:
- Validate before save
- Prevent partial writes

---

## Edge Cases:
- Concurrent updates
- Duplicate submissions

---

# 11. CONCURRENCY CONTROL

## Problem:
Two users editing same PO

## Solution:
- Optimistic locking
- Version field

---

# 12. SOFT DELETE ENFORCEMENT

Use:
```java
boolean isDeleted;
```

All queries must:
```sql
WHERE is_deleted = false
```

---

# 13. ANTI-PATTERNS (STRICTLY FORBIDDEN)

❌ Missing tenant filter  
❌ Using request companyId  
❌ Direct status update  
❌ Deleting referenced data  
❌ Inconsistent formatting


---

# 14. ENFORCEMENT CHECKLIST

Before merge:

- [ ] All business entities have `company_id` field
- [ ] Controllers use `SecurityContextHelper.getCompanyIdFromContext()`
- [ ] Service methods accept/pass `companyId` parameter
- [ ] Repository methods are tenant-scoped (`findByCompanyId`, `findByIdAndCompanyId`)
- [ ] No unfiltered `findAll()` in tenant-facing paths
- [ ] No `company_id` accepted from request parameters
- [ ] Owner sessions handled correctly (no tenant context)
- [ ] Cross-tenant access tests added for new endpoints

---

# FINAL RULE

This document is enforceable.

If any guideline is violated:
👉 REJECT PR
