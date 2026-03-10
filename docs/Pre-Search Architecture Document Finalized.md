### ---

**Phase 1: Context & Constraints**

**1\. Scale & Load Profile**

* **Users & Traffic Pattern:** Evaluator test runs (manual and automated).  
* **Real-time Requirements:** High responsiveness required; Vendor Service stub responses under 1 second, state transitions queryable within 1 second, and ledger postings within seconds of approval.

**2\. Budget & Cost Ceiling**

* **Monthly Spend Limit:** $0.  
* **Cost Approach:** 100% local development.

**3\. Time to Ship**

* **Timeline:** 3 days. Fast development and execution are the top priorities.

**4\. Compliance & Regulatory Needs**

* **Data Privacy:** Strictly synthetic data; absolutely no real PII, account numbers, or check images.  
* **Disclaimers:** A brief document outlining risks, limitations, and a disclaimer of "no compliance or regulatory claims" must be included.

**5\. Team & Skill Constraints**

* **Solo/Team:** Solo.  
* **Language:** Java. Chosen for its mature ecosystem, strong support for agentic development, and proven enterprise scalability.

### ---

**Phase 2: Architecture Discovery**

**6\. Hosting & Deployment**

* **Deployment Target:** Local deployment using Docker for fast iteration.

**7\. Authentication & Authorization**

* **Auth Approach:** Mock authentication using hardcoded profiles (e.g., Investor Profile, Operator Profile). Full JWT/Session management is deferred as a post-MVP enhancement.

**8\. Database & Data Layer**

* **Database Type:** PostgreSQL. While local storage was suggested, PostgreSQL running in a Docker container easily satisfies the local requirement while providing much better scalable performance for the ledger and transfer tables.

**9\. Backend/API Architecture**

* **Architecture:** Monolith. A single Java application hosting the REST API endpoints, the Vendor Service Stub, the Funding Service middleware, and the settlement logic.  
* **Communication:** Internal method calls between bounded contexts (e.g., Funding Service to Ledger) rather than network calls.

**10\. Frontend Framework & Rendering**

* **Client Interface:** A simple React frontend to simulate the mobile client (image upload, amount entry) and the operator review queue.

**11\. Third-Party Integrations**

* **External Services:** The required Vendor Service (for IQA, MICR, OCR, duplicate detection) is completely stubbed.  
* **Stub Control:** Deterministic responses (e.g., Pass, Fail-Blur, Duplicate) will be controlled via a local configuration file, allowing seamless scenario testing without code changes.

### ---

**Phase 3: Post-Stack Refinement**

**12\. Security Vulnerabilities**

* **Secrets Management:** All database credentials and app configs will be loaded via environment variables; a .env.example file will be provided.  
* **Logging:** Logs will be fully redacted to ensure no synthetic PII leaks into the console.

**13\. File Structure & Project Organization**

* **Framework standard:** Maven standard directory layout (src/main/java, src/test/java).  
* **Mandatory Folders:** The root will include the mandated /docs (decision log, architecture), /tests (test reports), and /reports folders.

**14\. Naming Conventions & Code Style**

* **Style:** Standard Java naming conventions, enforced by a Maven plugin (Spotless) to keep the codebase clean for the evaluators.

**15\. Testing Strategy**

* **Tools:** JUnit and Mockito.  
* **Coverage Target:** Minimum of 10 tests encompassing the happy path, vendor stub scenarios, business rule enforcement, state transitions, reversal logic, and settlement file output.

**16\. Recommended Tooling & DX**

* **Setup:** docker compose up will be the single-command entry point to spin up the PostgreSQL database, the Java backend, and the React frontend simultaneously.

---

With the architecture fully defined, we can move right into implementation.

Would you like me to generate the initial docker-compose.yml and Maven pom.xml to bootstrap this Java, React, and PostgreSQL monolith?