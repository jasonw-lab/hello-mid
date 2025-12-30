#!/usr/bin/env bash
set -euo pipefail

echo "waiting for redpanda..."
until rpk cluster metadata >/dev/null 2>&1; do
  sleep 1
done

TOPICS=("orders.events.v1" "payments.events.v1" "alerts.order_payment_inconsistency.v1")
for t in "${TOPICS[@]}"; do
  if rpk topic metadata "$t" >/dev/null 2>&1; then
    echo "topic $t exists"
  else
    echo "creating topic $t"
    rpk topic create --partitions 1 --replicas 1 "$t"
  fi
done

echo "topics created"
sleep 1


