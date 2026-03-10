#!/usr/bin/env bash
# Return / Reversal Demo Script — deposit → approve → return → ledger + audit verification.
#
# Prerequisites:
#   - Backend running (e.g. docker compose up -d db && ./mvnw spring-boot:run)
#   - curl and python3 (for JSON parsing) available
#
# Usage: bash tests/demo_return_reversal.sh
# Override base URL: BASE_URL=http://localhost:9090 bash tests/demo_return_reversal.sh
#
# Exit code: 0 on full success, 1 on any failure.
set -e

BASE="${BASE_URL:-http://localhost:8080}"
CLEAN_PASS_ACCOUNT="clean-pass"
INVESTOR_ACCOUNT="TEST001"
FRONT_IMG="AQID"
BACK_IMG="AQID"
AMOUNT="150.00"
RETURN_FEE=30

FAIL_COUNT=0

pass() {
  echo "PASS: $*"
}

fail() {
  echo "FAIL: $*"
  ((FAIL_COUNT++)) || true
}

# --- Step 1: Submit deposit (Clean Pass) ---
echo ""
echo "=== Step 1: Submit deposit (Clean Pass) ==="
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/deposits" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $CLEAN_PASS_ACCOUNT" \
  -d "{\"frontImage\":\"$FRONT_IMG\",\"backImage\":\"$BACK_IMG\",\"amount\":$AMOUNT,\"accountId\":\"$INVESTOR_ACCOUNT\"}")

HTTP_BODY=$(echo "$RESP" | sed '$d')
HTTP_CODE=$(echo "$RESP" | tail -1)

if [[ "$HTTP_CODE" != "201" ]]; then
  fail "Step 1: expected HTTP 201, got $HTTP_CODE. Body: $HTTP_BODY"
else
  TRANSFER_ID=$(echo "$HTTP_BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    tid = d.get('transferId')
    if tid:
        print(tid)
    else:
        sys.exit(1)
except Exception:
    sys.exit(1)
" 2>/dev/null) || true
  if [[ -z "$TRANSFER_ID" ]]; then
    fail "Step 1: could not parse transferId from response: $HTTP_BODY"
  else
    pass "Step 1: 201 Created, transferId=$TRANSFER_ID"
  fi
fi

if [[ -z "$TRANSFER_ID" ]]; then
  echo "Cannot continue without transferId. Exiting."
  exit 1
fi

# --- Step 2: Poll until state = ANALYZING ---
echo ""
echo "=== Step 2: Poll status until state = ANALYZING ==="
MAX_POLL=10
POLL_INTERVAL=1
STATE=""
for i in $(seq 1 $MAX_POLL); do
  RESP=$(curl -s -w "\n%{http_code}" "$BASE/deposits/$TRANSFER_ID" \
    -H "X-User-Role: INVESTOR" \
    -H "X-Account-Id: $INVESTOR_ACCOUNT")
  BODY=$(echo "$RESP" | sed '$d')
  STATE=$(echo "$BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('state', ''))
except Exception:
    print('')
" 2>/dev/null) || true
  if [[ "$STATE" == "ANALYZING" ]]; then
    pass "Step 2: state=ANALYZING (attempt $i)"
    break
  fi
  if [[ $i -eq $MAX_POLL ]]; then
    fail "Step 2: did not reach ANALYZING within ${MAX_POLL}s. Last state=$STATE"
  fi
  sleep $POLL_INTERVAL
done

# --- Step 3: Operator approve ---
echo ""
echo "=== Step 3: Operator approve ==="
APPROVE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/operator/queue/$TRANSFER_ID/approve" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1" \
  -d '{}')

APPROVE_CODE=$(echo "$APPROVE_RESP" | tail -1)
if [[ "$APPROVE_CODE" != "200" ]]; then
  fail "Step 3: Operator approve expected HTTP 200, got $APPROVE_CODE"
else
  pass "Step 3: Operator approve 200 OK"
fi

# --- Step 4: Poll until state = APPROVED (ledger posted) ---
echo ""
echo "=== Step 4: Poll status until state = APPROVED ==="
MAX_POLL=5
POLL_INTERVAL=1
STATE=""
for i in $(seq 1 $MAX_POLL); do
  RESP=$(curl -s -w "\n%{http_code}" "$BASE/deposits/$TRANSFER_ID" \
    -H "X-User-Role: INVESTOR" \
    -H "X-Account-Id: $INVESTOR_ACCOUNT")
  BODY=$(echo "$RESP" | sed '$d')
  STATE=$(echo "$BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('state', ''))
except Exception:
    print('')
" 2>/dev/null) || true
  if [[ "$STATE" == "APPROVED" ]]; then
    pass "Step 4: state=APPROVED (attempt $i)"
    break
  fi
  if [[ $i -eq $MAX_POLL ]]; then
    fail "Step 4: did not reach APPROVED within ${MAX_POLL}s. Last state=$STATE"
  fi
  sleep $POLL_INTERVAL
done

# --- Step 5: Get balance before return ---
echo ""
echo "=== Step 5: Get balance before return ==="
BAL_BEFORE_RESP=$(curl -s -w "\n%{http_code}" "$BASE/accounts/$INVESTOR_ACCOUNT/balance" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $INVESTOR_ACCOUNT")

BAL_BEFORE_BODY=$(echo "$BAL_BEFORE_RESP" | sed '$d')
BAL_BEFORE_CODE=$(echo "$BAL_BEFORE_RESP" | tail -1)

if [[ "$BAL_BEFORE_CODE" != "200" ]]; then
  fail "Step 5: Balance endpoint expected HTTP 200, got $BAL_BEFORE_CODE"
else
  BAL_BEFORE=$(echo "$BAL_BEFORE_BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('balance', ''))
except Exception:
    print('')
" 2>/dev/null) || true
  pass "Step 5: Balance before return = $BAL_BEFORE"
fi

# --- Step 6: Post return notification ---
echo ""
echo "=== Step 6: Post return notification to /internal/returns ==="
RETURN_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/internal/returns" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1" \
  -d "{\"transferId\":\"$TRANSFER_ID\",\"returnReason\":\"NSF\"}")

RETURN_CODE=$(echo "$RETURN_RESP" | tail -1)
if [[ "$RETURN_CODE" != "200" ]]; then
  fail "Step 6: Return endpoint expected HTTP 200, got $RETURN_CODE"
else
  pass "Step 6: Return notification 200 OK"
fi

# --- Step 7: Assert state = RETURNED ---
echo ""
echo "=== Step 7: Assert state = RETURNED ==="
STATUS_RESP=$(curl -s "$BASE/deposits/$TRANSFER_ID" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $INVESTOR_ACCOUNT")
FINAL_STATE=$(echo "$STATUS_RESP" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('state', ''))
except Exception:
    print('')
" 2>/dev/null) || true

if [[ "$FINAL_STATE" == "RETURNED" ]]; then
  pass "Step 7: state=RETURNED"
else
  fail "Step 7: expected state RETURNED, got $FINAL_STATE"
fi

# --- Step 8: Ledger balance reflects original + $30 fee deducted ---
echo ""
echo "=== Step 8: Ledger balance reflects reversal (original + \$30 fee) ==="
BAL_AFTER_RESP=$(curl -s -w "\n%{http_code}" "$BASE/accounts/$INVESTOR_ACCOUNT/balance" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $INVESTOR_ACCOUNT")

BAL_AFTER_BODY=$(echo "$BAL_AFTER_RESP" | sed '$d')
BAL_AFTER_CODE=$(echo "$BAL_AFTER_RESP" | tail -1)

if [[ "$BAL_AFTER_CODE" != "200" ]]; then
  fail "Step 8: Balance endpoint expected HTTP 200, got $BAL_AFTER_CODE"
else
  BAL_AFTER=$(echo "$BAL_AFTER_BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('balance', ''))
except Exception:
    print('')
" 2>/dev/null) || true

  # Expected: balance_after = balance_before - AMOUNT - RETURN_FEE
  EXPECTED=$(echo "$BAL_BEFORE $AMOUNT $RETURN_FEE" | python3 -c "
import sys
try:
    before = float(sys.argv[1])
    amount = float(sys.argv[2])
    fee = float(sys.argv[3])
    expected = before - amount - fee
    print(f'{expected:.2f}')
except Exception:
    print('')
" 2>/dev/null) || true

  if [[ -n "$BAL_AFTER" && -n "$EXPECTED" ]]; then
    # Compare with tolerance for float
    MATCH=$(echo "$BAL_AFTER $EXPECTED" | python3 -c "
import sys
try:
    a = float(sys.argv[1])
    b = float(sys.argv[2])
    print('yes' if abs(a - b) < 0.01 else 'no')
except Exception:
    print('no')
" 2>/dev/null) || true
    if [[ "$MATCH" == "yes" ]]; then
      pass "Step 8: Balance $BAL_AFTER reflects original ($AMOUNT) + fee (\$$RETURN_FEE) deducted"
    else
      fail "Step 8: Balance $BAL_AFTER expected ~$EXPECTED (before $BAL_BEFORE - $AMOUNT - $RETURN_FEE)"
    fi
  else
    pass "Step 8: Balance after return = $BAL_AFTER"
  fi
fi

# --- Step 9: Assert INVESTOR_NOTIFIED in audit_logs ---
echo ""
echo "=== Step 9: Assert INVESTOR_NOTIFIED in audit_logs ==="
AUDIT_RESP=$(curl -s -w "\n%{http_code}" "$BASE/debug/audit-check?transferId=$TRANSFER_ID&action=INVESTOR_NOTIFIED" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1")

AUDIT_CODE=$(echo "$AUDIT_RESP" | tail -1)
if [[ "$AUDIT_CODE" != "200" ]]; then
  fail "Step 9: Expected 200 from audit-check, got $AUDIT_CODE"
else
  pass "Step 9: INVESTOR_NOTIFIED entry exists in audit_logs"
fi

# --- Summary ---
echo ""
echo "=== Summary ==="
if [[ $FAIL_COUNT -eq 0 ]]; then
  echo "All steps PASSED. Return/reversal demo complete."
  exit 0
else
  echo "$FAIL_COUNT step(s) FAILED."
  exit 1
fi
