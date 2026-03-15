# Decision Log

This document records key stack choices and implementation decisions made during the Mobile Check Deposit System build, including rationale and alternatives considered. It complements the [Pre-Search Architecture Document](Pre-Search%20Architecture%20Document%20Finalized.md) and the original [PRD](Apex%20Fintech%20Services%20-%20Challenger%20Projects.md).

---

## Stack Choices

### 1. Java over Golang

**Decision:** Java.

**Rationale:** Mature ecosystem, strong support for agentic development, proven enterprise scalability, and familiarity for solo development under a 3-day timeline. Spring Boot provides rapid scaffolding for REST APIs, JPA, validation, and scheduling with minimal boilerplate.

**Alternatives considered:** Golang — faster compile times and simpler deployment, but less mature ORM/validation ecosystem for the ledger and transfer state machine; would require more custom code for the same feature set.

---

### 2. PostgreSQL over SQLite/JSON

**Rationale:** PostgreSQL satisfies the "local" requirement while providing robust support for double-entry ledger semantics, ACID transactions, and complex queries (operator queue filters, settlement batching). Proven solution for enterprise scalability. SQLite would work but lacks some concurrency guarantees; JSON files would require custom locking and are harder to query. PostgreSQL in Docker is trivial to spin up and aligns with production-like patterns.

**Alternatives considered:** SQLite — simpler, file-based; JSON — minimal setup but poor fit for relational ledger data and concurrent access.

---

### 3. Spring Boot Monolith

**PRD:** Funding Service middleware, REST API endpoints, Vendor Service stub, settlement file generation, return handling.

**Decision:** Single Spring Boot application (modular monolith) hosting all components. Internal method calls between bounded contexts (e.g., Funding Service → Ledger) rather than network calls.

**Rationale:** Zero-cost boundaries, no network latency, single deployment unit, and simpler debugging. Bounded contexts (deposit, vendor, funding, ledger, operator, settlement, trace) are enforced via package structure and Spring beans, not physical separation.

**Alternatives considered:** Microservices — overkill for evaluator test runs and $0 budget; would add complexity without benefit.

---

### 4. React + Vite over CLI/Minimal UI

**Decision:** React frontend with Vite, TypeScript, and Vitest.

**Rationale:** "Minimal UI" was interpreted as functional but not feature-rich. React provides a familiar, component-based model for the investor deposit form and operator queue. Vite offers fast HMR and simple proxy configuration for the backend. TypeScript improves maintainability. Vitest + Testing Library provide fast, Jest-compatible frontend tests.

**Alternatives considered:** CLI-only — would satisfy PRD but is harder to demo and less representative of a real mobile client simulation; plain HTML/JS — viable but more verbose for stateful forms and API integration.

---

### 5. Mock Authentication

**PRD:** Validate investor session and account eligibility; no explicit auth mechanism specified.

**Decision:** Mock authentication via HTTP headers (`X-User-Role`, `X-Account-Id`). Hardcoded profiles (Investor, Operator). No JWT or session management.

**Rationale:** PRD allows synthetic data and local development. Full auth is deferred as a post-MVP enhancement. Header-based mock auth is trivial to configure in curl and demo scripts, and keeps the API surface simple for evaluators.

---

### 6. Vendor Stub Trigger: Request Header

**PRD:** Stub configurable without code changes via request parameters, test account numbers, or configuration file.

**Decision:** Deterministic scenario selection via `X-Account-Id` header (e.g., `iqa-blur`, `duplicate`, `clean-pass`). Values map to `VendorScenario` enum in `StubVendorService`.

**Rationale:** Request headers are easy to set in curl, Postman, and frontend; no config file edits required between test runs. Account ID is a natural discriminator for "which investor is submitting" and aligns with the PRD's "test account numbers" option.

**Alternatives considered:** Config file — requires restart or file watch; query parameter — less RESTful for POST body; separate test endpoint — added `GET /debug/vendor-stub?accountId=<trigger>` for quick manual verification.

---

### 7. X9 ICL Structured JSON

**Decision:** Structured JSON equivalent of X9 ICL (check detail records, MICR data, image references, batch metadata) written to `./settlement-output/`, as opposed to yaml or plaintext.

**Rationale:** Full binary X9 ICL is complex and tooling-heavy. A JSON representation preserves the required semantics (MICR, amounts, images, batch) and is human-readable for evaluation. File naming includes date and batch ID for traceability.

---

### 8. Flyway for Schema Management

**Decision:** Flyway migrations in `src/main/resources/db/migration/` (V1–V7). Hibernate/JPA do not manage DDL.

**Rationale:** Versioned, repeatable schema changes; clear audit trail of schema evolution; avoids Hibernate auto-DDL in production. Aligns with enterprise practices.

---

### 9. H2 for Tests

**Decision:** H2 in-memory database for integration tests; PostgreSQL for local dev and Docker.

**Rationale:** Fast test execution, no Docker dependency for `./mvnw test`. `application-test.properties` switches datasource to H2. Schema is shared via Flyway.

---

### 10. Spotless for Code Style

**Decision:** Spotless Maven plugin with Google Java Format.

**Rationale:** Enforces consistent style for evaluators; single command (`mvn spotless:apply`) to format; `mvn spotless:check` for CI.

---

## Implementation Choices Not Directed by PRD

### 1. Routing Mismatch Scenario

**PRD:** Lists 7 vendor scenarios (IQA pass, IQA fail blur/glare, MICR failure, duplicate, amount mismatch, clean pass).

**Decision:** Added 8th scenario `ROUTING_MISMATCH` — vendor returns MICR with routing number that does not match the investor's account routing. Funding Service rejects before ledger posting.

**Rationale:** Demonstrates validation gating beyond vendor response (business rule: check routing must match account). Trigger: `X-Account-Id: routing-mismatch`.

---

### 2. Debug Endpoint for Vendor Stub

**PRD:** Not specified.

**Decision:** `GET /debug/vendor-stub?accountId=<trigger>` returns the assessment result for a given trigger without submitting a deposit.

**Rationale:** Quick manual verification of stub behavior; useful for evaluators and development.

---

### 3. One-Command Setup: Docker Compose + Make

**PRD:** One-command setup (e.g., `make dev` or `docker compose up`).

**Decision:** `docker compose --profile full up -d` for full stack; `docker compose up -d db` for DB-only (backend run locally with `./mvnw spring-boot:run`). `make test` runs Maven tests plus vendor-stub doc validation script.

**Rationale:** Supports both "run everything in Docker" and "run backend locally for faster iteration" workflows. Makefile provides `test` and other convenience targets.

---

### 11. npm workspaces over monorepo tools (Turborepo/Nx)

**Decision:** npm workspaces.

**Rationale:** Chosen for simplicity; two consumers don't warrant build graph orchestration overhead. Can be layered in later.

**Alternatives considered:** Turborepo — adds caching and task graph but introduces a dependency and configuration surface that is unnecessary for two packages sharing one library. Nx — similar trade-off with even more configuration.

---

### 12. `expo-image-picker` over `expo-camera`

**Decision:** `expo-image-picker`.

**Rationale:** Delegates to system camera UI (trusted, handles permissions); returns base64 directly. Custom viewfinder would require lifecycle management and adds no value for check capture.

**Alternatives considered:** `expo-camera` — full camera control with custom viewfinder, but requires managing camera lifecycle, permissions prompts, and capture UX. Overkill for capturing a check image.

---

### 13. CommonJS output for `@apex/shared`

**Decision:** CommonJS (`"module": "commonjs"`) in `packages/shared/tsconfig.json`.

**Rationale:** Metro (Expo's bundler in SDK 51) has incomplete ESM support for packages outside the project root. CJS avoids interop issues; Vite handles CJS fine. Revisit when Metro ESM stabilizes.

**Alternatives considered:** ESM output — cleaner for Vite but causes `ERR_REQUIRE_ESM` or unresolved import issues in Metro without additional configuration.

---

## Summary Table

| Area | PRD | Decision |
|------|-----|----------|
| Language | Golang or Java | Java |
| Data store | SQLite, JSON, or equivalent | PostgreSQL (Docker) |
| Architecture | Service boundaries implied | Modular monolith |
| UI | Minimal UI or CLI | React + Vite + TypeScript |
| Auth | Session validation | Mock headers |
| Stub config | Params, account IDs, or config file | `X-Account-Id` header |
| Settlement format | X9 ICL or JSON equivalent | Structured JSON |
| Schema | Not specified | Flyway migrations |
| Test DB | Not specified | H2 in-memory |
| Code style | Clean code | Spotless (Google Java Format) |
| Vendor scenarios | 7 required | 8 (added ROUTING_MISMATCH) |
| Debug endpoint | Not specified | `GET /debug/vendor-stub?accountId=<trigger>` |
| Setup | One-command (make dev or docker compose up) | Docker Compose + Make |
| Monorepo tooling | Not specified | npm workspaces (no Turborepo/Nx) |
| Mobile camera | Not specified | `expo-image-picker` (system camera UI) |
| Shared package format | Not specified | CommonJS output for Metro compat |
