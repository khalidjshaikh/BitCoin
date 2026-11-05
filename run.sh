#!/usr/bin/env bash
set -euo pipefail

# Run the Bitcoin price microservice.
# Usage: ./run.sh [port]
#   PORT env var or first arg overrides the default port (8080).

cd "$(dirname "$0")"

PORT="${1:-${PORT:-8080}}"

echo "Building and starting Bitcoin price service on port ${PORT}..."
exec mvn -q spring-boot:run -Dspring-boot.run.arguments="--server.port=${PORT}"