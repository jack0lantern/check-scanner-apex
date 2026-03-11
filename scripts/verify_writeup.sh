#!/usr/bin/env bash
# Verifies that docs/writeup.md exists and is non-empty.
# Phase 11.6: One-Page Architecture Write-up validation.
# Usage: ./scripts/verify_writeup.sh

set -e

DOC_PATH="docs/writeup.md"

if [[ ! -f "$DOC_PATH" ]]; then
  echo "ERROR: $DOC_PATH does not exist"
  exit 1
fi

if [[ ! -s "$DOC_PATH" ]]; then
  echo "ERROR: $DOC_PATH is empty"
  exit 1
fi

echo "OK: $DOC_PATH exists and is non-empty"
exit 0
