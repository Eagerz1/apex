#!/usr/bin/env bash
# Full offline compile: API stubs -> core -> Bukkit platform layer.
set -euo pipefail
cd "$(dirname "$0")/../.."
source tooling/offline/env.sh
OUT=build/offline
rm -rf $OUT/stubs $OUT/paper && mkdir -p $OUT/stubs $OUT/paper
find tooling/offline/stubs -name '*.java' > $OUT/stub-sources.txt
javac_run -nowarn -d $OUT/stubs @$OUT/stub-sources.txt
./tooling/offline/compile-core.sh
find src/main/java/com/apexmc/skyblock/paper -name '*.java' > $OUT/paper-sources.txt
javac_run -Xlint:all -cp "$OUT/core:$OUT/stubs" -d $OUT/paper @$OUT/paper-sources.txt
echo "platform classes: $(find $OUT/paper -name '*.class' | wc -l)"
