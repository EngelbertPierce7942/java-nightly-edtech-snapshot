#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/build/classes"
mkdir -p "$OUT"
find "$ROOT/src/main/java" "$ROOT/src/test/java" -name '*.java' -print | sort > "$ROOT/build/sources.txt"
javac -d "$OUT" @"$ROOT/build/sources.txt"
java -cp "$OUT" dev.infrai.edtech.NightlySnapshotPolicyTest
