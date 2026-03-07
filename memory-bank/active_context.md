# Active Context

## Current Work Focus

Phase 4 Step 1 (Funding Service Business Rules) is complete. Next: Phase 4 Step 2 (Transactional Ledger Posting), Step 3 (Return Notification Endpoint), Step 4 (Return/Reversal Handling).

## Recent Changes

- **FundingService** — Business rules engine enforcing: (1) amount ≤ $5,000, (2) retirement account contribution type default + cap, (3) internal duplicate detection (fromAccountId + amount + micrData within configurable window)
- **FundingValidationResult** — pass/fail + rejectionReason + defaultContributionType
- **ResolvedAccount** — now includes `accountType` for retirement detection
- **TransferRepository** — `existsNonRejectedDuplicate`, `sumApprovedContributionsForAccountInYear`
- **Funding config** — `funding.max-deposit-amount`, `funding.duplicate-window-hours`, `funding.contribution-cap.retirement`
- **FundingServiceTest** — 6 unit tests: $5k passes, $5,001 fails, retirement default, contribution cap violation, duplicate rejected, non-retirement no default

## Next Steps

1. **Phase 4 Step 2** — Transactional Ledger Posting
2. **Phase 4 Step 3** — Return Notification Endpoint
3. **Phase 4 Step 4** — Return/Reversal Handling with Investor Notification

## Active Decisions

- Stub uses account ID for scenario selection (no config file yet)
- Default scenario when account ID unknown: CLEAN_PASS
- Mock auth via `X-User-Role` and `X-Account-Id`; no JWT

## Future Considerations

- **Transfer ownership validation:** When implementing `retryForTransferId`, attach ownership (e.g. investor account ID) to the Transfer so retries can validate the caller owns the transfer before allowing updates. See detailed-plan Phase 3.3.
