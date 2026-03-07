#!/usr/bin/env bash
# Curl commands for manual API testing. Requires X-User-Role and X-Account-Id headers.
# Usage: ./scripts/curl-tests.sh
# Override base URL: BASE_URL=http://localhost:9090 ./scripts/curl-tests.sh
set -e
BASE="${BASE_URL:-http://localhost:8080}"

curl_req() {
  curl -s -w "\nHTTP %{http_code}\n" \
    -H "X-User-Role: INVESTOR" \
    -H "X-Account-Id: TEST001" \
    "$@"
}

echo "=== Debug: Auth Test ==="
curl_req "$BASE/debug/auth-test"
echo

echo "=== Debug: Account Resolve (TEST001) ==="
curl_req "$BASE/debug/account-resolve?accountId=TEST001"
echo

echo "=== Vendor Stub: IQA Pass ==="
curl_req "$BASE/debug/vendor-stub?accountId=iqa-pass&amount=150.00"
echo

echo "=== Vendor Stub: IQA Fail (Blur) ==="
curl_req "$BASE/debug/vendor-stub?accountId=iqa-blur&amount=150.00"
echo

echo "=== Vendor Stub: IQA Fail (Glare) ==="
curl_req "$BASE/debug/vendor-stub?accountId=iqa-glare&amount=150.00"
echo

echo "=== Vendor Stub: MICR Read Failure ==="
curl_req "$BASE/debug/vendor-stub?accountId=micr-fail&amount=150.00"
echo

echo "=== Vendor Stub: Duplicate Detected ==="
curl_req "$BASE/debug/vendor-stub?accountId=duplicate&amount=150.00"
echo

echo "=== Vendor Stub: Amount Mismatch ==="
curl_req "$BASE/debug/vendor-stub?accountId=amount-mismatch&amount=150.00"
echo

echo "=== Vendor Stub: Clean Pass ==="
curl_req "$BASE/debug/vendor-stub?accountId=clean-pass&amount=100.00"
echo
