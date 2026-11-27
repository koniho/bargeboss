#!/bin/sh
# Offline asset work only. Run after check.sh has compiled shared Java classes.
set -eu
cd "$(dirname "$0")/../.."
mkdir -p build/materials
javac -Xlint:-options --release 8 -cp build/check -d build/materials \
    tools/materials/*.java tools/TugPoseGuide.java tools/BargePoseGuide.java \
    tools/YachtPoseGuide.java tools/BakeMooredBoats.java tools/RacingBoatBake.java tools/InstallRacingBoat.java tools/PoliceBoatBake.java
java -Djava.awt.headless=true -cp build/materials:build/check com.bargeboss.game.BakeWatercraftMaterials
java -Djava.awt.headless=true -cp build/materials:build/check com.bargeboss.game.WatercraftMaterialCheck
java -Djava.awt.headless=true -cp build/materials:build/check com.bargeboss.game.WatercraftMaterialReview
