#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/preview
find core desktop -name '*.java' | sort > build/preview/sources.txt
echo tools/Preview.java >> build/preview/sources.txt
javac --release 8 -encoding UTF-8 -d build/preview @build/preview/sources.txt
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.Preview
