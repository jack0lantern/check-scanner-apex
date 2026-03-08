# Demo Reference — Mobile Check Deposit System

A quick-reference guide for demonstrating the Apex Fintech Mobile Check Deposit System.

---

## System Overview

**What it does:** End-to-end mobile check deposit for brokerage accounts — capture, validation, compliance gating, operator review, ledger posting, settlement, and return/reversal.

**Tech stack:** Java (Spring Boot), PostgreSQL, React (Vite), Docker Compose

**Auth:** Mock headers — `X-User-Role: INVESTOR` or `OPERATOR`, `X-Account-Id` for account/scenario selection

---

## Key Features

### 1. Deposit Capture & Submission

- **POST /deposits** — Submit front/back check images (Base64), amount, account ID
- Optional `retryForTransferId` for re-submission after IQA failure (updates existing transfer)
- Returns `201 Created` with transfer ID and state, or `422` with `actionableMessage` on validation failure

### 2. Vendor Service Stub (8 Scenarios)

Deterministic responses triggered by `X-Account-Id` or `accountId` in request:

| Scenario | Trigger Account ID | Result |
|----------|--------------------|--------|
| IQA Pass | `iqa-pass` | Proceeds to MICR/OCR |
| IQA Fail (Blur) | `iqa-blur` | 422: "Image too blurry — please retake in better lighting" |
| IQA Fail (Glare) | `iqa-glare` | 422: "Glare detected — please move to a darker surface" |
| MICR Read Failure | `micr-fail` | Flagged for operator review |
| Duplicate Detected | `duplicate` | Reject: "This check has already been deposited" |
| Amount Mismatch | `amount-mismatch` | Flagged for operator review |
| Routing Mismatch | `routing-mismatch` | 422: FundingService rejects (check routing ≠ account routing) |
| Clean Pass | `clean-pass` or unknown | Full success; MICR, OCR, risk scores returned |

### 3. Funding Service Business Rules

- Routing number match — check MICR must match deposit account routing
- Amount limit — max $5,000 per deposit
- Retirement accounts — default `contributionType` to `INDIVIDUAL`; enforce contribution cap
- Internal duplicate detection — reject if same `fromAccountId` + `amount` + `micrData` within configurable window

### 4. Operator Workflow

- **GET /operator/queue** — Flagged deposits with full detail: transferId, state, investorAccountId, enteredAmount, ocrAmount, micrData, micrConfidence, vendorScore, risk flags, frontImage, backImage, submittedAt
- **Filters:** `?status=`, `?dateFrom=`, `?dateTo=`, `?accountId=`, `?minAmount=`, `?maxAmount=`
- **POST /operator/queue/{transferId}/approve** — Optional body: `{ "contributionTypeOverride": "ROTH" }`
- **POST /operator/queue/{transferId}/reject** — Required body: `{ "reason": "..." }`
- All operator actions require `X-User-Role: OPERATOR`
- Structured audit logging for approve, reject, contribution type override

### 5. Ledger Posting

- Transactional: debit omnibus, credit investor; shared `transactionId` and `timestamp`
- Transfer attributes: `type=MOVEMENT`, `memo=FREE`, `subType=DEPOSIT`, `transferType=CHECK`, `currency=USD`

### 6. Return / Reversal

- **POST /internal/returns** — `{ "transferId": "...", "returnReason": "..." }` (requires OPERATOR)
- Creates reversal entries (debit investor, credit omnibus) + $30 fee debit
- State → `RETURNED`
- `INVESTOR_NOTIFIED` audit log entry

### 7. Transfer State Machine

| State | Description |
|-------|-------------|
| REQUESTED | Deposit submitted |
| VALIDATING | Sent to Vendor Service |
| ANALYZING | Funding Service applying rules; may be flagged for operator |
| APPROVED | Passed all checks; ledger posted |
| FUNDS_POSTED | Provisional credit posted |
| COMPLETED | Settlement confirmed |
| REJECTED | Failed validation, rules, or operator review |
| RETURNED | Check bounced; reversal + fee posted |

---

## API Quick Reference

| Method | Endpoint | Headers | Body |
|--------|----------|---------|------|
| POST | `/deposits` | `X-User-Role: INVESTOR`, `X-Account-Id: <trigger>` | `{ frontImage, backImage, amount, accountId, retryForTransferId? }` |
| GET | `/deposits/{transferId}` | `X-User-Role`, `X-Account-Id` | — |
| GET | `/deposits/{transferId}/trace` | `X-User-Role`, `X-Account-Id` | — |
| GET | `/operator/queue` | `X-User-Role: OPERATOR` | Query: status, dateFrom, dateTo, accountId, minAmount, maxAmount |
| POST | `/operator/queue/{transferId}/approve` | `X-User-Role: OPERATOR` | `{ contributionTypeOverride?: "ROTH" }` |
| POST | `/operator/queue/{transferId}/reject` | `X-User-Role: OPERATOR` | `{ "reason": "..." }` |
| POST | `/internal/returns` | `X-User-Role: OPERATOR` | `{ transferId, returnReason }` |
| POST | `/internal/settlement/ack` | `X-User-Role: OPERATOR` | `{ batchId, status: "ACCEPTED"\|"REJECTED", details? }` |

### Debug Endpoints (Development)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/debug/account-resolve?accountId=TEST001` | Resolve account → internal number, routing, omnibus |
| GET | `/debug/auth-test` | Test auth headers |
| GET | `/debug/vendor-stub?accountId=<trigger>` | Exercise Vendor stub directly |
| POST | `/debug/ledger-post?transferId=<id>` | Manually trigger ledger posting |
| POST | `/debug/batch-settlement-deposits?count=10` | Create and approve N deposits with settlementDate = today |

---

## Demo Flows

### Batch Settlement Deposits (Settlement File Testing)

```bash
# Create 10 approved deposits with settlementDate = today (run before 6:30 PM CT)
./scripts/batch-settlement-deposits.sh 10

# Or via curl:
curl -X POST http://localhost:8080/debug/batch-settlement-deposits?count=10&accountId=TEST001 \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: OP-001"
```

### Settlement Bank Acknowledgment

After a settlement file is generated, the batchId is in the JSON file (e.g. `settlement-output/settlement-YYYY-MM-DD-<batchId-prefix>.json`). POST an ack to record bank acceptance:

```bash
# Get batchId from the generated settlement file, then:
curl -X POST http://localhost:8080/internal/settlement/ack \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: OP-001" \
  -d '{"batchId": "<uuid-from-settlement-file>", "status": "ACCEPTED", "details": "Batch processed"}'
```

If no ack is received within `settlement.ack-timeout-minutes` (default 60), the monitor logs a `SETTLEMENT_ACK_TIMEOUT` warning.

---

### Happy Path (Clean Pass)

```bash
# 1. Submit deposit (use clean-pass or TEST001)
curl -X POST http://localhost:8080/deposits \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: clean-pass" \
  -d '{"frontImage":"aGVsbG8=","backImage":"d29ybGQ=","amount":100.00,"accountId":"TEST001"}'

# 2. Poll status (use transferId from step 1)
curl http://localhost:8080/deposits/{transferId}

# 3. Approve (OPERATOR)
curl -X POST http://localhost:8080/operator/queue/{transferId}/approve \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1" \
  -d '{}'

# 4. View decision trace
curl http://localhost:8080/deposits/{transferId}/trace \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: TEST001"
```

### IQA Failure → Re-submission

```bash
# 1. Submit with iqa-blur → expect 422 with actionableMessage
curl -X POST http://localhost:8080/deposits \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: iqa-blur" \
  -d '{"frontImage":"aGVsbG8=","backImage":"d29ybGQ=","amount":50.00,"accountId":"TEST001"}'

# 2. Re-submit with retryForTransferId (use transferId from 422 response)
curl -X POST http://localhost:8080/deposits \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: clean-pass" \
  -d '{"frontImage":"bmV3aW1n","backImage":"bmV3aW1n","amount":50.00,"accountId":"TEST001","retryForTransferId":"<transferId>"}'
```

### Operator Reject (MICR Failure)

```bash
# 1. Submit with micr-fail → deposit flagged for operator
curl -X POST http://localhost:8080/deposits \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: micr-fail" \
  -d '{"frontImage":"aGVsbG8=","backImage":"d29ybGQ=","amount":200.00,"accountId":"TEST001"}'

# 2. Fetch queue
curl http://localhost:8080/operator/queue -H "X-User-Role: OPERATOR" -H "X-Account-Id: op1"

# 3. Reject
curl -X POST http://localhost:8080/operator/queue/{transferId}/reject \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1" \
  -d '{"reason":"MICR unreadable; customer to deposit at branch"}'
```

### Return / Reversal

```bash
# After a deposit is COMPLETED, simulate a bounced check
curl -X POST http://localhost:8080/internal/returns \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1" \
  -d '{"transferId":"<uuid>","returnReason":"NSF"}'
```

---

## Test Account IDs

| Account ID | Use |
|------------|-----|
| TEST001 | Standard test account (routing 021000021) |
| clean-pass | Vendor Clean Pass |
| iqa-blur | IQA Blur failure |
| iqa-glare | IQA Glare failure |
| micr-fail | MICR read failure |
| duplicate | Duplicate detected |
| amount-mismatch | OCR vs entered amount mismatch |
| routing-mismatch | Check routing ≠ account routing |

---

## Setup Commands

```bash
cp .env.example .env
# Set POSTGRES_PASSWORD in .env
docker compose up -d db
export $(grep -v '^#' .env | xargs) && ./mvnw spring-boot:run
```

---

## 7-Component Architecture

1. **Deposit capture / Vendor API integration** — Receives images, calls Vendor stub
2. **Vendor Service stub** — IQA, MICR, OCR, duplicate detection; swappable implementation
3. **Funding Service** — Business rules (routing, amount, contribution caps, duplicates)
4. **Ledger posting** — Transactional debit/credit
5. **Operator review workflow** — Queue, approve, reject, audit
6. **Settlement file generation** — EOD batch, X9 ICL (Phase 6)
7. **Return/reversal handling** — Reversal entries, $30 fee, investor notification

---

## Notes for Demo

- **Base64 images:** Use short strings like `aGVsbG8=` (hello) or `d29ybGQ=` (world) for synthetic images
- **403 without OPERATOR:** Operator endpoints require `X-User-Role: OPERATOR`
- **401/403 without headers:** Investor/operator endpoints require auth headers
- **Synthetic data only** — No real PII or check images; for demo/evaluation only
