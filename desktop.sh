#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/desktop
mkdir -p build/desktop
find core desktop -name '*.java' | sort > build/desktop/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/desktop @build/desktop/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -cp build/desktop -d build/desktop tools/DesktopProgressTest.java
java -cp build/desktop com.bargeboss.game.DesktopProgressTest
rm build/desktop/com/bargeboss/game/DesktopProgressTest.class
jar cfe build/bargeboss.jar com.bargeboss.game.Desktop -C build/desktop com -C . assets/sprites
if [ "${1:-}" != '--build-only' ]; then java -jar build/bargeboss.jar; fi
