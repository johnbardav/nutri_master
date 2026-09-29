#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$ROOT"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}"
OUT="build/domain-checks"
mkdir -p "$OUT"
"${JAVA_BIN}javac" --release 17 -encoding UTF-8 -d "$OUT" app/src/main/java/com/poligran/nutrimaster/model/Recipe.java app/src/main/java/com/poligran/nutrimaster/domain/*.java app/src/test/java/com/poligran/nutrimaster/DomainChecks.java
"${JAVA_BIN}java" -cp "$OUT" com.poligran.nutrimaster.DomainChecks
