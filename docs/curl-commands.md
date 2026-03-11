# Curl Commands for Manual Testing

All endpoints require auth headers: `X-User-Role` and `X-Account-Id`. Use `INVESTOR` and `TEST001` for debug endpoints.

**Run all tests:** `./scripts/curl-tests.sh`

## Individual Commands

### Debug: Auth Test
```bash
curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" http://localhost:8080/debug/auth-test
```

### Debug: Account Resolve
```bash
curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/account-resolve?accountId=TEST001"
```

### Vendor Stub (7 scenarios)

| Scenario | accountId param | Curl |
|----------|-----------------|-----|
| IQA Pass | `iqa-pass` | `curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/vendor-stub?accountId=iqa-pass&amount=150.00"` |
| IQA Fail (Blur) | `iqa-blur` | `curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/vendor-stub?accountId=iqa-blur&amount=150.00"` |
| IQA Fail (Glare) | `iqa-glare` | `curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/vendor-stub?accountId=iqa-glare&amount=150.00"` |
| MICR Read Failure | `micr-fail` | `curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/vendor-stub?accountId=micr-fail&amount=150.00"` |
| Duplicate Detected | `duplicate` | `curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/vendor-stub?accountId=duplicate&amount=150.00"` |
| Amount Mismatch | `amount-mismatch` | `curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/vendor-stub?accountId=amount-mismatch&amount=150.00"` |
| Clean Pass | `clean-pass` | `curl -H "X-User-Role: INVESTOR" -H "X-Account-Id: TEST001" "http://localhost:8080/debug/vendor-stub?accountId=clean-pass&amount=100.00"` |

### Return Notification (OPERATOR only)

Requires an APPROVED or COMPLETED transfer (e.g. NSF—insufficient funds at sending account). First create one via deposit submission + `POST /debug/ledger-post?transferId=<uuid>`, or use a transfer that has reached COMPLETED via EOD settlement.

```bash
curl -X POST -H "X-User-Role: OPERATOR" -H "X-Account-Id: OP-001" \
  -H "Content-Type: application/json" \
  -d '{"transferId":"<YOUR_TRANSFER_ID>","returnReason":"NSF"}' \
  http://localhost:8080/internal/returns
```

Verify: 3 new `ledger_entries`, transfer `state = RETURNED`, `INVESTOR_NOTIFIED` in `audit_logs` with `feeAmount=30`.
