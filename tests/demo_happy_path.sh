#!/usr/bin/env bash
# Happy Path Demo Script — fully automated shell script that walks a deposit through
# submission → ANALYZING → operator approve → COMPLETED → ledger balance.
#
# Prerequisites:
#   - Backend running (e.g. docker compose up -d db && ./mvnw spring-boot:run)
#   - curl and python3 (for JSON parsing) available
#
# Usage: bash tests/demo_happy_path.sh
# Override base URL: BASE_URL=http://localhost:9090 bash tests/demo_happy_path.sh
#
# Exit code: 0 on full success, 1 on any failure.
set -e

BASE="${BASE_URL:-http://localhost:8080}"
# Use amount-mismatch so deposit goes to ANALYZING (operator queue); clean-pass auto-approves and skips the queue
SCENARIO_ACCOUNT="amount-mismatch"
INVESTOR_ACCOUNT="TEST001"
# Synthetic Base64 check images (minimal valid payload; vendor stub ignores content)
FRONT_IMG="AQID"
BACK_IMG="AQID"
AMOUNT="100.00"

FAIL_COUNT=0

pass() {
  echo "PASS: $*"
}

fail() {
  echo "FAIL: $*"
  ((FAIL_COUNT++)) || true
}

# --- Step 1: Submit deposit ---
echo ""
echo "=== Step 1: Submit deposit (amount-mismatch → ANALYZING) ==="
TRANSFER_ID=""
for attempt in 1 2 3; do
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/deposits" \
    -H "Content-Type: application/json" \
    -H "X-User-Role: INVESTOR" \
    -H "X-Account-Id: $SCENARIO_ACCOUNT" \
    -d "{\"frontImage\":\"$FRONT_IMG\",\"backImage\":\"$BACK_IMG\",\"amount\":$AMOUNT,\"accountId\":\"$INVESTOR_ACCOUNT\"}")

  HTTP_BODY=$(echo "$RESP" | sed '$d')
  HTTP_CODE=$(echo "$RESP" | tail -1)

  if [[ "$HTTP_CODE" == "201" ]]; then
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
    if [[ -n "$TRANSFER_ID" ]]; then
      pass "Submit deposit: 201 Created, transferId=$TRANSFER_ID"
      break
    fi
  fi
  if [[ "$HTTP_CODE" == "422" ]] && echo "$HTTP_BODY" | grep -q "Duplicate"; then
    echo "  Attempt $attempt: 422 Duplicate (vendor stub check number collision). Retrying..."
    sleep 1
    continue
  fi
  if [[ $attempt -eq 3 ]]; then
    fail "Submit deposit: expected HTTP 201, got $HTTP_CODE. Body: $HTTP_BODY"
  fi
done

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
  CODE=$(echo "$RESP" | tail -1)
  STATE=$(echo "$BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('state', ''))
except Exception:
    print('')
" 2>/dev/null) || true
  if [[ "$STATE" == "ANALYZING" ]]; then
    pass "Poll status: state=ANALYZING (attempt $i)"
    break
  fi
  if [[ $i -eq $MAX_POLL ]]; then
    fail "Poll status: did not reach ANALYZING within ${MAX_POLL}s. Last state=$STATE, body=$BODY"
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
  fail "Operator approve: expected HTTP 200, got $APPROVE_CODE"
else
  pass "Operator approve: 200 OK"
fi

# --- Step 4: Poll until state = COMPLETED ---
echo ""
echo "=== Step 4: Poll status until state = COMPLETED ==="
# EOD runs every minute in dev; allow up to 90s
MAX_POLL=18
POLL_INTERVAL=5
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
  if [[ "$STATE" == "COMPLETED" ]]; then
    pass "Poll status: state=COMPLETED (attempt $i)"
    break
  fi
  if [[ $i -eq $MAX_POLL ]]; then
    fail "Poll status: did not reach COMPLETED within ~90s. Last state=$STATE. (EOD batch may not have run yet.)"
  fi
  sleep $POLL_INTERVAL
done

# --- Step 5: Ledger balance ---
echo ""
echo "=== Step 5: Ledger balance ==="
BAL_RESP=$(curl -s -w "\n%{http_code}" "$BASE/accounts/$INVESTOR_ACCOUNT/balance" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $INVESTOR_ACCOUNT")

BAL_BODY=$(echo "$BAL_RESP" | sed '$d')
BAL_CODE=$(echo "$BAL_RESP" | tail -1)

if [[ "$BAL_CODE" != "200" ]]; then
  fail "Ledger balance: expected HTTP 200, got $BAL_CODE"
else
  BALANCE=$(echo "$BAL_BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('balance', ''))
except Exception:
    print('')
" 2>/dev/null) || true
  if [[ -n "$BALANCE" ]]; then
    pass "Ledger balance: $BALANCE (account=$INVESTOR_ACCOUNT)"
  else
    pass "Ledger balance: 200 OK, body=$BAL_BODY"
  fi
fi

# --- Summary ---
echo ""
echo "=== Summary ==="
if [[ $FAIL_COUNT -eq 0 ]]; then
  echo "All steps PASSED. Happy path demo complete."
  exit 0
else
  echo "$FAIL_COUNT step(s) FAILED."
  exit 1
fi
