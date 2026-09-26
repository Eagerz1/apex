#!/usr/bin/env bash
# Runs the manual end-to-end sanity scenario against the shipped configuration.
set -euo pipefail
cd "$(dirname "$0")/../.."
source tooling/offline/env.sh
./tooling/offline/test.sh > /dev/null
OUT=build/offline
mkdir -p $OUT/smoke
javac_run -nowarn -cp "$OUT/core:$OUT/test:$OUT/testkit" -d $OUT/smoke tooling/offline/smoke/SmokeRun.java
"$JAVA_BIN" -cp "$OUT/core:$OUT/test:$OUT/testkit:$OUT/smoke:src/main/resources" SmokeRun
