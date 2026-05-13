#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}

capture_json=$(curl -s -X POST "$BASE_URL/api/captures" \
  -H 'Content-Type: application/json' \
  --data @examples/failing-capture.json)

echo "Capture response:"
echo "$capture_json"

capture_id=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<< "$capture_json")

replay_json=$(curl -s -X POST "$BASE_URL/api/replays" \
  -H 'Content-Type: application/json' \
  -d "{\"captureId\":\"$capture_id\",\"targetUrlOverride\":\"http://fake-crm-api:8081/fixed/customers\"}")

echo "Replay job response:"
echo "$replay_json"

job_id=$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<< "$replay_json")

echo "Polling replay job: $job_id"
sleep 4
curl -s "$BASE_URL/api/replays/$job_id"
echo
