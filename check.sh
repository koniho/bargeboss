#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/check
mkdir -p build/check
find core tests -name '*.java' | sort > build/check/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/check @build/check/sources.txt
java -cp build/check com.bargeboss.game.CoreTest
java -cp build/check com.bargeboss.game.MooredBoatsTest
java -cp build/check com.bargeboss.game.PoliceBoatTest

# Asset contract is host-only; native game remains pure shared Java.
javac -Xlint:-options --release 8 -d build/check tools/FlameAssetCheck.java
java -cp build/check FlameAssetCheck
javac -Xlint:-options --release 8 -d build/check tools/InstallBoostCanister.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.InstallBoostCanister --check
javac -Xlint:-options --release 8 -encoding UTF-8 -cp build/check -d build/check tools/CargoAssetCheck.java tools/GenerateCargoShadows.java tools/GenerateSurfaceLighting.java
java -cp build/check com.bargeboss.game.CargoAssetCheck
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.GenerateCargoShadows --check
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.GenerateSurfaceLighting --check
javac -Xlint:-options --release 8 -encoding UTF-8 -cp build/check -d build/check tools/BargeEquipmentAssetCheck.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.BargeEquipmentAssetCheck
# Geometric normal maps must preserve every registered boat silhouette.
javac -Xlint:-options --release 8 -encoding UTF-8 -cp build/check -d build/check desktop/com/bargeboss/game/*.java tools/NormalBoatReview.java tools/MontroseReview.java tools/BiminiReview.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.NormalBoatReview
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/InstallRacingBoat.java tools/DialogueReview.java tools/RacingBoatReview.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.InstallRacingBoat --check
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.RacingBoatReview
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.BiminiReview
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/VertexShadePixelTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.VertexShadePixelTest

javac -Xlint:-options --release 8 -cp build/check -d build/check tools/BakeMooredBoats.java tools/RacingBoatBake.java tools/PoliceBoatBake.java tools/PoliceBoatReview.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.PoliceBoatBake --check
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.PoliceBoatReview

# Fixed Indiana shore modules must stay dry and within the geometry budget.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/IndianaShoreTilesTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.IndianaShoreTilesTest

# Scrolling overview uses a fixed viewport and retains its slider after release.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/OverviewMapPixelTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.OverviewMapPixelTest

# Cached training terrain must preserve wall faces without dynamic lighting.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/ShoreOcclusionPixelTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.ShoreOcclusionPixelTest terrain

# Registered painted island ground must occlude actual boats at every heading.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/IslandShorePixelTest.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.IslandShorePixelTest

# Packed equipment must remain pixel-identical to its source/effect sheets.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/PackBargeFrames.java
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.PackBargeFrames --check

# Portrait cutouts must keep a stable painted base across all talking frames.
javac -Xlint:-options --release 8 -cp build/check -d build/check tools/PortraitRegistrationReview.java
mkdir -p out
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.PortraitRegistrationReview corrected > out/portrait-registration-check.log
