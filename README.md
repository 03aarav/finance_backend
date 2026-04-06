# Finance Dashboard Backend

A production-structured REST API for a multi-role organisational finance dashboard. Built with **Spring Boot 3**, **MongoDB Atlas**, and **JWT-based Spring Security**. Supports financial record management, role-based access control, and a pre-aggregated summary engine that keeps dashboard reads instant.

---

## Live API

| | |
|---|---|
| **Base URL** | `http://56.228.17.29:8080` |
| **Swagger UI** | `http://56.228.17.29:8080/swagger-ui.html` |
| **API Docs (JSON)** | `http://56.228.17.29:8080/v3/api-docs` |

---

## Default Admin Credentials

> There is no public registration endpoint. The system ships with a seeded admin account. All other users must be created by an admin after logging in.

| Field | Value |
|---|---|
| Email | `admin@gmail.com` |
| Password | `admin` |
| Role | `ADMIN` |

**Login flow:**
```
POST /auth/login
→ copy the token from the response
→ add header: Authorization: Bearer <token>
→ now you can access all protected endpoints
```

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Design Decisions & Tradeoffs](#design-decisions--tradeoffs)
- [Assumptions](#assumptions)
- [Local Setup](#local-setup)
- [API Reference](#api-reference)
  - [Auth](#auth)
  - [Users](#users)
  - [Categories](#categories)
  - [Records](#records)
  - [Dashboard](#dashboard)
- [Role & Permission Matrix](#role--permission-matrix)
- [Data Models](#data-models)
- [Seeded Test Data](#seeded-test-data)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Database | MongoDB Atlas |
| Security | Spring Security + JWT |
| Validation | Jakarta Bean Validation |
| Build Tool | Maven |
| API Docs | Swagger UI |
| Boilerplate | Lombok |

---

## Architecture Overview

```
Client Request
      │
      ▼
 JWT Auth Filter         ← validates Bearer token on every protected request
      │
      ▼
 Role Guard              ← @PreAuthorize per endpoint (ADMIN / ANALYST / VIEWER)
      │
      ▼
 Controller Layer        ← request mapping, response shaping
      │
      ▼
 Service Layer           ← business logic, input validation, summary updates
      │
      ├──► RecordRepository      (MongoRepository)
      ├──► CategoryRepository    (MongoRepository)
      ├──► UserRepository        (MongoRepository)
      └──► SummaryService        (MongoTemplate atomic upserts)
                │
                ▼
         summary_snapshots       ← pre-aggregated buckets (week / month / year)
```

---

## Design Decisions & Tradeoffs

### 1. Pre-aggregated Summary Table

This is the core architectural decision of the system.

Instead of running `SUM` and `GROUP BY` aggregations every time a dashboard API is called, the system maintains a `summary_snapshots` collection that is **updated on every write operation** — create, update, and delete.

**How it works:**

When a record is saved with date `2026-04-03`, the service derives three period keys:

```
week  →  "2026-W14"
month →  "2026-04"
year  →  "2026"
```

For each period, two MongoDB upserts fire — one for the specific category bucket and one for a global `__all__` bucket. This means a dashboard read is always a **single indexed document lookup** — O(1) regardless of how many records exist.

```
POST /records  →  insert record
               →  upsert week   + category bucket
               →  upsert week   + __all__ bucket
               →  upsert month  + category bucket
               →  upsert month  + __all__ bucket
               →  upsert year   + category bucket
               →  upsert year   + __all__ bucket
```

**On UPDATE:** the service captures the old record state before mutation, subtracts it from the relevant buckets with multiplier `-1`, then adds the new values with multiplier `+1`. This correctly handles date changes (different week/month bucket), type changes (income ↔ expense), category changes, and amount changes.

**On DELETE:** the record's values are subtracted from all its period buckets before deletion using multiplier `-1`.

**Tradeoff:** Write operations trigger 6 upsert operations each. For a finance dashboard with far more reads than writes, this is the correct tradeoff. Dashboard reads never touch the `records` collection at all.

**Recalculation safety net:** A `POST /dashboard/recalculate` admin endpoint drops and rebuilds all summaries from scratch. Use this if records were inserted directly into MongoDB bypassing the API.

---

### 2. Organisation-wide Data Model

All financial records belong to the **organisation**, not individual users. The `userId` on a record is an audit field ("who entered this entry"), not a data ownership boundary.

This means all roles see the same underlying financial data. What differs per role is the **depth of access**:

- **Viewer** — summary totals and recent activity only
- **Analyst** — raw records, filters, category breakdowns, trends
- **Admin** — full read and write access plus user management

This model was chosen because the role hierarchy only makes sense in a shared-data context. Per-user private finance records would make the Analyst and Viewer roles meaningless.

---

### 3. Category Type Enforcement

When a record is created or updated, the service fetches the referenced category and validates that `category.type` matches `record.type`. An `EXPENSE` category cannot be assigned to an `INCOME` record and vice versa. This prevents silent data corruption at the write layer.

---

### 4. No User Self-Registration

User creation is admin-only. There is no public `/auth/register` endpoint. This is intentional — in an organisational finance system, access should be granted by an administrator, not self-provisioned. The system ships with a seeded admin account as the entry point.

---

### 5. Hard Delete with Summary Rollback

Records are hard-deleted. Before deletion, the service subtracts the record's values from all summary buckets, keeping summaries accurate. Soft delete was not implemented to avoid adding an `isDeleted: false` filter to every query in the system.

---

## Assumptions

1. **Single organisation** — the system models one organisation's finances. There is no multi-tenancy.
2. **Admin is the sole writer** — only admins create, update, or delete records and categories. Analysts and Viewers are read-only.
3. **Categories are admin-managed** — categories are not free-text. They are pre-defined documents in the `categories` collection. Records reference a category by ID.
4. **Type values are case-insensitive on input** — `INCOME`, `income`, `Income` are all accepted and normalised to lowercase internally.
5. **ISO 8601 week numbering** — weeks start on Monday. Week 1 is the week containing the first Thursday of the year.
6. **JWT is stateless** — tokens have a 24-hour TTL. No refresh token flow is implemented.
7. **`userId` on records is informational** — it records who submitted the entry. It does not scope data visibility.

---

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB Atlas account or local MongoDB on port `27017`

### Clone and Run

```bash
git clone https://github.com/your-username/finance-backend.git
cd finance-backend
mvn spring-boot:run
```

### application.properties

```properties
spring.application.name=finance
spring.data.mongodb.uri=mongodb+srv://<user>:<password>@cluster0.xxxxx.mongodb.net/financedb?retryWrites=true&w=majority

app.jwt.secret=your-256-bit-secret
app.jwt.expiration=86400000

server.port=8080
```

---

## API Reference

> **Base URL:** `http://56.228.17.29:8080`
>
> All endpoints except `POST /auth/login` require:
> `Authorization: Bearer <token>`

---

### Auth

#### `POST /auth/login`

Authenticate and receive a JWT token. This is the **only public entry point** into the system.

**Request body:**
```json
{
  "email": "admin@gmail.com",
  "password": "admin"
}
```

**Response `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "role": "ADMIN"
}
```

**Error `401`:**
```json
{
  "error": true,
  "message": "Invalid email or password"
}
```

---

### Users

> All user management endpoints require `ADMIN` role.

#### `POST /users`

Create a new user. Only admins can do this — there is no self-registration.

**Request body:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "securepassword",
  "role": "ANALYST"
}
```

Valid roles: `ADMIN`, `ANALYST`, `VIEWER`

**Response `201`:**
```json
{
  "id": "69d37921fe876ec7f90e3469",
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "ANALYST",
  "active": true
}
```

---

#### `GET /users`

Fetch all users.

---

#### `GET /users/{id}`

Fetch a single user by ID.

**Error `404`:**
```json
{ "error": true, "message": "User not found" }
```

---

#### `PUT /users/{id}`

Update user name, role, or active status.

**Request body:**
```json
{
  "name": "Jane Smith",
  "role": "ADMIN",
  "active": true
}
```

---

#### `DELETE /users/{id}`

Delete a user permanently.

**Response `200`:**
```json
"User deleted successfully"
```

---

#### `GET /users/me`

Returns the currently authenticated user's profile. Available to all roles.

---

### Categories

> Seeded categories are already in the live database. No setup required for testing.

#### `POST /categories` — `ADMIN` only

Create a new category.

**Request body:**
```json
{
  "name": "Salary",
  "type": "INCOME"
}
```

`type` must be `INCOME` or `EXPENSE` (case-insensitive).

**Response `201`:**
```json
{
  "id": "cat_salary",
  "name": "Salary",
  "type": "income",
  "createdBy": "admin@gmail.com"
}
```

---

#### `GET /categories` — All roles

Returns all available categories.

**Response `200`:**
```json
[
  { "id": "cat_food",          "name": "Food & Dining",     "type": "EXPENSE" },
  { "id": "cat_transport",     "name": "Transportation",    "type": "EXPENSE" },
  { "id": "cat_shopping",      "name": "Shopping",          "type": "EXPENSE" },
  { "id": "cat_bills",         "name": "Bills & Utilities", "type": "EXPENSE" },
  { "id": "cat_health",        "name": "Health & Medical",  "type": "EXPENSE" },
  { "id": "cat_entertainment", "name": "Entertainment",     "type": "EXPENSE" },
  { "id": "cat_education",     "name": "Education",         "type": "EXPENSE" },
  { "id": "cat_salary",        "name": "Salary",            "type": "INCOME"  },
  { "id": "cat_freelance",     "name": "Freelance",         "type": "INCOME"  },
  { "id": "cat_investment",    "name": "Investment",        "type": "INCOME"  }
]
```

---

#### `GET /categories/{id}` — All roles

Fetch a single category by ID.

---

#### `DELETE /categories/{id}` — `ADMIN` only

Delete a category.

> Note: deleting a category referenced by existing records will leave those records with a dangling `categoryId`. Delete categories with caution.

---

### Records

> Write operations (`POST`, `PUT`, `DELETE`) require `ADMIN` role.
> Read operations (`GET`) require `ADMIN` or `ANALYST` role.
>
> Every write automatically updates the `summary_snapshots` collection.

#### `POST /records`

Create a new financial record.

**Request body:**
```json
{
  "userId": "69d37921fe876ec7f90e3469",
  "amount": 50000,
  "type": "INCOME",
  "categoryId": "cat_salary",
  "date": "2026-04-01",
  "notes": "Monthly salary"
}
```

**Validation rules:**

| Field | Rule |
|---|---|
| `amount` | Required. Must be greater than `0.00` |
| `type` | Required. Must be `INCOME` or `EXPENSE` |
| `categoryId` | Required. Must reference an existing category |
| `category.type` | Must match `record.type` — EXPENSE category cannot go on an INCOME record |
| `date` | Required. Format: `YYYY-MM-DD` |
| `userId` | Required |

**Response `201`:** the created record object.

**Error `400` — type mismatch:**
```json
{ "error": true, "message": "Category type must match record type" }
```

**Error `404` — category not found:**
```json
{ "error": true, "message": "Category not found" }
```

---

#### `GET /records`

Fetch records with optional filters.

**Query parameters (all optional):**

| Parameter | Type | Example | Description |
|---|---|---|---|
| `userId` | string | `69d37921fe876ec7f90e3469` | Filter by submitting user |
| `categoryId` | string | `cat_salary` | Filter by category |
| `type` | string | `INCOME` | Filter by type |
| `startDate` | date | `2026-04-01` | Range start inclusive |
| `endDate` | date | `2026-04-30` | Range end inclusive |

**Examples:**
```
GET /records
GET /records?type=EXPENSE
GET /records?startDate=2026-04-01&endDate=2026-04-30
GET /records?categoryId=cat_food&type=EXPENSE
GET /records?userId=69d37921fe876ec7f90e3469&startDate=2026-04-01
```

**Error `400` — invalid date range:**
```json
{ "error": true, "message": "Start date cannot be after end date" }
```

---

#### `GET /records/{id}`

Fetch a single record by ID.

---

#### `PUT /records/{id}`

Update an existing record. Summary buckets are automatically corrected — old values subtracted, new values added. Handles date changes (moves to different week/month bucket), type flips, category changes, and amount changes correctly.

**Request body:** same structure as `POST /records`

---

#### `DELETE /records/{id}`

Delete a record. Summary buckets are decremented before deletion.

**Response `200`:**
```json
"Record deleted successfully"
```

---

### Dashboard

> All summary data is pre-computed. Dashboard reads are instant indexed lookups — no aggregation runs at request time.

#### `GET /dashboard/summary` — All roles

Overall totals across all time.

**Response `200`:**
```json
{
  "periodType": "all",
  "periodKey": "all-time",
  "categoryId": "__all__",
  "income": 60500.00,
  "expense": 15530.00,
  "net": 44970.00,
  "recordCount": 15
}
```

---

#### `GET /dashboard/categories` — `ADMIN`, `ANALYST`

Category-wise breakdown for a specific period. Returns one row per category, excluding the `__all__` global row.

**Query parameters:**

| Parameter | Default | Examples |
|---|---|---|
| `periodType` | `month` | `week`, `month`, `year` |
| `periodKey` | required | `2026-04`, `2026-W14`, `2026` |

**Example:**
```
GET /dashboard/categories?periodType=month&periodKey=2026-04
```

**Response `200`:**
```json
[
  { "categoryId": "cat_salary",        "income": 50000, "expense": 0,    "net": 50000,  "recordCount": 1 },
  { "categoryId": "cat_freelance",     "income": 8000,  "expense": 0,    "net": 8000,   "recordCount": 1 },
  { "categoryId": "cat_investment",    "income": 2500,  "expense": 0,    "net": 2500,   "recordCount": 1 },
  { "categoryId": "cat_food",          "income": 0,     "expense": 430,  "net": -430,   "recordCount": 2 },
  { "categoryId": "cat_transport",     "income": 0,     "expense": 2100, "net": -2100,  "recordCount": 2 },
  { "categoryId": "cat_shopping",      "income": 0,     "expense": 5400, "net": -5400,  "recordCount": 2 },
  { "categoryId": "cat_bills",         "income": 0,     "expense": 2700, "net": -2700,  "recordCount": 2 },
  { "categoryId": "cat_entertainment", "income": 0,     "expense": 1300, "net": -1300,  "recordCount": 2 }
]
```

---

#### `GET /dashboard/trends` — `ADMIN`, `ANALYST`

All summary buckets for a period type ordered by `periodKey`. Designed for line and bar charts.

**Query parameters:**

| Parameter | Default | Options |
|---|---|---|
| `periodType` | `month` | `week`, `month`, `year` |

**Examples:**
```
GET /dashboard/trends?periodType=week
GET /dashboard/trends?periodType=month
GET /dashboard/trends?periodType=year
```

**Response `200`:**
```json
[
  { "periodKey": "2026-W14", "income": 58500, "expense": 8450, "net": 50050, "recordCount": 8 },
  { "periodKey": "2026-W15", "income": 2500,  "expense": 7080, "net": -4580, "recordCount": 7 }
]
```

---

#### `GET /dashboard/recent` — All roles

Most recent N records sorted by date descending.

**Query parameters:**

| Parameter | Default | Max |
|---|---|---|
| `limit` | `10` | `50` |

**Example:**
```
GET /dashboard/recent?limit=5
```

---

#### `POST /dashboard/recalculate` — `ADMIN` only

Drops and rebuilds the entire `summary_snapshots` collection from scratch by replaying every record. Use when records have been inserted directly into MongoDB, or to verify full consistency.

**Response `200`:**
```json
{
  "status": "success",
  "message": "Recalculation complete. Processed: 15 records. Skipped: 0 invalid records.",
  "durationMs": 87,
  "timestamp": "2026-04-06T10:42:31.123"
}
```

The `skipped` count shows how many records in the database have null or invalid fields and could not be processed. A non-zero skip count means there is dirty data in the `records` collection.

---

## Role & Permission Matrix

| Endpoint | VIEWER | ANALYST | ADMIN |
|---|:---:|:---:|:---:|
| `POST /auth/login` | ✓ | ✓ | ✓ |
| `GET /users/me` | ✓ | ✓ | ✓ |
| `POST /users` | ✗ | ✗ | ✓ |
| `GET /users` | ✗ | ✗ | ✓ |
| `GET /users/{id}` | ✗ | ✗ | ✓ |
| `PUT /users/{id}` | ✗ | ✗ | ✓ |
| `DELETE /users/{id}` | ✗ | ✗ | ✓ |
| `GET /categories` | ✓ | ✓ | ✓ |
| `GET /categories/{id}` | ✓ | ✓ | ✓ |
| `POST /categories` | ✗ | ✗ | ✓ |
| `DELETE /categories/{id}` | ✗ | ✗ | ✓ |
| `GET /records` | ✗ | ✓ | ✓ |
| `GET /records/{id}` | ✗ | ✓ | ✓ |
| `POST /records` | ✗ | ✗ | ✓ |
| `PUT /records/{id}` | ✗ | ✗ | ✓ |
| `DELETE /records/{id}` | ✗ | ✗ | ✓ |
| `GET /dashboard/summary` | ✓ | ✓ | ✓ |
| `GET /dashboard/categories` | ✗ | ✓ | ✓ |
| `GET /dashboard/trends` | ✗ | ✓ | ✓ |
| `GET /dashboard/recent` | ✓ | ✓ | ✓ |
| `POST /dashboard/recalculate` | ✗ | ✗ | ✓ |

---

## Data Models

### User
```json
{
  "id":       "string — MongoDB ObjectId",
  "name":     "string",
  "email":    "string — unique",
  "password": "string — bcrypt hashed",
  "role":     "ADMIN | ANALYST | VIEWER",
  "active":   "boolean"
}
```

### Category
```json
{
  "id":        "string",
  "name":      "string",
  "type":      "INCOME | EXPENSE",
  "createdBy": "string — email of creating admin"
}
```

### FinancialRecord
```json
{
  "id":         "string — MongoDB ObjectId",
  "userId":     "string — audit trail of who submitted",
  "amount":     "decimal — minimum 0.01",
  "type":       "income | expense — normalised to lowercase",
  "categoryId": "string — must reference existing category of matching type",
  "date":       "ISO 8601 date (YYYY-MM-DD)",
  "notes":      "string — optional"
}
```

### SummarySnapshot
```json
{
  "id":          "string — MongoDB ObjectId",
  "periodType":  "week | month | year",
  "periodKey":   "2026-W14 | 2026-04 | 2026",
  "categoryId":  "string | __all__",
  "income":      "decimal",
  "expense":     "decimal",
  "net":         "decimal — income minus expense",
  "recordCount": "integer"
}
```

> Rows with `categoryId: "__all__"` hold the global total for that period.
> Used by `/dashboard/summary` and `/dashboard/trends`.
> Category-specific rows are used by `/dashboard/categories`.

---

## Seeded Test Data

The following data is already loaded in the live database.

### Admin User
```
email:    admin@gmail.com
password: admin
role:     ADMIN
id:       69d37921fe876ec7f90e3469
```

### Categories in Database
| ID | Name | Type |
|---|---|---|
| `cat_food` | Food & Dining | EXPENSE |
| `cat_transport` | Transportation | EXPENSE |
| `cat_shopping` | Shopping | EXPENSE |
| `cat_bills` | Bills & Utilities | EXPENSE |
| `cat_health` | Health & Medical | EXPENSE |
| `cat_entertainment` | Entertainment | EXPENSE |
| `cat_education` | Education | EXPENSE |
| `cat_salary` | Salary | INCOME |
| `cat_freelance` | Freelance | INCOME |
| `cat_investment` | Investment | INCOME |

### April 2026 Records Summary
| Type | Count | Total Amount |
|---|---|---|
| Income | 3 | ₹60,500 |
| Expense | 12 | ₹15,530 |
| **Net** | **15** | **₹44,970** |

---

### Quick Test Sequence

**Step 1 — Login and get token:**
```
POST /auth/login
Body: { "email": "admin@gmail.com", "password": "admin" }
```

**Step 2 — View all-time summary:**
```
GET /dashboard/summary
```

**Step 3 — View April 2026 category breakdown:**
```
GET /dashboard/categories?periodType=month&periodKey=2026-04
```

**Step 4 — View weekly trends:**
```
GET /dashboard/trends?periodType=week
```

**Step 5 — Filter only expense records in April:**
```
GET /records?type=EXPENSE&startDate=2026-04-01&endDate=2026-04-30
```

**Step 6 — Add a new record and watch summary update:**
```
POST /records
Body: {
  "userId": "69d37921fe876ec7f90e3469",
  "amount": 5000,
  "type": "INCOME",
  "categoryId": "cat_freelance",
  "date": "2026-04-06",
  "notes": "New freelance payment"
}
```
Then call `GET /dashboard/summary` again — income and net will have increased by 5000.

**Step 7 — Verify full consistency:**
```
POST /dashboard/recalculate
```
