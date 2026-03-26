# Mobile Check Deposit System

A minimal end-to-end mobile check deposit system that allows investors to deposit checks into brokerage accounts via a mobile application. The system integrates with a stubbed Vendor Service for check image capture, validation, and verification, routes validated deposits through a Funding Service middleware for business rule enforcement and ledger posting, and settles approved deposits with a Settlement Bank.

## Project Overview

This solution handles the full lifecycle—capture, validation, compliance gating, operational review, ledger posting, settlement, and return/reversal scenarios—with a clear operator workflow, audit trail, and stubbed vendor integration that supports differentiated responses for comprehensive scenario testing.

**Tech Stack:** Java (Spring Boot), PostgreSQL, React (Vite), Docker Compose

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17+ and Maven (or use `./mvnw` wrapper)
- Node.js 18+ (for local frontend builds)

### One-Command Setup

```bash
cp .env.example .env
# Edit .env and set POSTGRES_PASSWORD (required)

# Option A: Full stack (db + backend + frontend)
docker compose up -d

# Option B: make dev (db only, foreground)
make dev

# Option C: make run (db + backend via Maven)
make run
```

### Alternative: Database Only

For local development with the Java backend running outside Docker:

```bash
cp .env.example .env
# Set POSTGRES_PASSWORD in .env
docker compose up -d db

# Load .env and run the backend (Maven does not load .env automatically)
export $(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run
```

### Connect to the Database

If `psql` is not installed locally, run it inside the container:

```bash
docker compose exec db psql -U checkuser -d checkdeposit
```

## Configuration

Required environment variables (see `.env.example`):

| Variable | Description | Default |
|----------|-------------|---------|
| `POSTGRES_USER` | PostgreSQL username | `checkuser` |
| `POSTGRES_PASSWORD` | PostgreSQL password | *(required)* |
| `POSTGRES_DB` | Database name | `checkdeposit` |
| `POSTGRES_PORT` | PostgreSQL port (host mapping; avoids conflict with local Postgres on 5432) | `5435` |

## Architecture

The system is a **modular monolith** with the following components:

- **Mobile Client Simulation** — React frontend for deposit submission (image upload, amount, account info)
- **Vendor Service Stub** — Configurable stub returning differentiated responses (IQA pass/fail, MICR failure, duplicate, amount mismatch, clean pass)
- **Funding Service** — Business rule enforcement, account resolution, ledger posting
- **Ledger / Data Store** — PostgreSQL for transfers, ledger entries, operator actions, audit logs
- **Operator UI** — Review queue for flagged deposits (approve/reject, view check images)
- **Settlement Engine** — X9 ICL file generation, EOD batch (6:30 PM CT cutoff)

See [`/docs/architecture.md`](docs/architecture.md) for detailed diagrams and data flow.

## Transfer State Machine

| State | Description |
|-------|-------------|
| Requested | Deposit submitted by investor |
| Validating | Sent to Vendor Service for IQA/MICR/OCR |
| Analyzing | Business rules being applied by Funding Service |
| Approved | Passed all checks; awaiting ledger posting |
| FundsPosted | Provisional credit posted to investor account |
| Completed | Settlement confirmed by Settlement Bank |
| Rejected | Failed validation, business rules, or operator review |
| Returned | Check bounced after settlement; reversal posted |

## Vendor Service Stub Scenarios

The stub supports these deterministic response scenarios (selectable via test account ID, header, or config):

- **IQA Pass** — Image quality acceptable, proceed to MICR/OCR
- **IQA Fail (Blur)** — Image too blurry, prompt retake
- **IQA Fail (Glare)** — Glare detected, prompt retake
- **MICR Read Failure** — Cannot read magnetic ink line, flag for manual review
- **Duplicate Detected** — Check previously deposited, reject
- **Amount Mismatch** — OCR amount differs from user-entered amount, flag for review
- **Clean Pass** — All checks pass, return extracted MICR data and transaction ID

## How to Demo

Automated demo scripts exercise all four flows. Start the stack first (`docker compose up -d`, or `docker compose up -d db` plus `./mvnw spring-boot:run` for local backend), then run:

```bash
bash tests/demo_happy_path.sh      # Clean pass → approve → completed
bash tests/demo_rejection.sh       # IQA failure, duplicate, or routing mismatch
bash tests/demo_manual_review.sh   # MICR failure → operator approve/reject
bash tests/demo_return_reversal.sh # Bounced check → reversal + $30 fee
```

Manual flow: submit deposit via investor form → Vendor stub validation → Funding Service → operator review (if flagged) → ledger posting → EOD settlement → return/reversal.

## Data Flow (Happy Path End-to-End)

1. **Investor submits** — POST `/deposits` with front/back images, amount, account ID.
2. **Vendor stub** — IQA/MICR/OCR validation; returns extracted data or actionable error.
3. **Funding Service** — Enforces routing match, $5k limit, duplicate window, contribution caps.
4. **State transitions** — `REQUESTED` → `VALIDATING` → `ANALYZING` → `APPROVED` (or flagged for operator).
5. **Operator review** — If flagged (e.g. MICR failure), approve or reject via `/operator/queue/{id}/approve` or `/reject`.
6. **Ledger posting** — Debit omnibus, credit investor; state → `FUNDS_POSTED`.
7. **EOD settlement** — Cron (6:30 PM CT) generates X9 ICL file; state → `COMPLETED`.
8. **Return (optional)** — POST `/internal/returns` reverses credit and posts $30 fee; state → `RETURNED`.

## Project Structure

```
├── docs/
│   ├── architecture.md      # System diagram, service boundaries, data flow
│   ├── decision_log.md      # Key decisions and alternatives
│   └── Apex Fintech Services - Challenger Projects.md  # Full spec
├── reports/                 # Test results and scenario coverage
├── tests/                   # Unit and integration tests
├── .env.example             # Required environment variables
├── docker-compose.yml       # One-command setup
└── README.md
```

## Tests

Run tests:

```bash
make test
# or
./mvnw test
```

Generate coverage and reports (JaCoCo + Surefire):

```bash
make test-report
# or
./mvnw clean test jacoco:report
```

View reports: open `reports/index.html` in a browser for test execution and coverage.

Minimum 10 tests covering: happy path, each Vendor stub scenario, business rules, state machine, reversal posting, settlement file validation.

### Pre-commit Hooks

Tests run automatically before each commit. Install the pre-commit hook:

```bash
# Install pre-commit (one-time): pip install pre-commit  or  brew install pre-commit
pre-commit install
# or
make pre-commit-install
```

After installation, `mvn test` runs on every `git commit`. If tests fail, the commit is aborted.

## Submission Summary

- **Project name:** Mobile Check Deposit System
- **Summary:** A minimal end-to-end mobile check deposit system for brokerage accounts. Uses a modular Java monolith with a configurable Vendor Service stub, Funding Service for business rules and ledger posting, operator review workflow, and X9 ICL settlement. Key trade-offs: monolith for simplicity and local iteration; stubbed vendor for deterministic scenario testing; mock auth via headers for MVP.
- **How to run:** `cp .env.example .env` → set `POSTGRES_PASSWORD` → `docker compose up -d` (full stack) or `docker compose up -d db` (database only for local Java)
- **Test/eval results:** See `/reports` for test execution and scenario coverage
- **With one more week, we would:** Add full JWT auth, production-grade error handling, and richer operator search/filter
- **Risks and limitations:** See below
- **Production readiness:** Requires real Vendor API integration, production auth, hardened secrets management, and compliance review

## Risks and Limitations

- **Synthetic data only** — No real PII, account numbers, or check images. For demonstration and evaluation only.
- **No compliance or regulatory claims** — This system is not certified for production use. It does not constitute legal, regulatory, or compliance advice.
- **Stubbed integrations** — Vendor Service and Settlement Bank are stubbed. Real integrations would require additional security, retries, and reconciliation.
- **Mock authentication** — Uses `X-User-Role` and `X-Account-Id` headers for MVP. Production requires proper session/JWT management.
- **Local development** — Designed for local Docker deployment. Cloud deployment would need additional configuration and security hardening.

## License

Proprietary — Apex Fintech Services Challenger Project.
