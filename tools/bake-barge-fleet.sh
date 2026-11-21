#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/fleet64
find core -name '*.java' | sort > build/fleet64/sources.txt
javac -Xlint:-options --release 8 -d build/fleet64 @build/fleet64/sources.txt \
 tools/BargePoseGuide.java tools/TugPoseGuide.java tools/YachtPoseGuide.java tools/BakeMooredBoats.java \
 tools/materials/WatercraftMesh.java tools/materials/WatercraftModels.java tools/materials/BakeWatercraftMaterials.java \
 tools/BakeBargeFleet.java tools/PackBargeFrames.java tools/GenerateVesselGlow.java tools/GenerateShadowMasks.java \
 tools/GenerateCargoShadows.java tools/GenerateSurfaceLighting.java tools/GenerateOcclusionMasks.java
java -Djava.awt.headless=true -cp build/fleet64 com.bargeboss.game.BakeBargeFleet
cp docs/art-studies/barge-fleet-64/*-64.png assets/sprites/
cp docs/art-studies/barge-fleet-64/*-registration.csv docs/art-studies/
java -cp build/fleet64 com.bargeboss.game.PackBargeFrames base
java -cp build/fleet64 com.bargeboss.game.GenerateVesselGlow
java -cp build/fleet64 com.bargeboss.game.GenerateShadowMasks
java -cp build/fleet64 com.bargeboss.game.GenerateCargoShadows
java -cp build/fleet64 com.bargeboss.game.GenerateSurfaceLighting
java -cp build/fleet64 com.bargeboss.game.GenerateOcclusionMasks core/com/bargeboss/game/OcclusionMasks.java
java -cp build/fleet64 com.bargeboss.game.PackBargeFrames
