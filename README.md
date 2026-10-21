# Folio Library

This is the first runnable version of the library system: React frontend, Spring
Boot REST API, Spring Security JWT authentication, and PostgreSQL storage.

## Project structure

- `backend/` contains the Spring Boot API, Maven wrapper, and backend container files.
- `frontend/` contains the React application.
- `helm/`, `terraform/`, `monitoring/`, and `jmeter/` contain deployment,
  infrastructure, observability, and load-test resources.

## Local setup

The backend uses the `dev` profile and connects to PostgreSQL at
`jdbc:postgresql://localhost:5432/librarydb` as `postgres`. Create the database
first if it does not exist. Schema updates and demo data are enabled only in the
development profile. Run backend commands from the `backend/` directory.

In one terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs at `http://localhost:8081` by default. Set `SERVER_PORT` to override
the port. Database settings can be overridden with `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`.

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Development accounts:

| Role | Email | Password |
|---|---|---|
| Student | `student@library.local` | `library123` |
| Admin | `admin@library.local` | `library123` |

The first version supports signing in, searching books, borrowing and returning
books, viewing loan history, calculating overdue fines at ₹5 per day, and adding
books as an admin.

## Production configuration

For production, set `SPRING_PROFILES_ACTIVE=prod` and provide
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and
`SPRING_DATASOURCE_PASSWORD`, plus a `JWT_SECRET` of at least 32 bytes. The
production profile validates the existing schema instead of changing it. Do not
use the development JWT secret or demo accounts in production.