# Order Manager - Deployment Guide

## Scope
This is the single source of truth for deploying `order-manager`.

## Table Of Contents
- Environment Profiles
- Required Environment Variables
- Local Run (PowerShell)
- Docker Run
- Render Deployment
- Flyway Migrations
- Health Checks
- Troubleshooting

---

## Environment Profiles

The app supports `dev`, `qa`, and `prod` profiles.

| Profile | Database Direction | Flyway | Swagger | Notes |
|---|---|---|---|---|
| `dev` | Local PostgreSQL | Enabled | Disabled | Fast local iteration |
| `qa` | Cloud DB (driver configurable) | Enabled | Disabled | Pre-prod validation |
| `prod` | PostgreSQL | Enabled | Disabled | Caching/compression enabled |

Set profile using environment variable:

```bash
SPRING_PROFILES_ACTIVE=dev
```

---

## Required Environment Variables

### Core

| Variable | Required | Used For |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | Active profile (`dev`, `qa`, `prod`) |
| `DB_USERNAME` | Yes | Database auth |
| `DB_PASSWORD` | Yes | Database auth |
| `MAIL_FROM` | Yes | Email sender |
| `APP_BASE_URL` | Yes (qa/prod) | Links in emails, keep-alive |

### Database

Use one of these patterns based on profile:

| Profile | Variables |
|---|---|
| `dev` | Local URL is configured in `application-dev.properties` (`jdbc:postgresql://localhost:5432/order_manager_db`) |
| `qa` | `DATABASE_URL`, optional `DB_DRIVER` |
| `prod` | `DB_HOST`, `DB_PORT` (optional), `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, optional `DB_DRIVER` |

### Email (Brevo API)

| Variable | Required | Used For |
|---|---|---|
| `BREVO_API_KEY` | Yes | Brevo HTTP API authentication |
| `MAIL_FROM` | Yes | Sender address |

### Owner Console Credentials

| Variable | Required | Used For |
|---|---|---|
| `OWNER_EMAIL` | Yes | Owner identity |
| `OWNER_USERNAME` | Yes | Owner login username |
| `OWNER_PASSWORD` | Yes | Owner login password (raw or encoded format) |

### Optional

| Variable | Default |
|---|---|
| `PORT` | `8080` |

---

## Local Run (PowerShell)

Prerequisites:
- Java 17+
- Maven 3.8+
- PostgreSQL running locally

Create local database (example):

```sql
CREATE DATABASE order_manager_db;
```

Run app with `dev` profile:

```powershell
cd "F:\AshLabsCompany\order-manager"
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="postgres"
$env:BREVO_API_KEY="<your-key>"
$env:MAIL_FROM="no-reply@example.com"
$env:APP_BASE_URL="http://localhost:8080"
$env:OWNER_EMAIL="owner@example.com"
$env:OWNER_USERNAME="owner"
$env:OWNER_PASSWORD="<strong-password>"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Docker Run

Build image:

```powershell
cd "F:\AshLabsCompany\order-manager"
docker build -t order-manager:latest .
```

Run container (production profile example):

```powershell
docker run -d `
  -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=prod `
  -e DB_HOST=<host> `
  -e DB_PORT=5432 `
  -e DB_NAME=<database> `
  -e DB_USERNAME=<username> `
  -e DB_PASSWORD=<password> `
  -e BREVO_API_KEY=<brevo-key> `
  -e MAIL_FROM=<sender-email> `
  -e APP_BASE_URL=https://your-app.onrender.com `
  -e OWNER_EMAIL=<owner-email> `
  -e OWNER_USERNAME=<owner-username> `
  -e OWNER_PASSWORD=<owner-password> `
  --name order-manager `
  order-manager:latest
```

---

## Render Deployment

### Preferred: Blueprint (`render.yaml`)

1) Push code to repository.
2) In Render, create a new Blueprint from the repo.
3) Set required secret environment variables from the table above.
4) Deploy and monitor logs.

### Manual Service Setup

Use Docker runtime and set at least:

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST=<postgres-host>
DB_PORT=5432
DB_NAME=<postgres-db>
DB_USERNAME=<postgres-user>
DB_PASSWORD=<postgres-password>
BREVO_API_KEY=<brevo-key>
MAIL_FROM=<sender-email>
APP_BASE_URL=https://your-app.onrender.com
OWNER_EMAIL=<owner-email>
OWNER_USERNAME=<owner-username>
OWNER_PASSWORD=<owner-password>
```

Notes:
- Keep database region close to app region.
- Health check path should be `/actuator/health`.

---

## Flyway Migrations

Migration folder:

```text
src/main/resources/db/migration/
```

Current sequence is versioned and must remain unique (`V1` ... `V9`).

### Rules
- Never create duplicate versions.
- Never edit old applied migration scripts in-place.
- Add new scripts only (forward-only).

### Commands

```powershell
cd "F:\AshLabsCompany\order-manager"
mvn flyway:validate
mvn flyway:info
mvn flyway:migrate
```

### If You See "Found more than one migration with version X"
- Rename the newer conflicting script to the next available version.
- Rebuild and restart.
- Re-run `mvn flyway:validate`.

---

## Health Checks

Endpoints:
- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

Quick check:

```powershell
curl https://your-app.onrender.com/actuator/health
```

Expected response includes:

```json
{
  "status": "UP"
}
```

---

## Troubleshooting

### App fails on startup
- Verify profile and required env vars are set.
- Verify DB connectivity and credentials.
- Check service logs.

### Flyway validation/migration failures
- Check `flyway_schema_history` table.
- Confirm migration versions are unique and ordered.
- Use `mvn flyway:repair` only when history metadata is broken and you know the impact.

### Email failures
- Confirm `BREVO_API_KEY` and `MAIL_FROM` are valid.
- Check Brevo API/network reachability from runtime.

### Owner login issues
- Confirm `OWNER_USERNAME` / `OWNER_EMAIL` and `OWNER_PASSWORD` are set.
- If password is encoded, ensure it is in supported format.

---

## Operational Notes
- Production enables template and static resource caching.
- `/api/**` is CSRF-ignored by configuration; MVC form posts remain CSRF-protected.
- Keep tenant isolation intact: avoid introducing unfiltered data access in new code.

