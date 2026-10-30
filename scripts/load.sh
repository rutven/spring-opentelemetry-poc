#!/usr/bin/env bash
# Generates mixed traffic against the gateway. Usage: scripts/load.sh [iterations]
set -euo pipefail
GW=${GATEWAY_URL:-http://localhost:8080}
ITEMS=(book pen laptop coffee)
USERS=(alice bob carol)

for ((i = 1; i <= ${1:-50}; i++)); do
  item=${ITEMS[RANDOM % ${#ITEMS[@]}]}
  user=${USERS[RANDOM % ${#USERS[@]}]}
  id=$(curl -s -X POST "$GW/api/orders" -H 'Content-Type: application/json' -H "X-User-Id: $user" \
    -d "{\"item\":\"$item\",\"quantity\":$((RANDOM % 5 + 1))}" | sed -E 's/.*"id":([0-9]+).*/\1/')
  curl -s -o /dev/null -H "X-User-Id: $user" "$GW/api/orders/$id"
  ((i % 5 == 0)) && curl -s -o /dev/null -H "X-User-Id: $user" "$GW/api/orders/$id?fail=true"   # error span
  ((i % 7 == 0)) && curl -s -o /dev/null -H "X-User-Id: $user" "$GW/api/orders/summary?ids=$id,1" # coroutine fan-out
  ((i % 11 == 0)) && curl -s -o /dev/null "$GW/api/orders/999999"                                   # 404
  printf '.'
done
echo " done"
