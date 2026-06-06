# Backend Infrastructure and Tech Stack

## Core Application

- Language: Java 21
- Framework: Spring Boot
- Build tool: Gradle via the checked-in Gradle Wrapper (`./gradlew`)
- API style: REST APIs with JSON
- Validation: Jakarta Bean Validation
- Configuration: Spring profiles for local, staging, and production

## Spring Boot Modules

- Spring Web for HTTP APIs
- Spring Data JPA for persistence
- Spring Boot Actuator for health checks, metrics, and operational endpoints
- Spring Validation for request validation

## Database

- Database: PostgreSQL
- Local development: Docker Compose with PostgreSQL
- Connection pooling: HikariCP, provided by Spring Boot defaults

## Infrastructure

- Containerization: Docker
- Local orchestration: Docker Compose
- Cloud deployment target: Render
- Runtime: Java 21 container image
- Secrets: environment variables or a managed secrets service

## Testing

- Unit tests: JUnit 5
- Mocking: Mockito
- Integration tests: Spring Boot Test
- API tests: MockMvc

## Frontend

The frontend React app lives in:

```text
https://github.com/albeortega/flip_iq
```
