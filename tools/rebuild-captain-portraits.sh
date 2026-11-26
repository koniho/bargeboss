#!/bin/sh
# Rebuilds the three player-captain portrait sources and runtime sheets from the user's local
# character sheets (never committed). Each expression frame is composited onto its neutral
# frame so only the eyes, brows and mouth change; see docs/art-prompts/player-captains-v2.md.
# Usage: tools/rebuild-captain-portraits.sh WHITNEY_SHEET WESLEY_SHEET DOT_SHEET
set -eu
if [ "$#" -ne 3 ]; then
  echo "usage: $0 WHITNEY_SHEET WESLEY_SHEET DOT_SHEET" >&2
  exit 2
fi
cd "$(dirname "$0")/.."
mkdir -p build/portraits
javac -Xlint:-options --release 8 -d build/portraits tools/PackGavinPortrait.java tools/CleanPortraitEdges.java
build() {
  captain=$1 sheet=$2 name=$3
  python3 tools/extract_sheet_portraits.py "$sheet" \
    "docs/art-studies/captains/$captain-source-$name.png" --captain "$captain"
  java -Djava.awt.headless=true -cp build/portraits com.bargeboss.game.PackGavinPortrait \
    "docs/art-studies/captains/$captain-source-$name.png" \
    "assets/sprites/portrait-captain-$captain-$name.png"
  java -Djava.awt.headless=true -cp build/portraits com.bargeboss.game.CleanPortraitEdges \
    "assets/sprites/portrait-captain-$captain-$name.png" \
    "assets/sprites/portrait-captain-$captain-$name.png"
  python3 tools/portrait_registration.py "assets/sprites/portrait-captain-$captain-$name.png" "$captain"
}
build whitney "$1" v1
build wesley "$2" v1
build dot "$3" v2
