# Personal Budget Tracker

[![CI](https://github.com/sanimanqezu/personal-budget-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/sanimanqezu/personal-budget-tracker/actions/workflows/ci.yml)

A full-stack personal finance application built with **Spring Boot** (backend REST API) and **Next.js 15** (frontend). Track income and expenses, visualize spending by category, and manage your monthly budget — all secured with JWT authentication.

---

## Tech Stack

| Layer     | Technology                                               |
|-----------|----------------------------------------------------------|
| Backend   | Java 25, Spring Boot 3.5.16, Spring Security, JJWT 0.12.3 |
| Database  | PostgreSQL, Flyway migrations                             |
| ORM       | Spring Data JPA / Hibernate                              |
| Frontend  | Next.js 15 (App Router), React 19, TypeScript            |
| Styling   | Tailwind CSS 4, dark mode support                        |
| State     | Zustand                                                  |
| Charts    | Recharts (Pie chart for category breakdown)              |
| Auth      | JWT (stateless, stored in localStorage via Zustand)      |
| Tests     | JUnit 5, Mockito, AssertJ, Spring MockMvc (H2 in-memory) |
| Container | Docker, Docker Compose                                   |

---

## Key design points

- **Layered architecture** — controller → service → repository, with Java records as immutable request/response DTOs and static `from(...)` mappers from entities.
- **Stateless JWT security** — a `OncePerRequestFilter` validates the bearer token and populates the `SecurityContext`; the filter chain is `STATELESS`, CSRF disabled, and auth endpoints are the only public routes.
- **Per-user data isolation** — every service resolves the authenticated username to a `User` and scopes all reads/writes by `userId` (e.g. `findByIdAndUserId`), so a user can never reach another user's categories or transactions.
- **Centralized exception handling** — a `@RestControllerAdvice` maps domain exceptions to a consistent `ErrorResponse` body: validation → 400, bad credentials → 401, not found → 404, conflicts → 409.
- **Aggregation in the database** — monthly and per-category summaries are computed with JPQL aggregate queries rather than in-memory loops; category percentages are derived with `BigDecimal` and `HALF_UP` rounding.
- **Centralized PostgreSQL** — the backend joins the shared portfolio Postgres via `DB_*` environment variables; schema is owned by Flyway with `ddl-auto: validate`.

---

## Features

- **JWT Authentication** — Register/Login, all protected routes require a valid token
- **Categories** — Create INCOME/EXPENSE categories per user; quick-add defaults provided
- **Transactions** — Add, filter (by month & type), and delete transactions
- **Dashboard** — Monthly summary cards (income, expenses, net balance, count) + Recharts pie chart
- **Data Isolation** — Each user only sees their own data; enforced at the service layer
- **Flyway Migrations** — Versioned SQL schema, reproducible across environments
- **Dockerized** — One `docker-compose up` starts Postgres + backend + frontend

---

## Project Structure

```
personal-budget-tracker/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/sanele/budgettracker/
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── service/         # Business logic
│   │   │   │   ├── repository/      # Spring Data JPA repos
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── dto/             # Java records (request/response)
│   │   │   │   ├── security/        # JWT filter, config, UserDetailsService
│   │   │   │   └── exception/       # Global error handler + custom exceptions
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/V1__create_tables.sql
│   │   └── test/                    # Service unit + controller slice + integration tests
│   ├── Dockerfile
│   ├── docker-compose.yml           # Backend-only compose (dev)
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx             # Redirect logic
│   │   │   ├── login/page.tsx
│   │   │   ├── register/page.tsx
│   │   │   ├── dashboard/page.tsx   # Summary + chart
│   │   │   ├── transactions/page.tsx
│   │   │   └── categories/page.tsx
│   │   ├── components/Navbar.tsx
│   │   ├── lib/api.ts               # Axios instance + API helpers
│   │   ├── store/
│   │   │   ├── authStore.ts         # Zustand auth store (persisted)
│   │   │   └── transactionStore.ts  # Zustand data store
│   │   └── types/index.ts
│   ├── Dockerfile
│   ├── package.json
│   └── .env.local.example
│
└── docker-compose.yml               # Full-stack compose
```

---

## Getting Started

### Prerequisites

- Java 25
- Maven 3.9+
- Node.js 22+
- Docker & Docker Compose

---

### 1. Start the centralized PostgreSQL

The backend connects to the shared portfolio PostgreSQL instance over an external Docker network. Bring it up first from the portfolio root:

```bash
# in the portfolio-root directory
docker-compose up -d
```

---

### 2. Run the backend

```bash
cd backend

# Defaults match the centralized Postgres; override only if needed
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=budgetdb
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=myVerySecretKeyThatIsAtLeast256BitsLongForHmacSha256Algorithm!!

mvn spring-boot:run
```

The API will start on `http://localhost:8080`.

#### Frontend

```bash
cd frontend

# Copy env file
cp .env.local.example .env.local
# Edit .env.local if needed

# Install dependencies
npm install

# Start dev server
npm run dev
```

The frontend will start on `http://localhost:3000`.

---

### Tests

```bash
cd backend
mvn test
```

**53 tests**, self-contained — no real database or network required. Three layers:

| Layer | Classes | What it covers |
|---|---|---|
| Service (unit) | `AuthServiceTest`, `CategoryServiceTest`, `TransactionServiceTest`, `SummaryServiceTest` | Mockito + AssertJ: happy paths, password hashing, duplicate/conflict detection, not-found cases, month/type filtering, summary maths and percentage rounding, and `BadCredentials` propagation |
| Controller (slice) | `AuthControllerTest`, `CategoryControllerTest`, `TransactionControllerTest`, `SummaryControllerTest` | `@WebMvcTest` with `@MockitoBean` services: status codes, request delegation, bean-validation → 400, and `GlobalExceptionHandler` mapping to 401/404/409 |
| Integration | `BudgetTrackerIntegrationTest` | Full `@SpringBootTest` + `MockMvc` over H2: register/login, JWT-protected access (403 without token), category/transaction CRUD, monthly and per-category summaries, and month filtering |

Controller slices run with `@AutoConfigureMockMvc(addFilters = false)` and `@WithMockUser`; the JWT filter is excluded from the slice context. Only the integration test exercises the real security chain.

---

## API Reference

### Auth
| Method | Endpoint              | Body                                      | Auth |
|--------|-----------------------|-------------------------------------------|------|
| POST   | `/api/auth/register`  | `{username, email, password}`             | No   |
| POST   | `/api/auth/login`     | `{username, password}`                    | No   |

### Categories
| Method | Endpoint              | Description                               | Auth |
|--------|-----------------------|-------------------------------------------|------|
| GET    | `/api/categories`     | List all categories for current user      | Yes  |
| GET    | `/api/categories/{id}`| Get single category                       | Yes  |
| POST   | `/api/categories`     | `{name, type}`                            | Yes  |
| PUT    | `/api/categories/{id}`| `{name, type}`                            | Yes  |
| DELETE | `/api/categories/{id}`| Delete category                           | Yes  |

### Transactions
| Method | Endpoint                    | Description                             | Auth |
|--------|-----------------------------|-----------------------------------------|------|
| GET    | `/api/transactions`         | List (filter: `?month=2024-03&type=EXPENSE`) | Yes  |
| GET    | `/api/transactions/{id}`    | Get single transaction                  | Yes  |
| POST   | `/api/transactions`         | `{categoryId, amount, description, transactionDate, type}` | Yes |
| DELETE | `/api/transactions/{id}`    | Delete transaction                      | Yes  |

### Summary
| Method | Endpoint                        | Description                           | Auth |
|--------|---------------------------------|---------------------------------------|------|
| GET    | `/api/summary/monthly?month=2024-03`    | `{totalIncome, totalExpenses, netBalance, transactionCount}` | Yes |
| GET    | `/api/summary/by-category?month=2024-03`| `[{categoryName, type, total, percentage}]` | Yes |

---

## Screenshots

> _Screenshots coming soon — run the app locally to preview_

| Dashboard | Transactions | Categories |
|-----------|--------------|------------|
| ![Dashboard](docs/screenshots/dashboard.png) | ![Transactions](docs/screenshots/transactions.png) | ![Categories](docs/screenshots/categories.png) |

---

## Environment Variables

### Backend (`application.yml` / environment)

| Variable      | Default        | Description                            |
|---------------|----------------|----------------------------------------|
| `DB_HOST`     | `localhost`    | PostgreSQL host                        |
| `DB_PORT`     | `5432`         | PostgreSQL port                        |
| `DB_NAME`     | `budgetdb`     | Database name                          |
| `DB_USERNAME` | `postgres`     | Database user                          |
| `DB_PASSWORD` | `postgres`     | Database password                      |
| `JWT_SECRET`  | _(change in production)_ | HMAC-SHA256 signing key (min 256 bits) |

### Frontend (`.env.local`)

| Variable               | Default                     | Description              |
|------------------------|-----------------------------|--------------------------|
| `NEXT_PUBLIC_API_URL`  | `http://localhost:8080`     | Backend base URL         |
| `NEXTAUTH_URL`         | `http://localhost:3000`     | NextAuth callback URL    |
| `NEXTAUTH_SECRET`      | _(change in production)_    | NextAuth session secret  |

---

## License

MIT
