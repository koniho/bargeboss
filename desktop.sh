#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/desktop
find core desktop -name '*.java' | sort > build/desktop/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/desktop @build/desktop/sources.txt
jar cfe build/bargeboss.jar com.bargeboss.game.Desktop -C build/desktop com
if [ "${1:-}" != '--build-only' ]; then java -jar build/bargeboss.jar; fi
