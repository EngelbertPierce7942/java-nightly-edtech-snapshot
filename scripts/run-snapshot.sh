#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/build/classes"
mkdir -p "$OUT"
find "$ROOT/src/main/java" -name '*.java' -print | sort > "$ROOT/build/main-sources.txt"
javac -d "$OUT" @"$ROOT/build/main-sources.txt"
java -cp "$OUT" dev.infrai.edtech.NightlySnapshotCommand
