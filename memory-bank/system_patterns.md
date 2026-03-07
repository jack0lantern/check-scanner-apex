# System Patterns

## Architecture Overview

Modular monolith with 7 bounded components. Internal method calls (no network between components).

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Mobile Client   │────▶│ Deposit Capture  │────▶│ Vendor Stub     │
│ (React)         │     │ /deposits        │     │ (IQA/MICR/OCR) │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                          │
                                                          ▼
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Operator UI     │◀───▶│ Funding Service  │◀────│ Account         │
│ /operator/queue │     │ (business rules) │     │ Resolution      │
└─────────────────┘     └────────┬─────────┘     └─────────────────┘
                                 │
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
             ┌──────────┐ ┌──────────┐ ┌──────────────────┐
             │ Ledger   │ │ Settlement│ │ Return/Reversal   │
             │ Posting  │ │ (X9 EOD) │ │ /internal/returns│
             └──────────┘ └──────────┘ └──────────────────┘
```

## Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Language | Java / Spring Boot | Mature ecosystem, enterprise scalability |
| Database | PostgreSQL | Ledger/transfer tables; Docker for local |
| Auth | Mock headers | MVP; JWT deferred |
| Vendor | Stub with account-ID trigger | Deterministic; no code changes to switch scenarios |
| State machine | 8 states | REQUESTED → VALIDATING → ANALYZING → APPROVED → FUNDS_POSTED → COMPLETED; REJECTED, RETURNED |

## Design Patterns

- **VendorService interface** — Swappable via `@Primary` or factory; stub is default
- **Account resolution** — External ID → internal number + omnibus from seeded config
- **Transactional ledger** — `@Transactional` for approve: update Transfer + 2 ledger entries
- **Trace events** — `trace_events` table; stage enum (SUBMISSION, VENDOR_RESULT, BUSINESS_RULE, OPERATOR_ACTION, SETTLEMENT, RETURN)

## Component Boundaries

- **Vendor Stub** — Owns IQA/MICR/OCR/duplicate; returns `VendorAssessmentResult`; no DB writes
- **Funding Service** — Owns business rules, account resolution; calls Vendor, Ledger
- **Ledger** — Owns `ledger_entries`; double-entry (debit omnibus, credit investor)
- **Operator** — Owns queue query, approve/reject; writes `audit_logs`
- **Settlement** — Owns EOD cron, X9 file generation, batch state
