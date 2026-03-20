# 📦 Order Manager - Multi-Tenant Order Management System

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Java](https://img.shields.io/badge/Java-17+-orange)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

A comprehensive web-based **Order Management System** with multi-tenant architecture, built using Spring Boot 3, Thymeleaf, and MySQL. Designed for businesses to manage orders, clients, and vendors with complete tenant isolation.

---

## 🚀 Features

### 🏢 **Multi-Tenancy (Company-Based Isolation)**
- **Complete tenant isolation** - Each company's data is fully separated
- **Secure context extraction** - Company ID extracted from authenticated user (never from request)
- **Automatic data filtering** - All queries filtered by company ID
- **Company registration** - Self-service company creation with admin user

### 🔐 **Authentication & Security**
- **BCrypt password hashing** - Industry-standard password encryption
- **Email verification** - Users must verify email before login (24-hour token expiry)
- **Password reset flow** - 3-step secure password reset with 6-digit codes
  - Request reset code → Verify code → Set new password
  - 15-minute code expiry with one-time use
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
- **MySQL Database** - Reliable relational database with foreign key constraints
- **Flyway Migrations** - Version-controlled database schema (optional)
- **DTO Validation** - Input validation with jakarta.validation
- **Email Service** - SMTP integration (Gmail/Brevo support)
- **RESTful API** - JSON endpoints for order operations
- **Swagger UI** - Interactive API documentation

---

## 📋 Prerequisites

- **Java 17+** (JDK 17 or higher)
- **Maven 3.6+**
- **MySQL 8.0+**
- **Gmail Account** (for email functionality) or Brevo SMTP

---

## ⚙️ Quick Start

### 1. Clone & Navigate
```powershell
cd F:\AshLabsCompany\order-manager
```

### 2. Configure Database
Create MySQL database:
```sql
CREATE DATABASE order_manager_db;
```

Set the required environment variable before running the application:
```bash
# Linux/macOS
export DB_PASSWORD=your_mysql_password

# Windows (Command Prompt)
set DB_PASSWORD=your_mysql_password

# Windows (PowerShell)
$env:DB_PASSWORD="your_mysql_password"
```

The `application.properties` reads this as `spring.datasource.password=${DB_PASSWORD}`.

### 3. Configure Email (Required for verification/password reset)
Set the following environment variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `MAIL_USERNAME` | SMTP account username (email address) | `your_email@gmail.com` |
| `MAIL_PASSWORD` | SMTP account password or app password | `your_app_password` |
| `MAIL_FROM` | From address used in sent emails | `your_email@gmail.com` |

```bash
# Linux/macOS
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_app_password
export MAIL_FROM=your_email@gmail.com

# Windows (Command Prompt)
set MAIL_USERNAME=your_email@gmail.com
set MAIL_PASSWORD=your_app_password
set MAIL_FROM=your_email@gmail.com

# Windows (PowerShell)
$env:MAIL_USERNAME="your_email@gmail.com"
$env:MAIL_PASSWORD="your_app_password"
$env:MAIL_FROM="your_email@gmail.com"
```

> **Gmail users**: Enable "App Passwords" in your Google Account settings and use the generated app password as `MAIL_PASSWORD`.

### 4. Run Application
```powershell
mvn spring-boot:run
```

### 5. Access Application
- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API Docs**: http://localhost:8080/v3/api-docs

---

## 🎯 User Flow

### **First Time Setup**
1. **Register Company** → http://localhost:8080/company/register
   - Enter company name, admin details
   - Admin user created with ADMIN role
   
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
| **Database** | MySQL 8.0 |
| **View Layer** | Thymeleaf, Bootstrap 5 |
| **Build Tool** | Maven |
| **Email** | Spring Mail (SMTP) |
| **Validation** | Jakarta Validation (Bean Validation) |
| **API Docs** | SpringDoc OpenAPI (Swagger) |

---

## 🔒 Security Features

✅ **BCrypt Password Hashing** - Passwords never stored in plain text  
✅ **CSRF Protection** - Disabled for development (enable in production)  
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
2. **Email Verification**: Check email and click link
3. **Login**: `/login` with verified credentials
4. **Admin Panel**: `/admin/users` (ADMIN role required)
5. **Order Management**: `/orders` (all authenticated users)
6. **Password Reset**: `/forgot-password` (public access)

### API Testing
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **Postman**: Import from `/v3/api-docs`

---

## 🐛 Troubleshooting

### Email Not Sending
- Check `MAIL_USERNAME` and `MAIL_PASSWORD` environment variables
- For Gmail: Enable "App Passwords" in Google Account settings
- Verify SMTP settings in `application.properties`

### Database Connection Issues
- Ensure MySQL is running: `mysql -u root -p`
- Verify database exists: `SHOW DATABASES;`
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

