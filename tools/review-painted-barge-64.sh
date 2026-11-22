#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
study=docs/art-studies/barge-painted-64
classes=build/painted64
mkdir -p "$classes"
javac -Xlint:-options --release 8 -encoding UTF-8 -d "$classes" \
    tools/BargePoseGuide.java tools/VesselRegistration.java \
    tools/PackVesselQuarters.java tools/TugConceptPreview.java tools/AtlasBounds.java
for quarter in 0 1 2 3; do
    java -cp "$classes" com.bargeboss.game.BargePoseGuide "--quarter=$quarter"
done
java -cp "$classes" com.bargeboss.game.PackVesselQuarters \
    "$study/hull-quarter-0-v1.png" "$study/hull-quarter-1-v3.png" \
    "$study/hull-quarter-2-v2.png" "$study/hull-quarter-3-v1.png" \
    "$study/hull-64-candidate-v1.png"
java -cp "$classes" com.bargeboss.game.VesselRegistration \
    docs/art-studies/barge-isometric-v1-64-pose-guide.png \
    "$study/hull-64-candidate-v1.png" "$study/hull-64-registration-v1.csv" 8 8
python tools/project-vessel-anchors.py \
    docs/art-studies/barge-isometric-v1-64-guide-anchors.csv \
    "$study/hull-64-registration-v1.csv" "$study/hull-64-candidate-v1.png" \
    "$study/hull-64-anchors-v1.json"
java -cp "$classes" com.bargeboss.game.TugConceptPreview \
    "$study/hull-64-candidate-v1.png" out/painted-barge-64 \
    "$study/hull-64-registration-v1.csv" \
    docs/art-studies/barge-isometric-v1-64-pose-guide.png "$study/hull-64-anchors-v1.json" 64
