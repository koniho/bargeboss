#!/bin/sh
# DDDumpling-style native pipeline: aapt2 -> javac -> d8 -> apksigner.
set -eu
cd "$(dirname "$0")"
unsigned=0
if [ "${1:-}" = --unsigned ]; then unsigned=1; shift; fi
[ "$#" -eq 0 ] || { echo 'Usage: ./build-android.sh [--unsigned]' >&2; exit 1; }
SDK_JAR=${ANDROID_JAR:-${ANDROID_HOME:-sdk}/platforms/android-36/android.jar}
if [ ! -f "$SDK_JAR" ]; then echo 'Set ANDROID_JAR to android.jar, or ANDROID_HOME to an Android SDK with API 36.' >&2; exit 1; fi
for tool in javac aapt2 d8 apksigner zip keytool; do command -v "$tool" >/dev/null || { echo "Missing $tool" >&2; exit 1; }; done
./check.sh
rm -rf build/android/classes build/android/dex
mkdir -p build/android/classes build/android/gen build/android/dex
aapt2 compile --dir res -o build/android/resources.zip
# Developer hooks are opt-in. The normal APK has neither debuggable flags nor scenario classes.
bargeboss_manifest=AndroidManifest.xml
if [ "${BARGEBOSS_PROFILE:-0}" = 1 ]; then
    sed 's/<application /<application android:debuggable="true" /' AndroidManifest.xml > build/android/profile-manifest.xml
    bargeboss_manifest=build/android/profile-manifest.xml
fi
bargeboss_gpu_assets=
if [ "${BARGEBOSS_VULKAN:-1}" = 1 ]; then
    ./tools/build-vulkan.sh
    bargeboss_gpu_assets='-A build/android/vulkan/assets'
fi
aapt2 link $bargeboss_gpu_assets -o build/android/base.apk -I "$SDK_JAR" --manifest "$bargeboss_manifest" -A assets --java build/android/gen --min-sdk-version 24 --target-sdk-version 36 build/android/resources.zip
find core android build/android/gen -name '*.java' | sort > build/android/sources.txt
if [ "${BARGEBOSS_PROFILE:-0}" = 1 ]; then
    find developer/shared developer/android -name '*.java' | sort >> build/android/sources.txt
    echo tests/com/bargeboss/game/ReplayInput.java >> build/android/sources.txt
fi
javac -Xlint:-options --release 8 -encoding UTF-8 -cp "$SDK_JAR" -d build/android/classes @build/android/sources.txt
# Host-only transport regression; never packaged into the Android game.
mkdir -p build/android/encoding-check
javac -Xlint:-options --release 8 -cp "build/android/classes:$SDK_JAR" -d build/android/encoding-check tools/VulkanEncodingTest.java tools/VulkanClipEncodingTest.java
java -cp "build/android/encoding-check:build/android/classes:$SDK_JAR" com.bargeboss.game.VulkanEncodingTest
java -cp "build/android/encoding-check:build/android/classes:$SDK_JAR" com.bargeboss.game.VulkanClipEncodingTest
find build/android/classes -name '*.class' | sort > build/android/classes.txt
d8 --lib "$SDK_JAR" --min-api 24 --output build/android/dex @build/android/classes.txt
(cd build/android/dex && zip -q -j ../base.apk classes*.dex)
if [ "${BARGEBOSS_VULKAN:-1}" = 1 ]; then
    (cd build/android/vulkan && zip -q ../base.apk lib/arm64-v8a/libbargeboss_vulkan.so)
fi
if command -v zipalign >/dev/null; then
    zipalign -f -p 4 build/android/base.apk build/android/aligned.apk
else
    cp build/android/base.apk build/android/aligned.apk
fi
if [ "$unsigned" = 1 ]; then
    echo 'Unsigned Android build: build/android/aligned.apk'
    exit 0
fi
if [ -n "${BARGEBOSS_KEYSTORE:-}" ]; then
    : "${BARGEBOSS_KEY_ALIAS:?Set the release key alias}"
    : "${BARGEBOSS_KEYSTORE_PASSWORD:?Set the release keystore password}"
    export BARGEBOSS_KEYSTORE_PASSWORD
    export BARGEBOSS_KEY_PASSWORD=${BARGEBOSS_KEY_PASSWORD:-$BARGEBOSS_KEYSTORE_PASSWORD}
    apksigner sign --ks "$BARGEBOSS_KEYSTORE" --ks-key-alias "$BARGEBOSS_KEY_ALIAS" \
        --ks-pass env:BARGEBOSS_KEYSTORE_PASSWORD --key-pass env:BARGEBOSS_KEY_PASSWORD \
        --out build/bargeboss-release.apk build/android/aligned.apk
    apksigner verify build/bargeboss-release.apk
    echo 'Signed Android build: build/bargeboss-release.apk'
else
    if [ ! -f build/debug.keystore ]; then
        keytool -genkeypair -keystore build/debug.keystore -storepass android -keypass android -alias bargeboss -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Barge Boss Debug' >/dev/null 2>&1
    fi
    apksigner sign --ks build/debug.keystore --ks-key-alias bargeboss --ks-pass pass:android --out build/bargeboss-debug.apk build/android/aligned.apk
    apksigner verify build/bargeboss-debug.apk
    echo 'Playable Android build: build/bargeboss-debug.apk'
fi
