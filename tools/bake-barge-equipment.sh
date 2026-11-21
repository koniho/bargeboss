#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/barge-equipment
find core -name '*.java' | sort > build/barge-equipment/sources.txt
javac -Xlint:-options --release 8 -d build/barge-equipment @build/barge-equipment/sources.txt \
  tools/BargePoseGuide.java tools/TugPoseGuide.java tools/YachtPoseGuide.java tools/BakeMooredBoats.java \
  tools/materials/WatercraftMesh.java tools/materials/WatercraftModels.java tools/materials/BakeWatercraftMaterials.java \
  tools/BakeBargeEquipment.java tools/GenerateShadowMasks.java tools/GenerateSurfaceLighting.java tools/BargeEquipmentAssetCheck.java
java -Djava.awt.headless=true -cp build/barge-equipment com.bargeboss.game.BakeBargeEquipment
cp out/equipment/fire-normal-height.png out/equipment/fire-material.png \
  out/equipment/fireworks-normal-height.png out/equipment/fireworks-material.png docs/art-studies/barge-equipment-v1/
java -Djava.awt.headless=true -cp build/barge-equipment com.bargeboss.game.GenerateShadowMasks
java -Djava.awt.headless=true -cp build/barge-equipment com.bargeboss.game.GenerateSurfaceLighting
java -Djava.awt.headless=true -cp build/barge-equipment com.bargeboss.game.BargeEquipmentAssetCheck
