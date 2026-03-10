#!/usr/bin/env bash
# Validates that vendor-stub-scenarios.md exists, is non-empty, and contains all 8 scenario names.
# Usage: ./scripts/validate-vendor-stub-docs.sh

set -e

DOC_PATH="docs/vendor-stub-scenarios.md"
SCENARIOS=(
  "IQA Pass"
  "IQA Fail (Blur)"
  "IQA Fail (Glare)"
  "MICR Read Failure"
  "Duplicate Detected"
  "Amount Mismatch"
  "Routing Mismatch"
  "Clean Pass"
)

if [[ ! -f "$DOC_PATH" ]]; then
  echo "ERROR: $DOC_PATH does not exist"
  exit 1
fi

if [[ ! -s "$DOC_PATH" ]]; then
  echo "ERROR: $DOC_PATH is empty"
  exit 1
fi

for scenario in "${SCENARIOS[@]}"; do
  if ! grep -qF "$scenario" "$DOC_PATH"; then
    echo "ERROR: $DOC_PATH does not contain scenario '$scenario'"
    exit 1
  fi
done

echo "OK: $DOC_PATH exists, is non-empty, and contains all scenario names:"
for scenario in "${SCENARIOS[@]}"; do
  echo "  - $scenario"
done
