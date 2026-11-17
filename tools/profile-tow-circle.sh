#!/bin/sh
# Requires an installed BARGEBOSS_PROFILE=1 APK and authorized adb device.
set -eu
cd "$(dirname "$0")/.."
bargeboss_adb=${ADB:-adb}
bargeboss_output=out/profiles/$(date +%Y%m%d-%H%M%S)-tow-tight-circle
bargeboss_remote=/sdcard/Android/data/com.bargeboss.game/files/profiles/tow-tight-circle-replay.csv
mkdir -p "$bargeboss_output"
if [ ! -f out/phone-screen-timeout-before.txt ]; then
    ./tools/phone-awake.sh on
    trap './tools/phone-awake.sh off' 0
    trap 'exit 130' 2
    trap 'exit 143' 15
fi
"$bargeboss_adb" shell am force-stop com.bargeboss.game
# Let package-replacement/activity teardown finish before delivering the intent.
sleep 1
"$bargeboss_adb" shell rm -f "$bargeboss_remote"
"$bargeboss_adb" shell am start -W -n com.bargeboss.game/com.bargeboss.game.MainActivity \
    --es bb_scene tow-tight-circle --es bb_mode replay --ei bb_warmup 360 --ei bb_frames 2400
bargeboss_start=$(date +%s)
while ! "$bargeboss_adb" shell test -s "$bargeboss_remote"; do
    if [ "$(($(date +%s)-bargeboss_start))" -gt 180 ]; then
        echo 'Capture timed out; unlock the phone and check the profiling build.' >&2; exit 1
    fi
    sleep 2
done
"$bargeboss_adb" pull "$bargeboss_remote" "$bargeboss_output/frames.csv"
python3 - "$bargeboss_output/frames.csv" <<'PYVERIFY'
import csv, sys
rows=list(csv.DictReader(open(sys.argv[1])))
samples=[r for r in rows if r['phase']=='sample']
assert len(samples)==2400, 'Incomplete capture'
assert all(float(r['tow_count'])==1 for r in rows), 'Tow detached during capture'
PYVERIFY
python3 tools/summarize-profile.py "$bargeboss_output/frames.csv" > "$bargeboss_output/summary.json"
git rev-parse HEAD > "$bargeboss_output/revision.txt"
git diff --stat >> "$bargeboss_output/revision.txt"
printf 'Towing circle capture: %s\n' "$bargeboss_output"
cat "$bargeboss_output/summary.json"
