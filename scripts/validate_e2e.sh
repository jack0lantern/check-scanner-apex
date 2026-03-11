#!/usr/bin/env bash
# Final End-to-End Validation Run (Phase 11.7)
#
# Runs all automated validation steps from the detailed plan:
#   - verify_setup.sh, verify_env.sh
#   - mvn clean test jacoco:report (≥10 passing, 0 failing)
#   - mvn spotless:check
#   - Confirm reports/index.html exists
#   - Run all 4 demo scripts (requires backend running)
#   - Confirm all required documentation files exist and are non-empty
#
# Prerequisites:
#   - For demo scripts: Backend must be running (docker compose up -d db && ./mvnw spring-boot:run)
#   - Run from project root
#
# Usage: ./scripts/validate_e2e.sh
#   --skip-demos    Skip demo scripts (use when backend is not running)
#
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

SKIP_DEMOS=false
for arg in "$@"; do
  case "$arg" in
    --skip-demos) SKIP_DEMOS=true ;;
  esac
done

FAIL_COUNT=0

pass() {
  echo "PASS: $*"
}

fail() {
  echo "FAIL: $*"
  ((FAIL_COUNT++)) || true
}

echo "=============================================="
echo "Final End-to-End Validation Run"
echo "=============================================="

# --- 1. verify_setup.sh ---
echo ""
echo "=== 1. verify_setup.sh ==="
if bash "$SCRIPT_DIR/verify_setup.sh"; then
  pass "verify_setup.sh"
else
  fail "verify_setup.sh"
fi

# --- 2. verify_env.sh ---
echo ""
echo "=== 2. verify_env.sh ==="
if bash "$SCRIPT_DIR/verify_env.sh"; then
  pass "verify_env.sh"
else
  fail "verify_env.sh"
fi

# --- 3. mvn clean test jacoco:report ---
echo ""
echo "=== 3. mvn clean test jacoco:report ==="
MVN_OUTPUT=$(./mvnw clean test jacoco:report 2>&1) || true
echo "$MVN_OUTPUT" | tail -30
if echo "$MVN_OUTPUT" | grep -q "BUILD FAILURE"; then
  fail "mvn clean test jacoco:report failed"
elif echo "$MVN_OUTPUT" | grep -q "BUILD SUCCESS"; then
  # Parse final summary: "Tests run: N, Failures: 0, Errors: 0"
  FINAL_LINE=$(echo "$MVN_OUTPUT" | grep -E "Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+" | tail -1)
  TESTS_RUN=$(echo "$FINAL_LINE" | grep -oE "Tests run: [0-9]+" | grep -oE "[0-9]+" || echo "0")
  TESTS_FAILURES=$(echo "$FINAL_LINE" | grep -oE "Failures: [0-9]+" | grep -oE "[0-9]+" || echo "0")
  TESTS_ERRORS=$(echo "$FINAL_LINE" | grep -oE "Errors: [0-9]+" | grep -oE "[0-9]+" || echo "0")
  if [[ "$TESTS_FAILURES" -gt 0 ]] || [[ "$TESTS_ERRORS" -gt 0 ]]; then
    fail "mvn test: expected 0 failures, 0 errors (got failures=$TESTS_FAILURES, errors=$TESTS_ERRORS)"
  elif [[ "$TESTS_RUN" -lt 10 ]]; then
    fail "mvn test: expected ≥10 tests passing (got $TESTS_RUN)"
  else
    pass "mvn clean test jacoco:report ($TESTS_RUN tests, 0 failures, 0 errors)"
  fi
else
  fail "mvn clean test jacoco:report (could not parse result)"
fi

# Run vendor stub docs validation (part of make test)
bash "$SCRIPT_DIR/validate-vendor-stub-docs.sh" || fail "validate-vendor-stub-docs.sh"

# --- 4. mvn spotless:check ---
echo ""
echo "=== 4. mvn spotless:check ==="
if ./mvnw spotless:check -q 2>&1; then
  pass "mvn spotless:check"
else
  fail "mvn spotless:check (run mvn spotless:apply to fix)"
fi

# --- 5. Confirm reports/index.html ---
echo ""
echo "=== 5. reports/index.html ==="
if bash "$SCRIPT_DIR/verify_reports.sh"; then
  pass "reports/index.html exists"
else
  fail "reports/index.html missing (run mvn clean test jacoco:report)"
fi

# --- 6. Demo scripts ---
echo ""
echo "=== 6. Demo scripts ==="
if [[ "$SKIP_DEMOS" == "true" ]]; then
  echo "Skipping demo scripts (--skip-demos)"
else
  BASE_URL="${BASE_URL:-http://localhost:8080}"
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 2 "$BASE_URL/deposits" 2>/dev/null || echo "000")
  if [[ "$HTTP_CODE" == "000" ]]; then
    echo "  Backend not reachable at $BASE_URL. Start with: docker compose up -d db && ./mvnw spring-boot:run"
    echo "  Run with --skip-demos to validate without demo scripts."
    fail "Demo scripts skipped (backend not running)"
  else
    for demo in demo_happy_path demo_rejection demo_manual_review demo_return_reversal; do
      echo "  Running tests/$demo.sh..."
      if BASE_URL="$BASE_URL" bash "tests/$demo.sh"; then
        pass "tests/$demo.sh"
      else
        fail "tests/$demo.sh"
      fi
    done
  fi
fi

# --- 7. Required documentation files ---
echo ""
echo "=== 7. Required documentation files ==="
REQUIRED_DOCS=(
  "README.md"
  "SUBMISSION.md"
  "docs/architecture.md"
  "docs/decision_log.md"
  "docs/vendor-stub-scenarios.md"
  "docs/writeup.md"
  ".env.example"
)
for doc in "${REQUIRED_DOCS[@]}"; do
  if [[ -f "$doc" ]] && [[ -s "$doc" ]]; then
    pass "$doc exists and is non-empty"
  else
    fail "$doc missing or empty"
  fi
done

# Verify SUBMISSION.md has all 7 sections
if bash "$SCRIPT_DIR/verify_submission.sh" 2>/dev/null; then
  pass "SUBMISSION.md has all 7 required sections"
else
  fail "SUBMISSION.md missing required sections"
fi

# --- Summary ---
echo ""
echo "=============================================="
echo "Summary"
echo "=============================================="
if [[ $FAIL_COUNT -eq 0 ]]; then
  echo "All validation steps PASSED."
  exit 0
else
  echo "$FAIL_COUNT step(s) FAILED."
  exit 1
fi
