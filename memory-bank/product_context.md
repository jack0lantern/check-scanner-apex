# Product Context

## Why This Project Exists

Apex Fintech Challenger Project — demonstrate a production-ready mobile check deposit flow for brokerage accounts. Investors deposit checks via mobile; operators review flagged items; the system posts to ledger and settles with a settlement bank.

## Problems It Solves

1. **End-to-end deposit flow** — From image capture through settlement and return handling
2. **Deterministic testing** — Vendor stub with 7 scenarios (no code changes to switch)
3. **Operator workflow** — Queue with full detail (MICR, OCR, risk scores, images) and search/filter
4. **Audit trail** — Structured audit logs, decision trace per deposit
5. **Settlement** — X9 ICL file, EOD cutoff, next-business-day rollover

## How It Works

1. **Investor** submits deposit (front/back images, amount, account ID) with `X-User-Role: INVESTOR`
2. **Vendor stub** assesses images; returns IQA/MICR/OCR result or actionable failure message
3. **Funding Service** applies business rules; resolves account → internal number + omnibus
4. **Operator** reviews flagged deposits; approves/rejects; can override contribution type
5. **Ledger** posts debit (omnibus) and credit (investor) atomically
6. **EOD batch** generates X9 file; marks transfers COMPLETED
7. **Return** — POST to `/internal/returns`; reversal + $30 fee; INVESTOR_NOTIFIED audit

## User Experience Goals

- **Investor:** Simple form; clear actionable messages on IQA failure; "Retake & Resubmit" flow
- **Operator:** Queue with images, MICR data, risk badges; filter by status/date/account/amount; approve with optional contribution override
- **Evaluator:** One-command setup; demo scripts for happy path, rejection, manual review, return/reversal
