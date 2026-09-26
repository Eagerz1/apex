#!/usr/bin/env bash
# Compiles and runs the JUnit-5 style test suite offline (no Maven Central access).
set -euo pipefail
cd "$(dirname "$0")/../.."
source tooling/offline/env.sh
OUT=build/offline
rm -rf $OUT/testkit $OUT/test && mkdir -p $OUT/testkit $OUT/test
find tooling/offline/testkit -name '*.java' > $OUT/testkit-sources.txt
javac_run -d $OUT/testkit @$OUT/testkit-sources.txt
./tooling/offline/compile-core.sh > /dev/null
find src/test/java -name '*.java' > $OUT/test-sources.txt
javac_run -Xlint:all -cp "$OUT/core:$OUT/testkit" -d $OUT/test @$OUT/test-sources.txt
"$JAVA_BIN" -cp "$OUT/core:$OUT/testkit:$OUT/test:src/main/resources" org.junit.jupiter.api.Runner "$OUT/test"
