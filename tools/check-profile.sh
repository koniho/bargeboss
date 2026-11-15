#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/profile-check
find core developer/shared developer/tests -name '*.java' | sort > build/profile-check/sources.txt
echo tests/com/bargeboss/game/ReplayInput.java >> build/profile-check/sources.txt
javac -Xlint:-options --release 8 -d build/profile-check @build/profile-check/sources.txt
java -cp build/profile-check com.bargeboss.game.ProfileScenarioTest
