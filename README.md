# Cloud-Based Online Library System

A cloud-hosted, containerized library management system for managing books, borrowing records, user roles, and deployment automation.

**Team:** Mrunal Nalawade (2025510043), Somesh Kate (2025510032), Omkumar Sirmalwad (2025510061)

---

## Project structure

```text
library-project/
├── README.md
├── docker-compose.yml
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── backend/
│   ├── .dockerignore
│   ├── .mvn/
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/library/
│       ├── main/resources/
│       └── test/java/com/library/
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   └── src/
├── helm/
│   └── library-backend/
├── monitoring/
├── jmeter/
├── terraform/
├── target/
├── .gitattributes
├── .gitignore
├── .dockerignore
└── .mvn/
```

---

## Tech stack

- Frontend: React.js
- Backend: Java, Spring Boot, Spring Security
- Database: PostgreSQL
- Cloud: AWS EC2, VPC, IAM, S3, RDS
- Containerization: Docker, Docker Compose
- Orchestration: Kubernetes, Helm, Minikube
- CI/CD: GitHub Actions, Terraform
- Monitoring: Prometheus, Grafana, Spring Boot Actuator

---

## Local setup

Start the database, backend, and frontend in separate PowerShell terminals. Run
the commands from the project root unless noted otherwise.

### 1. PostgreSQL database

The backend dev profile connects to `jdbc:postgresql://localhost:5432/librarydb`
as user `postgres`. Make sure the PostgreSQL Windows service is running:

```powershell
Get-Service *postgres*
Start-Service postgresql-x64-18
```

If the service is already running, skip `Start-Service`. If needed, run
PowerShell as Administrator to start it. Create the database once if it does
not already exist:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\createdb.exe" -U postgres librarydb
```

The dev profile reads optional overrides from `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`; defaults are the URL above, `postgres`, and an empty password.
Set `DB_PASSWORD` in the backend terminal if your PostgreSQL user has a
password.

### 2. Backend

In a second terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs on `http://localhost:8081` by default.

The dev profile creates/updates the schema and inserts demo data. To verify the
database connection, run this from another terminal:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -U postgres -d librarydb -c "SELECT count(*) FROM books;"
```

To run the backend test:

```powershell
cd backend
.\mvnw.cmd test
```

### 3. Frontend

In a third terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

---

## Production configuration

For production, set:

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (minimum 32 bytes)

Before starting a production deployment with the new reservation and renewal
features, back up the database and apply
[`backend/src/main/resources/db/manual/upgrade-prod-schema.sql`](backend/src/main/resources/db/manual/upgrade-prod-schema.sql)
to the production PostgreSQL database. The production profile validates the
schema and does not apply changes automatically.

## Library features

- Members can search books, borrow and return copies, see due dates and
  calculated overdue fines, and renew an eligible loan once for 14 days.
- Members can join a first-in/first-out reservation queue for unavailable
  books. When a copy is returned, the next member can claim it or cancel their
  reservation.
- Admins and librarians can view all loans and reservation queues, record
  returns, and edit book details and copy totals. Admins can also archive books
  without erasing loan history.
- Copy totals cannot be reduced below the number currently borrowed.

---

## Development notes

This project is structured as a combined monorepo with:

- `backend/` for the Spring Boot REST API
- `frontend/` for the React UI
- `terraform/` for cloud infrastructure
- `helm/` for Kubernetes deployments
- `monitoring/` and `jmeter/` for observability and load testing

The application is structured as a combined monorepo for local development and
cloud deployment.
