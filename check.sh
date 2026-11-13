#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/check
find core tests -name '*.java' | sort > build/check/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/check @build/check/sources.txt
java -cp build/check com.bargeboss.game.CoreTest
