# Progress

## What Works

| Phase | Status | Notes |
|-------|--------|-------|
| 1. Infrastructure | ✅ | Docker, Makefile, .env.example, Maven, Spotless, DB schema |
| 2. Domain & State | ✅ | TransferState enum, Transfer/Account entities, AccountResolutionService, MockAuthInterceptor |
| 3. Vendor Stub | ✅ | Stub + 7 scenarios + re-submission + vendor-stub-scenarios.md |

## Implemented

- **Database:** accounts, transfers, ledger_entries, audit_logs, trace_events (Flyway V1, V2, V3)
- **Transfer state machine:** REQUESTED, VALIDATING, ANALYZING, APPROVED, FUNDS_POSTED, COMPLETED, REJECTED, RETURNED
- **Account resolution:** TEST001, TEST002, etc. → internal number, routing, omnibus
- **Mock auth:** X-User-Role, X-Account-Id; 401/403 when missing
- **Vendor stub:** 7 scenarios; trigger by account ID; actionable messages for failures
- **Deposit endpoint:** POST /deposits with retryForTransferId; 422 with actionableMessage on IQA failure
- **Deposit status:** GET /deposits/{transferId} returns full Transfer record (state, timestamps, vendor data)
- **Re-submission:** retry updates existing Transfer, advances state on success
- **Operator queue:** GET /operator/queue returns flagged deposits with full detail; filters: status, dateFrom, dateTo, accountId, minAmount, maxAmount
- **Operator approve:** POST /operator/queue/{transferId}/approve with optional contributionTypeOverride; persists override, posts to ledger, audit log
- **Operator reject:** POST /operator/queue/{transferId}/reject with required reason; 403 without OPERATOR role

## What's Left to Build

| Phase | Items |
|-------|-------|
| 3 | — (complete) |
| 4 | FundingService, ledger posting, return endpoint, reversal logic |
| 5 | REST API: POST /deposits ✅, GET /deposits/{id} ✅, operator queue ✅, approve/reject ✅ |
| 6 | EOD batch, settlement file (X9), next-business-day rollover, ack tracking |
| 7 | Structured logging, trace events, monitors |
| 8 | React frontend (investor form, operator queue, ledger view) |
| 9 | Demo scripts |
| 10 | E2E tests, full test suite |
| 11 | Documentation (architecture, decision log, SUBMISSION.md) |

## Current Status

- **Tests passing:** DatabaseSchemaTest, TransferStateTest, TransferEntityDataJpaTest, AccountResolutionServiceTest, MockAuthInterceptorTest, StubVendorServiceTest
- **App runs:** `make run` or `./mvnw spring-boot:run` with `docker compose up -d db`

## Known Issues

- Deposit endpoint implemented (Phase 3.3); operator queue and approve/reject implemented (Phase 5)
- React frontend not started (Phase 8)
