#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p /tmp/xgimi-core-tests
kotlinc \
  app/src/main/java/dev/projectortv/core/Navigation.kt \
  app/src/main/java/dev/projectortv/core/Favorites.kt \
  tools/core-smoke.kt -include-runtime -d /tmp/xgimi-core-tests/check.jar
java -jar /tmp/xgimi-core-tests/check.jar
