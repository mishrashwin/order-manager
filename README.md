# 📦 Order Manager - Multi-Tenant Order Management System

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Java](https://img.shields.io/badge/Java-17+-orange)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

A comprehensive web-based **Order Management System** with multi-tenant architecture, built using Spring Boot 3, Thymeleaf, and PostgreSQL. Designed for businesses to manage orders, clients, vendors, and owner-approved company onboarding with complete tenant isolation.

---

## 🚀 Features

### 🏢 **Multi-Tenancy (Company-Based Isolation)**
- **Complete tenant isolation** - Each company's data is fully separated
- **Secure context extraction** - Company ID extracted from authenticated user (never from request)
- **Automatic data filtering** - All queries filtered by company ID
- **Company onboarding** - Public signup is disabled; companies register through `/company/register` and enter a pending-approval flow before users can log in

### 🔐 **Authentication & Security**
- **BCrypt password hashing** - Industry-standard password encryption
- **Email verification** - Users must verify email before login (24-hour token expiry)
- **Password reset flow** - 3-step secure password reset with 6-digit codes
  - Request reset code → Verify code → Set new password
  - 15-minute code expiry with one-time use
- **Owner approval workflow** - Pending, rejected, and inactive companies cannot authenticate until the owner approves them
- **Role-based access control** - USER, MANAGER, ADMIN roles
- **Spring Security integration** - Form-based authentication with custom failure handling

### 👥 **User Management**
- **Admin panel** - Company admins can create/manage users
- **Admin rules** - Business rules enforced:
  - Minimum 1 admin per company (cannot delete last admin)
  - Maximum 2 admins per company (hard limit)
  - Admin can edit own account but cannot delete self if only admin
  - Delete button auto-disables with visual feedback
- **Role management** - Assign USER, MANAGER, or ADMIN roles
- **Edit users** - Update user roles via admin panel
- **Email verification** - Automated verification emails with token links
- **Resend verification** - Users can resend verification emails if expired
- **Visual indicators** - "(You)" badge shows current user in lists
- **Admin count display** - Shows current admin count (X/2) with warnings

### 📊 **Business Operations**
- **Order Management** - Create, update, delete, and track orders with status workflow
- **Client Management** - Manage customer/client information with company isolation
- **Vendor Management** - Track vendor details and relationships
- **Dashboard** - Real-time overview of orders, clients, and vendors

### 🛠️ **Technical Features**
- **Spring Boot 3** - Modern Java framework with auto-configuration
- **Thymeleaf Templates** - Server-side rendering with Bootstrap 5
- **Spring Data JPA** - Simplified database operations
- **PostgreSQL Database** - Primary runtime database in the committed dev/qa/prod profiles
- **Flyway Migrations** - Version-controlled database schema (optional)
- **DTO Validation** - Input validation with jakarta.validation
- **Email Service** - Brevo API integration via WebClient
- **RESTful API** - JSON endpoints for order operations
- **OpenAPI Dependency** - SpringDoc is included, but Swagger UI is disabled by default in the committed profiles

---

## 📋 Prerequisites

- **Java 17+** (JDK 17 or higher)
- **Maven 3.6+**
- **PostgreSQL** (used by the active dev/qa/prod profiles)
- **Brevo API key** and `MAIL_FROM` value for outbound email

---

## ⚙️ Quick Start

### 1. Clone & Navigate
```powershell
cd F:\AshLabsCompany\order-manager
```

### 2. Configure Database
Create the PostgreSQL database:
```sql
CREATE DATABASE order_manager_db;
```

For the default dev profile, set the database credentials and ensure PostgreSQL is running locally:
```bash
# Linux/macOS
export DB_PASSWORD=your_postgres_password

# Windows (Command Prompt)
set DB_PASSWORD=your_postgres_password

# Windows (PowerShell)
$env:DB_PASSWORD="your_postgres_password"
```

The profile-specific configuration reads the database values from `DB_USERNAME`, `DB_PASSWORD`, and the active profile's datasource URL.

### 3. Configure Email (Required for verification/password reset)
Set the following environment variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `BREVO_API_KEY` | Brevo API token used for outbound mail | `xkeysib-...` |
| `MAIL_FROM` | From address used in sent emails | `noreply@yourdomain.com` |
| `APP_BASE_URL` | Base URL used for links in emails and callbacks | `https://your-app.example.com` |

```bash
# Linux/macOS
export BREVO_API_KEY=your_brevo_api_key
export MAIL_FROM=noreply@yourdomain.com
export APP_BASE_URL=https://your-app.example.com

# Windows (Command Prompt)
set BREVO_API_KEY=your_brevo_api_key
set MAIL_FROM=noreply@yourdomain.com
set APP_BASE_URL=https://your-app.example.com

# Windows (PowerShell)
$env:BREVO_API_KEY="your_brevo_api_key"
$env:MAIL_FROM="noreply@yourdomain.com"
$env:APP_BASE_URL="https://your-app.example.com"
```

> **Brevo users**: The app sends email through Brevo's HTTP API, so no SMTP username/password is required.

### 4. Run Application
```powershell
mvn spring-boot:run
```

### 5. Access Application
- **Application**: http://localhost:8080
- **API Docs**: disabled by default in the committed profiles; enable SpringDoc if you need interactive docs locally

---

## 🎯 User Flow

### **First Time Setup**
1. **Register Company** → http://localhost:8080/company/register
   - Enter company name, admin details
   - Admin user created with ADMIN role
   - Company stays pending until owner approval
   
2. **Verify Email** → Check inbox for verification link
   - Click verification link (valid 24 hours)
   
3. **Login** → http://localhost:8080/login
   - Use credentials to login
   
4. **Access Dashboard** → Manage orders, clients, vendors

### **Forgot Password Flow**
1. Click "Forgot password?" on login page
2. Enter email/username → 6-digit code sent to email
3. Enter code → Verify within 15 minutes
4. Set new password → Login with new credentials

### **Admin Functions**
- **Create Users** → `/admin/users/add` - Add users to your company
- **Manage Users** → `/admin/users` - View, edit, delete users
- **Role Assignment** → Assign USER, MANAGER, or ADMIN roles
- **Edit User** → Update user roles (read-only personal details)
- **Delete Users** → Remove users (with validation: min 1 admin, cannot delete self if only admin)
- **Admin Count** → View current admin count (max 2 per company)
- **Visual Feedback** → Disabled buttons, tooltips, and "(You)" badge for current user

---

## 📁 Project Structure

```
order-manager/
├── src/main/java/com/example/ordermanager/
│   ├── admin/controller/          # AdminController
│   ├── dashboard/controller/      # DashboardController
│   ├── error/controller/          # ErrorPageController
│   ├── company/                   # company/{controller,service,repository,entity,dto}
│   ├── order/                     # order/{controller,service,repository,entity,exception}
│   ├── client/                    # client/{controller,service,repository,entity,exception}
│   ├── vendor/                    # vendor/{controller,service,repository,entity}
│   ├── owner/                     # owner/{controller,service,dto}
│   ├── user/                      # user/{controller,service,repository,entity}
│   └── utils/                     # Utilities
│       └── SecurityContextHelper.java  # Tenant context extraction
├── src/main/resources/
│   ├── application.properties     # Configuration
│   ├── templates/                 # Thymeleaf views
│   │   ├── auth/                  # Login, signup, password reset
│   │   ├── admin/                 # Admin panel
│   │   │   ├── dashboard.html     # Admin overview
│   │   │   └── users/             # User management
│   │   │       ├── list.html      # Users list with conditional delete
│   │   │       ├── form.html      # Add user form
│   │   │       └── form-edit.html # Edit user form (role management)
│   │   ├── orders/                # Order management
│   │   ├── clients/               # Client management
│   │   ├── vendors/               # Vendor management
│   │   └── fragments/             # Reusable fragments (navbar, footer)
│   ├── db/migration/              # Flyway migrations
│   │   ├── V1__init.sql           # Initial schema
│   │   └── V2__add_password_reset_tokens.sql  # Password reset tables
│   └── static/                    # CSS, JS, images
└── pom.xml                        # Maven dependencies
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| **[ARCHITECTURE_OVERVIEW.md](ARCHITECTURE_OVERVIEW.md)** | Complete system architecture & flow diagrams |
| **[DEPLOYMENT.md](DEPLOYMENT.md)** | Deployment setup and runtime environment guidance |
| **[AGENTS.md](AGENTS.md)** | AI coding-agent conventions and project-specific implementation rules |

---

## 🔧 Technology Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 3.x, Spring MVC, Spring Security |
| **Data Access** | Spring Data JPA, Hibernate |
| **Database** | PostgreSQL |
| **View Layer** | Thymeleaf, Bootstrap 5 |
| **Build Tool** | Maven |
| **Email** | Brevo API via WebClient |
| **Validation** | Jakarta Validation (Bean Validation) |
| **API Docs** | SpringDoc OpenAPI dependency included; UI disabled by default |

---

## 🔒 Security Features

✅ **BCrypt Password Hashing** - Passwords never stored in plain text  
✅ **CSRF Protection** - Enabled for MVC, with `/api/**` and `/support` excluded for AJAX flows
✅ **Email Verification** - Prevents unauthorized account creation  
✅ **Password Reset** - Secure 3-step process with time-limited codes  
✅ **Tenant Isolation** - Company data completely separated  
✅ **Role-Based Access** - Fine-grained permission control  
✅ **Session Management** - Spring Security session handling  
✅ **SQL Injection Prevention** - Parameterized queries via JPA  

---

## 🧪 Testing

### Manual Testing
1. **Company Registration**: `/company/register`
2. **Owner Approval**: Approve or reject pending companies from `/owner/dashboard`
3. **Email Verification**: Check email and click link
4. **Login**: `/login` with verified credentials and an approved company
5. **Admin Panel**: `/admin/users` (ADMIN role required)
6. **Order Management**: `/orders` (all authenticated users)
7. **Password Reset**: `/forgot-password` (public access)

### API Testing
- **OpenAPI**: SpringDoc is present, but the UI is disabled by default in the shipped profiles
- **Postman**: Import from `/v3/api-docs` only after enabling SpringDoc locally

---

## 🐛 Troubleshooting

### Email Not Sending
- Check `BREVO_API_KEY`, `MAIL_FROM`, and `APP_BASE_URL`
- Verify Brevo API access and sender identity in `application.properties`

### Database Connection Issues
- Ensure PostgreSQL is running locally or that the remote database URL is reachable
- Verify the `order_manager_db` database exists
- Check credentials in `application.properties`

### Login Redirects to Login Page
- Check if email is verified (`users.enabled = true`)
- Verify password is correct
- Check console logs for authentication errors

### Forgot Password Not Working
- Ensure `/forgot-password`, `/verify-reset-code`, `/reset-password` are in SecurityConfig's permitAll list
- Check email service is configured
- Verify code hasn't expired (15-minute limit)

### Admin Panel Not Visible
- Verify user has `ADMIN` role in database (`users.role = 'ADMIN'`)
- Check `CustomUserDetailsService` uses actual role: `ROLE_` + user.getRole()
- Ensure `SecurityContextHelper.getCurrentUsername()` returns correct username
- Verify navbar uses: `hasRole('ADMIN')` for admin link visibility

### Delete Button Not Greyed Out
- Check `adminCount` is passed to template in `AdminController.listUsers()`
- Verify `currentUsername` is passed to template
- Ensure delete button uses Thymeleaf operators: `and`, `le` (not `&&`, `<=`)
- Check CSS includes `.btn-sm-custom:disabled { opacity: 0.5; }`

### Cannot Delete Admin User
- Expected if user is the only admin (business rule)
- Error message: "Cannot delete the last admin in the company"
- Allow deletion if 2 admins exist (one will remain)

---

## 🤝 Contributing

1. Update architecture docs when adding features
2. Follow Google Java Style Guide (formatters included)
3. Update `ARCHITECTURE_OVERVIEW.md` "Recent changes" section
4. Write meaningful commit messages
5. Test all authentication flows before committing

---

## 📝 Recent Updates

- ✅ **2026-02-28**: Admin management rules implemented - min 1 admin, max 2 admins, self-deletion protection
- ✅ **2026-02-28**: UI enhancements - delete button conditional disable, "(You)" badge, admin count display
- ✅ **2026-02-28**: Edit user form created with read-only fields and role management
- ✅ **2026-02-28**: Fixed Thymeleaf parsing errors in admin list template
- ✅ **2026-02-27**: Fixed forgot password security configuration - added endpoints to permitAll
- ✅ **2026-02-26**: Password reset feature with 6-digit codes and email verification
- ✅ **2026-02-25**: Admin panel for user management within companies
- ✅ **2026-02-24**: Implemented tenant-aware company assignment for all business entities
- ✅ **2026-02-24**: Added DTO validation for company registration with mobile number validation
- ✅ **2026-02-24**: Database schema cleanup and foreign key constraint implementation
- ✅ **2026-02-20**: Email verification system with 24-hour token expiry
- ✅ **2026-02-15**: Multi-tenant architecture with complete data isolation
- ✅ **2026-07-04**: Documentation aligned with the live PostgreSQL/Brevo runtime, owner approval flow, and disabled Swagger defaults

---

## 📄 License

MIT License - See LICENSE file for details

---

## 🌟 Key Highlights

- ✨ **Production-Ready** - Complete authentication & authorization
- 🔐 **Secure by Default** - Industry-standard security practices
- 🏢 **Multi-Tenant** - Perfect for SaaS applications
- 📧 **Email Integration** - Verification & password reset
- 👥 **Admin Panel** - Self-service user management
- 🎨 **Modern UI** - Bootstrap 5 responsive design
- 📖 **Well Documented** - Comprehensive guides & architecture docs

---

**Built with ❤️ using Spring Boot**

For detailed architectural information, see [ARCHITECTURE_OVERVIEW.md](ARCHITECTURE_OVERVIEW.md)

