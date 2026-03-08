### 🤖 Prompt for Composer: Apex Fintech Mobile Check Deposit System (TDD & Validation Mode)

**Context:**
You are building a minimal end-to-end mobile check deposit system for a brokerage platform. The architecture is a Modular Monolith using Java (Spring Boot), PostgreSQL, and a simple React frontend. The system runs entirely locally via Docker Compose. The solution must handle the full lifecycle — capture, validation, compliance gating, operational review, ledger posting, settlement, and return/reversal — with a clear operator workflow, audit trail, and stubbed vendor integration.

**Strict Constraints:**

* **Absolutely NO real PII, account numbers, or check images**; use synthetic data only.
* All secrets must be loaded via environment variables with a `.env.example` provided.
* Use `Spotless` via Maven plugin for code formatting and style checking.
* **CRITICAL:** You must not proceed to the next step until the Automated Validation and Manual Testing instructions for the current step pass successfully.
* Architecture must maintain clean separation of concerns across exactly **7 named components**: (1) Deposit capture / Vendor API integration, (2) Vendor Service stub (independently configurable), (3) Funding Service business rules, (4) Ledger posting, (5) Operator review workflow, (6) Settlement file generation, (7) Return/reversal handling. Each component must live in its own package and be independently testable.

---

#### Phase 1: Infrastructure & Project Scaffolding

1. **Initialize Git & Directories:** Create the required root folders: `/docs`, `/tests`, and `/reports`.
* *Automated Validation:* Write a quick bash script `verify_setup.sh` that asserts these directories exist.
* *Manual Testing:* Run `ls -la` in the root to visually confirm folder creation.


2. **Docker Setup:** Create a `docker-compose.yml` that includes a PostgreSQL database and a placeholder for the Java backend and React frontend. Ensure one-command setup via `docker compose up`. All credentials and ports must be read from `.env`.
* *Automated Validation:* Run `docker compose config` to validate the YAML syntax.
* *Manual Testing:* Run `docker compose up -d db` and connect to the database locally using a tool like DBeaver or `psql` to ensure the port is exposed and credentials work.


3. **Makefile & One-Command Setup:** Create a `Makefile` at the repo root with a `make dev` target that runs `docker compose up` (plus any pre-flight checks such as confirming `.env` exists). Both `make dev` and `docker compose up` must be equivalent one-command setups — either must work from a clean clone.
* *Automated Validation:* Run `make --dry-run dev` to confirm the target exists and the command structure is valid.
* *Manual Testing:* Run `make dev` from scratch and verify all services start correctly.


4. **Environment Variables & `.env.example`:** Create a `.env.example` file enumerating every required environment variable (database host, port, name, credentials; application port; any API keys or secrets) with placeholder values and comments explaining each. The `docker-compose.yml` must reference all secrets via `${VAR}` syntax pointing to `.env`.
* *Automated Validation:* Write a bash script `verify_env.sh` that asserts `.env.example` is non-empty and that every `${VAR}` referenced in `docker-compose.yml` appears as a key in `.env.example`.
* *Manual Testing:* Copy `.env.example` to `.env`, run `make dev`, and confirm the app starts without any hardcoded secrets in the source files.


5. **Maven Configuration:** Initialize a Java Spring Boot Maven project. Include dependencies for Spring Web, Spring Data JPA, PostgreSQL Driver, and Spring Boot Starter Test.
* *Automated Validation:* Run `mvn validate` and `mvn dependency:resolve` to ensure all packages download without conflict.
* *Manual Testing:* Open the project in your IDE and verify no red squiggly lines appear in the `pom.xml`.


6. **Spotless Integration:** Add the `spotless-maven-plugin` to the `pom.xml` configured for Java formatting (e.g., using Google Java Format).
* *Automated Validation:* Add a bad formatting line to the main application class and run `mvn spotless:check` (it must fail). Then run `mvn spotless:apply` and confirm `mvn spotless:check` passes.
* *Manual Testing:* Run `mvn spotless:apply` and visually verify the formatting is corrected.


7. **Database Schema:** Create SQL init scripts for the following tables: `accounts`, `transfers` (deposits), `ledger_entries`, `audit_logs`, and `trace_events`. The `transfers` table must include columns for all Transfer record attributes defined in Phase 2 Step 2.
* *Automated Validation:* Write a `@DataJpaTest` that successfully loads the context and executes a basic `SELECT 1` against the database.
* *Manual Testing:* Boot the app, log into the PostgreSQL container, and run `\dt` to verify all five tables exist.



---

#### Phase 2: Domain Models & State Machine

1. **Transfer State Enum:** Implement an enum for the transfer states: `REQUESTED`, `VALIDATING`, `ANALYZING`, `APPROVED`, `FUNDS_POSTED`, `COMPLETED`, `REJECTED`, and `RETURNED`.
* *Automated Validation:* Write a JUnit test `TransferStateTest` asserting that all exactly 8 enums exist and are spelled correctly.
* *Manual Testing:* N/A (Tested thoroughly via automation).


2. **Entities with Complete Transfer Record Attributes:** Create JPA Entities mapping to the database tables. The `Transfer` entity **must** include all of the following fields — this is the authoritative list of required attributes for the financial record:
   - `frontImageData` (byte[] or Base64 String) — distinct front-of-check image
   - `backImageData` (byte[] or Base64 String) — distinct back-of-check image
   - `amount` (BigDecimal) — user-entered deposit amount
   - `toAccountId` (String) — investor account (resolved destination)
   - `fromAccountId` (String) — Omnibus account for the investor's correspondent, resolved via client config
   - `type` (String, fixed value `"MOVEMENT"`)
   - `memo` (String, fixed value `"FREE"`)
   - `subType` (String, fixed value `"DEPOSIT"`)
   - `transferType` (String, fixed value `"CHECK"`)
   - `currency` (String, fixed value `"USD"`)
   - `sourceApplicationId` (String) — set to the TransferID (UUID)
   - `state` (TransferState enum)
   - `vendorScore` (Double) — overall risk/confidence score from Vendor Service
   - `micrData` (String) — raw MICR line extracted by Vendor stub
   - `micrConfidence` (Double) — MICR confidence score (0.0–1.0)
   - `ocrAmount` (BigDecimal) — amount recognized by OCR; compared against user-entered `amount`
   - `contributionType` (String) — defaults to `"INDIVIDUAL"` for retirement-type accounts
   - `depositSource` (String) — tag for differentiating deposit origin in logs (e.g., `"MOBILE"`, `"OPERATOR"`)
   - `settlementDate` (LocalDate) — the business day this deposit will be settled; set at submission time based on EOD cutoff
* *Automated Validation:* Write a `@DataJpaTest` that successfully saves and retrieves a dummy `Transfer` entity with every field populated and asserts all fields round-trip correctly.
* *Manual Testing:* Check the application startup logs to ensure Hibernate correctly maps the entities to the Flyway tables without throwing `SchemaManagementException`.


3. **Account Identifier Resolution Service:** Implement an `AccountResolutionService` that maps an incoming external account identifier to:
   - An internal account number and routing number
   - The corresponding Omnibus account ID for that investor's correspondent (looked up from a seeded client config table or static YAML config file)
   This service is used by the Funding Service to populate `toAccountId` and `fromAccountId` on the Transfer record.
* *Automated Validation:* Write unit tests passing known test account IDs and asserting the correct internal account number, routing number, and Omnibus account ID are returned. Also assert that an unknown account ID throws an appropriate exception.
* *Manual Testing:* Hit a diagnostic endpoint `GET /debug/account-resolve?accountId=TEST001` and verify the full resolved account object (internal number, routing, omnibus) appears in the JSON response.


4. **Mock Authentication Interceptor:** Create a Spring Interceptor that reads an `X-User-Role` HTTP header (e.g., `INVESTOR` or `OPERATOR`) and an `X-Account-Id` header to mock session validation and account eligibility. This is the identity context for all downstream operations.
* *Automated Validation:* Write a `MockMvc` test hitting a dummy endpoint. Assert it returns `200 OK` when headers are present and `401/403` when missing.
* *Manual Testing:* Use Postman or `curl` to hit the dummy endpoint with and without headers, verifying the correct HTTP status codes.



---

#### Phase 3: The Vendor Service Stub

1. **Stub Interface:** Create an internal `VendorService` interface for image quality assessment (IQA), MICR extraction, OCR, and duplicate detection. The interface must be independently configurable — its implementation must be swappable without modifying the Funding Service or deposit submission code (use Spring `@Primary` or a factory pattern).
* *Automated Validation:* Ensure the project compiles with `mvn clean compile`.
* *Manual Testing:* N/A.


2. **Deterministic Implementation with Actionable Messages:** Implement the stub to return varied, deterministic responses based on the incoming `X-Account-Id` or deposit amount. It MUST support the following 7 scenarios **without code changes** (selectable via request parameter, test account number, or a configuration file that maps account IDs to scenarios):
   - **IQA Pass** — image quality acceptable, proceed to MICR/OCR
   - **IQA Fail (Blur)** — actionableMessage: "Image too blurry — please retake in better lighting"
   - **IQA Fail (Glare)** — actionableMessage: "Glare detected — please move to a darker surface"
   - **MICR Read Failure** — actionableMessage: "Cannot read check routing line — please try again or deposit at a branch"
   - **Duplicate Detected** — actionableMessage: "This check has already been deposited"
   - **Amount Mismatch** — actionableMessage: "Recognized amount differs from entered amount — please verify"
   - **Clean Pass** — full MICR data, OCR amount, confidence scores, and risk score returned; no error
   Each response must include: `scenario` (enum), `vendorScore` (Double), `micrData` (String or null), `micrConfidence` (Double or null), `ocrAmount` (BigDecimal or null), `actionableMessage` (String or null). Response time must be < 1 second.
* *Automated Validation:* Write 7 parameterized JUnit tests — one per scenario — passing the specific trigger inputs and asserting the exact scenario enum, a non-null `actionableMessage` for failure scenarios, and response time < 1 second.
* *Manual Testing:* Create a temporary `@RestController` to trigger the stub via GET. Hit it from a browser with different parameters and visually confirm the changing JSON responses including the `actionableMessage` field for each failure type.


3. **Re-submission Support on IQA Failure:** Implement the re-submission flow. The deposit endpoint must accept an optional `retryForTransferId` field. When present, it updates the existing Transfer record rather than creating a new one, advancing it through the validation retry. IQA failure responses must include the `actionableMessage` from the Vendor stub so the UI can display it.
* *Automated Validation:* Write a `@WebMvcTest` that: (1) submits a deposit triggering IQA Blur, asserts `422` response with `actionableMessage` field present; (2) re-submits with `retryForTransferId`, asserts the existing Transfer is updated (not a new record) and state advances.
* *Manual Testing:* Use curl to POST a deposit triggering IQA Blur. Confirm the response body contains `actionableMessage`. Re-POST with `retryForTransferId` set. Verify in the database that only one Transfer record exists (no duplicate created).
* *Future Security:* Attach transfer ownership to the Transfer record so that retries can validate the caller's account (e.g. `X-Account-Id`) matches the transfer's owner before allowing updates.


4. **Stub Documentation Artifact:** Produce `/docs/vendor-stub-scenarios.md` documenting each of the 7 supported response scenarios: the trigger mechanism (which account ID or amount triggers which scenario), the expected full response shape (all fields), configuration options, and how to add a new scenario.
* *Automated Validation:* Write a bash script asserting `/docs/vendor-stub-scenarios.md` exists, is non-empty, and contains each of the 7 scenario names as a string.
* *Manual Testing:* Read the document and verify it contains an entry for all 7 scenarios by name with trigger instructions.



---

#### Phase 4: Funding Service & Ledger

1. **Business Rules Engine:** Implement a `FundingService` that enforces the following rules in order:
   - Reject deposits where the check's routing number (extracted from MICR) does not match the deposit account's routing number
   - Reject deposits where `amount > $5,000`
   - Apply contribution type defaults: if the resolved account type is a retirement account, default `contributionType` to `"INDIVIDUAL"`; enforce any contribution cap associated with that account type (in addition to the per-deposit $5k limit)
   - Perform internal duplicate deposit detection: reject if another non-REJECTED Transfer with the same `fromAccountId` + `amount` + `micrData` exists within a configurable time window
* *Automated Validation:* Write unit tests covering: $5,000 passes; $5,001 fails; correct contribution type defaulted for a retirement account; contribution cap violation rejected; duplicate transfer rejected.
* *Manual Testing:* N/A (Will be manually tested via the API in Phase 5).


2. **Transactional Ledger Posting:** Create a method annotated with `@Transactional` that handles approved deposits. It must simultaneously:
   - Update Transfer state to `APPROVED`
   - Create a ledger entry **debiting** the Correspondent Omnibus Account (`fromAccountId`) for the validated deposit amount
   - Create a ledger entry **crediting** the Investor Account (`toAccountId`) for the validated deposit amount
   - Both entries share the same `transactionId` (UUID) and `timestamp`
   - Populate all required Transfer attributes: `type="MOVEMENT"`, `memo="FREE"`, `subType="DEPOSIT"`, `transferType="CHECK"`, `currency="USD"`, `sourceApplicationId=transferId`
* *Automated Validation:* Write a `@SpringBootTest` that calls this method. Assert: exactly two new `ledger_entries` with matching `transactionId`; `transfer` state = `APPROVED`; all named Transfer attributes are non-null and have the correct fixed values.
* *Manual Testing:* Trigger the method via a dummy endpoint. Inspect the database to confirm both ledger entries share the same `transactionId` and all Transfer attribute fields are populated correctly.


3. **Return Notification Endpoint:** Implement `POST /internal/returns` (requires `X-User-Role: OPERATOR`) accepting `{ "transferId": "...", "returnReason": "..." }` to simulate an inbound return notification from the Settlement Bank.
* *Automated Validation:* Write `@WebMvcTest` tests: 403 without OPERATOR header; 200 with valid payload; 404 for unknown transferId.
* *Manual Testing:* Use curl to POST a return notification for a completed transfer and verify the endpoint responds 200.


4. **Return/Reversal Handling with Investor Notification:** On receiving a return notification, implement logic to:
   - Create two reversal ledger entries (mirror of the original posting — debit investor, credit omnibus)
   - Create an additional ledger entry debiting the investor $30 (the return fee)
   - Transition the Transfer state to `RETURNED`
   - Write a structured `INVESTOR_NOTIFIED` event to `audit_logs` containing: `transferId`, `returnReason`, `feeAmount=$30`, `timestamp`
* *Automated Validation:* Write a unit test verifying the math: investor is debited the original amount + $30 fee (net investor impact = original amount + $30, not original amount - $30 — this is a deduction). Write an integration test asserting 3 new `ledger_entries` (2 reversal + 1 fee), state = `RETURNED`, and `INVESTOR_NOTIFIED` audit log entry present.
* *Manual Testing:* POST a return notification via curl. Query the database and verify: 3 ledger entries, state = RETURNED, `INVESTOR_NOTIFIED` in `audit_logs` with correct fee.



---

#### Phase 5: REST API & Operator Workflow

1. **Investor Endpoints:** Create the following endpoints:
   - `POST /deposits` — Submit a deposit. Request body: `{ "frontImage": "<Base64>", "backImage": "<Base64>", "amount": 1234.56, "accountId": "TEST001", "retryForTransferId": null }`. Returns `201 Created` with Transfer ID and initial state, or `422` with `actionableMessage` on IQA failure.
   - `GET /deposits/{transferId}` — Track transfer status. Returns full Transfer record including current state, all timestamps, and any Vendor Service messages.
* *Automated Validation:* Write `@WebMvcTest` cases: `201` on valid submission; `422` with `actionableMessage` body on IQA failure; `200` on status poll.
* *Manual Testing:* Use `curl` to POST a payload with both Base64 synthetic images and `X-User-Role: INVESTOR`. Verify response. Poll the status endpoint.


2. **Operator Endpoints with Full Queue Detail, Search/Filter, and Override:** Create the following endpoints (all require `X-User-Role: OPERATOR`):
   - `GET /operator/queue` — Returns flagged deposits. Each item includes: `transferId`, `state`, `investorAccountId`, `enteredAmount`, `ocrAmount`, `micrData`, `micrConfidence`, `vendorScore`, risk indicator flags, `frontImage` (Base64 or presigned URL), `backImage` (Base64 or presigned URL), `submittedAt`.
   - `GET /operator/queue` supports query parameters: `?status=`, `?dateFrom=`, `?dateTo=`, `?accountId=`, `?minAmount=`, `?maxAmount=` for search and filtering.
   - `POST /operator/queue/{transferId}/approve` — Approve. Optional body: `{ "contributionTypeOverride": "ROTH" }`. Persists the override on the Transfer record if provided.
   - `POST /operator/queue/{transferId}/reject` — Reject. Required body: `{ "reason": "..." }`.
* *Automated Validation:* Write `@WebMvcTest` tests: `403` without OPERATOR header; queue response contains `micrData`, `micrConfidence`, `vendorScore`, `frontImage`, `backImage`, `ocrAmount`; filter by `status` returns only matching records; `contributionTypeOverride` is persisted on approve; reject requires non-empty `reason`.
* *Manual Testing:* Use Postman to fetch the queue. Verify images appear. Copy a Transfer ID, POST approve with `contributionTypeOverride`, verify the field is saved in the DB. Test each filter parameter.


3. **Structured Audit Logging:** Every operator action (approve, reject, contribution type override) must be saved to `audit_logs` with structured fields: `operatorId` (from `X-Account-Id` header), `action` (enum: `APPROVE`, `REJECT`, `CONTRIBUTION_TYPE_OVERRIDE`), `transferId`, `detail` (JSON blob of the action payload), `timestamp`. Redact any mock PII from all application logs.
* *Automated Validation:* Write an integration test: `audit_logs COUNT()` increases by 1 after approve; `operatorId`, `action`, `transferId`, and `timestamp` fields are non-null; a contribution type override logs `action = CONTRIBUTION_TYPE_OVERRIDE` as a separate entry.
* *Manual Testing:* Check terminal console output for absence of raw sensitive data. Query `audit_logs` and verify all structured fields are populated correctly.


4. **Ledger / Cap-Table View Endpoints:** Create:
   - `GET /accounts/{accountId}/balance` — Returns current balance (net sum of all posted ledger entries for this account).
   - `GET /accounts/{accountId}/ledger` — Returns a paginated list of ledger entries (each entry: `entryId`, `type`, `amount`, `counterpartyAccountId`, `transactionId`, `timestamp`).
* *Automated Validation:* Write a `@SpringBootTest` that seeds two ledger entries for a test account (one credit, one debit), calls the balance endpoint, and asserts the correct net balance.
* *Manual Testing:* After posting and approving a deposit, call the ledger endpoint for the investor account. Confirm the new credit entry appears.


5. **Per-Deposit Decision Trace Endpoint:** Create `GET /deposits/{transferId}/trace` that returns a chronological list of `TraceEvent` objects, each with: `stage` (enum: `SUBMISSION`, `VENDOR_RESULT`, `BUSINESS_RULE`, `OPERATOR_ACTION`, `SETTLEMENT`, `RETURN`), `outcome`, `detail` (JSON), `timestamp`. This must be populated by all components as they process the deposit.
* *Automated Validation:* Write an integration test that walks a deposit through submission → vendor validation → funding service → operator approval, then calls the trace endpoint and asserts all 4 stages appear in order with correct outcomes.
* *Manual Testing:* Walk a full deposit through using curl. Call the trace endpoint and verify all stages appear with correct outcomes and timestamps.



---

#### Phase 6: EOD Batch Settlement

1. **Scheduled Task:** Use Spring's `@Scheduled` to create a cron job representing the 6:30 PM CT End-of-Day cutoff.
* *Automated Validation:* Write a test using `Awaitility` or a mock clock to verify the cron trigger fires at the expected time.
* *Manual Testing:* Temporarily change the cron expression to run every 1 minute. Watch the application logs to ensure it fires.


2. **Next-Business-Day Rollover Logic:** At deposit submission time, set `settlementDate` on the Transfer:
   - If submitted before 6:30 PM CT today → `settlementDate = today`
   - If submitted at or after 6:30 PM CT → `settlementDate = nextBusinessDay` (skip weekends and US holidays from a hardcoded list)
   The EOD batch must only include transfers where `settlementDate = today`. Post-cutoff deposits are excluded from the current run and held for the next business day.
* *Automated Validation:* Write a unit test creating one deposit stamped before cutoff and one after cutoff. Assert only the pre-cutoff deposit is selected by the batch query. Assert the post-cutoff deposit has `settlementDate = nextBusinessDay`. Write a separate test asserting a Friday deposit after cutoff rolls to the following Monday (skipping weekend).
* *Manual Testing:* Submit a deposit with the system clock manually set past 6:30 PM CT. Trigger the batch and verify the deposit is excluded. Advance the clock to the next business day, trigger again, verify inclusion.


3. **Settlement File Generation with Full X9 Content:** Query all `APPROVED` transfers with `settlementDate = today` and generate an X9 ICL format (or structured JSON equivalent) file. Each record in the file must include:
   - MICR data (from `micrData` field)
   - Front image reference (Base64 or file path) and back image reference
   - Deposit amount
   - Sequence number (1-based per record in this batch)
   - Batch metadata at the file level: `batchId` (UUID), `generationTimestamp`, `totalRecordCount`, `totalAmount`
   Update all included transfers to `COMPLETED` atomically within the same transaction.
* *Automated Validation:* Write an integration test: create 3 approved transfers with `settlementDate = today`; trigger the service; assert: (1) file created on disk, (2) file contains exactly 3 records, (3) each record has non-null `micrData` and image references, (4) batch `totalAmount` equals sum of the 3 amounts, (5) all 3 transfers are in `COMPLETED` state.
* *Manual Testing:* Run the batch. Open the generated file and confirm MICR data, image references, amounts, sequence numbers, and batch metadata are all present and correct.


4. **Settlement Bank Acknowledgment Tracking:** Implement `POST /internal/settlement/ack` accepting `{ "batchId": "...", "status": "ACCEPTED" | "REJECTED", "details": "..." }`. Store the ack status on the batch record. Implement a monitor that logs a `SETTLEMENT_ACK_TIMEOUT` warning if no acknowledgment is received within a configurable timeout after file generation.
* *Automated Validation:* Write a test: trigger file generation; POST ack with `ACCEPTED`; assert batch record `ackStatus = ACCEPTED`. Write a second test: simulate timeout by not posting an ack; assert `SETTLEMENT_ACK_TIMEOUT` warning-level log is emitted.
* *Manual Testing:* Generate a batch, POST an ack, verify batch record updates. Test the timeout warning by skipping the ack POST and waiting for the timeout log.



---

#### Phase 7: Observability & Logging

1. **Structured Request Logging:** Add a filter/interceptor that tags every request log entry with: `depositSource` (derived from account type or a request header — differentiates `MOBILE` vs `OPERATOR` origins), `transferId` (when available from the request path/body), and `traceId` (UUID per request). All logs must be redacted of any raw synthetic account numbers or PII-adjacent data.
* *Automated Validation:* Write a test that submits a request and captures log output via a test appender; assert `depositSource`, `transferId`, and `traceId` fields are present in the structured log output.
* *Manual Testing:* Make a deposit submission request and inspect the console. Verify structured fields appear and no raw account numbers are visible.


2. **Decision Trace Persistence (TraceEvent):** Each time a deposit moves through a decision point, write a `TraceEvent` to the `trace_events` table. Decision points are: Vendor stub response received, each business rule applied (pass or fail), operator action taken, settlement batch inclusion, return notification received. Fields: `transferId`, `stage` (enum), `outcome`, `detail` (JSON), `timestamp`.
* *Automated Validation:* Walk a deposit through all 4+ stages in a `@SpringBootTest`. Query `trace_events` and assert all stages appear with correct outcomes and timestamps.
* *Manual Testing:* Run a full happy-path flow. Call `GET /deposits/{transferId}/trace` and confirm all stages appear.


3. **Missing Settlement File Monitor:** After each scheduled EOD run, if APPROVED transfers exist for `settlementDate = today` but no file was successfully generated, log a structured `SETTLEMENT_FILE_MISSING` warning. Also emit `SETTLEMENT_ACK_TIMEOUT` if no ack is received within the configured window.
* *Automated Validation:* Write a test that creates an approved transfer for today but mocks file generation to fail; assert `SETTLEMENT_FILE_MISSING` warning log is emitted.
* *Manual Testing:* Break the file generation path temporarily, trigger the scheduled job, and confirm the warning appears in logs.



---

#### Phase 8: Minimal React Frontend

1. **App Setup:** Initialize a Vite React app. Add it to the `docker-compose.yml`.
* *Automated Validation:* Run `npm run build` to ensure it compiles without errors.
* *Manual Testing:* Run `docker compose up`, open a browser to `localhost:5173`, and confirm the app loads.


2. **Investor View with Front/Back Image Upload and IQA Re-submission:** Build a form with: amount input, account ID input, **front image file input** (labeled "Front of Check"), **back image file input** (labeled "Back of Check"), and a submit button. Both images must be Base64-encoded before sending to the backend. Use `X-User-Role: INVESTOR` header. On `422` response, display the `actionableMessage` prominently with a **"Retake & Resubmit"** button that re-submits using `retryForTransferId`.
* *Automated Validation:* Write React Testing Library tests: submit button exists; two file inputs exist with correct labels; form fires a submit event including both Base64 image fields; on `422` mock response, `actionableMessage` text renders and "Retake & Resubmit" button appears.
* *Manual Testing:* Fill out the form with both images, hit submit, verify the Network Tab payload includes both image fields. Trigger an IQA failure scenario; confirm the actionable error message displays; click Retake & Resubmit and verify the retry request includes `retryForTransferId`.


3. **Operator View with Full Queue Detail, Search/Filter, and Contribution Override:** Build a queue dashboard where each flagged deposit card shows: Transfer ID, investor account, entered amount, **OCR-recognized amount** (visually highlighted in amber if it differs from entered amount), **MICR data string**, **MICR confidence score** (as a percentage), **Vendor risk score** (color-coded badge: green/yellow/red), risk indicator flags. Add filter/search inputs at the top for status, date range, account ID, and amount range. Include **Approve** and **Reject** buttons. The Approve modal includes an optional "Override Contribution Type" dropdown. On action, remove or update the item in the queue. Use `X-User-Role: OPERATOR`.
* *Automated Validation:* Write tests: both deposits render from a mocked two-item queue; the amount-mismatch item shows an amber highlight; filtering by status narrows to one item; approve button fires a POST with correct payload including optional override; reject button fires a POST with required reason.
* *Manual Testing:* Load the queue; verify images, MICR data, scores, and risk badges appear correctly. Approve an item with a contribution type override; verify it leaves the queue. Check the DB for the override. Reject an item; verify it leaves the queue.


4. **Cap-Table / Ledger View:** Build a ledger dashboard accessible from a navigation link. Shows: account ID, current balance (large, prominent), and a paginated table of all ledger entries (columns: type, amount, counterparty account, transaction ID, timestamp). Add a navigation link to this view from the Investor View.
* *Automated Validation:* Write a test mocking the balance and ledger endpoints; assert the balance value renders; assert at least one ledger entry row appears in the table.
* *Manual Testing:* After posting and approving a deposit, navigate to the ledger view for the investor account. Confirm the new entry and updated balance appear.


5. **Transfer Status Tracking View:** Build a status view where any user can enter a Transfer ID and see the current state, submission timestamp, last-updated timestamp, and a list of state history entries.
* *Automated Validation:* Write a test mocking the `/deposits/{transferId}` response; assert the state and Transfer ID are displayed.
* *Manual Testing:* Submit a deposit, copy the Transfer ID, navigate to the status view, confirm the current state matches the database.



---

#### Phase 9: Demo Scripts

1. **Happy Path Demo Script:** Create `/tests/demo_happy_path.sh` — a fully automated shell script (using `curl`) that:
   - Submits a deposit with synthetic Base64 front + back images using the Clean Pass account ID
   - Polls status until state = `ANALYZING`
   - Calls the operator approve endpoint
   - Polls until state = `COMPLETED`
   - Calls the ledger endpoint and prints the final balance
   - Prints `PASS` / `FAIL` for each step with assertion details
* *Automated Validation:* Run `bash tests/demo_happy_path.sh` against a running local environment and assert exit code 0.
* *Manual Testing:* Watch the script output and confirm each step logs `PASS`.


2. **Rejection / Re-submission Demo Script:** Create `/tests/demo_rejection.sh`:
   - Submits with IQA Blur account ID; asserts `422` with `actionableMessage`
   - Re-submits with `retryForTransferId` using IQA Glare account ID; asserts another `422` with `actionableMessage`
   - Re-submits with Clean Pass account ID; follows through to `APPROVED`
   Prints `PASS` / `FAIL` per step.
* *Automated Validation:* Run `bash tests/demo_rejection.sh` and assert exit code 0.


3. **Manual Review Demo Script:** Create `/tests/demo_manual_review.sh`:
   - Submits with MICR Read Failure account ID
   - Polls until state = `ANALYZING` (flagged for operator review)
   - Calls operator queue; asserts the deposit appears with `micrData` field present
   - Calls operator reject endpoint
   - Asserts state = `REJECTED`
* *Automated Validation:* Run `bash tests/demo_manual_review.sh` and assert exit code 0.


4. **Return / Reversal Demo Script:** Create `/tests/demo_return_reversal.sh`:
   - Walks deposit through submission → approve → `COMPLETED`
   - Posts return notification to `/internal/returns`
   - Asserts state = `RETURNED`
   - Calls ledger endpoint and asserts balance reflects original deposit amount + $30 fee deducted
   - Queries `audit_logs` and asserts `INVESTOR_NOTIFIED` entry exists
* *Automated Validation:* Run `bash tests/demo_return_reversal.sh` and assert exit code 0.



---

#### Phase 10: Testing & Validation

1. **End-to-End Happy Path Test:** Write a single `@SpringBootTest` that walks a deposit through the complete happy path from submission (with front + back images) → Vendor Clean Pass → Funding Service rules pass → operator approve → both ledger entries created → EOD batch runs → Transfer state = `COMPLETED`. Assert each state transition and that the final ledger balance reflects the deposit. No mocked services — use the real Vendor stub, real Spring context, and real test PostgreSQL container (Testcontainers).
* *Automated Validation:* This test itself is the validation. It must pass.


2. **Vendor Stub Scenario Tests (7 individual tests):** Write one dedicated JUnit test per Vendor stub scenario: IQA Pass, IQA Fail (Blur), IQA Fail (Glare), MICR Read Failure, Duplicate Detected, Amount Mismatch, Clean Pass. Each test must assert: correct scenario enum returned; `actionableMessage` non-null for all failure scenarios; response time < 1 second.
* *Automated Validation:* All 7 tests pass. Maven output shows all 7 scenario names as distinct test cases.


3. **Business Rule Tests:** Write unit tests covering: $5,000 boundary; contribution type default for retirement account; contribution cap enforcement; internal duplicate detection rejection.


4. **State Machine Transition Tests — Valid and Invalid:** Write JUnit tests asserting:
   - Valid happy-path transitions proceed in order
   - Invalid transitions throw an exception: `COMPLETED → APPROVED`, `REJECTED → APPROVED`, `RETURNED → FUNDS_POSTED`
* *Automated Validation:* All transition tests pass.


5. **Reversal Posting & Fee Test:** Write a unit test verifying: investor is debited original amount + $30 fee; state = `RETURNED`; `INVESTOR_NOTIFIED` audit log entry created.


6. **Settlement File Contents Validation Test:** Create 3 approved transfers, trigger the batch, and assert: file created; 3 records; each record has non-null `micrData` and image references; batch `totalAmount` = sum of the 3; all 3 transfers = `COMPLETED`.


7. **Performance Benchmark Tests:** Write timing-aware tests using `Awaitility` or `StopWatch`:
   - Vendor stub response < 1 second
   - After flagging a deposit, it appears in `GET /operator/queue` within 1 second
   - After any state-changing action, the new state is queryable within 1 second


8. **Full Test Suite Validation:** Run `mvn clean test jacoco:report` and assert ≥ 10 tests pass (steps 1–7 above already exceed this).
* *Manual Testing:* Confirm zero failures in Maven console output.


9. **Format Code:** Run `mvn spotless:apply`.
* *Automated Validation:* Run `mvn spotless:check`. Must pass.


10. **Generate Reports:** Output test execution results to `/reports`.
* *Automated Validation:* Script verifies `index.html` exists in `/reports`.
* *Manual Testing:* Open `/reports/index.html` in a browser; click through results.



---

#### Phase 11: Documentation & Packaging

1. **Architecture Document:** Create `/docs/architecture.md` containing:
   - A system diagram (ASCII or embedded Mermaid) showing all 7 components and their interactions
   - Service boundaries: what each component owns and does NOT own
   - Data flow narrative: end-to-end path from deposit submission to settlement completion
   - Key schema summary: tables, primary relationships, and state machine diagram
* *Automated Validation:* Script checks `/docs/architecture.md` exists, is non-empty, and contains the strings `"Vendor"`, `"Funding"`, and `"Settlement"` as a proxy for content completeness.
* *Manual Testing:* Confirm a third-party evaluator can understand all system boundaries from this document alone.


2. **Decision Log:** Create `/docs/decision_log.md` documenting key choices with at minimum these 5 entries:
   - Language choice (Java/Spring Boot) and justification vs. alternatives
   - Settlement file format (X9 ICL vs structured JSON) and trade-offs
   - Vendor stub design (trigger mechanism choice and why)
   - State machine design (why 8 states; alternatives considered)
   - Data store choice (PostgreSQL vs SQLite)
* *Automated Validation:* Script checks file exists and is non-empty.


3. **Vendor Stub Scenarios Document:** Confirm `/docs/vendor-stub-scenarios.md` (produced in Phase 3, Step 4) is complete and current with the final implementation.
* *Automated Validation:* Script asserts file is non-empty and lists all 7 scenario names.


4. **README.md:** Create `README.md` covering:
   - Project name and one-paragraph description
   - Architecture overview (link to `/docs/architecture.md`)
   - Prerequisites (Java, Maven, Docker, Node)
   - Setup: `cp .env.example .env && make dev` (or `docker compose up`)
   - How to run demo scripts (`bash tests/demo_happy_path.sh`, etc.)
   - How to run tests and view reports
   - Data flow narrative (happy path end-to-end)
   - Explicit risks and limitations disclaimer (no compliance or regulatory claims)
* *Automated Validation:* Script checks `README.md` exists and is non-empty.
* *Manual Testing:* Have a third-party evaluator follow the README from scratch and confirm setup works without additional guidance.


5. **SUBMISSION.md:** Create `SUBMISSION.md` at the repo root containing all 7 required sections:
   - **Project name:**
   - **Summary (3–5 sentences):** What was built? Why these design choices? Key trade-offs?
   - **How to run (copy-paste commands):**
   - **Test/eval results:** Screenshot or log excerpt; link to `/reports`
   - **With one more week, we would:**
   - **Risks and limitations:**
   - **How should ACME evaluate production readiness?**
* *Automated Validation:* Script checks `SUBMISSION.md` exists and contains all 7 section headers as strings.
* *Manual Testing:* Read through and confirm every section is substantively answered.


6. **One-Page Architecture Write-up:** Produce a write-up of ≤1 page as a section within `SUBMISSION.md` or as `/docs/writeup.md` covering:
   - Architecture choices and reasoning
   - Vendor Service stub design rationale
   - Transfer state machine rationale (why 8 states; what each represents)
   - Known risks and limitations
* *Automated Validation:* Script checks the target file exists and is non-empty.
* *Manual Testing:* Read it; confirm it fits on one page and addresses all four topics.


7. **Final End-to-End Validation Run:**
   - `bash verify_setup.sh`
   - `bash verify_env.sh`
   - `mvn clean test jacoco:report` — must show ≥ 10 passing, 0 failing
   - `mvn spotless:check`
   - Confirm `index.html` in `/reports`
   - Run all 4 demo scripts: `demo_happy_path.sh`, `demo_rejection.sh`, `demo_manual_review.sh`, `demo_return_reversal.sh`
   - Confirm all required documentation files exist and are non-empty: `README.md`, `SUBMISSION.md`, `/docs/architecture.md`, `/docs/decision_log.md`, `/docs/vendor-stub-scenarios.md`, `/docs/writeup.md` (or equivalent section), `.env.example`
* *Manual Testing:* Walk through the entire system manually — deposit submission through the React UI → vendor validation → operator review → approve → ledger view → EOD settlement → return/reversal → trace endpoint. Confirm every view renders correctly and all audit/trace records are present in the database.


**Gap Coverage Summary (vs. Apex Fintech Challenger Projects spec):**

| Spec Requirement | Covered In |
|---|---|
| Front + back image as distinct fields | Ph 2.2, Ph 5.1, Ph 6.3, Ph 7.2 |
| Re-submission on IQA failure with actionable messages | Ph 3.3, Ph 5.1, Ph 7.2 |
| Stub configurable without code changes | Ph 3.2 |
| Stub documentation artifact | Ph 3.4, Ph 11.3 |
| Transfer record named attributes (Type, Memo, SubType, etc.) | Ph 2.2, Ph 4.2 |
| Account identifier resolution to internal numbers | Ph 2.3 |
| Omnibus account lookup via client config | Ph 2.3, Ph 4.2 |
| Contribution caps (beyond per-deposit $5k limit) | Ph 4.1 |
| Check images (front/back) in operator queue | Ph 5.2, Ph 8.3 |
| MICR data + confidence scores in operator queue | Ph 5.2, Ph 8.3 |
| Risk indicators + Vendor scores in operator queue | Ph 5.2, Ph 8.3 |
| Recognized vs. entered amount comparison | Ph 5.2, Ph 8.3 |
| Operator override of contribution type | Ph 5.2, Ph 8.3 |
| Search and filter by date/status/account/amount | Ph 5.2, Ph 8.3 |
| Audit log with who/what/when structured fields | Ph 5.3 |
| X9 file with MICR data in check detail records | Ph 6.3 |
| X9 file binary image references (front + back) | Ph 6.3 |
| X9 file amount + sequence/batch metadata | Ph 6.3 |
| Deposits after cutoff roll to next business day | Ph 6.2 |
| Settlement Bank acknowledgment tracking | Ph 6.4 |
| Investor notification on returned check | Ph 4.4 |
| Inbound return notification endpoint (simulated) | Ph 4.3 |
| Per-deposit decision trace (structured, queryable) | Ph 5.5, Ph 7.2 |
| Deposit source differentiation in logs | Ph 7.1 |
| Monitor for missing/delayed settlement files | Ph 7.3 |
| Cap-table / ledger view | Ph 5.4, Ph 8.4 |
| Tests for invalid state machine transitions | Ph 10.4 |
| Single end-to-end happy path test | Ph 10.1 |
| One test per Vendor stub scenario (7 total) | Ph 10.2 |
| Settlement file contents validation test | Ph 10.6 |
| Demo scripts for all 4 paths | Ph 9.1–9.4 |
| /docs/architecture.md | Ph 11.1 |
| .env.example | Ph 1.4 |
| Makefile with `make dev` | Ph 1.3 |
| SUBMISSION.md with all 7 required fields | Ph 11.5 |
| One-page write-up on architecture/stub/state machine | Ph 11.6 |
| Performance tests for 4 of 5 benchmarks | Ph 10.7 |
| 7-component separation enforced and documented | Ph 2 context, Ph 11.1 |
