#!/usr/bin/env bash
# Verifies that docs/architecture.md exists, is non-empty, and contains required content.
# Required strings: "Vendor", "Funding", "Settlement" (proxy for content completeness).
# Usage: ./scripts/verify_architecture.sh

set -e

DOC_PATH="docs/architecture.md"
REQUIRED_STRINGS=("Vendor" "Funding" "Settlement")

if [[ ! -f "$DOC_PATH" ]]; then
  echo "ERROR: $DOC_PATH does not exist"
  exit 1
fi

if [[ ! -s "$DOC_PATH" ]]; then
  echo "ERROR: $DOC_PATH is empty"
  exit 1
fi

for str in "${REQUIRED_STRINGS[@]}"; do
  if ! grep -qF "$str" "$DOC_PATH"; then
    echo "ERROR: $DOC_PATH does not contain required string '$str'"
    exit 1
  fi
done

echo "OK: $DOC_PATH exists, is non-empty, and contains Vendor, Funding, Settlement"
exit 0
