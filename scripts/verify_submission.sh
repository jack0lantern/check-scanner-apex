#!/usr/bin/env bash
# Verifies that SUBMISSION.md exists and contains all 7 required section headers.
# Required sections: Project name, Summary, How to run, Test/eval results,
#   With one more week we would, Risks and limitations, How should ACME evaluate production readiness.
# Usage: ./scripts/verify_submission.sh

set -e

SUBMISSION_PATH="SUBMISSION.md"
REQUIRED_SECTIONS=(
  "Project name"
  "Summary (3–5 sentences)"
  "How to run (copy-paste commands)"
  "Test/eval results"
  "With one more week, we would"
  "Risks and limitations"
  "How should ACME evaluate production readiness"
)

if [[ ! -f "$SUBMISSION_PATH" ]]; then
  echo "ERROR: $SUBMISSION_PATH does not exist"
  exit 1
fi

if [[ ! -s "$SUBMISSION_PATH" ]]; then
  echo "ERROR: $SUBMISSION_PATH is empty"
  exit 1
fi

for section in "${REQUIRED_SECTIONS[@]}"; do
  if ! grep -qF "$section" "$SUBMISSION_PATH"; then
    echo "ERROR: $SUBMISSION_PATH does not contain required section '$section'"
    exit 1
  fi
done

echo "OK: $SUBMISSION_PATH exists and contains all 7 required section headers"
exit 0
