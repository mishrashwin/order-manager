# MULTI-TENANCY ARCHITECTURE OVERVIEW

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         USER LAYER                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Browser/Client Requests                                            │
│  └─→ /company/register  (Public)                                    │
│  └─→ /signup            (Public)                                    │
│  └─→ /verify            (Public)                                    │
│  └─→ /login             (Public)                                    │
│  └─→ /dashboard         (Protected)                                 │
│  └─→ /orders            (Protected, Company-Filtered)               │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    SECURITY & ROUTING LAYER                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Spring Security FilterChain                                        │
│  ├─ CSRF Disabled                                                   │
│  ├─ Public URLs: /login, /signup, /company/register, /verify       │
│  ├─ Protected URLs: All others require authentication               │
│  └─ Form Login: /login POST                                         │
│                                                                       │
│  Authentication Flow:                                               │
│  ├─ Submit credentials (/login)                                     │
│  ├─ DaoAuthenticationProvider validates                             │
│  ├─ CustomUserDetailsService.loadUserByUsername()                   │
│  ├─ User entity loaded with Company (EAGER)                         │
│  ├─ Spring sets SecurityContext                                     │
│  └─ User is now authenticated with company context                  │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    CONTROLLER LAYER (Endpoints)                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  Public Controllers:                                                │
│  ├─ CompanyController.registerCompany()                             │
│  │  └─ Creates company, redirects to signup                         │
│  │                                                                   │
│  ├─ AuthController.signupPage()                                     │
│  │  └─ Shows form with company context                              │
│  │                                                                   │
│  └─ AuthController.registerUser()                                   │
│     └─ Associates user with company                                 │
│                                                                       │
│  Protected Controllers:                                             │
│  ├─ DashboardController                                             │
│  │  ├─ Calls SecurityContextHelper.getCompanyIdFromContext()        │
│  │  ├─ Passes company ID to service layer                           │
│  │  └─ All data filtered by company                                 │
│  │                                                                   │
│  ├─ AdminController (NEW - Admin Panel)                             │
│  │  ├─ listUsers() - Shows company users with admin count           │
│  │  ├─ showAddUserForm() - Add user form                            │
│  │  ├─ addUser() - Create user with company assignment              │
│  │  ├─ showEditUserForm() - Edit user form (role management)        │
│  │  ├─ updateUserRole() - Update user role (max 2 admins enforced) │
│  │  ├─ deleteUser() - Delete with validation (min 1 admin required) │
│  │  └─ All operations enforce company ownership                     │
│  │                                                                   │
│  ├─ OrderController                                                 │
│  │  ├─ Extract company from auth context                            │
│  │  ├─ Set company on entities before saving                        │
│  │  └─ Verify ownership on reads/updates/deletes                    │
│  │                                                                   │
│  └─ ClientController, VendorController (similar pattern)            │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                UTILITY LAYER - TENANT CONTEXT                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  SecurityContextHelper                                              │
│  ├─ getCompanyIdFromContext()                                       │
│  │  ├─ Reads from Spring Security context                           │
│  │  ├─ Looks up user in database                                    │
│  │  ├─ Returns user's company_id                                    │
│  │  └─ ✅ NEVER accepts from request parameters                     │
│  │                                                                   │
│  ├─ getUserFromContext()                                            │
│  │  ├─ Returns full User entity with Company                        │
│  │  └─ All user's company data available                            │
│  │                                                                   │
│  └─ getCurrentUsername()                                            │
│     ├─ Returns username of authenticated user                       │
│     └─ Used for identifying current user in UI (e.g., "(You)" badge)│
│                                                                       │
│  This is the TRUST ANCHOR - enforces tenant isolation               │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    SERVICE LAYER (Business Logic)                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  CompanyService                                                     │
│  ├─ registerCompany()                                               │
│  ├─ getCompanyById()                                                │
│  └─ deactivateCompany()                                             │
│                                                                       │
│  RegistrationService (Updated)                                      │
│  ├─ registerUserWithCompany()                                       │
│  │  ├─ Validates company exists                                     │
│  │  ├─ Sets user.company reference                                  │
│  │  └─ Saves user with company assignment                           │
│  │                                                                   │
│  └─ sendVerificationEmail()                                         │
│     └─ Creates verification token                                   │
│                                                                       │
│  UserService (Admin Management)                                     │
│  ├─ createUserByAdmin() - Admin creates user with company           │
│  ├─ getUsersByCompany() - List users in company                     │
│  ├─ deleteUserByAdmin() - Delete with validation (min 1 admin)      │
│  ├─ updateUserRole() - Update role (max 2 admins enforced)          │
│  ├─ getAdminCountInCompany() - Count admins in company              │
│  └─ isLastAdminInCompany() - Check if user is last admin            │
│                                                                       │
│  OrderService (Company-Aware)                                       │
│  ├─ getOrdersByCompanyId(Long companyId) ✅ TENANT-AWARE            │
│  ├─ createOrder()                                                   │
│  ├─ updateOrder()                                                   │
│  └─ deleteOrder()                                                   │
│                                                                       │
│  ClientService (Similar pattern)                                    │
│  └─ All methods filter by company                                   │
│                                                                       │
│  VendorService (Similar pattern)                                    │
│  └─ All methods filter by company                                   │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                 REPOSITORY LAYER (Data Access)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  CompanyRepository                                                  │
│  ├─ save()                                                          │
│  ├─ findById()                                                      │
│  └─ findByName()                                                    │
│                                                                       │
│  OrderRepository (Enhanced)                                         │
│  ├─ findAll() ⚠️ DEPRECATED (returns unfiltered)                   │
│  ├─ findByCompanyId() ✅ USE THIS                                   │
│  └─ findById()                                                      │
│                                                                       │
│  ClientRepository (Enhanced)                                        │
│  ├─ findAll() ⚠️ DEPRECATED                                        │
│  ├─ findByCompanyId() ✅ USE THIS                                   │
│  └─ findById()                                                      │
│                                                                       │
│  VendorRepository (Enhanced)                                        │
│  ├─ findAll() ⚠️ DEPRECATED                                        │
│  ├─ findByCompanyId() ✅ USE THIS                                   │
│  └─ findById()                                                      │
│                                                                       │
│  UserRepository                                                     │
│  └─ findByUsername() - Returns user with company loaded             │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    DATABASE LAYER (Persistence)                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  MySQL Database Tables:                                             │
│                                                                       │
│  companies (Tenant Master)                                          │
│  ├─ id (PK)                                                         │
│  ├─ name (UNIQUE)                                                   │
│  ├─ created_at                                                      │
│  └─ active                                                          │
│                                                                       │
│  users                                                              │
│  ├─ id (PK)                                                         │
│  ├─ username (UNIQUE)                                               │
│  ├─ email (UNIQUE)                                                  │
│  ├─ password                                                        │
│  ├─ role (USER, MANAGER, ADMIN) ✅ DEFAULT 'USER'                  │
│  ├─ company_id (FK → companies) ✅ REQUIRED                        │
│  └─ enabled                                                         │
│                                                                       │
│  orders                                                             │
│  ├─ id (PK)                                                         │
│  ├─ customer_name                                                   │
│  ├─ status                                                          │
│  ├─ company_id (FK → companies) ✅ REQUIRED                        │
│  └─ ... other fields                                                │
│                                                                       │
│  clients                                                            │
│  ├─ id (PK)                                                         │
│  ├─ name                                                            │
│  ├─ company_id (FK → companies) ✅ REQUIRED                        │
│  └─ ... other fields                                                │
│                                                                       │
│  vendors                                                            │
│  ├─ id (PK)                                                         │
│  ├─ company_name                                                    │
│  ├─ company_id (FK → companies) ✅ REQUIRED                        │
│  └─ ... other fields                                                │
│                                                                       │
│  verification_token                                                 │
│  ├─ id (PK)                                                         │
│  ├─ token (UNIQUE)                                                  │
│  ├─ user_id (FK → users)                                            │
│  └─ expiry_date                                                     │
│                                                                       │
│  password_reset_token                                               │
│  ├─ id (PK)                                                         │
│  ├─ code (6-digit)                                                  │
│  ├─ user_id (FK → users)                                            │
│  ├─ expiry_date (15 min expiry)                                     │
│  ├─ verified (one-time use flag)                                    │
│  └─ created_at                                                      │
│                                                                       │
│  ✅ All business data tied to company via FK                       │
│  ✅ Cascade delete prevents orphaned records                        │
│  ✅ Indexes on company_id for fast queries                         │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## KEY COMPONENTS AT A GLANCE

| Component | Purpose | Location |
|-----------|---------|----------|
| Company Entity | Tenant master | entity/Company.java |
| SecurityContextHelper | Extracts company from auth | utils/SecurityContextHelper.java |
| CompanyService | Company management | service/CompanyService.java |
| CompanyController | Company endpoints | controller/CompanyController.java |
| CompanyRepository | Company data access | repository/CompanyRepository.java |

---

---

**Phase 1: Complete Multi-Tenancy Foundation** ✅  
**Phase 2: Authentication & User Management** ✅  
**Phase 3: Business Operations & Dashboard** ✅

---

## 📝 RECENT CHANGES (Canonical Changelog)

### **2026-02-28: Admin Management Rules & UI Enhancements**
- **Added**: Complete admin management rules with multi-layer protection
- **Business Rules Implemented**:
  - Admin can edit their own account details (no restrictions)
  - Admin cannot delete themselves if they are the only admin
  - Maximum 2 admins per company (hard limit enforced)
  - If 2 admins exist, each can delete/edit the other
  - Minimum 1 admin must always exist per company
- **UI Enhancements**:
  - Delete button auto-disables for self-admin (only admin scenario)
  - "(You)" badge displays next to current user in admin list
  - Admin count display: "Admin Count: X/2" with warning at max
  - Helpful tooltips on disabled buttons
  - CSS styling for disabled buttons (greyed out, not-allowed cursor)
- **New Template**: `admin/users/form-edit.html` - Edit user page with read-only fields
  - Editable: Role (USER, MANAGER, ADMIN)
  - Read-only: Name, Email, Username, Mobile Number
  - Status badge: Shows verification status
- **Files Modified**: 
  - `UserService.java` - Enhanced delete/role update with validation
  - `AdminController.java` - Refined delete logic, passes adminCount
  - `admin/users/list.html` - Delete button conditional logic with Thymeleaf operators
  - `SecurityContextHelper.java` - Added `getCurrentUsername()` method
- **Security**: Multi-layer protection (UI, Controller, Service) prevents invalid operations
- **Thymeleaf Syntax**: Fixed parsing errors by using proper operators (`and`, `le`) and `th:attrappend`

### **2026-02-27: Password Reset Security Fix**
- **Fixed**: Added `/forgot-password`, `/verify-reset-code`, `/reset-password` to Spring Security's `permitAll()` list
- **Impact**: Password reset flow now works properly (was redirecting to login with HTTP 302)
- **Files Modified**: `SecurityConfig.java`
- **Feature**: 3-step password reset flow now fully functional
  - Step 1: Request 6-digit code via email
  - Step 2: Verify code (15-minute expiry)
  - Step 3: Set new password with BCrypt hashing
- **Security**: One-time use tokens, verified flag prevents replay attacks

### **2026-02-26: Password Reset Feature Implementation**
- **Added**: Complete forgot password functionality with 6-digit codes
- **New Entities**: `PasswordResetToken` (code, user_id, expiry_date, verified, created_at)
- **New Service**: `PasswordResetService` with code generation, verification, and password reset
- **New Templates**: 
  - `forgot-password.html` - Request reset code
  - `verify-reset-code.html` - Verify 6-digit code (with countdown timer)
  - `reset-password.html` - Set new password
- **Email Integration**: Password reset codes sent via EmailService
- **Database Migration**: `V2__add_password_reset_tokens.sql`
- **Security Features**:
  - 6-digit random codes (100000-999999)
  - 15-minute code expiry
  - Two-step verification (code verification + password reset)
  - One-time use tokens (deleted after successful reset)
  - BCrypt password hashing

### **2026-02-25: Admin Panel Implementation**
- **Added**: Complete admin panel for company user management
- **New Controller**: `AdminController` with RBAC (`@PreAuthorize("hasRole('ADMIN')")`)
- **New Endpoints**:
  - `GET /admin/dashboard` - Admin overview
  - `GET /admin/users` - List company users (with admin count and current user indicator)
  - `GET /admin/users/add` - Add user form
  - `POST /admin/users/add` - Create user (auto-assigns admin's company)
  - `GET /admin/users/{id}/edit` - Edit user form (role management)
  - `POST /admin/users/{id}/role` - Update user role
  - `POST /admin/users/{id}/delete` - Delete user (with validation)
  - `POST /admin/users/{id}/resend-verification` - Resend verification email
- **New Templates**: 
  - `admin/dashboard.html` - Admin overview with quick links
  - `admin/users/list.html` - Users table with conditional delete button
  - `admin/users/form.html` - Add user form
  - `admin/users/form-edit.html` - Edit user form (read-only fields, editable role)
- **Business Rules**:
  - Minimum 1 admin required per company
  - Maximum 2 admins allowed per company
  - Admin can edit own account but cannot delete self if only admin
  - Delete button conditionally disabled with visual feedback
- **Security**: All admin operations enforce company ownership via `SecurityContextHelper`
- **Email Verification**: Admin-created users receive verification emails

### **2026-02-24: Tenant-Aware Entity Management**
- **Enhanced**: Implemented tenant-aware company assignment for all business entities (Client, Vendor, Order)
- **Pattern**: Controllers now extract company ID from authenticated user via `SecurityContextHelper` 
- **Impact**: Services set `company` relationship before saving, preventing `not-null property references null` errors
- **Files Modified**: `OrderController`, `ClientController`, `VendorController`, respective services
- **Security**: Users cannot create entities in other companies' contexts

### **2026-02-24: DTO Validation Enhancement**
- **Added**: DTO-based validation for company registration
- **New DTO**: `CompanyRegistrationDTO` with Jakarta Validation annotations
- **Enhanced**: Mobile number validation with specific error messages
  - Frontend: HTML5 pattern validation
  - Backend: Custom validation in controller
  - Consistent error messages across UI and API
- **User Experience**: Clear, actionable validation feedback

### **2026-02-24: Database Schema Cleanup**
- **Action**: Database cleaned and schema reinitialized
- **Purpose**: Remove legacy orphaned rows preventing FK constraint addition
- **Script**: `CLEAN_DATABASE.sql` created for reference
- **Result**: All foreign key constraints successfully added
- **Impact**: Referential integrity now enforced at database level

### **2026-02-20: Email Verification System**
- **Added**: Email verification for user registration
- **New Entities**: `VerificationToken` with 24-hour expiry
- **New Service**: `RegistrationService` for token management
- **New Service**: `EmailService` with SMTP integration (Gmail/Brevo)
- **New Endpoints**: 
  - `GET /verify?token=...` - Email verification
  - `GET /resend-verification` - Resend verification email
- **Security**: Users cannot login until email verified (`enabled=false` by default)
- **Templates**: `resend-verification.html` for expired tokens

### **2026-02-18: Multi-Tenant Foundation**
- **Implemented**: Complete multi-tenant architecture with company-based isolation
- **New Entity**: `Company` (id, name, contact_person, email, mobile_number, created_at, active)
- **Enhanced Entity**: `User` with company foreign key (EAGER loaded)
- **Enhanced Entities**: `Order`, `Client`, `Vendor` with company foreign key
- **New Utility**: `SecurityContextHelper` for tenant context extraction
- **Pattern**: Company ID always extracted from auth context, never from request
- **Security**: Complete data isolation between companies
- **Repositories**: Added company-aware query methods (`findByCompany_Id`)

### **2026-02-15: Company Registration Flow**
- **Added**: Public company registration endpoint
- **New Controller**: `CompanyController` with registration logic
- **New DTO**: `CompanyRegistrationDTO` for validated input
- **Process**: 
  1. Company creation
  2. Admin user creation with ADMIN role
  3. User-company association
  4. Email verification flow
- **Template**: `company/register.html` with Bootstrap 5 design
- **Validation**: DTO-level validation with error feedback

### **2026-02-10: Initial Project Setup**
- **Created**: Spring Boot 3.x application with MySQL integration
- **Dependencies**: Spring Web, Spring Data JPA, Thymeleaf, Spring Security, MySQL Driver
- **Entities**: `Order`, `Client`, `Vendor` (basic structure)
- **Controllers**: CRUD operations for orders, clients, vendors
- **Security**: Basic Spring Security configuration
- **Templates**: Thymeleaf templates with Bootstrap 5
- **Database**: MySQL with Hibernate DDL auto-update

---

## 📚 DOCUMENTATION POLICY

**Primary Documentation:**
- `ARCHITECTURE_OVERVIEW.md` - System architecture, flows, and technical design (this file)
- `README.md` - Project overview, quick start, and feature summary
- `GETTING_STARTED.md` - Detailed setup guide and environment configuration

**Feature-Specific Documentation:**
- `COMPANY_CREATION_GUIDE.md` - Step-by-step company registration
- `FORGOT_PASSWORD_FIX_SUMMARY.md` - Password reset feature documentation
- `SECURITY_CREDENTIALS_GUIDE.md` - Security best practices and credential management

**Update Guidelines:**
1. **When adding features**: Update `ARCHITECTURE_OVERVIEW.md` "Recent changes" section first
2. **When fixing bugs**: Document the fix, impact, and files modified
3. **Avoid proliferation**: Don't create per-change markdown files for minor updates
4. **Create new docs only for**: Substantial features, public APIs, or major architecture changes
5. **Keep README concise**: Point readers to this file for technical details

---

## 🎯 FUTURE ENHANCEMENTS

### **Potential Features**
- [ ] **API Rate Limiting** - Prevent brute force attacks on login/password reset
- [ ] **Two-Factor Authentication** - SMS or TOTP-based 2FA
- [ ] **Audit Logging** - Track all data modifications with user attribution
- [ ] **File Upload** - Attach documents to orders
- [ ] **Export Functionality** - CSV/PDF export of orders, clients, vendors
- [ ] **Advanced Search** - Full-text search across entities
- [ ] **Dashboard Analytics** - Charts and graphs for order trends
- [ ] **REST API** - Complete REST API for mobile/external integrations
- [ ] **Webhook Support** - Event notifications to external systems
- [ ] **Multi-Language** - i18n support for international users

### **Performance Optimizations**
- [ ] **Caching** - Redis for session storage and frequently accessed data
- [ ] **Query Optimization** - Review N+1 queries, add missing indexes
- [ ] **Pagination** - Implement pagination for large data sets
- [ ] **Lazy Loading** - Review EAGER fetch strategies

### **DevOps**
- [ ] **Docker** - Containerize application and database
- [ ] **CI/CD** - Automated testing and deployment pipeline
- [ ] **Monitoring** - Application performance monitoring (APM)
- [ ] **Health Checks** - Spring Boot Actuator endpoints

---

## 🏆 BEST PRACTICES IMPLEMENTED

✅ **Security**
- BCrypt password hashing (never plain text)
- Email verification before login
- Secure password reset with time-limited codes
- Role-based access control (RBAC)
- CSRF protection (can be enabled for production)
- SQL injection prevention (parameterized queries via JPA)

✅ **Multi-Tenancy**
- Complete data isolation between companies
- Company context extracted from auth, never from requests
- Foreign key constraints enforce referential integrity
- Company-aware query methods in all repositories

✅ **Admin Management Rules**
- Minimum 1 admin per company (prevents admin deletion if only one)
- Maximum 2 admins per company (hard limit enforced at service layer)
- Admin self-management (can edit own details)
- Admin cross-management (can edit/delete other admins if multiple exist)
- Multi-layer protection (UI disable, Controller validation, Service validation)
- Visual feedback (disabled buttons, tooltips, badges, admin count display)

✅ **Code Quality**
- Service layer separation (business logic)
- Repository pattern (data access)
- DTO validation (input validation)
- Exception handling (GlobalExceptionHandler)
- Consistent naming conventions
- Google Java Style Guide formatting

✅ **User Experience**
- Flash messages for user feedback
- Responsive design (Bootstrap 5)
- Form validation (client + server)
- Clear error messages
- Email notifications
- Visual indicators for current user
- Helpful tooltips on disabled actions

✅ **Maintainability**
- Comprehensive documentation
- Clear architecture diagrams
- Changelog maintained
- Configuration externalized (application.properties)
- Environment variables for secrets

---

## 🔐 ADMIN BUSINESS RULES (Detailed)

### Rule Set for Admin Management

**Rule 1: Minimum Admin Requirement**
```
At least 1 admin must always exist per company
├─ Cannot delete the last admin
├─ Cannot demote the last admin to lower role
└─ Error message: "Cannot delete the last admin in the company..."
```

**Rule 2: Maximum Admin Limit**
```
Maximum 2 admins allowed per company
├─ Cannot promote 3rd user to admin role
├─ Error message: "Cannot create more than 2 admins per company. Current admins: X"
└─ Admin count display shows warning when limit reached
```

**Rule 3: Self-Management**
```
Admin can manage their own account
├─ Can edit own details (unrestricted)
├─ Can delete self ONLY if NOT the only admin
├─ Delete button auto-disables if only admin
└─ "(You)" badge indicates current user
```

**Rule 4: Cross-Management**
```
Admin can manage other admins
├─ Can edit other admin's role
├─ Can delete other admin (if 2 exist, min 1 remains)
├─ Can demote other admin to USER/MANAGER
└─ All operations validated at multiple layers
```

### Protection Layers

| Layer | Implementation | Purpose |
|-------|----------------|---------|
| **UI** | `th:disabled` on buttons | Visual prevention |
| **Template** | Conditional rendering | Hide/show elements |
| **Controller** | Self-deletion check | Request validation |
| **Service** | Business rule enforcement | Core validation |
| **Database** | FK constraints | Data integrity |

### Scenarios

**Scenario A: Single Admin (Admin A only)**
- ✅ Can edit own account
- ❌ Cannot delete self (button disabled)
- ✅ Can delete other users (non-admins)
- ✅ Can promote user to admin (becomes 2)

**Scenario B: Two Admins (Admin A + Admin B)**
- ✅ Admin A can delete Admin B (A remains)
- ✅ Admin B can delete Admin A (B remains)
- ✅ Both can edit each other
- ❌ Cannot promote 3rd user to admin

**Scenario C: One Admin + Users (Admin A + User B + User C)**
- ✅ Admin A can delete User B or C anytime
- ✅ Admin A can promote User B (becomes 2 admins)
- ❌ Cannot promote User C if B already promoted

---

## 🏆 BEST PRACTICES IMPLEMENTED

✅ **Security**
- BCrypt password hashing (never plain text)
- Email verification before login
- Secure password reset with time-limited codes
- Role-based access control (RBAC)
- CSRF protection (can be enabled for production)
- SQL injection prevention (parameterized queries via JPA)

✅ **Multi-Tenancy**
- Complete data isolation between companies
- Company context extracted from auth, never from requests
- Foreign key constraints enforce referential integrity
- Company-aware query methods in all repositories

✅ **Code Quality**
- Service layer separation (business logic)
- Repository pattern (data access)
- DTO validation (input validation)
- Exception handling (GlobalExceptionHandler)
- Consistent naming conventions
- Google Java Style Guide formatting

✅ **User Experience**
- Flash messages for user feedback
- Responsive design (Bootstrap 5)
- Form validation (client + server)
- Clear error messages
- Email notifications

✅ **Maintainability**
- Comprehensive documentation
- Clear architecture diagrams
- Changelog maintained
- Configuration externalized (application.properties)
- Environment variables for secrets

---

**Last Updated**: 2026-02-28  
**Document Version**: 2.1  
**Status**: Production Ready ✅
