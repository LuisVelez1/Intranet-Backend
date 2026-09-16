# Enterprise Intranet API

A Spring Boot REST API for a generic enterprise intranet. The repository contains the current backend foundation for user directory and profile operations, authentication, areas, requirements, news and comments, document uploads, fixed assets, and reservations.

## Current capabilities

- JWT-based login and authenticated user operations.
- User directory, profiles, registration, and birthday queries.
- Area, requirement, requirement type, and comment endpoints.
- News and news comment endpoints, including multipart image handling.
- Document upload, download, categorization, and management.
- Fixed asset and reservation management.
- Spring Boot Actuator health, info, and metrics endpoints.

The available HTTP endpoints are rooted at `/api` and the application listens on port `8081` by default. Endpoint paths and JSON contracts are part of the current application and are intentionally unchanged in this phase.

## Technology stack

- Java 21
- Spring Boot 4.0.6
- Spring Web, Spring Data JPA, Spring Security, and Bean Validation
- MySQL for the normal runtime database
- JJWT for token handling
- Lombok
- Maven Wrapper
- JUnit and Spring Boot test support

## Current architecture

The application currently uses a conventional layered Spring Boot structure under the `com.backendintranet` package. Controllers expose HTTP endpoints, services contain application operations, repositories access persistence, and entities and DTOs model data. A package-by-feature migration is intentionally deferred to a later phase.

## Project structure

```text
src/main/java/com/backendintranet/
├── config/          Security and application configuration
├── controller/      REST controllers
├── dto/             Request and response data transfer objects
├── entity/          JPA entities
├── exception/       Exception handling
├── repository/      Spring Data repositories
└── service/         Application services
```

## Prerequisites

- JDK 21.
- A MySQL instance for normal application execution.
- Docker is optional for container execution.

Maven is provided by the wrapper; a separate Maven installation is not required.

## Local development

1. Set the environment variables required by your local database and JWT configuration, including the required `JWT_SECRET`. The repository includes `.env.example` as a reference; load its values into your shell without committing a real `.env` file.

   ```powershell
   $env:DATABASE_URL = "jdbc:mysql://localhost:3306/intranet_corporativa?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   $env:DATABASE_USERNAME = "local_user"
   $env:DATABASE_PASSWORD = "local_password"
   $env:JWT_SECRET = "replace-with-a-secret-at-least-32-characters-long"
   $env:JWT_EXPIRATION_MS = "86400000"
   ```

2. Start the application:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

The context path is `/api`; for example, login is available at `POST http://localhost:8081/api/auth/login`.

## Maven commands

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
java -jar target/enterprise-intranet-api-0.0.1-SNAPSHOT.jar
```

The test profile uses an in-memory H2 database for the context smoke test. Other application behavior still targets MySQL.

## Docker

Build and run the existing multi-stage image:

```powershell
docker build -t enterprise-intranet-api .
docker run --rm -p 8081:8081 `
  -e DATABASE_URL="jdbc:mysql://host.docker.internal:3306/intranet_corporativa?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
  -e DATABASE_USERNAME="local_user" `
  -e DATABASE_PASSWORD="local_password" `
  -e JWT_SECRET="replace-with-a-secret-at-least-32-characters-long" `
  enterprise-intranet-api
```

The image builds the JAR with tests skipped and runs it as a non-root user on port `8081`. Run the Maven test command separately before building an image.

## Security and configuration

Authentication uses Spring Security and a custom JWT filter. Clients send a bearer token in the `Authorization` header after login. Current authorization rules, CORS behavior, token settings, and endpoint contracts are preserved as implemented.

Configuration is read from environment variables when provided:

| Variable | Purpose | Default |
| --- | --- | --- |
| `DATABASE_URL` | JDBC connection URL | Local MySQL URL |
| `DATABASE_USERNAME` | Database user | `local_user` |
| `DATABASE_PASSWORD` | Database password | `local_password` |
| `JWT_SECRET` | Signing secret | Required; no default |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds | `86400000` |

Do not commit real credentials or secrets. Local environment files are ignored by Git.

## Known limitations and technical debt

- The normal runtime still expects a compatible MySQL schema; migrations are not included yet.
- Existing debug SQL, security, and application logging remains enabled for now.
- Multipart upload limits remain at 1 GB to preserve current behavior.
- The project has no CI pipeline, OpenAPI contract, container orchestration, or production deployment profile yet.
- Test coverage is limited; this phase adds only deterministic configuration for the existing context smoke test.
