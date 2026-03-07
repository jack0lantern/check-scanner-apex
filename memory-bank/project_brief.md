# Project Brief

## Project Name

Mobile Check Deposit System (Apex Fintech Check Scanner)

## Core Requirements

Build a minimal end-to-end mobile check deposit system for a brokerage platform. The system must handle the full lifecycle:

- **Capture** — Front and back check images, amount, account ID
- **Validation** — IQA (image quality), MICR extraction, OCR, duplicate detection via Vendor Service
- **Compliance gating** — Business rules (amount limits, contribution caps, duplicate detection)
- **Operational review** — Operator queue for flagged deposits (approve/reject, contribution override)
- **Ledger posting** — Debit omnibus, credit investor; transactional
- **Settlement** — EOD batch (6:30 PM CT cutoff), X9 ICL file generation
- **Return/reversal** — Handle bounced checks with fee and investor notification

## Strict Constraints

- **No real PII, account numbers, or check images** — synthetic data only
- All secrets via environment variables; `.env.example` provided
- Use **Spotless** (Maven) for code formatting
- **7 named components** with clean separation:
  1. Deposit capture / Vendor API integration
  2. Vendor Service stub (independently configurable)
  3. Funding Service business rules
  4. Ledger posting
  5. Operator review workflow
  6. Settlement file generation
  7. Return/reversal handling

## Architecture

- **Modular monolith** — Java (Spring Boot), PostgreSQL, React (Vite)
- **Local deployment** — Docker Compose; one-command setup
- **Mock auth** — `X-User-Role` (INVESTOR/OPERATOR) and `X-Account-Id` headers

## Source of Truth

This document and `/docs/detailed-plan.md` define project scope. The detailed plan has 11 phases with automated validation and manual testing steps per phase.
