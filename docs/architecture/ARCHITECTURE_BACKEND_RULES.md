
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
