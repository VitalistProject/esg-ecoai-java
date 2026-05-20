# ESG EcoAl API

REST API for the EcoAL Android app, enabling companies to track ESG goals, tasks and analytics.

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- Spring Security + JWT
- Spring Data JPA + Flyway
- Oracle Database 23ai Free
- Docker / Docker Compose

## Quick Start (Docker — recommended)

```bash
docker compose up --build
```

API available at http://localhost:8080  
Swagger UI at http://localhost:8080/swagger-ui.html

**Default seed credentials:**

| Email | Password | Role |
|---|---|---|
| alice@example.com | password | admin |
| bob@example.com | password | user |

## Local Development

**Prerequisites:** Java 21, Maven 3.9+, Oracle Database 23ai Free

The easiest way to run Oracle locally is via Docker:

```bash
docker run -d --name oracle-local \
  -e ORACLE_PASSWORD=ecoal \
  -e APP_USER=ecoal \
  -e APP_USER_PASSWORD=ecoal \
  -p 1521:1521 \
  gvenzl/oracle-free:23-slim
```

1. Set environment variables (or rely on defaults):
   ```bash
   export DB_URL=jdbc:oracle:thin:@localhost:1521/FREEPDB1
   export DB_USER=ecoal
   export DB_PASSWORD=ecoal
   export JWT_SECRET=your-secret
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| DB_URL | jdbc:oracle:thin:@localhost:1521/FREEPDB1 | Oracle JDBC URL |
| DB_USER | ecoal | DB username |
| DB_PASSWORD | ecoal | DB password |
| JWT_SECRET | ecoal-dev-secret-key-... | JWT signing secret (min 256 bits for HS256) |
| SPRING_PROFILES_ACTIVE | (none) | Set to `docker` when running in container |

## Routes

### Auth

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/auth/sign-up | Public | Register user (company must exist via seed) |
| POST | /api/auth/sign-in | Public | Login, returns JWT |
| GET | /api/auth/me | Bearer | Get current user |

### Companies

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/companies | Bearer | Create company |
| GET | /api/companies/:id | Bearer | Get company (tenant-scoped) |
| PUT | /api/companies/:id | Bearer | Update company |
| DELETE | /api/companies/:id | Bearer | Delete company |

### Goals

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/goals | Bearer | List goals (filters: dimension, created_at) |
| POST | /api/goals | Bearer | Create goal (max 10 per dimension) |
| GET | /api/goals/:id | Bearer | Get goal with tasks |
| PUT | /api/goals/:id | Bearer | Update goal |
| DELETE | /api/goals/:id | Bearer | Delete goal |

### Tasks

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/goals/:goalId/tasks | Bearer | List tasks (filters: completed, created_at) |
| POST | /api/goals/:goalId/tasks | Bearer | Create task (max 10 per goal) |
| GET | /api/goals/:goalId/tasks/:id | Bearer | Get task |
| PUT | /api/goals/:goalId/tasks/:id | Bearer | Update task |
| DELETE | /api/goals/:goalId/tasks/:id | Bearer | Delete task |

### Analytics

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | /api/analytics/dimensions | Bearer | ESG dimension progress (period: monthly\|quarterly\|annual) |
| GET | /api/analytics/score | Bearer | Company score |
| GET | /api/analytics/dashboard | Bearer | Dashboard (departments, contributions, energy, training) |

## Entity Schemas

### Company

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated |
| name | String | Company name |
| cnpj | String | Unique identifier |
| global_score_goal | Double | Target score (default: 100) |

### User

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated |
| name | String | Full name |
| email | String | Unique |
| role | String | User role (admin, user, etc.) |
| company_id | Long | FK to companies |
| department | String | Department name |

### Goal

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated |
| title | String | Goal title |
| description | String | Optional |
| dimension | String | environmental, social, governance |
| company_id | Long | FK to companies |

### Task

| Field | Type | Description |
|---|---|---|
| id | Long | Auto-generated |
| title | String | Task title |
| description | String | Optional |
| score | Double | Score value (default: 0) |
| completed | Boolean | Completion status |
| completed_at | Timestamp | Set automatically on completion |
| goal_id | Long | FK to goals |

## Business Rules

- Max 10 goals per ESG dimension per company
- Max 10 tasks per goal
- All data is tenant-scoped by company (multi-tenant)
- Task `completed_at` is set automatically when `completed` transitions to true
