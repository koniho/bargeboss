# Barge Boss

A landscape industrial river action game with a top-down harbor and shaded arcade boats. Drive a heavy tug, push container barges
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

First Watch asks you to deliver **6 loads in 3 minutes**. Contracts expire after 68 seconds; new loads
arrive every 18 seconds when the entry area is clear. Watch crossing traffic and
avoid hard impacts. Pause is in the upper-right corner (Escape on desktop).

Open **Challenges** from the title or results screen to build your captain career:

| Challenge | Target | Unlock |
| --- | --- | --- |
| First Watch | 6 loads in 3:00 | Available immediately |
| Quick Turn | 6 loads in 2:30 | Complete First Watch |
| Heavy Shift | 8 loads in 3:00 | Complete Quick Turn |

Survive until the shift timer ends and meet its quota to earn one star. Earn an
additional star for no expired loads, and another for finishing with at least
80% hull. Each challenge saves its best stars and best successful score. Replays
can improve records; duplicate stars do not count again. Practice earns no career
rewards and leaves your suspended shift intact.

Unique stars earn gold captain stripes and playful ranks: **Deck Duck** (0),
**Line Tamer** (1), **Wake Wrangler** (3), **Harbor Hero** (6), and **Captain Quack**
(9). The challenge board and results show your badge, promotions and next unlock.
Progress and selected challenge are saved locally on each device; there is no
cloud synchronization. Android/iOS use native preference storage; desktop uses
`~/.bargeboss/progress.dat`.

The prototype is offline and silent. Audio, more stages, fleet/upgrades and store
releases remain future work, tracked in the [concept backlog](https://github.com/koniho/bargeboss/issues).

## One game on every platform

This follows **DDDumpling's architecture**, using the same pure Java game and
renderer on Android and iOS, plus a small desktop host. There is no browser port
or separate JavaScript/Swift gameplay implementation.

| Platform | Host | Gameplay and drawing source |
| --- | --- | --- |
| Android 7+ | Native Canvas / Choreographer | `core/`, compiled by javac/D8 |
| iOS 15+ | UIKit / Core Graphics / CADisplayLink | the same `core/`, translated by J2ObjC 3.1 |
| Windows, macOS, Linux | Swing / Java2D, Java 8+ runtime | the same `core/`, on the JVM |

Every host fills its safe display bounds with the same **responsive landscape layout**,
using 960×600 as the reference size. Wide screens put the HUD and controls in side
panels; other aspect ratios retain top and bottom controls. Sprites keep their
proportions, and Android uses immersive fullscreen with swipe-accessible system bars.
The complete river is visible at once, with a compact channel and larger boat
footprints that make cargo, wakes and rope actions easier to read. The camera
rotates the shared world into the board; it does not scroll or crop the stage.
Touch/mouse gestures, hold thresholds, haptic timing, tutorial, physics, scoring,
traffic, HUD, menus, glyphs and sprite animation all live in shared Java.

The same PNG atlases are bundled on Android, iOS and desktop. Every rotating
boat and payload has **32 headings**, spaced 11.25° apart, interleaved from two
4×4 sheets. Shared code calibrates each frame’s X/Y size and footprint anchor.
It holds each drawing crisp, then blends across a brief 2.25° transition. Native hosts sum their weighted premultiplied colors in one layer,
so an opaque boat stays opaque between headings. This smooths transitions between
painted views; it is not skeletal animation or a live 3D model. Native
antialiasing and bitmap sampling can still differ slightly.
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
./preview.sh --animations # Phase sequences plus rotation; GIFs when ffmpeg is installed
./preview.sh --rotation   # 240 frames at 30 fps; fixed crosshairs for boats and payloads
./check-art.sh   # Optional Termux check on the actual Android Java runtime
```

GitHub Actions builds Android, desktop on three operating systems, and iOS on a
Mac. The iOS job runs the same deterministic gameplay/drawing replay on the JVM
and J2ObjC and rejects mismatches. An iPhone simulator test exercises native
drag controls, lasso attachment, crew unloading, the complete interactive tutorial, pause/resume and backgrounding. Workflow artifacts contain the APK, JAR and
simulator app. Native touch/lifecycle acceptance still needs actual device play;
headless checks are not a substitute for that.

The visual update combines generated directional sprite atlases with shared
procedural scenery, lettering and effects. The built-in `image_gen` tool created
the boat and cargo artwork from the user-supplied
[concept reference](docs/reference/barge-boss-concept.png). Production PNGs live
in [assets/sprites](assets/sprites/), and their generation instructions are retained
in [docs/art-prompts](docs/art-prompts/). Title, gameplay, tutorial and results use
the same art and phase-driven animation.

No artwork, audio, game content, credentials or signing keys were imported from
DDDumpling. See [provenance](docs/provenance.md).

After each completed development round, run the relevant checks, build the
Android APK, install it on the connected development phone using the existing
debug signing key, and launch it. This is the user's requested development loop;
the authorized Shizuku/rish path is available when adb is unavailable.
