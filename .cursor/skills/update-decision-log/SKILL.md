---
name: update-decision-log
description: Updates the project decision log (docs/decision_log.md) when new stack or implementation decisions are made. Use when the user records a new decision, documents a choice, adds to the decision log, or asks to log a decision.
---

# Update Decision Log

When a new decision is made for this project, add it to `docs/decision_log.md` following the existing structure.

## Decision Types

**Stack Choices** — Technology or architecture choices (language, database, framework, tooling). Often has a PRD reference or explicit alternatives.

**Implementation Choices Not Directed by PRD** — Design or implementation details not specified in the original requirements.

## Adding a New Entry

### 1. Choose the section

- Stack choice (tech, framework, tool) → **Stack Choices**
- Implementation detail not in PRD → **Implementation Choices Not Directed by PRD**

### 2. Use the template

**Stack Choice template:**

```markdown
### N. [Short Title]

**PRD:** [Relevant PRD requirement, or "Not specified"]

**Decision:** [One-sentence decision]

**Rationale:** [Why this choice; 1–3 sentences]

**Alternatives considered:** [Brief list of alternatives and why rejected]
```

**Implementation Choice template:**

```markdown
### N. [Short Title]

**PRD:** [What PRD says, or "Not specified"]

**Decision:** [Concrete implementation detail]

**Rationale:** [Why this approach]
```

### 3. Numbering

- Use the next sequential number in the section (e.g., if Stack Choices has 1–10, new entry is 11)
- No gaps; renumber if entries were removed

### 4. Update the Summary Table

Add a row to the table at the bottom:

```markdown
| [Area label] | [PRD or "Not specified"] | [Decision summary] |
```

Keep rows ordered: Stack Choice areas first (Language, Data store, Architecture, …), then Implementation Choice areas.

## Editing Existing Entries

- Preserve the template structure (Decision, Rationale, PRD, Alternatives)
- After adding or removing entries, renumber all items in the affected section sequentially (1, 2, 3, …)
- Sync the Summary Table: add rows for new entries, remove rows for deleted entries, keep Area labels consistent

## File Location

`docs/decision_log.md` — always update this file.
