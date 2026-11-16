#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
[ -x "$J2OBJC_HOME/j2objc" ] || { echo 'Run ios/scripts/bootstrap.sh or set J2OBJC_HOME to DDDumpling’s existing J2ObjC 3.1 dist.' >&2; exit 1; }
rm -rf "$IOS_ROOT/build/generated"
mkdir -p "$IOS_ROOT/build/generated"
find "$REPO_ROOT/core" -name '*.java' | sort > "$IOS_ROOT/build/sources.txt"
if [ "${BARGEBOSS_IOS_PROFILE:-0}" = 1 ]; then
    printf '%s\n' "$REPO_ROOT/developer/shared/com/bargeboss/game/ProfileScenario.java" "$REPO_ROOT/tests/com/bargeboss/game/ReplayInput.java" >> "$IOS_ROOT/build/sources.txt"
fi
"$J2OBJC_HOME/j2objc" --prefixes "$IOS_ROOT/prefixes.properties" -sourcepath "$REPO_ROOT/core:$REPO_ROOT/tests:$REPO_ROOT/developer/shared" -d "$IOS_ROOT/build/generated" @"$IOS_ROOT/build/sources.txt"
# Ship the translator runtime's attribution with the app.
python3 - "$J2OBJC_HOME" "$IOS_ROOT/build/ThirdPartyNotices.txt" <<'PY'
import pathlib, sys
root = pathlib.Path(sys.argv[1]).parent
files = sorted(set(root.glob('LICENSE*')) | set(root.glob('NOTICE*')) | set((root / 'dist').glob('LICENSE*')))
if not files:
    raise SystemExit('J2ObjC license missing from distribution')
pathlib.Path(sys.argv[2]).write_text('\n\n'.join(p.name + '\n' + p.read_text() for p in files if p.is_file()))
PY
