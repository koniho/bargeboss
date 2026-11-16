#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/check
mkdir -p build/check
find core tests -name '*.java' | sort > build/check/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/check @build/check/sources.txt
java -cp build/check com.bargeboss.game.CoreTest

# Asset contract is host-only; native game remains pure shared Java.
javac -Xlint:-options --release 8 -encoding UTF-8 -cp build/check -d build/check tools/CargoAssetCheck.java tools/GenerateCargoShadows.java tools/GenerateSurfaceLighting.java
java -cp build/check com.bargeboss.game.CargoAssetCheck
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.GenerateCargoShadows --check
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.GenerateSurfaceLighting --check
