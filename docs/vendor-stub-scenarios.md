# Vendor Stub Scenarios

The `StubVendorService` returns deterministic responses for testing and development. Each scenario is triggered by passing a specific account ID (e.g. via `X-Account-Id` header or `accountId` parameter).

## Trigger Mechanism

| Scenario | Trigger Account ID |
|----------|-------------------|
| IQA Pass | `iqa-pass` |
| IQA Fail (Blur) | `iqa-blur` |
| IQA Fail (Glare) | `iqa-glare` |
| MICR Read Failure | `micr-fail` |
| Duplicate Detected | `duplicate` |
| Amount Mismatch | `amount-mismatch` |
| Clean Pass | `clean-pass` (or any unknown account ID) |

**Manual testing:** Use `GET /debug/vendor-stub?accountId=<trigger>` to exercise each scenario. For deposit submissions, set `X-Account-Id` to the trigger value.

## Response Shape

All responses include:

| Field | Type | Description |
|-------|------|-------------|
| `scenario` | enum | `IQA_PASS`, `IQA_FAIL_BLUR`, `IQA_FAIL_GLARE`, `MICR_READ_FAILURE`, `DUPLICATE_DETECTED`, `AMOUNT_MISMATCH`, `CLEAN_PASS` |
| `vendorScore` | Double | Quality/confidence score (0.0–1.0) |
| `micrData` | String or null | Extracted MICR line |
| `micrConfidence` | Double or null | MICR read confidence |
| `ocrAmount` | BigDecimal or null | OCR-extracted amount |
| `actionableMessage` | String or null | User-facing message for failures; null on success |
| `riskScore` | Double | Risk score from vendor |

## Scenario Details

### 1. IQA Pass

- **Trigger:** `accountId=iqa-pass`
- **Description:** Image quality acceptable; proceeds to MICR/OCR.
- **Response:** `scenario=IQA_PASS`, full MICR data, `ocrAmount` = entered amount, `actionableMessage=null`.

### 2. IQA Fail (Blur)

- **Trigger:** `accountId=iqa-blur`
- **Description:** Image too blurry.
- **Response:** `scenario=IQA_FAIL_BLUR`, `actionableMessage="Image too blurry — please retake in better lighting"`, `vendorScore=0.4`, `micrData=null`, `micrConfidence=null`, `ocrAmount=null`.

### 3. IQA Fail (Glare)

- **Trigger:** `accountId=iqa-glare`
- **Description:** Glare detected on image.
- **Response:** `scenario=IQA_FAIL_GLARE`, `actionableMessage="Glare detected — please move to a darker surface"`, `vendorScore=0.4`, `micrData=null`, `micrConfidence=null`, `ocrAmount=null`.

### 4. MICR Read Failure

- **Trigger:** `accountId=micr-fail`
- **Description:** Cannot read check routing line.
- **Response:** `scenario=MICR_READ_FAILURE`, `actionableMessage="Cannot read check routing line — please try again or deposit at a branch"`, `vendorScore=0.6`, `micrData=null`, `micrConfidence=null`, `ocrAmount=enteredAmount`.

### 5. Duplicate Detected

- **Trigger:** `accountId=duplicate`
- **Description:** Check has already been deposited.
- **Response:** `scenario=DUPLICATE_DETECTED`, `actionableMessage="This check has already been deposited"`, full MICR data.

### 6. Amount Mismatch

- **Trigger:** `accountId=amount-mismatch`
- **Description:** Recognized amount differs from entered amount.
- **Response:** `scenario=AMOUNT_MISMATCH`, `actionableMessage="Recognized amount differs from entered amount — please verify"`, full MICR data.

### 7. Clean Pass

- **Trigger:** `accountId=clean-pass` or any unknown account ID
- **Description:** Full success; no errors.
- **Response:** `scenario=CLEAN_PASS`, full MICR data, `ocrAmount` = entered amount, `actionableMessage=null`.

## Configuration Options

- No configuration file is used by default. Scenario selection is driven by the account ID passed to `assessCheck`.
- To swap implementations, provide another `VendorService` bean and annotate it `@Primary`, or use a factory/qualifier.
- The stub is case-insensitive: `accountId` is trimmed and lowercased before lookup.

## Adding a New Scenario

1. Add the enum value to `VendorScenario`.
2. Add the mapping in `StubVendorService.ACCOUNT_TO_SCENARIO` (account ID → scenario).
3. If the scenario has an actionable message, add it to `ACTIONABLE_MESSAGES`.
4. Add a `case` branch in the `switch` block of `assessCheck`.
5. Update this document with the new scenario and trigger instructions.
