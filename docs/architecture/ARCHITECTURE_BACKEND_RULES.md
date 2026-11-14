
# ARCHITECTURE & BACKEND RULES (ULTRA-DETAILED – FINAL VERSION)

## PURPOSE
This document is the **authoritative engineering contract** for backend development in the Order Manager system.

It ensures:
- Deterministic behavior
- Strict separation of concerns
- Production-grade scalability
- Fault tolerance
- Observability
- Security

Violation of any rule → **DO NOT MERGE**

---

# 1. SYSTEM DESIGN PRINCIPLES

## 1.1 Clean Architecture Alignment
System follows:
- Presentation Layer (Controller)
- Application Layer (Service)
- Domain Layer (Business rules)
- Infrastructure Layer (Repository, DB)

Rule:
👉 Dependencies must always point inward (Controller → Service → Domain → Infra)

---

## 1.2 Single Responsibility Principle (SRP)
Each class must have ONE reason to change.

Bad:
- Service handling validation + DB + formatting

Good:
- ValidationService
- VendorPoService
- PricingService

---

## 1.3 Separation of Concerns
STRICT separation:
- UI logic → Controller/View
- Business logic → Service
- Persistence → Repository

---

# 2. REQUEST LIFECYCLE (DETAILED)

## Step-by-step Flow:

1. HTTP Request received
2. Controller maps to DTO
3. DTO validated (annotation-level)
4. Service invoked
5. Business validation executed
6. Repository interaction
7. Entity persisted
8. Response DTO created
9. Response returned

---

# 3. CONTROLLER DESIGN (ADVANCED)

## Rules:
- Must be thin
- Must not contain loops/conditions for business rules
- Must not transform entities

## Template:
```
@PostMapping
public ResponseEntity<ApiResponse> create(@Valid @RequestBody CreateDto dto) {
    return ResponseEntity.ok(service.create(dto));
}
```

---

# 4. SERVICE DESIGN (CORE LOGIC)

## Rules:
- All business rules here
- Must be stateless
- Must be idempotent where required

## Idempotency Example:
- Creating same order twice should not duplicate data

---

## Transaction Boundary
```
@Transactional
public void createOrder(...)
```

Rules:
- Only at service layer
- Avoid nested transactions unless required
- For bulk work requiring row-level failure isolation, keep each row write within its repository/service transaction; record the row outcome and continue after a failed row. Do not wrap the whole batch in one transaction unless all-or-nothing behavior is intended.
- Catch recoverable row failures only after the row transaction has rolled back (for example, repository integrity violations). Unexpected persistence/runtime failures must abort the batch response rather than produce partial counts that imply the batch completed reliably.
- For externally retried bulk writes, claim a tenant-scoped idempotency key under a database unique constraint and commit that claim before processing. Bind the key to a content fingerprint, persist the final response, and never automatically reclaim unresolved `IN_PROGRESS` work unless each committed row has a durable atomic checkpoint.

### Spreadsheet Upload Validation
- Enforce configured multipart byte limits before controller processing and validate the workbook's actual OOXML content; never trust the filename extension or MIME header alone.
- Validate required worksheet headers and cap populated data rows before mapping records. Do not evaluate formulas from user-uploaded workbooks; reject unsupported cell types instead of coercing them into valid-looking values.
- Return controlled file-level errors separately from row-level business validation errors, without exposing parser internals.
- Bulk import normalizes one DTO representation before field validation and duplicate lookups; preview and registration must both reparse and run the same normalization/validation path. Keep those normalizations scoped to bulk import unless single-record behavior is deliberately changed.

---

# 5. DTO MAPPING STRATEGY

## Options:
- Manual mapping (recommended for control)
- MapStruct (for large systems)

## Rules:
- Never expose Entity
- Always convert both ways

---

# 6. ERROR HANDLING (PRODUCTION GRADE)

## Global Exception Handler

```
@ControllerAdvice
public class GlobalExceptionHandler {}
```

## Categories:
- ValidationException
- BusinessException
- SystemException

---

# 7. LOGGING STANDARD

## Requirements:
- Every request must have correlationId
- Log format must be structured

Example:
```
[correlationId] [service] [method] [status]
```

---

## Logging Levels:
- INFO → business events
- WARN → unexpected but recoverable
- ERROR → system failure

---

# 8. PERFORMANCE ENGINEERING

## MUST:
- Use pagination
- Use indexes on DB
- Avoid N+1 queries

## Lazy vs Eager:
- Default → Lazy
- Use eager only when needed
- When a Spring Data JPA `@EntityGraph` is used, explicitly include every association read by the view or service after the repository call. Fetch graphs can leave omitted relationships unavailable after the persistence context closes, even when the entity mapping declares them eager.

---

## Caching Strategy
- Use caching for read-heavy data
- Invalidate cache on updates

---

# 9. DATABASE STRATEGY

## Flyway Rules:
- Versioned scripts
- No manual DB edits

---

## Naming:
- snake_case in DB
- camelCase in Java

---

# 10. SECURITY HARDENING

## Rules:
- Validate all inputs
- Never trust client-side validation
- Enforce role-based access in backend

---

## CSRF:
- Enabled for MVC
- Disabled for /api/**

---

# 11. CONCURRENCY & DATA INTEGRITY

## Scenarios:
- Multiple users editing same PO
- Duplicate submissions

## Solutions:
- Optimistic locking
- Version fields

---

# 12. FAILURE SCENARIOS

## Must handle:
- DB failure mid-transaction
- Partial updates
- Network failure
- Document/PDF rendering must treat an absent optional collection as empty when an empty section is valid, rather than failing during iteration.

---

# 13. API VERSIONING

## Strategy:
- /api/v1/
- Backward compatibility maintained

---

# 14. ANTI-PATTERNS (STRICT)

❌ Fat controllers  
❌ Business logic duplication  
❌ Direct DB exposure  
❌ Missing validation  
❌ Ignoring transaction boundaries  

---

# 15. ENFORCEMENT CHECKLIST

Before merge:

- [ ] Controller thin
- [ ] DTO used
- [ ] Service owns logic
- [ ] Validation complete
- [ ] Logging added
- [ ] Performance considered
- [ ] Tests written

---

# FINAL RULE

This document is enforceable.

If any guideline is violated:
👉 REJECT PR
# Vendor PO lifecycle safety

- Vendor PO updates must load the record with `findByIdAndCompany_Id` using the authenticated company context and may mutate it only while its status is `DRAFT`.
- Every non-DRAFT status is temporarily immutable until the dedicated revision workflow exists. MVC handlers must not reset status after update; the service guard is authoritative.

# Company Address rules

- Company Address records are tenant-owned through a required `company_id`; tenant-facing reads and mutations must derive Company ID from `SecurityContextHelper` and use Company-scoped repository queries.
- Serialize address create/default/deactivate operations by locking the owning Company row. Database partial unique indexes enforce at most one active Bill To and Ship To default per Company; application workflows must preserve exactly one of each.
- Keep legacy Company address/GSTIN/state columns intact and do not synchronize later Company profile edits into existing Company Address records.
- Validate address forms both at MVC binding and service boundaries. Require moving default roles before deactivation; never hard-delete address history.
- Activation preserves address fields and never assigns a default. Default deactivation requires explicit active replacements for every role held by that address; Company Details reads only the active default Bill To address and uses its GSTIN with legacy Company GSTIN fallback.
- Drug License Number is optional address metadata; persist and display it for future workflows without consuming it in Vendor PO output until the snapshot phase.
- Bean Validation annotations require a validation provider on the runtime classpath; use `spring-boot-starter-validation` when MVC and service validation are part of a feature.

## Vendor PO snapshot and revision rules
- New and revised POs must resolve Company, Vendor, Product, address, and delivery-term identifiers through Company-scoped repository methods using the authenticated tenant. Resolve default Terms & Conditions from the authenticated Company configuration and persist the resolved text on the PO revision.
- Store issue-time buyer/vendor/address/terms/financial values on the PO revision. Issued PDF generation reads those snapshots and persisted totals; it does not load live Company or Vendor data or calculate tax.
- DRAFT is the only editable PO status. Amendments create a new revision row and retain the old issued PDF. A Company+PO number+revision unique constraint protects numbering.
- Money uses BigDecimal and explicit two-decimal HALF_UP rounding. GST place-of-supply uses Bill To and Vendor GST state codes; missing codes follow the documented inter-state fallback. Charge tax treatment must be explicit.
