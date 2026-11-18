# Barge Boss

A landscape industrial river action game with an isometric harbor and directional boat sprites. Drive a heavy tug, push container barges
into matching berths, solve delivery order, and move a convoy through a timed lock.
Levels finish when their goals are complete; there is no overall countdown.
The setting is a stylized **Calumet River industrial corridor** with steel yards,
freight rail, bulk storage, cranes, a lift bridge and crossing workboats. It is
an original gameplay layout, not a navigational map of the real river.

The original project code, tooling and documentation are open source under the
[MIT License](LICENSE). See [provenance](docs/provenance.md) for generated artwork,
supplied reference images and third-party notices.

The shared [isometric conversion](docs/isometric-migration.md) uses the approved
[32-frame tug](docs/art-studies/isometric-tug-v5-review.md), registered
[barge and coal layers](docs/art-studies/isometric-barge-runtime-review.md), and
industrial scenery with animated light and smoke. Water and wakes respect canal
banks and piers. Steel/container cargo and unloading machinery still use
transitional art. Reusable art workflows are maintained in
[watercraft](skills/bargeboss-watercraft-sprites/SKILL.md),
[static scenery](skills/bargeboss-scenery-sprites/SKILL.md), and
[animated shore equipment](skills/bargeboss-shore-animation/SKILL.md) skills.

## Play

Choose **Learn to Drive** for fourteen hands-on lessons without a timer, traffic or
hull damage: circling two lit buoys, coasting, a scrolling rectilinear industrial canal, drag-to-push,
assisted pushing, docking, throwing a mooring
line, towing to a berth, double towing under a bridge, reconfiguring a train,
narrow-channel towing and uncoupling. Lessons advance only after the action. **Reset Lesson** gives another try;
**Exit Practice** restores your suspended level. Circle each gold beacon once in either direction, following the soft water ring;
its percentage shows your lap progress. Stay near the ring—cutting across the buoy
or leaving its area restarts that unfinished lap. Then steer
upstream through seven gates along a narrow industrial channel spanning several
screens. Rectilinear steel quay walls prevent shortcuts; the camera follows
the tug while keeping the original drag anchor fixed on the screen. The canal
opens into the same practice harbor, with all three teaching barges already waiting.
Sixteen factory modules, varied building heights, active exhausts and warm
lighting populate packed industrial blocks behind the quays. Aligned road tiles
separate the factory rows; the working shoreline apron stays clear.
Towing practice continues by bringing a barge to the marked berth, connecting a
second barge directly to the tug, and pulling both underneath the opening bridge.
Then release one tug line and reconnect that same barge to the other barge to form
a train. Pull both through the narrow seawalled channel; the lesson waits until
the last hull clears the exit. All of this stays in the same continuous scene.

The harbor uses a dark industrial palette with animated warm factory lights,
while boats retain bright, saturated paintwork and registered vessel lamps.
Only mouths between distinct regions have paired red and green navigation buoys,
fixed to the returning direction (red on the right). Screenshot-based steel lantern
sprites rock and bob with the waves; curved waterline masking and soft foam show
their submerged bases. Tutorial entrances use fixed lights on concrete plinths
beside the canal, while the two yellow circling buoys stay afloat. Every watercraft carries steady
red port, green starboard and white stern lights; powered craft also have a white
masthead light. Their visibility changes naturally as the vessels turn.
The working camera is closer by default, with tighter docking and modestly closer
transit framing. Automatic fitting keeps the current tow and berth targets visible.
Shared visibility culling and redraw scheduling reduce work in wide views;
see the [night harbor rendering notes](docs/performance/night-harbor.md).

- **Joystick:** move the bottom-corner thumb stick toward the direction you want
  to travel on screen. More travel gives more thrust; release to coast. It responds
  immediately. **Settings → Joystick position → Left / Right / Off** is available
  on the title screen and during play, and is saved across restarts.
- **Drive:** press the tug and drag in the direction you want to travel. Pull
  farther for more thrust. Keep your finger down for 0.2 seconds to engage; you
  can start dragging immediately. The control ring stays at your original touch point;
  there is no need to chase the boat. Center the control or release to coast.
  Drag behind the tug to apply astern thrust without turning it around. Tapping the water does not set a route.
- **Push:** press a cargo barge, then drag to steer the push. Keep your finger
  down; the tug lines up and pushes physically. Return to the ring and hold still
  for assistance toward the matching berth. The tutorial animates this gesture and requires pushing the load into a marked
  practice zone before moving on.
- **Pull:** bring the tug near an unmoored barge, drag **from the barge to the
  tug**, then release to throw a towline. The tug glows as the target; the curved
  guide turns green over it. Drive the tug to take up slack and pull the barge.
  The line applies force at the ends of the hulls, so cargo trails, swings and
  retains momentum. **Tap the connected barge** to release it before pushing again;
  a successful shore-bollard catch also releases the tow. Link multiple barges
  directly to the tug or to another barge within 170 river units; heavier trains
  reduce the resulting speed. Pick up feeder loads as soon as they are dropped off;
  secured loaded barges and loads still aboard an incoming feeder cannot be towed.
  Unloaded barges remain physical hulls: drag an empty to the tug or another
  barge to tow it clear. Its shore line releases only after a successful tow catch.
  Blocked or overstretched lines release. Practice includes connecting, pulling
  the load a real distance, and explicitly releasing it.
- **Slow down:** drag against your motion to apply counter-thrust, then release
  to coast. There is no brake button. Pausing or backgrounding cancels input.
- **Tie up:** once the barge center enters the zone matching its displayed berth
  letter, the hull and its bank bollard glow with a **READY TO TIE**
  badge. Moving barges are eligible too. Lift your finger,
  then drag from the barge to aim. A curved trajectory guide and landing marker
  show the throw while the rope stays coiled. **Release to throw**: the lasso
  flies out, flashes on a bollard hit, or retracts after a miss. The guide turns
  green when aimed at the matching bollard.
- A successful catch sends an expanding flash across the barge, snaps the line
  taut immediately, and starts drawing it parallel to shore over **1.2 seconds**.
  Then a crane, forklift and dock crew
  unload the barge for **3 seconds**. The barge stays secured while you work on
  another load, and stays in the water after delivery. Pull empties away to free
  occupied berths. Scoring happens once per barge when unloading finishes, with a floating score
  and burst of stars. The crew and equipment then return to idle.
- Movement, turning, pushing and passing workboats run at **2× the original
  pace**, using two original-size physics steps per real tick. Hold gestures,
  lock windows, mooring and unloading retain their real durations.
- The supplied concept guides the pixel-styled harbor: a red/cream tug with
  raised wheelhouse and tire fenders, charcoal-blue steel barges with orange coamings carrying steel, containers or
  bulk, shaded concrete quays, yellow machinery, teal reflections and foam wakes.
  The title restores the panoramic sunset, sculpted clouds, distant skyline and
  raised bridge in the current cartoon style. Skyline, bridge and curved water
  waves scroll at different speeds behind a larger gameplay tug and barge, with
  bobbing, engine smoke and warm wave reflections across open foreground water.
  Separate headland, inlet, peninsula and islet sprites break up the title shore.
  Both hulls leave widening wakes with receding crests, side foam and bow spray;
  the tug churns prop wash while compressed push knees and traveling gold arrows
  show it pushing the barge. These presentation effects do not advance gameplay.
  Title artwork prompts are in `docs/art-prompts/title-backdrop-v2.txt` and
  `docs/art-prompts/title-harbor-v2.txt`; shoreline sprite provenance is in
  `docs/art-prompts/title-land-v1.txt`.
  The play view uses a fixed 2.5D ground projection with independent painted
  warehouses, stockpiles, container stacks, quays and solid finger piers. Foreground
  scenery fades when it overlaps a boat, active cargo or lasso path. A faint outline
  keeps solid pier footprints visible while their artwork is translucent.
- **Arcade scenery:** one fixed overhead perspective, exaggerated industrial
  proportions, bold outlines and broad color blocks replace the dense surface
  texture. Matching upper-left lighting, cast shadows and warm dock lamps give
  the quieter water and concrete clear depth without hiding the action.
- **Water motion:** floating hulls and feeder buoys rise and fall with local waves.
  Curved waves travel, rise and fade across the water, with shimmering dock-light
  reflections. Title and gameplay share this wave renderer instead of water strips.
  Passing wakes increase that motion nearby. Underway vessels emit the main foam
  burst at the bottom of each fall, with a continuous trail between bursts. Emitted
  waves stay behind in the river and spread outward for five seconds, even after
  the boat stops or turns. Wake
  energy affects vertical motion, without secretly changing horizontal controls.
- **Barge momentum:** loaded barges are heavier than the tug, glide along their keel,
  resist broadside motion, and carry yaw inertia. Holding push assist deliberately
  settles a load once it fits the berth; released cargo keeps gliding. A lasso-ready
  cue requires the center to enter its assigned berth, with no speed or heading
  restriction. A hull against the shore remains eligible even when its ends
  extend outside the berth. Full-hull fit is required only after winching, before
  unloading.
  During flight a moving hull can drift within 105 world units of the berth center
  on each axis; beyond that the throw misses and retracts.
- **Automatic berthing:** a caught line snaps taut immediately and the dock
  winch draws the barge alongside and turns it parallel to shore over 1.2 seconds.
  The crew's three-second unloading cycle begins only after alignment finishes.
- **Engine smoke:** idle boats give off a light trail; acceleration, reverse thrust
  and pushing cargo increase the number, density and size of smokestack puffs.
  Feeder boats produce more exhaust with a load than on their empty return trip.
- **Haptics ON/OFF** controls gesture, contact, impact and delivery feedback.
  Android/iOS use native feedback when supported and allowed by system settings;
  desktop play has the same visual cues without mouse vibration.

Open **Challenges** from the title or results screen to build your captain career.
**Montrose Harbor** (challenge page two, available immediately) is a parkland marina
mission: drag from the 80-foot yacht to the tug and release to connect, tow through
the inlet, then align and settle the yacht in its highlighted wooden slip. New
wood dock, grass, woodland and limestone sprites replace the industrial scenery
for this level. Its layout is inspired by Montrose, with enlarged gameplay
clearances. The yacht retains momentum and slows the tug under load. See the
[asset and mission notes](docs/art-studies/montrose/README.md).

Each level ends immediately when its goal is complete. Cargo does not expire,
and feeders dispatch only the barges required by that level.

| Level | Goal | Unlock |
| --- | --- | --- |
| Two Banks | Deliver two barges, one to berth A on the left bank and one to B on the right | Available immediately |
| Shared Slip | Unload three loads through one narrow slip; pull each empty clear before swapping in another | Complete Two Banks |
| Lock Run | Get both barges into the lock, wait for its water cycle, then clear the exit with both | Complete Shared Slip |
| Deep First | Work three berths in a dead-end channel; outer barges physically block the inner ones | Complete Lock Run |

Shared Slip and Deep First use solid quay geometry and single-file entrances.
There is no enforced A-B-C sequence: the hulls themselves limit access. After
unloading, drag the empty to the tug and tow it out through the channel mouth.
Shared Slip marks an optional empty holding area in the open water beyond its
quay. The last unload completes each stage without requiring a final cleanup.

Lock Run has timed obstacles within an otherwise untimed level. Preparing the
convoy has no deadline. On approach with both loads available, the entry gate
starts a **45-second window**, including its opening motion. Bring the tug and
both complete barge hulls inside. The entry closes over **5 seconds**, the lock
fills for **8 seconds**, and the exit opens over **5 seconds**. Once the exit is
fully open, clear both complete barge hulls within **35 seconds**. Missing either
window ends the attempt with a reason and a replay option. Steer against the
opposing entrance/exit flow and shifting filling currents; drifting water streaks
show their direction. A persistent water-level bar and percentage, wall gauges
and a rising wet line make the level change visible; vessel navigation
still uses the shared fixed water plane. See the [goal rules and authoring notes](docs/gameplay/goal-levels.md).

The camera defaults to a **2.15× working view**, rising to **2.5× near a berth**
when the tug, load and bollard fit. Lock transit uses a wider **1.2× view**. The
camera holds still during lasso aiming and flight, eases out after delivery, and
keeps the overview for three seconds before returning to the close working view.
Inland bollards sit 50 world units behind the bank. Barges retain keel momentum;
forward, sideways and turning resistance help released loads settle, with gentle
berth fender damping for aiming.

Two NPC pushers bring the level's loads in from edge slots, call their target
berth or **TO THE LOCK!**, and back out. A blocked captain calls **HURRY UP!** at
four-second intervals. Queued loads wait outside the tug's pickup approach, and
slot fenders hold handed-off barges until pickup. In both three-load stages, the
remaining third load waits for a free feeder lane. The calls are visual speech bubbles; there is
no voice audio yet. The opening bridge remains part of the continuous towing
tutorial. Pause is in the upper-right corner (Escape on desktop).
Choose **Title Screen** from the pause menu to leave a voyage or tutorial.
Career progress and settings are kept; starting again begins a fresh voyage.

Completing the goal earns one star. Earn an additional star for no lost loads,
and another for finishing with at least 80% hull. Each level saves its best stars
and best successful score. Replays can improve records; duplicate stars do not
count again. Practice earns no career rewards and preserves the suspended level.

Unique stars earn gold captain stripes and playful ranks: **Deck Duck** (0),
**Line Tamer** (1), **Wake Wrangler** (3), **Harbor Hero** (6), and **Captain Quack**
(9). The challenge board and results show your badge, promotions and next unlock.
Progress and selected challenge are saved locally on each device; there is no
cloud synchronization. Android/iOS use native preference storage; desktop uses
`~/.bargeboss/progress.dat`. The catalog uses stable append-only level IDs; old
three-level careers retain their records and initialize the fourth level empty.
The board shows three cards per page, with twelve total unique stars available.

The prototype is offline and silent. Audio, more stages, fleet/upgrades and store
releases remain future work, tracked in the [concept backlog](https://github.com/koniho/bargeboss/issues).

## One game on every platform

This follows **DDDumpling's architecture**, using the same pure Java game and
renderer on Android and iOS, plus a small desktop host. There is no browser port
or separate JavaScript/Swift gameplay implementation.

| Platform | Host | Gameplay and drawing source |
| --- | --- | --- |
| Android 7+ | Native Canvas / Choreographer | `core/`, compiled by javac/D8 |
| iOS 15+ | UIKit / Metal / CADisplayLink; Core Graphics reference painter | the same `core/`, translated by J2ObjC 3.1 |
| Windows, macOS, Linux | Swing / Java2D, Java 8+ runtime | the same `core/`, on the JVM |

Every host fills its safe display bounds with the same **responsive landscape layout**,
using 960×600 as the reference size. The river uses the largest uniform scale that
frames the active work, follows the tug along the lock route, and zooms for docking, with compact HUD/control overlays along the shore. The ground plane uses a 45° yaw and 35.264° elevation; sprites retain their painted
proportions, and Android uses immersive fullscreen with swipe-accessible system bars.
The delivery overview shows more of the harbor. The lock route and rectilinear
tutorial canal scroll, while docking zoom keeps the tug, barge and bollard visible.
Touch/mouse gestures, hold thresholds, haptic timing, tutorial, physics, scoring,
traffic, HUD, menus, glyphs and sprite animation all live in shared Java.

The same PNG atlases are bundled on Android, iOS and desktop. Every rotating
boat and payload has **32 headings**, spaced 11.25° apart. The tug, barge and coal
use individual 8×4 sheets with one uniform scale per sheet and registered
waterline pivots. Steel and container payloads still interleave two 4×4 sheets
with per-frame size calibration.
Shared code holds each drawing crisp, then blends across a brief 2.25° transition. Native hosts sum their weighted premultiplied colors in one layer,
so an opaque boat stays opaque between headings. This smooths transitions between
painted views; it is not skeletal animation or a live 3D model. Native
antialiasing and bitmap sampling can still differ slightly.
See [architecture and parity](docs/architecture.md).

Practice is one [continuous canal and harbor](docs/continuous-tutorial.md): drive
through the gates, push and unload the first barge, then sail to the second barge
for towing. Boats, water and scenery stay in place between lessons; only an
explicit reset restarts an exercise.

## Design a level

Use a top-down sketch and the [level-design brief and template](docs/level-design.md).
A shared level-file format and visual editor are still an authoring gap.

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
XcodeGen and Xcode's Metal Toolchain. The bootstrap script is adapted from DDDumpling and pins the same
checksum-verified J2ObjC 3.1 source archive.

```sh
brew install openjdk@21 maven xcodegen
xcodebuild -downloadComponent MetalToolchain
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
Use `CONFIGURATION=Release ./ios/scripts/build.sh` for performance testing.
Waterline, bow and trailing foam share a Metal/Android 13+ shader kernel, with
textured/geometry fallbacks on other backends; see [water effects](docs/performance/water-shaders.md).
Metal is the default painter; launch with `--ios-coregraphics` for the reference
backend. See [optimization notes](optimization.md) for profiling and known differences.
For a signed device build, set `DEVELOPMENT_TEAM` and run
`./ios/scripts/build.sh iphoneos`. Without a team, it produces an unsigned device
build for compilation validation only. Store signing and delivery are documented in [store release setup](docs/store-release.md).

## Verification

```sh
./check.sh       # Rules, physics, gestures, timing, bounded goal-completion player
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

The reference-derived v3 barge hull uses 32 registered views shared by every load
type. Bulk is one broad grain pile that lowers as the crew unloads it. Source
atlases are `assets/sprites/barge-arcade-v3-16.png` and
`assets/sprites/barge-arcade-v3-half.png`; exact built-in image generation prompts
and input roles are in `docs/art-prompts/barge-arcade-v3-*.txt`.

Dynamic surface lighting now covers watercraft, cargo and floating buoys. Nearby
quay, work and lock lamps, plus tutorial factory lights, remove a soft navy
shading layer from facing surfaces with continuous falloff. Unlit faces darken;
lit faces recover their original colors. Navigation lamps and glowing cores stay bright. The shared response-mask system is ready for selected
scenery receivers; see [surface lighting and rollout](docs/rendering/surface-lighting.md).

Rendering and level work should follow the [performance development rules](docs/performance/development-rules.md)
from the start. They capture tested texture/cache choices, profiling requirements,
visual invariants and the remaining long-frame causes.
