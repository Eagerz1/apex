#!/usr/bin/env bash
# Compiles the dependency-free core (no Bukkit imports) with javac 21.
set -euo pipefail
cd "$(dirname "$0")/../.."
source tooling/offline/env.sh
rm -rf build/offline/core && mkdir -p build/offline/core
find src/main/java/com/apexmc/skyblock/core -name '*.java' > build/offline/core-sources.txt
javac_run -Xlint:all -d build/offline/core @build/offline/core-sources.txt
echo "core classes: $(find build/offline/core -name '*.class' | wc -l)"
