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

### Backend

The backend uses the `dev` profile and PostgreSQL at `jdbc:postgresql://localhost:5432/librarydb`.

From the project root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The API runs on `http://localhost:8081` by default.

### Frontend

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

---

## Development notes

This project is structured as a combined monorepo with:

- `backend/` for the Spring Boot REST API
- `frontend/` for the React UI
- `terraform/` for cloud infrastructure
- `helm/` for Kubernetes deployments
- `monitoring/` and `jmeter/` for observability and load testing

The application includes JWT-based authentication, book search, issue/return flow, loan history, admin operations, and cloud-ready deployment support.
