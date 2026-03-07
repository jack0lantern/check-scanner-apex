# Active Context

## Current Work Focus

Phase 3 (Vendor Service Stub) is largely complete. The stub is implemented with 7 scenarios; tests exist. Next steps are re-submission support (Phase 3.3), stub documentation (Phase 3.4), and then Funding Service (Phase 4).

## Recent Changes

- **VendorService** interface and **StubVendorService** implementation
- **VendorScenario** enum (IQA_PASS, IQA_FAIL_BLUR, IQA_FAIL_GLARE, MICR_READ_FAILURE, DUPLICATE_DETECTED, AMOUNT_MISMATCH, CLEAN_PASS)
- **VendorAssessmentResult** DTO with scenario, vendorScore, micrData, micrConfidence, ocrAmount, actionableMessage
- Trigger via `X-Account-Id`: `iqa-pass`, `iqa-blur`, `iqa-glare`, `micr-fail`, `duplicate`, `amount-mismatch`, `clean-pass`
- **StubVendorServiceTest** — 7 parameterized tests (one per scenario)
- **DebugController** — `/debug/account-resolve?accountId=...` for account resolution
- **WebMvcConfig** — Interceptor registration for mock auth

## Next Steps

1. **Phase 4** — Funding Service: business rules, ledger posting, return notification endpoint

## Active Decisions

- Stub uses account ID for scenario selection (no config file yet)
- Default scenario when account ID unknown: CLEAN_PASS
- Mock auth via `X-User-Role` and `X-Account-Id`; no JWT

## Future Considerations

- **Transfer ownership validation:** When implementing `retryForTransferId`, attach ownership (e.g. investor account ID) to the Transfer so retries can validate the caller owns the transfer before allowing updates. See detailed-plan Phase 3.3.
