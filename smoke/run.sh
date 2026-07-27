#!/usr/bin/env bash
# Both smoke suites against a running stack. Exits non-zero if either fails.
#
#     docker compose up -d --build --scale app=1
#     smoke/run.sh
#
# See smoke/README.md for what these cover and why they exist alongside `./gradlew test`.
set -uo pipefail
cd "$(dirname "$0")/.."

FAILED=0

./smoke/database.sh || FAILED=1
echo
python3 ./smoke/api.py || FAILED=1

echo
if [ "$FAILED" -eq 0 ]; then
  echo "smoke: ALL PASSED"
else
  echo "smoke: FAILURES ABOVE"
fi
exit "$FAILED"
