#!/usr/bin/env bash
# Submits a batch of deposits with settlementDate = today and automatically approves them.
# Use for settlement file generation testing.
#
# Prerequisites:
#   - Server running (make run or docker compose up)
#   - Server clock before 6:30 PM CT (otherwise settlementDate = next business day)
#
# Usage: ./scripts/batch-settlement-deposits.sh [count]
#   count: number of deposits to create (default: 10)
#
# Example: ./scripts/batch-settlement-deposits.sh 10
set -e

COUNT="${1:-10}"
BASE="${BASE_URL:-http://localhost:8080}"

echo "Creating $COUNT approved deposits with settlementDate = today..."
echo "Base URL: $BASE"
echo

RESPONSE=$(curl -s -w "\nHTTP %{http_code}" -X POST \
  -H "X-User-Role: OPERATOR" \
  -H "X-Account-Id: OP-001" \
  "$BASE/debug/batch-settlement-deposits?count=$COUNT&accountId=TEST001")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Response ($HTTP_CODE):"
echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
echo

if [[ "$HTTP_CODE" != *"200"* ]]; then
  echo "✗ Request failed. Ensure server is running and reachable at $BASE"
  exit 1
fi

CREATED=$(echo "$BODY" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    print(d.get('count', 0))
except:
    print(0)
" 2>/dev/null || echo "0")

if [ "$CREATED" -eq "$COUNT" ] 2>/dev/null; then
  echo "✓ Created and approved $COUNT deposits. They will be included in the next EOD settlement batch."
else
  echo "✓ Batch request completed. Check response above for details."
fi
