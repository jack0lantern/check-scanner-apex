#!/usr/bin/env bash
# Manual Review Demo Script — MICR Read Failure → operator queue → reject.
#
# Prerequisites:
#   - Backend running (e.g. docker compose up -d db && ./mvnw spring-boot:run)
#   - curl and python3 (for JSON parsing) available
#
# Usage: bash tests/demo_manual_review.sh
# Override base URL: BASE_URL=http://localhost:9090 bash tests/demo_manual_review.sh
#
# Exit code: 0 on full success, 1 on any failure.
set -e

BASE="${BASE_URL:-http://localhost:8080}"
MICR_FAIL_ACCOUNT="micr-fail"
INVESTOR_ACCOUNT="TEST001"
FRONT_IMG="AQID"
BACK_IMG="AQID"
AMOUNT="200.00"

FAIL_COUNT=0

pass() {
  echo "PASS: $*"
}

fail() {
  echo "FAIL: $*"
  ((FAIL_COUNT++)) || true
}

# --- Step 1: Submit with MICR Read Failure → expect 201, deposit flagged for operator ---
echo ""
echo "=== Step 1: Submit with MICR Read Failure (expect 201) ==="
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/deposits" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $MICR_FAIL_ACCOUNT" \
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
    state = d.get('state')
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

# --- Step 3: Call operator queue; assert deposit appears with micrData field present ---
echo ""
echo "=== Step 3: Operator queue contains deposit with micrData field ==="
QUEUE_RESP=$(curl -s "$BASE/operator/queue" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1")

FOUND=$(echo "$QUEUE_RESP" | TRANSFER_ID="$TRANSFER_ID" python3 -c "
import json, os, sys
try:
    tid = os.environ.get('TRANSFER_ID', '')
    items = json.load(sys.stdin)
    if not isinstance(items, list):
        sys.exit(1)
    for item in items:
        item_tid = item.get('transferId') or item.get('transfer_id')
        if str(item_tid) == tid:
            if 'micrData' in item:
                print('yes')
                sys.exit(0)
            else:
                sys.exit(2)
    sys.exit(1)
except Exception:
    sys.exit(1)
" 2>/dev/null) || true

if [[ "$FOUND" == "yes" ]]; then
  pass "Step 3: Deposit in queue with micrData field present"
else
  fail "Step 3: Deposit not in queue or micrData field missing. Queue: $QUEUE_RESP"
fi

# --- Step 4: Call operator reject endpoint ---
echo ""
echo "=== Step 4: Operator reject ==="
REJECT_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/operator/queue/$TRANSFER_ID/reject" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1" \
  -d '{"reason":"MICR unreadable; customer to deposit at branch"}')

REJECT_CODE=$(echo "$REJECT_RESP" | tail -1)
if [[ "$REJECT_CODE" != "200" ]]; then
  fail "Step 4: Operator reject expected HTTP 200, got $REJECT_CODE"
else
  pass "Step 4: Operator reject 200 OK"
fi

# --- Step 5: Assert state = REJECTED ---
echo ""
echo "=== Step 5: Assert state = REJECTED ==="
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

if [[ "$FINAL_STATE" == "REJECTED" ]]; then
  pass "Step 5: state=REJECTED"
else
  fail "Step 5: expected state REJECTED, got $FINAL_STATE"
fi

# --- Summary ---
echo ""
echo "=== Summary ==="
if [[ $FAIL_COUNT -eq 0 ]]; then
  echo "All steps PASSED. Manual review demo complete."
  exit 0
else
  echo "$FAIL_COUNT step(s) FAILED."
  exit 1
fi
