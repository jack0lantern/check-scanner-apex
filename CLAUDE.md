# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Spring Boot / Maven)

```bash
# Start DB only (required for local backend dev)
docker compose up -d db

# Run backend locally (loads .env first)
export $(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run

# Run all tests (Maven unit/integration + vendor-stub doc validation script)
make test
# or equivalently:
./mvnw test && ./scripts/validate-vendor-stub-docs.sh

# Run a single test class
./mvnw test -Dtest=DepositSubmissionTest

# Run a single test method
./mvnw test -Dtest=DepositSubmissionTest#testHappyPath

# Final end-to-end validation (Phase 11.7)
make validate
# or: ./scripts/validate_e2e.sh
# Use --skip-demos when backend is not running
```

### Frontend (React / Vite / Vitest)

```bash
cd frontend
npm install
npm run dev          # dev server at http://localhost:5173
npm run build        # tsc + vite build
npm run lint         # eslint
npm run test         # vitest run (single pass)
npm run test:watch   # vitest watch mode
```

### Mobile (React Native / Expo)

```bash
# Install dependencies (run from repo root after initial setup)
npm install

# Start Expo dev server
cd mobile && expo start

# Open on iOS Simulator (press 'i' in expo terminal, requires Xcode)
# Open on Android Emulator (press 'a', requires Android Studio)

# Physical device — set your machine's LAN IP:
EXPO_PUBLIC_API_BASE_URL=http://192.168.x.x:8080 expo start
# Android emulator uses: http://10.0.2.2:8080
# Find LAN IP: ipconfig getifaddr en0

# TypeScript check
cd mobile && npx tsc --noEmit
```

### Shared Package (@apex/shared)

```bash
# Build shared types + API factories (required before first npm install)
npm run build:shared
# or: cd packages/shared && npx tsc

# Run all workspace tests
npm run test:web        # frontend vitest
```

### Full Stack via Docker

```bash
cp .env.example .env   # set POSTGRES_PASSWORD
docker compose --profile full up -d   # db + backend + frontend
```

## Architecture

**Modular monolith** — all components are Spring beans with direct method calls; no microservice network hops.

### Request Flow

```
React (5173) → POST /api/deposits → [Vite proxy strips /api] → Spring Boot (8080)
Expo (mobile) → POST /deposits → Spring Boot (8080)
```

1. `DepositController` extracts `X-Account-Id` via `MockAuthInterceptor` → `AuthContextHolder`
2. `DepositService.submit()` orchestrates: account resolution → vendor assessment → funding validation → ledger posting
3. `StubVendorService` selects scenario deterministically from the `X-Account-Id` value (see table below)
4. `FundingService` enforces business rules (max $5000, 24h duplicate window, retirement contribution cap)
5. Transfer is saved with a `TransferState` and trace events written to `trace_events`

### Key Packages

| Package | Responsibility |
|---------|---------------|
| `deposit` | `DepositService` — orchestrates the full submit/retry flow |
| `vendor` | `VendorService` interface + `StubVendorService` (@Primary) |
| `funding` | `FundingService` (business rules), `AccountResolutionService`, `MicrParser` |
| `ledger` | `LedgerPostingService` (double-entry), `ReturnService` ($30 fee reversal) |
| `operator` | `OperatorService` (approve/reject queue), `TransferSpecs` (JPA Specification filters) |
| `settlement` | `EodSchedulerService` (cron), `SettlementFileService` (X9 ICL JSON), `SettlementAckService` |
| `trace` | `TraceEventService` — writes structured audit rows per stage |
| `auth` | `MockAuthInterceptor` reads `X-User-Role`/`X-Account-Id`; `OperatorRoleInterceptor` guards `/operator/**` |
| `@apex/shared` | TypeScript types, API fetch factories (shared between web and mobile) |

### API Endpoints

| Method | Path | Auth Header |
|--------|------|-------------|
| `POST` | `/deposits` | `X-User-Role: INVESTOR` |
| `GET` | `/deposits/{id}` | any |
| `GET` | `/deposits/{id}/trace` | any |
| `GET` | `/operator/queue` | `X-User-Role: OPERATOR` |
| `POST` | `/operator/queue/{id}/approve` | `X-User-Role: OPERATOR` |
| `POST` | `/operator/queue/{id}/reject` | `X-User-Role: OPERATOR` |
| `POST` | `/internal/returns` | internal |
| `POST` | `/internal/settlement/ack` | internal |
| `GET` | `/debug/vendor-stub` | dev only |
| `GET` | `/accounts/{id}/ledger` | any |

### Vendor Stub Scenarios

Scenario is selected by the `X-Account-Id` header value passed with the deposit request:

| `X-Account-Id` | Scenario |
|----------------|----------|
| `iqa-pass` | IQA_PASS |
| `iqa-blur` | IQA_FAIL_BLUR → 422 |
| `iqa-glare` | IQA_FAIL_GLARE → 422 |
| `micr-fail` | MICR_READ_FAILURE → 422 |
| `duplicate` | DUPLICATE_DETECTED → 422 |
| `amount-mismatch` | AMOUNT_MISMATCH → 422 |
| `routing-mismatch` | ROUTING_MISMATCH (caught by FundingService) → 422 |
| anything else | CLEAN_PASS |

Test the stub directly: `GET /debug/vendor-stub?accountId=<trigger>`

### Transfer State Machine

`REQUESTED → VALIDATING → ANALYZING → APPROVED → FUNDS_POSTED → COMPLETED`
Terminal failure states: `REJECTED`, `RETURNED`

Retries are allowed from `VALIDATING` or `REQUESTED` by including `retryForTransferId` in the deposit request body.

### Database & Migrations

- PostgreSQL (prod) / H2 in-memory (tests via `application-test.properties`)
- Flyway migrations in `src/main/resources/db/migration/` (V1–V7)
- Schema: `transfers`, `ledger_entries`, `audit_logs`, `trace_events`, `accounts`, `settlement_batches`

### Configuration

Key properties in `application.properties` / `.env`:

| Property | Default | Notes |
|----------|---------|-------|
| `eod.cron` | `0 * * * * *` (every min, dev) | Change to `0 30 18 * * *` for prod |
| `settlement.output-path` | `./settlement-output` | X9 ICL JSON files written here |
| `settlement.ack-timeout-minutes` | `60` | Logs `SETTLEMENT_ACK_TIMEOUT` if exceeded |
| `funding.max-deposit-amount` | `5000` | Business rule cap |
| `return.fee-amount` | `30` | Fee charged on returned checks |
| `cors.allowed-origins` | `http://localhost:5173` | Comma-separated for multi-origin |

### Frontend

- Vite proxies `/api/*` → `http://localhost:8080` (strips `/api` prefix)
- Single view: `InvestorView.tsx` — deposit form with IQA error + retry flow
- API layer: `frontend/src/api/depositApi.ts`
- Tests use Vitest + Testing Library (`jsdom` environment)

### Adding a New Vendor Scenario

1. Add enum to `VendorScenario`
2. Add mapping in `StubVendorService.ACCOUNT_TO_SCENARIO`
3. Add `actionableMessage` in `ACTIONABLE_MESSAGES` if it produces a user-facing error
4. Add `case` branch in `assessCheck` switch
5. Update `docs/vendor-stub-scenarios.md`
