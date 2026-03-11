# Submission

## Project name

Mobile Check Deposit System

## Summary (3–5 sentences)

A minimal end-to-end mobile check deposit system for brokerage accounts. Investors submit front/back check images and amounts; the system validates via a configurable Vendor Service stub, enforces business rules (routing match, $5k limit, duplicate detection), routes flagged deposits through an operator review queue, posts to a double-entry ledger, and settles via X9 ICL JSON files. **Design choices:** Modular monolith for simplicity and local iteration; Vendor stub triggered by `X-Account-Id` header for deterministic scenario testing without code changes; mock auth via headers for MVP. **Key trade-offs:** Monolith over microservices (zero network hops, single deployment); stubbed integrations over real APIs (enables full lifecycle demo); structured JSON over binary X9 ICL (human-readable for evaluation).

## How to run (copy-paste commands)

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env and set POSTGRES_PASSWORD (required)

# 2. Start full stack (db + backend + frontend)
docker compose --profile full up -d

# 3. Access the app
# Frontend: http://localhost:5173
# Backend:  http://localhost:8080
```

**Alternative (DB only, run backend locally):**

```bash
cp .env.example .env
docker compose up -d db
export $(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run
# In another terminal: cd frontend && npm install && npm run dev
```

**Run demo scripts (requires backend running):**

```bash
bash tests/demo_happy_path.sh
bash tests/demo_rejection.sh
bash tests/demo_manual_review.sh
bash tests/demo_return_reversal.sh
```

**Run tests and generate reports:**

```bash
make test-report
# View: open reports/index.html
```

## Test/eval results

- **Maven tests:** `./mvnw test` — 50+ tests covering happy path, all 7 Vendor stub scenarios, business rules, state machine transitions, reversal posting, settlement file validation, and performance benchmarks.
- **JaCoCo coverage:** ~86% instruction coverage, ~63% branch coverage (see `reports/index.html`).
- **Demo scripts:** All four flows (happy path, rejection/resubmission, manual review, return/reversal) pass when run against a live backend.

**Log excerpt (Maven test summary):**

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.apexfintech.checkdeposit.DepositSubmissionTest
...
[INFO] Tests run: 50+, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```

**Reports:** See [`reports/index.html`](reports/index.html) for JaCoCo coverage and Surefire test results.

## With one more week, we would

1. **Production auth** — Replace mock headers with JWT/session-based authentication and RBAC.
2. **Real Vendor API integration** — Swap `StubVendorService` for a real check imaging vendor; add retries, circuit breakers, and reconciliation.
3. **Richer operator search/filter** — Full-text search, export to CSV, and bulk approve/reject.
4. **Compliance hardening** — PII redaction in logs, audit retention policies, and regulatory documentation.
5. **End-to-end load testing** — Simulate concurrent deposits and measure latency under load.

## Risks and limitations

- **Synthetic data only** — No real PII, account numbers, or check images. For demonstration and evaluation only.
- **No compliance or regulatory claims** — This system is not certified for production use. It does not constitute legal, regulatory, or compliance advice.
- **Stubbed integrations** — Vendor Service and Settlement Bank are stubbed. Real integrations require additional security, retries, and reconciliation.
- **Mock authentication** — Uses `X-User-Role` and `X-Account-Id` headers for MVP. Production requires proper session/JWT management.
- **Local development focus** — Designed for local Docker deployment. Cloud deployment would need additional configuration and security hardening.
- **EOD cron** — Defaults to every minute in dev; production should use `0 30 18 * * *` (6:30 PM CT).

## How should ACME evaluate production readiness?

1. **Replace stubs** — Integrate real Vendor API and Settlement Bank; validate X9 ICL format against bank requirements.
2. **Auth and secrets** — Implement JWT/OAuth; move all secrets to a vault (e.g., HashiCorp Vault, AWS Secrets Manager).
3. **Observability** — Add distributed tracing (OpenTelemetry), structured logging to a SIEM, and alerting for settlement ack timeouts and missing files.
4. **Compliance review** — Engage legal/compliance for Reg CC, BSA/AML, and data retention; document controls.
5. **Load and resilience** — Run load tests; validate idempotency and duplicate handling under concurrency.
6. **Deployment** — Containerize for target cloud (e.g., EKS, GKE); configure CI/CD, blue-green or canary releases.
