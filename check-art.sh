#!/bin/sh
# Optional on-device parity check for the Termux development environment.
set -eu
cd "$(dirname "$0")"
ART=/apex/com.android.art/bin/dalvikvm
[ -x "$ART" ] || { echo 'This check needs Android ART (run in Termux).' >&2; exit 1; }
command -v d8 >/dev/null || { echo 'Install d8 first.' >&2; exit 1; }
./check.sh
mkdir -p build/art/data/dalvik-cache
find build/check -name '*.class' | sort > build/art/classes.txt
rm -f build/art/classes.dex
d8 --min-api 24 --output build/art @build/art/classes.txt
# Modern ART requires dynamically loaded dex files to be read-only.
chmod 444 build/art/classes.dex
if ! env -u LD_LIBRARY_PATH -u LD_PRELOAD ANDROID_DATA="$PWD/build/art/data" "$ART" \
    -Xuse-stderr-logger -Xusejit:true -Xnoimage-dex2oat \
    -cp "$PWD/build/art/classes.dex" com.bargeboss.game.CoreTest > build/art/run.log 2>&1; then
    tail -40 build/art/run.log
    exit 1
fi
tail -2 build/art/run.log
