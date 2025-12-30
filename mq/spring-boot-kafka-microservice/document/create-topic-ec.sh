#!/usr/bin/env bash
set -euo pipefail

# Create Kafka topics for the event-centric environment (docker-compose-env.yml).
# Requires docker compose stack to be up so the "kafka" service is running.

COMPOSE_FILE="docker-compose-env.yml"
SERVICE="kafka"
BOOTSTRAP="localhost:29092"
TOPICS=(
  "orders.events.v1"
  "payments.events.v1"
  "alerts.order_payment_inconsistency.v1"
)

echo "Creating topics in ${SERVICE} (${BOOTSTRAP}) using ${COMPOSE_FILE}..."

for topic in "${TOPICS[@]}"; do
  docker compose -f "${COMPOSE_FILE}" exec "${SERVICE}" \
    kafka-topics --bootstrap-server "${BOOTSTRAP}" \
    --create --if-not-exists --topic "${topic}" \
    --partitions 1 --replication-factor 1
done

echo "Done."
