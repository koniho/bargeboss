#!/bin/sh
# Build an Android App Bundle through the same tested code/resource pipeline as the APK.
if [ -z "${BASH_VERSION:-}" ]; then exec bash "$0" "$@"; fi
set -euo pipefail
cd "$(dirname "$0")"
UNSIGNED=0
if [ "${1:-}" = --unsigned ]; then UNSIGNED=1; shift; fi
[ "$#" -eq 0 ] || { echo 'usage: ./build-bundle.sh [--unsigned]' >&2; exit 1; }
if [ "$UNSIGNED" -eq 0 ]; then
    : "${BARGEBOSS_KEYSTORE:?Set the upload keystore, or use --unsigned for local validation}"
    : "${BARGEBOSS_KEY_ALIAS:?Set the upload key alias}"
    : "${BARGEBOSS_KEYSTORE_PASSWORD:?Set the upload keystore password}"
    [ -f "$BARGEBOSS_KEYSTORE" ] || { echo 'Upload keystore not found' >&2; exit 1; }
fi
BUNDLETOOL=build/tools/bundletool-all-1.18.3.jar
BUNDLETOOL_SHA=a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29
mkdir -p build/tools
if [ ! -f "$BUNDLETOOL" ]; then
    curl --fail --location --retry 3 --output "$BUNDLETOOL" \
        https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar
fi
printf '%s  %s\n' "$BUNDLETOOL_SHA" "$BUNDLETOOL" | shasum -a 256 --check --status
BARGEBOSS_PROFILE=0 ./build-android.sh --unsigned
# Convert the already-linked APK so its SDK/version/resources cannot drift from the bundle.
aapt2 convert --output-format proto -o build/bundle-res.apk build/android/base.apk
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
with ZipFile('build/bundle-res.apk') as source, ZipFile('build/base-module.zip', 'w', ZIP_DEFLATED) as module:
    for entry in source.infolist():
        name = entry.filename
        if name.startswith('META-INF/') or name.endswith('.dex'):
            continue
        if name == 'AndroidManifest.xml':
            name = 'manifest/AndroidManifest.xml'
        module.writestr(name, source.read(entry))
    for dex in sorted(Path('build/android/dex').glob('classes*.dex')):
        module.write(dex, 'dex/' + dex.name)
PY
java -jar "$BUNDLETOOL" build-bundle --modules=build/base-module.zip \
    --output=build/BargeBoss.aab --overwrite
java -jar "$BUNDLETOOL" validate --bundle=build/BargeBoss.aab
if [ "$UNSIGNED" -eq 0 ]; then
    export BARGEBOSS_KEYSTORE_PASSWORD
    export BARGEBOSS_KEY_PASSWORD=${BARGEBOSS_KEY_PASSWORD:-$BARGEBOSS_KEYSTORE_PASSWORD}
    jarsigner -keystore "$BARGEBOSS_KEYSTORE" -storepass:env BARGEBOSS_KEYSTORE_PASSWORD \
        -keypass:env BARGEBOSS_KEY_PASSWORD build/BargeBoss.aab "$BARGEBOSS_KEY_ALIAS"
    jarsigner -verify build/BargeBoss.aab
    echo 'Signed Play upload bundle: build/BargeBoss.aab'
else
    echo 'Unsigned validation bundle: build/BargeBoss.aab (sign before Play upload)'
fi
