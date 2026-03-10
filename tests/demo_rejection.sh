#!/usr/bin/env bash
# Rejection / Re-submission Demo Script — IQA Blur → IQA Glare → Clean Pass retry flow.
#
# Prerequisites:
#   - Backend running (e.g. docker compose up -d db && ./mvnw spring-boot:run)
#   - curl and python3 (for JSON parsing) available
#
# Usage: bash tests/demo_rejection.sh
# Override base URL: BASE_URL=http://localhost:9090 bash tests/demo_rejection.sh
#
# Exit code: 0 on full success, 1 on any failure.
set -e

BASE="${BASE_URL:-http://localhost:8080}"
IQA_BLUR_ACCOUNT="iqa-blur"
IQA_GLARE_ACCOUNT="iqa-glare"
CLEAN_PASS_ACCOUNT="clean-pass"
INVESTOR_ACCOUNT="TEST001"
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

# --- Step 1: Submit with IQA Blur → expect 422 with actionableMessage ---
echo ""
echo "=== Step 1: Submit with IQA Blur (expect 422) ==="
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/deposits" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $IQA_BLUR_ACCOUNT" \
  -d "{\"frontImage\":\"$FRONT_IMG\",\"backImage\":\"$BACK_IMG\",\"amount\":$AMOUNT,\"accountId\":\"$INVESTOR_ACCOUNT\"}")

HTTP_BODY=$(echo "$RESP" | sed '$d')
HTTP_CODE=$(echo "$RESP" | tail -1)

if [[ "$HTTP_CODE" != "422" ]]; then
  fail "Step 1: expected HTTP 422, got $HTTP_CODE. Body: $HTTP_BODY"
else
  TRANSFER_ID=$(echo "$HTTP_BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    tid = d.get('transferId')
    msg = d.get('actionableMessage')
    if tid and msg:
        print(tid)
    else:
        sys.exit(1)
except Exception:
    sys.exit(1)
" 2>/dev/null) || true
  if [[ -z "$TRANSFER_ID" ]]; then
    fail "Step 1: could not parse transferId and actionableMessage from 422 response: $HTTP_BODY"
  else
    pass "Step 1: 422 with actionableMessage, transferId=$TRANSFER_ID"
  fi
fi

if [[ -z "$TRANSFER_ID" ]]; then
  echo "Cannot continue without transferId. Exiting."
  exit 1
fi

# --- Step 2: Re-submit with retryForTransferId, IQA Glare → expect 422 ---
echo ""
echo "=== Step 2: Re-submit with retryForTransferId, IQA Glare (expect 422) ==="
RESP2=$(curl -s -w "\n%{http_code}" -X POST "$BASE/deposits" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $IQA_GLARE_ACCOUNT" \
  -d "{\"frontImage\":\"$FRONT_IMG\",\"backImage\":\"$BACK_IMG\",\"amount\":$AMOUNT,\"accountId\":\"$INVESTOR_ACCOUNT\",\"retryForTransferId\":\"$TRANSFER_ID\"}")

HTTP_BODY2=$(echo "$RESP2" | sed '$d')
HTTP_CODE2=$(echo "$RESP2" | tail -1)

if [[ "$HTTP_CODE2" != "422" ]]; then
  fail "Step 2: expected HTTP 422, got $HTTP_CODE2. Body: $HTTP_BODY2"
else
  MSG2=$(echo "$HTTP_BODY2" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('actionableMessage', '') or '')
except Exception:
    print('')
" 2>/dev/null) || true
  if [[ -n "$MSG2" ]]; then
    pass "Step 2: 422 with actionableMessage (Glare)"
  else
    fail "Step 2: 422 but no actionableMessage in response: $HTTP_BODY2"
  fi
fi

# --- Step 3: Re-submit with retryForTransferId, Clean Pass → 201, follow through to APPROVED ---
echo ""
echo "=== Step 3: Re-submit with retryForTransferId, Clean Pass (expect 201) ==="
RESP3=$(curl -s -w "\n%{http_code}" -X POST "$BASE/deposits" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: INVESTOR" \
  -H "X-Account-Id: $CLEAN_PASS_ACCOUNT" \
  -d "{\"frontImage\":\"$FRONT_IMG\",\"backImage\":\"$BACK_IMG\",\"amount\":$AMOUNT,\"accountId\":\"$INVESTOR_ACCOUNT\",\"retryForTransferId\":\"$TRANSFER_ID\"}")

HTTP_BODY3=$(echo "$RESP3" | sed '$d')
HTTP_CODE3=$(echo "$RESP3" | tail -1)

if [[ "$HTTP_CODE3" != "201" ]]; then
  fail "Step 3: expected HTTP 201, got $HTTP_CODE3. Body: $HTTP_BODY3"
else
  pass "Step 3: 201 Created"
fi

# --- Step 4: Poll until state = ANALYZING ---
echo ""
echo "=== Step 4: Poll status until state = ANALYZING ==="
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
    pass "Step 4: state=ANALYZING (attempt $i)"
    break
  fi
  if [[ $i -eq $MAX_POLL ]]; then
    fail "Step 4: did not reach ANALYZING within ${MAX_POLL}s. Last state=$STATE"
  fi
  sleep $POLL_INTERVAL
done

# --- Step 5: Operator approve ---
echo ""
echo "=== Step 5: Operator approve ==="
APPROVE_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/operator/queue/$TRANSFER_ID/approve" \
  -H "Content-Type: application/json" \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: op1" \
  -d '{}')

APPROVE_CODE=$(echo "$APPROVE_RESP" | tail -1)
if [[ "$APPROVE_CODE" != "200" ]]; then
  fail "Step 5: Operator approve expected HTTP 200, got $APPROVE_CODE"
else
  pass "Step 5: Operator approve 200 OK"
fi

# --- Step 6: Assert state = APPROVED ---
echo ""
echo "=== Step 6: Assert state = APPROVED ==="
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

if [[ "$FINAL_STATE" == "APPROVED" ]]; then
  pass "Step 6: state=APPROVED"
else
  fail "Step 6: expected state APPROVED, got $FINAL_STATE"
fi

# --- Summary ---
echo ""
echo "=== Summary ==="
if [[ $FAIL_COUNT -eq 0 ]]; then
  echo "All steps PASSED. Rejection/re-submission demo complete."
  exit 0
else
  echo "$FAIL_COUNT step(s) FAILED."
  exit 1
fi
