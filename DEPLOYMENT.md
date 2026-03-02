# Order Manager - Deployment Guide

## 📋 Table of Contents
- [Environment Configuration](#environment-configuration)
- [Local Development](#local-development)
- [Docker Deployment](#docker-deployment)
- [Render.com Deployment](#rendercom-deployment)
- [Environment Variables](#environment-variables)
- [Database Migration](#database-migration)

---

## 🌍 Environment Configuration

The application supports three environments:
- **DEV** (Development) - Local development with full debugging
- **QA** (Quality Assurance) - Testing environment
- **PROD** (Production) - Live production environment

### Profile Selection

Set the active profile using environment variable:
```bash
SPRING_PROFILES_ACTIVE=dev   # or qa, prod
```

---

## 💻 Local Development

### Prerequisites
- Java 17+
- Maven 3.6+
- MySQL 8.0+

### Setup Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd order-manager
   ```

2. **Create `.env` file**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Create database**
   ```sql
   CREATE DATABASE order_manager_db;
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

5. **Access the application**
   - Dashboard: http://localhost:8080/dashboard
   - Swagger UI: http://localhost:8080/swagger-ui/index.html
   - API Docs: http://localhost:8080/v3/api-docs

---

## 🐳 Docker Deployment

### Using Docker Compose (Recommended for Local)

1. **Build and start services**
   ```bash
   docker-compose up -d
   ```

2. **View logs**
   ```bash
   docker-compose logs -f app
   ```

3. **Stop services**
   ```bash
   docker-compose down
   ```

### Using Docker Only

1. **Build the image**
   ```bash
   docker build -t order-manager:latest .
   ```

2. **Run the container**
   ```bash
   docker run -d \
     -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=prod \
     -e DATABASE_URL=jdbc:mysql://your-db-host:3306/order_manager_db \
     -e DB_USERNAME=your_username \
     -e DB_PASSWORD=your_password \
     -e MAIL_USERNAME=your-email@gmail.com \
     -e MAIL_PASSWORD=your-app-password \
     -e MAIL_FROM=your-email@gmail.com \
     -e APP_FRONTEND_URL=https://your-domain.com \
     -e APP_BASE_URL=https://your-domain.com \
     --name order-manager \
     order-manager:latest
   ```

---

## 🚀 Render.com Deployment

### Method 1: Using Blueprint (render.yaml)

1. **Push code to GitHub**
   ```bash
   git add .
   git commit -m "Add deployment configuration"
   git push origin main
   ```

2. **Create New Blueprint on Render**
   - Go to [Render Dashboard](https://dashboard.render.com/)
   - Click "New" → "Blueprint"
   - Connect your GitHub repository
   - Render will auto-detect `render.yaml`

3. **Configure Environment Variables**
   In Render Dashboard, set these secret values:
   - `MAIL_USERNAME`: Your Gmail address
   - `MAIL_PASSWORD`: Gmail app-specific password
   - `MAIL_FROM`: Your Gmail address

4. **Deploy**
   - Click "Apply" to deploy
   - Wait for build and deployment to complete

### Method 2: Manual Setup

1. **Create Web Service**
   - New → Web Service
   - Connect GitHub repository
   - Select branch: `main`
   - Environment: `Docker`
   - Region: Choose closest to your users

2. **Configure Build & Deploy**
   - Build Command: (leave empty, Docker handles it)
   - Start Command: (leave empty, Docker handles it)
   - Dockerfile Path: `./Dockerfile`

3. **Set Environment Variables**
   ```
   SPRING_PROFILES_ACTIVE=prod
   DATABASE_URL=<from-render-database>
   DB_USERNAME=<from-render-database>
   DB_PASSWORD=<from-render-database>
   MAIL_USERNAME=<your-gmail>
   MAIL_PASSWORD=<gmail-app-password>
   MAIL_FROM=<your-gmail>
   APP_FRONTEND_URL=https://your-app.onrender.com
   APP_BASE_URL=https://your-app.onrender.com
   ```

4. **Create Database**
   - New → MySQL
   - Database Name: `order_manager_db`
   - Region: Same as web service
   - Plan: Starter (Free tier available)

5. **Link Database to Web Service**
   - Copy database connection details
   - Add as environment variables to web service

---

## 🔐 Environment Variables

### Required Variables (All Environments)

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev`, `qa`, `prod` |
| `DATABASE_URL` | Database JDBC URL | `jdbc:mysql://localhost:3306/order_manager_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `your_password` |
| `MAIL_USERNAME` | Email account username | `your-email@gmail.com` |
| `MAIL_PASSWORD` | Email app password | `xxxx xxxx xxxx xxxx` |
| `MAIL_FROM` | Email sender address | `your-email@gmail.com` |

### Production-Specific Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `APP_FRONTEND_URL` | Frontend URL | `https://order-manager.onrender.com` |
| `APP_BASE_URL` | Backend API base URL | `https://order-manager.onrender.com` |
| `JAVA_OPTS` | JVM options | `-Xmx512m -Xms256m` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `DB_DRIVER` | JDBC driver class | `com.mysql.cj.jdbc.Driver` |
| `HIBERNATE_DIALECT` | Hibernate dialect | `org.hibernate.dialect.MySQL8Dialect` |
| `MAIL_HOST` | SMTP host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP port | `587` |

---

## 🗄️ Database Migration

### Flyway Configuration

Database migrations are managed using Flyway:

- **DEV**: Migrations disabled (uses `spring.jpa.hibernate.ddl-auto=update`)
- **QA**: Migrations enabled (automatic)
- **PROD**: Migrations enabled (mandatory)

### Migration Files Location
```
src/main/resources/db/migration/
├── V1__init.sql
├── V2__add_password_reset_tokens.sql
└── V3__your_next_migration.sql
```

### Creating New Migrations

1. Create file following pattern: `V{version}__{description}.sql`
   ```
   V3__add_user_roles.sql
   ```

2. Write SQL migration script:
   ```sql
   ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'USER';
   ```

3. Deploy - Flyway will automatically apply on startup

### Manual Migration Commands

```bash
# Validate migrations
mvn flyway:validate

# Check migration status
mvn flyway:info

# Apply migrations
mvn flyway:migrate

# Repair migration history
mvn flyway:repair
```

---

## 🔍 Health Checks

### Endpoints

- **Health Check**: `/actuator/health`
- **Info**: `/actuator/info`
- **Metrics**: `/actuator/metrics`

### Production Health Check

```bash
curl https://your-app.onrender.com/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

---

## 📝 Gmail App Password Setup

1. Enable 2-Step Verification on your Gmail account
2. Go to: https://myaccount.google.com/apppasswords
3. Select "Mail" and your device
4. Generate password
5. Use the 16-character password in `MAIL_PASSWORD`

---

## 🐛 Troubleshooting

### Application won't start

1. Check database connection:
   ```bash
   mysql -h <host> -u <username> -p
   ```

2. Verify environment variables are set:
   ```bash
   echo $SPRING_PROFILES_ACTIVE
   echo $DATABASE_URL
   ```

3. Check application logs:
   ```bash
   docker logs order-manager-app
   # or
   docker-compose logs -f app
   ```

### Database migration errors

1. Check migration files syntax
2. Verify database state:
   ```sql
   SELECT * FROM flyway_schema_history;
   ```
3. If needed, repair:
   ```bash
   mvn flyway:repair
   ```

### Email not sending

1. Verify Gmail App Password (not regular password)
2. Check 2FA is enabled on Gmail
3. Test SMTP connection:
   ```bash
   telnet smtp.gmail.com 587
   ```

---

## 📊 Monitoring

### Render Monitoring

- Dashboard shows CPU, Memory, and Request metrics
- Set up alerts for downtime
- Configure auto-deploy on push

### Custom Monitoring

Use actuator endpoints to monitor:
- Application health
- JVM metrics
- HTTP request metrics
- Database connection pool

---

## 🔄 CI/CD

### Auto-deploy on Render

Render automatically deploys when:
1. Code is pushed to `main` branch
2. Build succeeds
3. Health check passes

### Manual Deploy

```bash
# Push changes
git add .
git commit -m "Your changes"
git push origin main

# Render will automatically trigger deployment
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Render Documentation](https://render.com/docs)
- [Flyway Documentation](https://flywaydb.org/documentation/)
- [Docker Documentation](https://docs.docker.com/)

---

## 🆘 Support

For issues or questions:
1. Check application logs
2. Review this deployment guide
3. Check Render status page
4. Contact system administrator

