#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
device="${SIMULATOR_ID:-$(xcrun simctl list devices available -j | python3 -c '
import json,sys
devices=[d for values in json.load(sys.stdin)["devices"].values() for d in values if "iPhone" in d["name"]]
if not devices: raise SystemExit("Install an iPhone simulator runtime in Xcode")
devices.sort(key=lambda d: d["state"]!="Booted")
print(devices[0]["udid"])
')}"
state=$(xcrun simctl list devices available -j | python3 -c 'import json,sys; print(next(d["state"] for values in json.load(sys.stdin)["devices"].values() for d in values if d["udid"]==sys.argv[1]))' "$device")
if [ "$state" != Booted ]; then xcrun simctl boot "$device"; fi
xcrun simctl bootstatus "$device" -b
xcodebuild -project "$IOS_ROOT/BargeBoss.xcodeproj" -scheme BargeBoss -configuration Debug \
    -destination "platform=iOS Simulator,id=$device" -derivedDataPath "$IOS_ROOT/build/DerivedData" \
    -resultBundlePath "$IOS_ROOT/build/Smoke-$(date +%s).xcresult" "J2OBJC_HOME=$J2OBJC_HOME" \
    CODE_SIGNING_ALLOWED=NO test -only-testing:BargeBossUITests
xcrun simctl launch --terminate-running-process "$device" com.bargeboss.game.ios
sleep 3
xcrun simctl io "$device" screenshot "$IOS_ROOT/build/native-title.png"
