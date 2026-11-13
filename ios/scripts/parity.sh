#!/bin/bash
# Run the SAME replay and drawing trace as Java and translated native Objective-C.
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$REPO_ROOT"
rm -rf ios/build/parity/generated ios/build/parity/classes
mkdir -p ios/build/parity/generated ios/build/parity/classes
find core -name '*.java' | sort > ios/build/parity/sources.txt
echo tests/com/bargeboss/game/Parity.java >> ios/build/parity/sources.txt
echo tests/com/bargeboss/game/ReplayInput.java >> ios/build/parity/sources.txt
javac -d ios/build/parity/classes @ios/build/parity/sources.txt
java -cp ios/build/parity/classes com.bargeboss.game.Parity > ios/build/parity/java.txt
"$J2OBJC_HOME/j2objc" --prefixes ios/prefixes.properties -sourcepath core:tests -d ios/build/parity/generated @ios/build/parity/sources.txt
cat > ios/build/parity/main.m <<'OBJC'
#import <Foundation/Foundation.h>
#import "com/bargeboss/game/Parity.h"
int main(void) {
    @autoreleasepool { printf("%s\n",[[BBParity signature] UTF8String]); }
    return 0;
}
OBJC
find ios/build/parity/generated -name '*.m' | sort > ios/build/parity/objc.txt
mapfile_compat=()
while IFS= read -r source; do mapfile_compat+=("$source"); done < ios/build/parity/objc.txt
clang -fno-objc-arc -ffp-contract=off -I"$J2OBJC_HOME/include" -Iios/build/parity/generated \
    "${mapfile_compat[@]}" ios/build/parity/main.m -L"$J2OBJC_HOME/lib/macosx" \
    -ObjC -ljre_emul -liconv -lz -framework Foundation -framework Security -o ios/build/parity/native
ios/build/parity/native > ios/build/parity/native.txt
diff -u ios/build/parity/java.txt ios/build/parity/native.txt
echo "JVM / J2ObjC gameplay and drawing parity passed: $(cat ios/build/parity/java.txt)"
