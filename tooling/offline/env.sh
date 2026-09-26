# Offline JDK 21 toolchain used inside the Arena sandbox (Maven Central is not reachable).
TOOLS=/home/user/tools
export JAVA_BIN="$TOOLS/jdk4py/jdk4py/java-runtime/bin/java"
JAVAC_CP="$TOOLS/mods/jdk.compiler:$TOOLS/mods/jdk.internal.opt"
javac_run() { "$JAVA_BIN" --add-exports java.base/jdk.internal.javac=ALL-UNNAMED -cp "$JAVAC_CP" com.sun.tools.javac.Main "$@"; }
