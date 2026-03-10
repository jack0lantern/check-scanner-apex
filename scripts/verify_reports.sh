#!/usr/bin/env bash
# Verifies that test reports were generated and index.html exists in /reports.
# Run after: mvn clean test jacoco:report
# Usage: ./scripts/verify_reports.sh

set -e

REPORTS_INDEX="reports/index.html"

if [[ ! -f "$REPORTS_INDEX" ]]; then
  echo "ERROR: $REPORTS_INDEX does not exist. Run: mvn clean test jacoco:report"
  exit 1
fi

echo "OK: $REPORTS_INDEX exists"
exit 0
