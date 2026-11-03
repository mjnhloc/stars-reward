# Ngôi Sao Rewards Management

Production-oriented monorepo for student reward points.

## Stack
- React + TypeScript + Vite + TanStack Query
- Java 21 + Spring Boot + Spring Data JPA (Hibernate) + Spring Security Resource Server
- PostgreSQL + Flyway
- Docker Compose

## Run with Docker
```bash
cp .env.example .env
docker compose up --build
```
Open `http://localhost:8081`. Local Compose disables authentication only for development.

## Run for development
```bash
docker compose up db
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd frontend && npm install && npm run dev
```

## Production security
Set `APP_SECURITY_ENABLED=true`, configure `JWT_ISSUER_URI` and `JWT_JWK_SET_URI`, serve HTTPS, rotate secrets, and remove demo seed migration if not required. JWT must contain `sub`; application roles and class scope are resolved from PostgreSQL.

## Accuracy and consistency
- Scores use PostgreSQL `numeric` and Java `BigDecimal`.
- Stars and VND are integers.
- Every balance mutation locks the balance row with `SELECT ... FOR UPDATE`.
- Every write requires a unique `Idempotency-Key`.
- Ledger rows are immutable; corrections use reversal transactions.
- Database checks prohibit a negative balance.

## Test
```bash
cd backend && mvn test
cd frontend && npm ci && npm run test && npm run build
```

See `docs/ARCHITECTURE.md`, `docs/API.md`, and `docs/GOOGLE_SHEETS.md`.
