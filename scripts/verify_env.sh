#!/usr/bin/env bash
# Verifies that .env.example is non-empty and that every ${VAR} referenced in
# docker-compose.yml appears as a key in .env.example.
# Usage: ./scripts/verify_env.sh

set -e

ENV_EXAMPLE=".env.example"
COMPOSE_FILE="docker-compose.yml"

if [[ ! -f "$ENV_EXAMPLE" ]]; then
  echo "ERROR: $ENV_EXAMPLE does not exist"
  exit 1
fi

if [[ ! -s "$ENV_EXAMPLE" ]]; then
  echo "ERROR: $ENV_EXAMPLE is empty"
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "ERROR: $COMPOSE_FILE does not exist"
  exit 1
fi

# Extract variable names from ${VAR} or ${VAR:-default} in docker-compose.yml
COMPOSE_VARS=$(grep -oE '\$\{[A-Za-z0-9_]+' "$COMPOSE_FILE" | sed 's/\${//' | sort -u)

for var in $COMPOSE_VARS; do
  if ! grep -qE "^${var}=" "$ENV_EXAMPLE"; then
    echo "ERROR: Variable \$${var} is used in $COMPOSE_FILE but not defined in $ENV_EXAMPLE"
    exit 1
  fi
done

echo "OK: $ENV_EXAMPLE is non-empty and contains all variables referenced in $COMPOSE_FILE"
exit 0
