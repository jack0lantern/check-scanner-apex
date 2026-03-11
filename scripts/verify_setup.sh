#!/usr/bin/env bash
# Verifies that required root directories exist: /docs, /tests, /reports.
# Usage: ./scripts/verify_setup.sh

set -e

REQUIRED_DIRS=(docs tests reports)

for dir in "${REQUIRED_DIRS[@]}"; do
  if [[ ! -d "$dir" ]]; then
    echo "ERROR: Required directory '$dir' does not exist"
    exit 1
  fi
done

echo "OK: All required directories exist (docs, tests, reports)"
exit 0
