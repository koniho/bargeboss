#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/check
mkdir -p build/check
find core tests -name '*.java' | sort > build/check/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/check @build/check/sources.txt
java -cp build/check com.bargeboss.game.CoreTest
java -cp build/check com.bargeboss.game.MooredBoatsTest

# Asset contract is host-only; native game remains pure shared Java.
javac -Xlint:-options --release 8 -encoding UTF-8 -cp build/check -d build/check tools/CargoAssetCheck.java tools/GenerateCargoShadows.java tools/GenerateSurfaceLighting.java
java -cp build/check com.bargeboss.game.CargoAssetCheck
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.GenerateCargoShadows --check
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.GenerateSurfaceLighting --check
# Geometric normal maps must preserve every registered boat silhouette.
javac -Xlint:-options --release 8 -encoding UTF-8 -cp build/check -d build/check desktop/com/bargeboss/game/*.java tools/NormalBoatReview.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.NormalBoatReview

# Fixed Indiana shore modules must stay dry and within the geometry budget.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/IndianaShoreTilesTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.IndianaShoreTilesTest

# Scrolling overview uses a fixed viewport and retains its slider after release.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/OverviewMapPixelTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.OverviewMapPixelTest

# Cached training terrain must preserve wall faces without dynamic lighting.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/ShoreOcclusionPixelTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.ShoreOcclusionPixelTest terrain
