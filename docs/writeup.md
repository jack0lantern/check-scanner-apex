# One-Page Architecture Write-up

Mobile Check Deposit System — Architecture, Stub Design, State Machine, and Limitations

---

## Architecture Choices and Reasoning

The system is a **modular monolith**: a single Spring Boot application hosting seven bounded components (Deposit Capture, Vendor Service, Funding Service, Ledger Posting, Operator Workflow, Settlement, Return/Reversal). Components communicate via direct method calls rather than network hops.

**Rationale:** A monolith minimizes latency, deployment complexity, and operational overhead for a local evaluation environment. The seven-component boundary is enforced via package structure and Spring beans—each component owns a distinct responsibility and is independently testable. Alternatives such as microservices would add network latency and deployment complexity without benefit for the scope. PostgreSQL provides ACID guarantees for double-entry ledger semantics; React + Vite provides a minimal but functional UI for investor and operator workflows.

---

## Vendor Service Stub Design Rationale

The Vendor Service is an interface with a deterministic `StubVendorService` implementation. Scenario selection is driven by the `X-Account-Id` header (e.g., `iqa-blur`, `duplicate`, `clean-pass`), mapping to a `VendorScenario` enum.

**Rationale:** Request headers are easy to set in curl, Postman, and the frontend—no config file edits or restarts between test runs. The account ID is a natural discriminator for "which investor is submitting" and aligns with the PRD's "test account numbers" option. The stub returns a consistent `VendorAssessmentResult` shape (scenario, vendorScore, micrData, micrConfidence, ocrAmount, actionableMessage), enabling the Funding Service and Deposit flow to treat all vendor responses uniformly. The implementation is swappable via Spring `@Primary` or a factory pattern without modifying downstream code. A debug endpoint `GET /debug/vendor-stub?accountId=<trigger>` allows quick manual verification of each scenario.

---

## Transfer State Machine Rationale (8 States)

The transfer lifecycle uses eight states to model distinct decision points and terminal outcomes:

| State | Meaning |
|-------|---------|
| **REQUESTED** | Deposit submitted; initial persistence before vendor assessment |
| **VALIDATING** | Vendor stub (IQA/MICR/OCR) and Funding Service rules in progress |
| **ANALYZING** | Passed automated checks; awaiting operator review (or auto-approve path) |
| **APPROVED** | Operator approved; ledger posting authorized |
| **FUNDS_POSTED** | Double-entry posted (debit omnibus, credit investor); awaiting settlement |
| **COMPLETED** | Included in EOD settlement file; lifecycle complete |
| **REJECTED** | Terminal failure (vendor, funding, or operator reject) |
| **RETURNED** | Check bounced post-approval; reversal and $30 fee posted |

**Why 8 states:** Six states model the happy path (REQUESTED → VALIDATING → ANALYZING → APPROVED → FUNDS_POSTED → COMPLETED); two terminal failure states (REJECTED, RETURNED) capture distinct outcomes—rejection before funds post vs. return after approval. Splitting APPROVED and FUNDS_POSTED separates "authorized" from "posted," enabling return handling for posted transfers. Returns are accepted from APPROVED, FUNDS_POSTED, or COMPLETED (e.g. NSF—insufficient funds at sending account—can occur after settlement). Retries are allowed from REQUESTED or VALIDATING via `retryForTransferId`.

---

## Known Risks and Limitations

- **Synthetic data only** — No real PII, account numbers, or check images. For demonstration and evaluation only.
- **No compliance or regulatory claims** — Not certified for production use. Does not constitute legal, regulatory, or compliance advice.
- **Stubbed integrations** — Vendor Service and Settlement Bank are stubbed. Real integrations require additional security, retries, and reconciliation.
- **Mock authentication** — Uses `X-User-Role` and `X-Account-Id` headers. Production requires proper session/JWT management.
- **Local development** — Designed for local Docker deployment. Cloud deployment needs additional configuration and security hardening.
- **Return handling scope** — Returns are accepted for APPROVED, FUNDS_POSTED, or COMPLETED transfers (e.g. NSF bounce after settlement).
