# Eid Cricket Fest

Backend API for managing Eid Cricket Fest tournaments, teams, drafts, fixtures, live cricket scoring, scorecards, standings, awards, and player history.

## Current Backend Stack

- Spring Boot 4.1.1
- Java 25
- PostgreSQL
- Flyway
- JWT
- WebSocket/STOMP
- Testcontainers
- Docker

## Repository Layout

- `backend/` Spring Boot backend application
- `backend/src/main/resources/db/migration/` Flyway database migrations
- `backend/src/test/` backend unit and integration tests
- `docs/deployment/` deployment notes
- `docs/backend/` backend release/freeze notes
- `.github/workflows/` GitHub Actions CI

## Local Development Prerequisites

- Java 25
- Docker
- PostgreSQL for local development
- Maven wrapper from `backend/mvnw`

## Start

```bash
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

## Tests

```bash
cd backend
./mvnw clean test
```

Integration tests use Testcontainers and start PostgreSQL automatically.

## Docker

```bash
docker build -t eid-cricket-fest-backend ./backend
```

## Swagger In Dev

With the dev profile running:

```text
/swagger-ui.html
/v3/api-docs
```

## Health Endpoint

```text
/actuator/health
/actuator/health/readiness
/actuator/health/liveness
```

## WebSocket Topic

```text
/topic/matches/{matchId}
```

Live match payloads include `innings.inningsId` and `innings.scoreRevision`. Clients can ignore stale messages when the incoming revision is older for the same innings. Revision ordering resets when the current innings ID changes.

## Production Configuration

See `docs/deployment/backend-deployment.md`.
