# Barge Boss

A top-down industrial river action game. Drive a heavy tug, push container barges
into matching berths and manage incoming cargo before a three-minute shift ends.
Stage 01 is a stylized **Calumet River industrial corridor** with steel yards,
freight rail, bulk storage, cranes, a lift bridge and crossing workboats. It is
an original gameplay layout, not a navigational map of the real river.

## Play

Choose **Learn to Drive** for six hands-on lessons without a timer, traffic or
hull damage: driving, braking, pushing, steering, docking and throwing a mooring
line. Lessons advance only after the action. **Reset Lesson** gives another try;
**Exit Practice** restores your suspended shift.

- **Drive:** press the tug and drag in the direction you want to travel. Pull
  farther for more thrust. The control ring stays at your original touch point;
  there is no need to chase the boat. Bring your finger into the ring to brake,
  or release to coast. Tapping the water does not set a route.
- **Push:** hold a cargo barge to line up behind it and push toward its matching
  berth. Keep your finger still for assistance; drag outside the ring to steer.
- **Stop:** release, then tap **Brake**. Pausing or backgrounding cancels input.
- **Tie up:** once the entire barge fits its matching **A / steel**, **B / freight**,
  or **C / bulk** zone and has slowed, its bank bollard glows. Lift your finger,
  then drag from the barge to aim. A curved trajectory guide and landing marker
  show the throw while the rope stays coiled. **Release to throw**: the lasso
  flies out, flashes on a bollard hit, or retracts after a miss. The guide turns
  green when aimed at the matching bollard.
- The line tightens for **1.2 seconds**, flashes and recoils as it snaps taut,
  then a crane, forklift and dock crew
  unload the barge for **3 seconds**. The barge stays secured while you work on
  another load. Scoring happens once unloading finishes, with a floating score
  and burst of stars. The crew and equipment then return to idle.
- Movement, turning, pushing and passing workboats run at **2× the original
  pace**, using two original-size physics steps per real tick. Hold gestures,
  the shift clock, contracts, mooring and unloading retain their real durations.
- The supplied concept guides the pixel-styled harbor: a red/cream tug with
  raised wheelhouse and tire fenders, rust barges carrying steel, containers or
  bulk, shaded concrete quays, yellow machinery, teal reflections and foam wakes.
  A sunset harbor title, matching HUD and animated results complete the style.
  Wakes are visual effects; they do not push cargo.
- **Haptics ON/OFF** controls gesture, contact, impact and delivery feedback.
  Android/iOS use native feedback when supported and allowed by system settings;
  desktop play has the same visual cues without mouse vibration.

Deliver **6 loads in 3 minutes**. Contracts expire after 68 seconds; new loads
arrive every 18 seconds when the entry area is clear. Watch crossing traffic and
avoid hard impacts. Pause is in the upper-right corner (Escape on desktop).

The prototype is offline and silent. Scores are per shift; persistence, audio,
additional stages and store releases are future work.

## One game on every platform

This follows **DDDumpling's architecture**, using the same pure Java game and
renderer on Android and iOS, plus a small desktop host. There is no browser port
or separate JavaScript/Swift gameplay implementation.

| Platform | Host | Gameplay and drawing source |
| --- | --- | --- |
| Android 7+ | Native Canvas / Choreographer | `core/`, compiled by javac/D8 |
| iOS 15+ | UIKit / Core Graphics / CADisplayLink | the same `core/`, translated by J2ObjC 3.1 |
| Windows, macOS, Linux | Swing / Java2D, Java 8+ runtime | the same `core/`, on the JVM |

Every host fits the same 600x960 interface within safe bounds. The complete
river is scaled uniformly above the control panel; its world coordinates are unchanged. Touch/mouse gestures,
drag handling, hold thresholds, haptic event timing, tutorial, physics,
scoring, traffic, HUD, menus and vector glyphs live in
shared code. Rendering differences at antialiased edges remain native-specific.
See [architecture and parity](docs/architecture.md).

## Build and run

Use JDK 21 for building. No Gradle, npm, game engine or third-party gameplay library
is required. Build outputs stay under `build/` and `ios/build/`.

### Desktop

```sh
./desktop.sh
# Build only; then launch the same JAR on any desktop platform:
./desktop.sh --build-only
java -jar build/bargeboss.jar
```

Windows users can run the build scripts through Git Bash. Running the JAR only
requires Java; a graphical desktop session is needed for play.

### Android

As in DDDumpling, the build uses `aapt2`, `javac`, `d8`, `zip`, `zipalign` when
available, and `apksigner`. Install Android platform 36 and build-tools 36.0.0,
and put the build-tools directory on `PATH`. Termux's native tools also work.

```sh
export ANDROID_HOME=/path/to/android-sdk
export PATH="$ANDROID_HOME/build-tools/36.0.0:$PATH"
./build-android.sh
adb install -r build/bargeboss-debug.apk
```

Alternatively set `ANDROID_JAR=/path/to/android.jar`; this can point to the
existing local DDDumpling SDK. Each checkout creates its own ignored debug key.
Preserve `build/debug.keystore` for updates; CI builds have ephemeral keys.
The app ID is `com.bargeboss.game` and does not replace DDDumpling.

### iOS

Requires a Mac with full Xcode, an iOS simulator runtime, JDK 21, Maven and
XcodeGen. The bootstrap script is adapted from DDDumpling and pins the same
checksum-verified J2ObjC 3.1 source archive.

```sh
brew install openjdk@21 maven xcodegen
./ios/scripts/bootstrap.sh
./ios/scripts/parity.sh
./ios/scripts/build.sh
# Install in a booted simulator:
xcrun simctl install booted ios/build/DerivedData/Build/Products/Debug-iphonesimulator/BargeBoss.app
xcrun simctl launch booted com.bargeboss.game.ios
```

To reuse DDDumpling's existing toolchain on the same Mac, export
`J2OBJC_HOME=/path/to/dddumpling/ios/vendor/j2objc-3.1/dist` instead of bootstrapping.
`ios/project.yml` is authoritative; XcodeGen creates the untracked project.
For a signed device build, set `DEVELOPMENT_TEAM` and run
`./ios/scripts/build.sh iphoneos`. Without a team, it produces an unsigned device
build for compilation validation only. Store provisioning/distribution is not configured.

## Verification

```sh
./check.sh       # Rules, physics, gestures, timing, full-shift bounded player
./preview.sh     # Real phase, title/menu and tutorial contact sheets in out/
./preview.sh --animations # 12 fps sequences; GIFs when ffmpeg is installed
./check-art.sh   # Optional Termux check on the actual Android Java runtime
```

GitHub Actions builds Android, desktop on three operating systems, and iOS on a
Mac. The iOS job runs the same deterministic gameplay/drawing replay on the JVM
and J2ObjC and rejects mismatches. An iPhone simulator test exercises native
drag controls, lasso attachment, crew unloading, the complete interactive tutorial, pause/resume and backgrounding. Workflow artifacts contain the APK, JAR and
simulator app. Native touch/lifecycle acceptance still needs actual device play;
headless checks are not a substitute for that.

Game art is implemented as shared procedural geometry, guided by the user-supplied
[concept reference](docs/reference/barge-boss-concept.png). Lettering is authored here.
No artwork, audio, game content, credentials or signing keys were imported from
DDDumpling. See [provenance](docs/provenance.md).
