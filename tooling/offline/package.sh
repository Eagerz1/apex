#!/usr/bin/env bash
# Produces build/offline/apex-skyblock-progression-1.0.0.jar from the offline build.
set -euo pipefail
cd "$(dirname "$0")/../.."
./tooling/offline/compile-all.sh > /dev/null
OUT=build/offline
JAR=$OUT/apex-skyblock-progression-1.0.0.jar
rm -f "$JAR"
python3 - "$JAR" <<'PY'
import os, sys, zipfile
jar = sys.argv[1]
with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as zf:
    zf.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nCreated-By: offline build\n")
    for root in ("build/offline/core", "build/offline/paper"):
        for base, _, files in os.walk(root):
            for name in files:
                path = os.path.join(base, name)
                zf.write(path, os.path.relpath(path, root))
    for name in os.listdir("src/main/resources"):
        zf.write(os.path.join("src/main/resources", name), name)
print(jar)
PY
ls -lh "$JAR"
