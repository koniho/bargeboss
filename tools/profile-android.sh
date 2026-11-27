#!/bin/sh
# Install an opt-in BARGEBOSS_PROFILE=1 build first. Requires authorized Shizuku/rish.
set -eu
cd "$(dirname "$0")/.."
scene=${1:-barge-push}
mode=${2:-live}
case "$scene" in montrose-pavilion-study|montrose-concession-study|islands-cottage-study|islands-channel-study|islands-lift-study|training-spotlight|montrose-arrival|montrose-swimmers|montrose-swimmers-lit|montrose-lift-carry|montrose-lift-water|montrose-lift-lowering|montrose-tow-one|montrose-tow-two|montrose-chas|montrose-storm-return|montrose-racer|montrose-police|montrose-police-radio|montrose-arrival|montrose-moorings-dusk|buoy-complete|montrose-dispatch|montrose-haulout|tutorial-fireworks|tutorial-firefight|barge-abilities|calumet-offload|calumet-bridge|calumet-skyway|indiana-bridges|calumet-inlet|calumet-basin|indiana-harbor|indiana-hick|chicago-start|chicago-downtown|chicago-wolf|shared-slip-wide|tow-tight-circle|basin-entry|canal-entry|canal-middle|barge-arrival|barge-push|harbor-close|barge-tow|barge-bridge|montrose-inlet|montrose-docks|montrose-overview) ;; *) echo 'Unknown scene' >&2; exit 2;; esac
case "$mode" in freeze|live|replay) ;; *) echo 'Mode must be freeze, live or replay' >&2; exit 2;; esac
bargeboss_reference_water=${BARGEBOSS_PROFILE_REFERENCE_WATER:-false}
case "$bargeboss_reference_water" in true|false) ;; *) exit 2;; esac
bargeboss_exclude=${BARGEBOSS_PROFILE_EXCLUDE:-0}
case "$bargeboss_exclude" in *[!0-9]*|'') exit 2;; esac
bargeboss_frames=${BARGEBOSS_PROFILE_FRAMES:-360}
bargeboss_warmup=${BARGEBOSS_PROFILE_WARMUP:-120}
case "$bargeboss_frames:$bargeboss_warmup" in *[!0-9:]*) echo 'Frame counts must be integers' >&2; exit 2;; esac
review_zoom=${BARGEBOSS_PROFILE_ZOOM:-0}
case "$review_zoom" in *[!0-9.]*|'') echo 'Invalid profile zoom' >&2; exit 2;; esac
system_trace=${BARGEBOSS_PROFILE_SYSTEM_TRACE:-false}
case "$system_trace" in true|false) ;; *) exit 2;; esac
trace=${BARGEBOSS_PROFILE_TRACE:-false}
case "$trace" in true|false) ;; *) exit 2;; esac
compound_paths=${BARGEBOSS_PROFILE_COMPOUND_PATHS:-false}
frame_textures=${BARGEBOSS_PROFILE_FRAME_TEXTURES:-true}
case "$compound_paths:$frame_textures" in true:true|true:false|false:true|false:false) ;; *) echo 'Strategy switches must be true or false' >&2; exit 2;; esac
water_grouping=${BARGEBOSS_PROFILE_WATER_GROUPING:-false}
case "$water_grouping" in true|false) ;; *) exit 2;; esac
async_layers=${BARGEBOSS_PROFILE_ASYNC_LAYERS:-true}
case "$async_layers" in true|false) ;; *) exit 2;; esac
rotation_prefetch=${BARGEBOSS_PROFILE_ROTATION_PREFETCH:-true}
case "$rotation_prefetch" in true|false) ;; *) exit 2;; esac
frame_cache_mib=${BARGEBOSS_PROFILE_FRAME_CACHE_MIB:-160}
case "$frame_cache_mib" in ''|*[!0-9]*) exit 2;; esac
if [ "$frame_cache_mib" -lt 1 ] || [ "$frame_cache_mib" -gt 160 ]; then exit 2; fi
export RISH_APPLICATION_ID=com.termux
# Respect an awake scope already owned by the surrounding development round.
if [ ! -f out/phone-screen-timeout-before.txt ]; then
    ./tools/phone-awake.sh on
    trap './tools/phone-awake.sh off' 0
    trap 'exit 130' 2
    trap 'exit 143' 15
else
    ./tools/phone-awake.sh on
fi
remote=/storage/emulated/0/Android/data/com.bargeboss.game/files/profiles
output=out/profiles/$(date +%Y%m%d-%H%M%S)-$scene-$mode
mkdir -p "$output"
printf 'compound_paths=%s\nframe_textures=%s\n' "$compound_paths" "$frame_textures" > "$output/strategies.txt"
printf 'camera_zoom_override=%s\n' "$review_zoom" >> "$output/strategies.txt"
printf 'water_grouping=%s\n' "$water_grouping" >> "$output/strategies.txt"
printf 'async_layers=%s\nrotation_prefetch=%s\nframe_cache_mib=%s\n' "$async_layers" "$rotation_prefetch" "$frame_cache_mib" >> "$output/strategies.txt"
printf 'reference_water=%s\n' "$bargeboss_reference_water" >> "$output/strategies.txt"
printf 'method_trace=%s\nsystem_trace=%s\n' "$trace" "$system_trace" >> "$output/strategies.txt"
rish -c "rm -f '$remote/$scene-$mode.csv'"
rish -c 'input keyevent KEYCODE_WAKEUP'
rish -c 'dumpsys gfxinfo com.bargeboss.game reset' > "$output/reset.txt" 2>&1
rish -c "am start -S -W -n com.bargeboss.game/com.bargeboss.game.MainActivity --es bb_scene '$scene' --es bb_mode '$mode' --ef bb_camera_zoom $review_zoom --ei bb_warmup $bargeboss_warmup --ei bb_frames $bargeboss_frames --ez bb_reference_water $bargeboss_reference_water --ei bb_exclude $bargeboss_exclude --ez bb_trace $trace --ez bb_system_trace $system_trace --ez bb_compound_paths $compound_paths --ez bb_frame_textures $frame_textures --ez bb_async_layers $async_layers --ez bb_water_grouping $water_grouping --ez bb_rotation_prefetch $rotation_prefetch --ei bb_frame_cache_mib $frame_cache_mib" > "$output/launch.txt" 2>&1
started=$(date +%s)
ready=
while [ "$(($(date +%s)-started))" -lt 180 ]; do
    ready=$(rish -c "if test -s '$remote/$scene-$mode.csv'; then printf ready; fi" 2>&1)
    if [ "$ready" = ready ]; then break; fi
    sleep 1
done
if [ "$ready" != ready ]; then echo "Timed out waiting for profile export; keep the phone unlocked" >&2; exit 1; fi
# rish can interleave large stdout/stderr transfers. Stage files and copy them
# through shared storage instead; this also preserves binary method traces.
staging=/storage/emulated/0/Download/BargeBoss-profiles
rish -c "mkdir -p '$staging'
cp '$remote/$scene-$mode.csv' '$staging/frames.csv'
dumpsys gfxinfo com.bargeboss.game framestats > '$staging/gfxinfo.txt'
dumpsys meminfo com.bargeboss.game > '$staging/meminfo.txt'
logcat -d --pid=\$(pidof com.bargeboss.game) -s BargeBossProfile:I AndroidRuntime:E > '$staging/logcat.txt'
screencap -p '$staging/scene.png'"
# On the phone (Termux) staging is local; from a USB/ADB host pull it instead.
fetch() { if [ -e "$1" ]; then cp "$1" "$2"; else adb pull "$1" "$2" >/dev/null; fi; }
for name in frames.csv gfxinfo.txt meminfo.txt logcat.txt scene.png; do fetch "$staging/$name" "$output/$name"; done
if [ "$trace" = true ]; then
    rish -c "cp '$remote/$scene-$mode.trace' '$staging/methods.trace'"
    fetch "$staging/methods.trace" "$output/methods.trace"
    python3 tools/summarize-method-trace.py "$output/methods.trace" > "$output/method-hotspots.txt"
fi
git rev-parse HEAD > "$output/revision.txt"
git diff --stat >> "$output/revision.txt"
printf 'Profile saved to %s\n' "$output"
python3 - "$output/frames.csv" "$bargeboss_frames" <<'PYCOUNT'
import csv,sys
rows=list(csv.DictReader(open(sys.argv[1])))
measured=sum(row.get('phase')=='sample' for row in rows)
expected=max(30,min(3600,int(sys.argv[2])))
if measured!=expected:raise SystemExit('Incomplete capture: %d of %d frames' % (measured,expected))
PYCOUNT
python3 tools/summarize-profile.py "$output/frames.csv"
python3 tools/summarize-gfx-profile.py "$output" > "$output/gfx-summary.json"
