# Tech Context

## Technologies

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot |
| Database | PostgreSQL (Docker) |
| ORM | Spring Data JPA, Flyway migrations |
| Frontend | React, Vite (planned) |
| Build | Maven |
| Formatting | Spotless (Google Java Format) |
| Tests | JUnit 5, Mockito, MockMvc |

## Development Setup

```bash
cp .env.example .env
# Set POSTGRES_PASSWORD in .env
docker compose up -d db
# Backend: export $(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run
# Full stack: docker compose --profile full up -d
```

## Project Structure

```
├── docs/                    # Architecture, decision log, detailed plan
├── memory-bank/             # AI context (this directory)
├── src/main/java/.../
│   ├── auth/                # MockAuthInterceptor, AuthContext
│   ├── config/              # WebMvcConfig
│   ├── controller/          # DebugController (account-resolve)
│   ├── domain/              # Account, Transfer, TransferState
│   ├── dto/                 # ResolvedAccount, VendorAssessmentResult
│   ├── exception/           # GlobalExceptionHandler
│   ├── funding/             # AccountResolutionService
│   ├── repository/          # AccountRepository, TransferRepository
│   └── vendor/              # VendorService, StubVendorService, VendorScenario
├── src/main/resources/
│   ├── db/migration/        # V1__init_schema.sql, V2, V3
│   └── application.properties
├── src/test/java/           # JUnit tests
├── docker-compose.yml
├── Makefile                 # make run, make test, etc.
├── pom.xml
└── .env.example
```

## Technical Constraints

- **Synthetic data only** — No real PII
- **Local only** — Docker Compose; no cloud deployment
- **Vendor stub** — Trigger via `X-Account-Id`: `iqa-pass`, `iqa-blur`, `iqa-glare`, `micr-fail`, `duplicate`, `amount-mismatch`, `clean-pass`
- **Spotless** — Run `mvn spotless:apply` before commit; `mvn spotless:check` in CI

## Dependencies (Key)

- spring-boot-starter-web, spring-boot-starter-data-jpa
- postgresql driver
- spotless-maven-plugin (Google Java Format)
