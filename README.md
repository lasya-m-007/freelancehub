# FreelanceHub — Java Full Stack (ACSD30)

Freelance marketplace: **Clients** post projects, **Freelancers** bid, Client accepts a bid → completes → reviews. **Admin** manages users and categories.

**Stack:** Java 17 · Spring Boot 3.5 · Spring MVC + Thymeleaf + Bootstrap 5 · Spring Data JPA/Hibernate · MySQL 8 · REST API (JSON) · Postman · Docker · Railway

## Architecture
Browser → Controller → Service → Repository → MySQL. Business rules live in `service/impl`. REST controllers (`controller/api`) reuse the same services and return DTOs. Session login (BCrypt hashes, role guards via `AuthHelper`). Errors: `ApiExceptionHandler` (JSON) and `WebExceptionHandler` + `templates/error/*` (pages).

## Run locally
```sql
CREATE DATABASE freelancehub_db;
CREATE USER 'freelancehub'@'localhost' IDENTIFIED BY 'freelancehub';
GRANT ALL ON freelancehub_db.* TO 'freelancehub'@'localhost';
```
```bash
mvn spring-boot:run        # http://localhost:8080
mvn test                   # service-layer unit tests
```
Defaults live in `application.properties`; override with env vars `MYSQLHOST, MYSQLPORT, MYSQLDATABASE, MYSQLUSER, MYSQLPASSWORD` (or `DB_URL`).

Seeded accounts (first start): `admin@freelancehub.com / Admin@123`, `client@freelancehub.com / Client@123`, `freelancer@freelancehub.com / Freelancer@123`.

## Run with Docker
```bash
docker compose up --build   # app + MySQL on one network, http://localhost:8080
```

## Deploy on Railway
1. Push this folder to a GitHub repo.
2. Railway → **New Project → Deploy from GitHub repo**. `railway.json` + `Dockerfile` are picked up automatically.
3. In the project: **+ New → Database → MySQL**.
4. App service → **Variables** → add (service name `MySQL` assumed; adjust if renamed):

| Variable | Value |
|---|---|
| `MYSQLHOST` | `${{MySQL.MYSQLHOST}}` |
| `MYSQLPORT` | `${{MySQL.MYSQLPORT}}` |
| `MYSQLDATABASE` | `${{MySQL.MYSQLDATABASE}}` |
| `MYSQLUSER` | `${{MySQL.MYSQLUSER}}` |
| `MYSQLPASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `ADMIN_EMAIL` | your email |
| `ADMIN_PASSWORD` | strong password (**do not keep the default**) |
| `SEED_DEMO` | `false` for real use, `true` to keep demo users/project |

5. App service → **Settings → Networking → Generate Domain**.
6. Healthcheck is `/health` (set in `railway.json`). Railway injects `PORT`; the app reads it.

Notes: `prod` profile is on in Docker (no SQL logging, `Secure` session cookie → use the https Railway URL). `SESSION_COOKIE_SECURE=false` only for plain-http local Docker.

## REST API / Postman
Import `postman/FreelanceHub.postman_collection.json`, set `baseUrl`, run top-to-bottom in the Collection Runner. Write endpoints need a session: `POST /api/auth/login` first (Postman keeps the cookie).

| Resource | Endpoints |
|---|---|
| Auth | `POST /api/auth/register · login · logout`, `GET /api/auth/me` |
| Projects | `GET /api/projects?category=&status=&keyword=&minBudget=&maxBudget=`, `GET/PUT/DELETE /api/projects/{id}`, `POST /api/projects`, `POST /{id}/close · complete · review`, `GET /{id}/bids · review` |
| Bids | `GET/POST /api/bids`, `GET/PUT/DELETE /api/bids/{id}`, `POST /api/bids/{id}/accept` |
| Users | `GET/POST /api/users` (admin), `GET/PUT/DELETE /api/users/{id}` |
| Categories | `GET /api/categories`, `POST`/`DELETE` (admin) |

## Business rules
Bid ≤ project budget · bids only on OPEN projects · one bid per freelancer per project · accept = ACCEPTED + all others REJECTED + project IN_PROGRESS (one transaction, row lock) · review only on COMPLETED projects, once · freelancer average rating recomputed via `AVG()` query.

## ER diagram
See `docs/ER-diagram.md` (Mermaid → export PNG for the report).

## Future scope (deliberately deferred)
Messaging, notifications, file-upload portfolios, analytics dashboards, payments/escrow — out of scope for a 2-credit skill course.
