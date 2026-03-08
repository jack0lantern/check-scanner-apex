# Active Context

## Current Work Focus

Phase 4 Step 2 (Transactional Ledger Posting) is complete. Next: Phase 4 Step 3 (Return Notification Endpoint), Step 4 (Return/Reversal Handling).

## Recent Changes

- **FundingService** — Business rules engine enforcing: (1) routing number match (check MICR vs account), (2) amount ≤ $5,000, (3) retirement account contribution type default + cap, (4) internal duplicate detection (fromAccountId + amount + micrData within configurable window)
- **FundingValidationResult** — pass/fail + rejectionReason + defaultContributionType
- **ResolvedAccount** — now includes `accountType` for retirement detection
- **TransferRepository** — `existsNonRejectedDuplicate`, `sumApprovedContributionsForAccountInYear`
- **Funding config** — `funding.max-deposit-amount`, `funding.duplicate-window-hours`, `funding.contribution-cap.retirement`
- **FundingServiceTest** — 8 unit tests: routing match, routing mismatch rejected, missing MICR rejected, $5k passes, $5,001 fails, retirement default, contribution cap violation, duplicate rejected, non-retirement no default
- **LedgerPostingService** — @Transactional postApprovedDeposit(transferId): updates Transfer to APPROVED, creates debit (omnibus) + credit (investor) ledger entries with shared transactionId/timestamp, populates Transfer attributes
- **LedgerEntry** entity, **LedgerEntryRepository** — DEBIT/CREDIT entries with account_id, transaction_id, counterparty_account_id
- **POST /debug/ledger-post?transferId=** — dummy endpoint for manual testing

## Next Steps

1. **Phase 4 Step 3** — Return Notification Endpoint
2. **Phase 4 Step 4** — Return/Reversal Handling with Investor Notification

## Active Decisions

- Stub uses account ID for scenario selection (no config file yet)
- **Routing validation:** Check MICR routing number must match deposit account's routing number; enforced at deposit time via FundingService
- Default scenario when account ID unknown: CLEAN_PASS
- Mock auth via `X-User-Role` and `X-Account-Id`; no JWT

## Future Considerations

- **Transfer ownership validation:** When implementing `retryForTransferId`, attach ownership (e.g. investor account ID) to the Transfer so retries can validate the caller owns the transfer before allowing updates. See detailed-plan Phase 3.3.
- **Payer name:** Add payer/check-writer name extraction (e.g. via Vendor OCR) and storage on Transfer; currently only MICR data (routing, account, check number) is tracked, not the payer's name.
