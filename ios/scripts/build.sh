#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
sdk="${1:-iphonesimulator}"
case "$sdk" in iphonesimulator|iphoneos) ;; *) echo 'Use iphonesimulator or iphoneos' >&2; exit 2;; esac
bash "$IOS_ROOT/scripts/translate.sh"
xcodegen generate --spec "$IOS_ROOT/project.yml"
args=(-project "$IOS_ROOT/BargeBoss.xcodeproj" -scheme BargeBoss -configuration "${CONFIGURATION:-Debug}" -sdk "$sdk"
      -derivedDataPath "$IOS_ROOT/build/DerivedData" "J2OBJC_HOME=$J2OBJC_HOME" "BARGEBOSS_IOS_PROFILE=${BARGEBOSS_IOS_PROFILE:-0}")
if [ "$sdk" = iphonesimulator ]; then
    args+=(-destination 'generic/platform=iOS Simulator' "ARCHS=$(uname -m)" CODE_SIGNING_ALLOWED=NO)
elif [ -n "${DEVELOPMENT_TEAM:-}" ]; then
    args+=(-destination 'generic/platform=iOS' "DEVELOPMENT_TEAM=$DEVELOPMENT_TEAM")
else
    args+=(-destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO)
fi
xcodebuild "${args[@]}" build
