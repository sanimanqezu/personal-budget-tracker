# Personal Budget Tracker

A full-stack personal finance application built with **Spring Boot** (backend REST API) and **Next.js 15** (frontend). Track income and expenses, visualize spending by category, and manage your monthly budget — all secured with JWT authentication.

---

## Tech Stack

| Layer     | Technology                                              |
|-----------|---------------------------------------------------------|
| Backend   | Java 17, Spring Boot 3.2.5, Spring Security, JJWT 0.12.3 |
| Database  | PostgreSQL 16, Flyway migrations                        |
| ORM       | Spring Data JPA / Hibernate                             |
| Frontend  | Next.js 15 (App Router), React 19, TypeScript           |
| Styling   | Tailwind CSS 4, dark mode support                       |
| State     | Zustand                                                 |
| Charts    | Recharts (Pie chart for category breakdown)             |
| Auth      | JWT (stateless, stored in localStorage via Zustand)     |
| Container | Docker, Docker Compose                                  |

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
│   │   └── test/                    # Integration tests (H2 in-memory)
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

- Java 17+
- Maven 3.8+
- Node.js 20+
- PostgreSQL 16 **or** Docker

---

### Option 1 — Docker Compose (recommended)

```bash
# Clone the repo
git clone https://github.com/yourusername/personal-budget-tracker.git
cd personal-budget-tracker

# Start everything (Postgres + Backend + Frontend)
docker-compose up --build

# App will be available at:
#   Frontend:  http://localhost:3000
#   Backend:   http://localhost:8080
```

---

### Option 2 — Run Locally

#### Backend

```bash
cd backend

# Set environment variables (or use defaults in application.yml)
export DB_URL=jdbc:postgresql://localhost:5432/budgetdb
export DB_USERNAME=budgetuser
export DB_PASSWORD=budgetpass
export JWT_SECRET=myVerySecretKeyThatIsAtLeast256BitsLongForHmacSha256Algorithm!!

# Run
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

### Running Tests

```bash
cd backend
mvn test
```

Tests use H2 in-memory database — no Postgres required. Covers:

- Duplicate registration rejection (409)
- Wrong-password login rejection (401)
- Protected endpoint without token (403)
- Category CRUD
- Transaction creation + monthly summary
- Transaction deletion + 404 check
- Category-based percentage summary
- Month-based transaction filtering

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

| Variable      | Default                          | Description              |
|---------------|----------------------------------|--------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/budgetdb` | JDBC URL    |
| `DB_USERNAME` | `budgetuser`                     | Database user            |
| `DB_PASSWORD` | `budgetpass`                     | Database password        |
| `JWT_SECRET`  | _(change in production)_         | HMAC-SHA256 signing key (min 256 bits) |

### Frontend (`.env.local`)

| Variable               | Default                     | Description              |
|------------------------|-----------------------------|--------------------------|
| `NEXT_PUBLIC_API_URL`  | `http://localhost:8080`     | Backend base URL         |
| `NEXTAUTH_URL`         | `http://localhost:3000`     | NextAuth callback URL    |
| `NEXTAUTH_SECRET`      | _(change in production)_    | NextAuth session secret  |

---

## License

MIT
