# Shared architecture and parity

`Game` owns all state, gestures, transitions, the shift clock, incoming cargo,
delivery rules and AI assistance. `Physics` uses 60 Hz semi-implicit integration.
Each hull is three overlapping discs attached to one rigid body; impulses transfer
linear and angular momentum, while speed-dependent keel and broadside drag
model orientation-dependent resistance. Loaded barges have mass 5 versus the tug's 2,
gradual flow alignment and stronger directional/yaw damping than the earlier glide tuning. Current adds a small downstream acceleration.

The tug uses a touch-anchored direction/thrust drag with a neutral/braking ring.
Holding cargo gives an assisted stern approach; dragging overrides its heading.
Cargo receives propulsion through contact impulses. Near a berth, fender
alignment and active hold-assist damping help it settle; passive fenders gently resist drift without snapping the hull to a stop. A matching bollard accepts a rope drop
only when the complete hull is inside its berth and speed is below 4 world units/second.
Dragging an armed lasso shows a curved aiming guide; release starts an 18-tick
flight. A hit attaches at impact and flashes for 24 ticks; a miss retracts for 18
instead. These are real-time shared simulation phases. Aiming cancellation
never launches a rope. After 72 ticks of tightening, the line emits an 18-tick
tension pulse and quiver. `Mooring` then eases the hull alongside over 72 ticks,
using the shortest turn to a heading parallel to shore. Its changing footprint
stays within the berth and ends ten units clear of the bank. Unloading starts
only once that translation and rotation finish.
Attached lines anchor the hull during tightening, winching and three seconds of unloading;
only completed unloading scores. Unloading and crew animation share the same
counter, so pause/background freezes both. Equipment returns to idle afterward.

The 2× pace comes from two original-size motion substeps for each 60 Hz real
step. Steering, thrust, drag, current, fenders and collisions advance together,
preserving collision substeps and orientation-dependent coasting. Contract, shift,
gesture, haptic, mooring and unloading clocks advance once per real step.
Crossing boats cover their route twice as fast; warning and arrival cycles keep
their original timing. Wake energy drives vertical heave; it does not add hidden horizontal forces.

`Body.engineLoad` smooths the controller's requested thrust and acceleration effort,
with higher load during cargo contact and reverse braking. Releasing thrust lets
the exhaust settle toward idle even while the hull coasts. NPC feeder loads depend
on hauling versus empty return; blocked feeders idle. `VesselView` draws 5–20
overlapping smoke puffs, increasing their size, density and darkness with load.
The tug has 32 source-cell chimney anchors: 31 visually calibrated, with the
hidden rim in frame 10 explicitly estimated. The anchor
uses the same pivot, scale corrections and blend as its hull, including the river
projection. A throat puff connects the stack to the brighter drifting plume.

`Renderer` emits geometry and atlas sprite commands through `Painter`. Lettering
is a shared 5x7 vector alphabet, avoiding different platform font metrics. There
are no native menus overlaying the game. `Viewport` exposes a full logical canvas
at a uniform pixel scale, with 960×600 as the reference size. Each `Game` owns an
immutable `Layout` and `RiverCamera`; resizing cancels active gestures and updates
practice too. Compact top/bottom HUD overlays replace reserved side rails.
Buttons receive input priority over the projected world. Industrial scenery fills
responsive margins. HUD targets stay fixed while the river camera moves.

`RiverCamera` stores the viewport origin, uniform scale, world origin and zoom.
The overview spans world x=140..460, y=148..836. Ground yaw is 45° and elevation
35.264°, with equal foreshortening along both world axes. The projected bounding
box is `1008/sqrt(2)` by `1008/sqrt(6)`; base scale fits it inside canvas width and
canvas height minus 104, then applies docking zoom. Both screen coordinates use
both world axes; input uses the paired inverse documented in
[the migration notes](isometric-migration.md). Sprites retain
uniform scale, fixed 32-view anchors and upright scenery; physics never changes
with camera transforms. Navigable water is x=170..430. Normal stages span
y=215..817; Bridge Run extends upstream to y=-520, with berth A at y=-400.

`Game.updateCamera` runs once per simulation tick. It eases toward the tug on
Bridge Run and toward the tug/load/berth group when a nearby picked-up barge
approaches its destination. Local work uses 1.5× and docking reaches 1.75×. Both drag endpoints are reprojected
so a stationary physical finger remains stationary on screen. Rope aim and flight
freeze the camera. Pause freezes it too; restart restores the overview. The
tutorial follows the tug at 1.9× in its opening driving basin and 1.4× through the canal, then blends into the harbor work camera. Hosts contain no camera rules or gameplay
exceptions; iOS automation reads the shared projection to aim real native touches.
Android hides system bars with transient swipe access and hosts retain safe cutout bounds.

First Watch has one feeder at y=730 and its sole berth at y=290, opposite ends
of the visible river, with no piers or crossing hazard. Bridge Run has
two feeders, one upstream berth and a shared `Bridge` state machine: approach
requests a six-second wait, two-second opening, at least twelve seconds open,
then two-second closing once all hulls clear. Closing reverses for an occupied
passage. A closed deck is a physical barrier to all three hull discs. Cargo
assistance waits before the gate and resumes after clearance. Heavy Shift keeps
the original piers, three berths and crossing traffic. Queued feeder loads stay
outside the river until their slot's pickup approach is free.

Platform hosts forward stable pointer IDs, cancel on interruptions, feed elapsed
time into `Game.advance`, and implement four geometry primitives plus the shared
`Painter.sprite` and `Painter.spriteBlend` operations. One pointer
owns the current tug, cargo or rope drag. Extra fingers cannot replace it.
The original touch point remains fixed as the hull moves. Releasing removes
thrust and coasts; returning to the ring brakes. Rope cancellation never attaches
a line; release launches it and impact checks the correct bollard.
Backgrounding pauses the shift, releases gestures and removes thrust; returning requires an
explicit shared Resume action. Process termination starts a fresh shift; completed career records and challenge selection persist.

`Tutorial` runs a separate shared `Game` instance, leaving any suspended shift
intact. Six lessons require actual tug dragging, braking, cargo pushing, steering,
docking and a lasso drop followed by unloading. There is no expiry, damage or traffic in practice.
Resetting a lesson restores its fixture; completion or exit returns through the
same input path as normal menus.

Haptic events are generated in `Game`, never inferred by native hosts.
Gesture changes have a short cooldown, contact is edge-triggered with a longer cooldown,
and delivery/impact take priority over ticks. Android uses View haptic feedback
and respects system settings; iOS uses selection, impact and notification
feedback generators. Desktop drains events without physical output. Turning
haptics off never changes simulation state.

## Concept art and animation coverage

The [user-supplied reference](reference/barge-boss-concept.png) guides the shared
palette and silhouettes. The built-in `image_gen` tool produced transparent
directional boat and cargo atlases in [assets/sprites](../assets/sprites/), with
source instructions in [docs/art-prompts](art-prompts/). These are production
source assets, not screenshots or generated build output.

`SpriteAtlas` owns filenames, grid dimensions and per-frame hull/payload
registration points. All five rotating assets use 32 logical views, ordered
clockwise from east in 11.25° steps. The v5 tug uses one 8×4 sheet with a single
uniform scale and registered waterline pivots. Its production smoke points are
in `TugArt`; unverified light/fender guide points remain in the art-study metadata.
The barge and coal layer also use individual 8×4 sheets with uniform registration
in `BargeArt`. The coal geometry guide includes foreground-coaming occlusion;
unloading fades the layer without shifting or stretching that mask.
The transitional steel and container payloads each use two physical 4×4 sheets:
even logical frames use the corrected 16-view base sheet, and odd frames use
intermediate headings (11.25°, 33.75°, …, 348.75°).
`file(atlas, frame)` chooses the sheet; `sourceFrame(atlas, frame)` maps to the
physical cell. Irregular scenery cells use shared normalized sourceLeft/Top/Right/Bottom
bounds. All hosts cache source images by path across atlas IDs, then cache their
logical frames. Shared Java chooses adjacent frames, their
blend weight, scale, pivots, opacity and decorative animation from the same
phase state on every platform. Both frames place their registered footprint at
the same world point; the stage applies no residual bitmap rotation. Per-frame
`widthScale` and `heightScale` calibrate drawing size before applying each pivot.
For the transitional atlases, base views use scale one; intermediate views match the average normalized
ink bounds of their neighboring base drawings. This compensates for different
padding and drawing sizes across the two sheets. Each native blend layer covers
the union of both independently scaled cells. The atlas
camera and painted lighting stay fixed. Crossfading smooths transitions between
adjacent drawings, rather than generating skeletal motion or true 3D geometry.
Turning holds a crisp drawing through most of each 11.25° sector; a smooth blend
covers only the central 2.25°, limiting doubled contours.
Silhouette and perspective differences in the painted source views can remain
visible during a turn.

Android Canvas, iOS Core Graphics and Java2D cache/decode the same bundled PNGs.
They read shared grid metadata and round each cell boundary, including sheets
whose dimensions are not divisible by four. `Painter.spriteBlend` renders the
two registered cells into a transparent layer covering their combined bounds.
Weights sum to one; premultiplied colors and alpha are added, then overall
opacity is applied once when compositing the layer. This preserves opaque
overlaps midway between frames. Android uses PorterDuff ADD, iOS uses Core Graphics
PlusLighter, and desktop uses an equivalent Java2D composite. Hosts do not choose
independent frames or animation states. Drawing replay parity includes both
frames, source files/cells, pivots, X/Y calibration, grid dimensions and blend weight; pixel raster output remains a
native validation boundary.

`SpriteGrade` defines one deterministic integer color transform for industrial
scenery. Android, Java2D and iOS apply it only when decoding a source image into
the cache. Neutral scenery keeps roughly half its original brightness with a
cool cast; bright warm window pixels retain their source color. Watercraft,
cargo, title vessels, water, sky and UI are excluded. Alpha is unchanged, so
darkening does not make opaque buildings translucent. Source PNGs remain
unchanged. Cache keys distinguish graded and original uses of the same file.

Android grades its mutable decoded bitmap using one row buffer. Java2D uses a
row buffer and caches only the resulting ARGB image. iOS decodes into explicit
premultiplied RGBA, uses the shared conversion/grading methods, and retains that
one buffer as the cached CGImage's data provider; each source has a temporary
autorelease pool. This adds one-time cold-decode CPU work, with no per-frame
pixel filtering or additional draw commands. Native decoding/premultiplication
may round antialiased edge channels differently; identical shared arithmetic
does not prove pixel-identical native rasterization. `SpriteGradeTest` verifies
alpha, bright-vessel exclusions, warm windows and premultiplied conversion, and
the JVM/J2ObjC parity signature includes representative grading outputs.

`VisualStyle` sets the palette; `VesselView`, `HarborView`, `DockView`,
`MooringView`, `ArcadeView` and `ScreenView` compose artwork, procedural scenery,
lettering and effects. Directional sprites preserve raised cabins/decks and
camera lighting as boats turn. Decorative depth does not change the physical
hull or its touch target. The quays and machinery fit the compact landscape
shoreline, and world labels stay upright.

`Game.visualTicks` advances in fixed 60 Hz steps during play, title and results.
Title/results animation never advances gameplay timers, bodies or scores.
Pause/help freeze both clocks. Rendering only reads clocks and phase state;
there is no host-specific timing, randomness or state mutation in drawing.

`FrameGate` shares redraw eligibility across Android, iOS and desktop. Hosts
still call `Game.advance`, poll haptics and save progress on every timing
callback. They request an animation redraw only when the active game's fixed
simulation/visual clocks, mode, layout, identity or visible settings change.
At 120/144 Hz callback rates this avoids submitting the same 60 Hz visual state
twice; paused/help screens remain idle. Pointer events, cancellation, resize,
back/pause, focus and lifecycle changes mark the gate dirty and immediately
request drawing. A dirty request is consumed only after an actual completed
draw, so native event coalescing cannot discard a pending input update. Native
exposure paints are still allowed even when the shared state is unchanged.

`FrameGateTest` checks 60/120/144 Hz cadence, unchanged simulation, paused idle,
input dirtiness, practice enter/exit, lifecycle and independent haptic delivery.
These shared tests and JVM compilation do not validate Android frame scheduling
or iOS UIKit drawing on a device. Native build/device checks remain required;
the generated `BBFrameGate` selectors are compiled by the iOS CI build.

| Phase | Boat/cargo | Harbor, crew and feedback |
| --- | --- | --- |
| Idle | Gentle bob, light exhaust, tiny surface ripples | Lamp reflections, bridge beacons, crane/crew idle cycles |
| Driving/coasting | Cabin/navlights, speed-scaled V foam and propeller churn | Live drive indicator and textured flowing water |
| Braking | Reverse propeller wash and shrinking motion | Brake indicator and centered control ring |
| Aligning/pushing/steering | Engine strain, heavier exhaust, contact foam | Approach guide, hold meter, steering indicator |
| Berth ready | Settled cargo and pulsing cleat | Animated berth arrow, lit bollard, crew ready pose |
| Lasso armed | Coil stays aboard | Continuous trajectory arc, moving direction dots, landing target |
| Released throw | Loop flies on the shown arc | Unspooling rope and crew catching pose |
| Miss | Loop reels back to the barge | Retraction indicator; no attachment or scoring |
| Bollard hit | Rope attached to stationary barge | Bright flash, expanding ring and spark burst |
| Tightening | Slack pulled out, loop becomes a knot | Traveling taut-line flash, recoil and endpoint shock rings |
| Unloading | Three payload groups disappear with the lifts | Three-second crane/forklift/worker cycle and progress bar |
| Delivered | Cargo clears once | Stars, floating score, crew salute then idle |
| Hard impact | Hull flash and spray | Surface shock ring and impact feedback |
| Title | Enlarged gameplay tug/barge with registered exhaust and wake | Gameplay scenery in two scrolling layers, curved waves, pulsing start cue |
| Tutorial/results | Same gameplay art and animated focus cues | Lesson progress; result stars reflect quota, losses and hull |
| Pause/help | World frozen in place | Matching cabinet-style panels and existing button hitboxes |

`./preview.sh` exports actual renderer captures, including all phases and menus.
`./preview.sh --animations` exports 12 fps title, mooring and results frames and,
when FFmpeg is available, looping GIFs. The mooring sequence preserves the real
flight, tightening, unloading and celebration durations. It also includes the
rotation export. `./preview.sh --rotation` exports 240 frames at 30 fps for isolated
assets, the tug, and empty/loaded barges against stationary crosshairs, with a
32-heading contact sheet and `out/concept-rotation.gif` when FFmpeg is installed. Each preview
run first checks the desktop blend primitive with synthetic opaque and
transparent pixels.

## Verification boundaries

- Shared tests cover drag direction/ownership/neutral/release, cancelled input,
  lasso eligibility/aim/release/flight/miss retraction/cancellation, anchoring, three-second unloading,
  one-time scoring, tutorial completion/restoration, haptic timing and toggle,
  2× motion with real-time clocks, shift expiry, collision momentum,
  bounded catch-up, pure rendering, title/results clocks, registered rotation/blend
  continuity and identical 30/144 Hz state.
- A bounded touch player must meet the quota during a complete shift.
- `check-art.sh` runs the suite and canonical signature on Android ART from
  Termux, using the same D8 compilation path as the APK.
- `Parity` runs a scripted shift and hashes state plus emitted drawing commands,
  quantized to 0.01 logical units. CI compares JVM and translated Objective-C
  output (including drag/lasso practice and haptic event traces) and also pins the JVM signature to catch accidental replay changes.
  Filename hashing explicitly uses Java’s UTF-16 algorithm, avoiding native
  NSString hash differences. A mismatch exports individual trace values and a
  diff under `ios/build/parity/` without relaxing numeric quantization.
- Desktop pixel tests verify full opacity during overlaps, premultiplied
  transparent edges, overall group alpha, exact frame endpoints and fixed centers
  for calibrated footprint markers. These do
  not replace native Android/iOS blend inspection.
- Native builds use `-ffp-contract=off` so compiler-specific fused arithmetic
  does not silently alter the physics path.
- CI compilation proves native API wiring; it does not prove visual pixel
  identity or physical-device responsiveness. Native antialiasing, alpha blending and bitmap sampling can differ.
- The iPhone simulator smoke test delivers cargo by holding and lassoing, completes all tutorial lessons, and
  verifies pause/resume and background lifecycle transitions through UIKit.

## Device acceptance before release

On Android and iPhone/iPad: complete a shift; test tug dragging, neutral, release, brake, cargo steering and lasso throws;
add/remove/cancel an extra finger while holding a drag; cancel via an OS gesture; background
and resume; inspect landscape notches and home indicators; verify docking zoom, bridge follow, lasso targeting and visible HUD controls.
On Windows/macOS/Linux: resize the game and verify responsive reflow, move the window between
different pixel-density monitors, lose focus while holding and resume.
Compare corresponding native captures with the shared renderer previews. Feel
gesture, contact, impact and delivery on real phones, and verify the haptic toggle
and OS settings. Simulator events do not establish physical vibration quality.

This first prototype has one stage, no saved progress and no audio. Native
screen-reader navigation is not implemented; letters supplement berth colors.

## Challenge career and local saves

`Challenges` defines three Calumet contracts with per-shift targets and timers.
`Career` owns unlocked challenges, selected challenge, best successful scores,
best stars and captain rank. Total stars sum each challenge's maximum, so repeated
runs do not farm rank. `Game` records a result exactly once at a successful timer
expiry; abandoned shifts, wrecks and practice cannot award progress.

Hosts restore an opaque versioned string before their first draw, then observe
`progressVersion()` and persist `saveProgress()` only when it changes. Shared Java
validates and restores the bounded snapshot atomically. Android SharedPreferences,
iOS NSUserDefaults and desktop atomic file replacement transport the same data;
no native adapter interprets unlocks or awards. A failed write leaves gameplay
running and can be retried on a later change or lifecycle flush. Saves are local,
not synchronized between devices.

Desktop storage is `~/.bargeboss/progress.dat`. Its regression tests exercise
replacement, bounded reads, Unicode transport and failure behavior. Native iOS
smoke tests use test-only reset/opaque fixture launch arguments, select a challenge
through the real UI to trigger a save, then terminate and relaunch without fixture
arguments. Shared career tests separately verify earned rewards and complete the
harder challenges with the bounded player. JVM/J2ObjC parity includes completed
career results, snapshot round trips, challenge screens and responsive layouts.


## Edge feeders

First Watch owns one `Feeder` at world x=260; later challenges add a second at x=360.
Their loads enter from beyond y=836, move to y=730 and hand off a fresh `Body`
into a free cargo slot. Two-slot stages start with simultaneous arrivals; later
requests rotate through available slots every 14 real seconds, or 30 seconds on
Bridge Run, until the final 30 seconds. Busy slots
retain their current load; dispatch retries rather than overwriting it. An NPC
backs out with its bow still facing the channel after handoff. Occupied approach
or return paths wait and show a throttled HURRY UP call every 240 ticks.

Before handoff, incoming hulls are NPC obstacles, never scored contracts. The
slot fenders hold handed-off cargo until actual tug contact during a push gesture; contract age
starts at handoff. Pause, help, background and practice suspend the root feeder
clocks. Tutorial fixtures use no NPC arrivals. `FeederView` draws markings,
wakes, per-slot warnings, destination bubbles and berth pulses; calls are visual
only, with no platform speech engine. Render methods never advance these states.
Regression tests cover simultaneous arrival, independent blocked slots, retries,
exactly-once handoff, expiration origin, fenders, pause/reset/practice and bounded
completion of all three challenges. Native/JVM parity includes arrival, departure
and blocked-call frames. iOS UI tests wait for real feeder handoff before pushing.


## Layered harbor and occlusion

The current scenery uses one transparent 4×3 `harbor-arcade-v2.png` atlas: eight
chunky industrial props, two finger piers and two raised bridge halves. All cells
share the same high orthographic camera, parallel ground axes, bold silhouettes
and upper-left lighting. A projection correction removes tapering pier footprints;
registered opaque bounds place the visible pier walls on the existing colliders.
The built-in image generator produced this atlas; exact generation/edit prompts
live in `docs/art-prompts`. The previous `water-arcade-v2.png` tile and title-only
images remain source history; the live water and title no longer render them.
Earlier painted v1 sheets are retained as source history but no longer draw scenery.

`SceneryView` draws broad concrete quay planes with the same shared camera,
placing four independent props on each bank. `LightingView` adds consistent
lower-right cast shadows, hull contact shadows and warm animated lamp pools.
Sparse pavement seams and softly curved water waves keep detail below the boats,
berth markers and lasso. Solid footprints belong to `HarborGeometry`; assisted
pushes use a small visibility graph around expanded quay footprints; a hull
inside a planning margin first escapes toward the open channel. Rendered sprites
never define physics implicitly. All vessels, including NPC arrivals, are ordered
by their projected ground depth. Unloading cranes and personnel remain independently
animated in screen-upright coordinates.

`SceneryOcclusion` records vessel and cargo sprite submissions in a per-render
context. Only buildings whose opaque silhouettes project over navigable water
are eligible to fade, and only over opaque pixels of a watercraft already drawn
behind them. Placement eligibility is cached independently of camera movement. Shared 64×64 interior masks derive from the production PNG alpha and
registered atlas crops, including each visible turning-frame blend. Transparent
padding, wake, smoke, lasso lines, bollards, gates and proximity margins do not
trigger fading. Factory/stack visibility bounds still include their smoke, but
only the solid building silhouette determines the building's opacity.

Bridge structures, dock equipment, standalone stacks, ground and shore caps stay
opaque. Eligible building alpha eases from 1 to 0.28 over the first few
overlapping samples, entirely inside the opaque overlap.
Background scenery stays solid because it has no previously painted vessel to
obscure. The context is rebuilt each render, with no gameplay mutations or stale
fade after motion, reset, pause or camera changes. Generate masks with
`GenerateOcclusionMasks`; `preview.sh` verifies they match the production artwork.
The masks intentionally exclude soft edges and sub-cell slivers rather than
fading a nearby structure. No runtime bitmap readback or platform image APIs enter
shared scene composition.

## Waves, heave and trough-triggered wake

`WaveView` fills the river with curved, tapered wavelets. Each crest travels,
rises, changes thickness and fades on the shared presentation clock. The title
uses the same renderer; no cropped image strips form its water. Shore art covers
overscan. Dock-light reflections remain independent, so idle water stays animated
without wake emissions or hidden horizontal forces. Pause freezes these effects.
`./preview.sh --idle-water` exports a six-second idle animation.

`WaterSurface` owns 192 bounded, persistent wake packets with a 300-tick lifetime.
Ambient crossing waves plus nearby packet energy drive a filtered heave response
for each hull; moored or staged loads respond more gently. Buoys sample the same
field. Rise/fall offsets are in screen-vertical space and move hull and payload
together without changing their horizontal collision footprint or rotational
anchor. At a negative-to-positive heave-velocity crossing, an underway hull emits
one strong wake packet and an 18-tick foam burst, with a cooldown preventing
chatter. A weaker stern packet every eight ticks maintains a continuous underway
trail between troughs. Local hull wash stays short; the long wake belongs to the
persistent water field.
Wake heading follows actual travel direction, including astern motion. Packets
expand from their emission positions and keep fading after their source stops.
The small powered-boat prop wash is separate from the trough-triggered main wake.
Water state advances at the same fixed 60 Hz on every platform and freezes with
pause/background; drawing only samples state. Dedicated checks cover directional
glide, reverse motion, yaw inertia, wake timing, local wave amplitude and render purity.

## Shared-style title voyage

`TitleView` combines the panoramic sunset, separate industrial/bridge shore and
varied land sprites with the gameplay tug, barge and cargo atlases. Sky and shore
pan at different speeds for an endless voyage. The compact logo anchors to the
upper-left safe canvas; subtitle lines and the menu's instruction block are
removed. The shift card and its three actions anchor to the right edge, using
the same `Layout.titleBox` / `Game.controlBox` positions as input. Other screens keep their existing
centered menu rules. The rank strip sits 24 logical pixels above the bottom edge.

The convoy scales within the water left of the menu, stays above the rank strip,
and adapts its position to the full available width. The higher horizon leaves
more open foreground water. Framing and touch-target tests cover 960×600, the
phone screenshot's approximately 1243×600 safe canvas, wide phones and 960×720
tablets throughout the bob animation.

The tug pushes its barge while both bob independently, with registered chimney
smoke. `WaveView` supplies moving curved waves. Wake geometry is clipped below
the horizon and drawn before shoreline art; the vessels draw afterward. Fewer,
lighter wake packets retain movement without piling bright rings over the scene.
Every phase is a pure function of `visualTicks`, so drawing cannot alter game
state and animation matches across display refresh rates. `TitleLayoutReview`
exports the actual composition at four aspect ratios under `out/title-layout`.
The 446 title framing, water clipping and touch-target checks run on JVM and ART;
replay signature `e8ba8e4b:11:8970` includes the intentional title drawing changes.
The final Android build was installed, launched and visually checked on the
development phone; `BargeBoss-title-fixed-device-v2.png` is saved in Downloads.
`./preview.sh --title` exports eight seconds to `out/concept-title.gif`.
`./preview.sh` also exports all 32 smoke headings and a seven-second wake trail
sequence showing outward propagation after the tug stops.

## Continuous tutorial world and raised terrain

Practice uses one connected `TrainingChannel.WATER_BOUNDARY` and rectangle union
through every lesson. A broad basin starts with two steering targets and a
coasting lesson before the rectilinear canal. The tug and two barges persist; progression does not call
fixture setup. The first unloaded barge stays moored and solid using its
`deliveryRecorded` flag, while the second original barge becomes the tow job.
Camera follow blends into the harbor by world position, not lesson number.
See [the continuous tutorial contract](continuous-tutorial.md).

`TrainingGroundView` subtracts the actual water geometry from the raised yard,
then renders corrugated steel retaining faces and caps. Source ground texture is
used only on wholly dry cells; partial shore cells use matching concrete.
`GroundArt.LAND_RISE` lifts yard, factory bases, lamps and dock foundations.
Bollard projection is exposed through `Game.bollardX/Y` to keep input, aiming
and rope rendering attached to the same raised fitting.

Sixteen registered factory sprites reserve 64-unit whole-cell footprints. Their
actual foundations fit those reservations uniformly, without warping; permanent
placements have height-dependent setbacks and share depth sorting with vessels.
`FactoryArtData` owns source pivots and per-fixture light/smoke UVs. World XYZ
in the source study is inferred and must not be reused as exact 3-D geometry.

`LassoPath` supplies both preview and released arc geometry. The tether root is
stored in barge-local coordinates at the throw-facing edge. The flying endpoint
follows the released trajectory while the root follows vessel translation and
rotation; a missed throw reels back to that moving root. Hull damage uses a
separate shared timer for two brief player-sprite pulses, independent of ordinary
contact feedback.

## Articulated industrial structures

`ArticulatedModel` stores rigid local XYZ parts under named parents. Mechanical
pivots and parent transforms move the complete mesh before the shared isometric
projection. `BridgeModel` supplies two hinged truss leaves, fixed abutments and
mechanism towers. Bridge timing, collision and passage permission remain in
`Bridge`; the mesh only presents that state.

Light fixtures have stable IDs, local positions and outward normals parented to
their rigid parts. Their projection, visibility and organic glow use the same
transforms as the steelwork; bridge structures remain opaque. Future booms and other rotating
shore equipment should use this contract; fixed-view raster scenery cannot be
rotated into a new perspective. Existing unloading machinery is not converted
by the bridge change.

## Dense-scene drawing cost

`ViewportPainter` rejects fully offscreen or transparent primitives before they
cross the host drawing boundary. Intersecting primitives are passed through
unchanged, including registered sprite pivots, rotations, blends and a conservative
antialias margin. Hosts still clip to the real viewport. `ViewportPixelTest`
compares raw and culled desktop pixels, including edge strokes and rotated sprites.

Tutorial terrain tests projected tile bounds before submitting textures and
geometry; permanent road eligibility and connections are computed once. The wall
pass skips back faces that have no visible geometry. These terrain changes are
pixel-identical in nine captured scene/viewport combinations.

Factory light and smoke emitters have individual conservative visibility bounds.
Below 1.35× zoom, small factory lights use compact glow kernels and exhaust retains
all eighteen primary puffs with fewer secondary lobes. Close views retain the full
factory effect detail. Vessel exhaust, wakes, water and game feedback use their
existing animation paths. Production sprite pixels/resolution and placements are
unchanged. Authored font rows are decoded once rather than allocating substrings
and parsing hexadecimal values for every letter on every frame.

`RenderBudgetTest` caps drawing commands for real tutorial states at three widths,
with minimum factory/effect counts to prevent satisfying a budget by hiding the
scene. `ScenePerformanceBenchmark` separates shared command generation from
Java2D raster cost; these timings are not Android or iOS frame-rate claims.


`StaticTerrain` records immutable raised ground, roads, corrugated retaining walls
and caps in 256-unit projection-space chunks. Background and foreground caps use
separate passes; boats and factory sprites retain their original depth order.
Chunk keys do not contain camera offsets or fractional zoom. A bounded shared
geometry cache holds at most 256 chunks. Smoke and water never enter those chunks.

`Painter.layer` lets Android Canvas, iOS Core Graphics and desktop Java2D retain
native raster layers under a 48 MiB LRU budget. All hosts use the shared 2×/3×
raster-density rule and build at most one new layer per frame. Misses draw the
same content directly, clipped to its declared bounds; alpha applies to the whole
group. Pan and animation do not rebuild immutable layers. Default command
expansion keeps deterministic JVM/translated traces independent of cache warmth.
Native antialiasing and cached resampling are validated separately.

Organic lights reuse small static glow kernels while their pulse, position, core
and accents remain live. Factory sprites stay separate: combining them into a
single image would interfere with vessel insertion, selective building fading
and smoke behind nearer roofs. The existing source-image cache already avoids
repeated factory PNG decoding. `StaticLayerPixelTest` covers clipped cold/warm
rendering, group alpha, density changes, eviction and reuse. See
[cache profiling](performance/cache-baseline.md) for source-memory costs and
controlled submission measurements; these do not establish device frame rate.

## Watercraft highlight and diffuse quay lighting

`VesselHighlight` supplies alpha-derived, registered hull rim/bloom textures for
all 32 tug and barge views. Every craft has a subdued warm edge; active driving
and tutorial targets breathe more strongly, while lasso readiness uses mint.
The glow shares the vessel's pivot, heading crossfade, bob and depth order. It
never participates in scenery-occlusion masks. See
[effect texture generation](art-prompts/watercraft-glow.md) for the reproducible
alpha filtering and padding contract.

Quay lamps use cached `DiffuseLight` beam and pool layers (`0x42000001/2`).
Overlapping soft lobes disperse beyond the lamp foot instead of ending in a
filled triangle. A wide low-energy ground pool and water-clipped diffuse
reflections soften the surrounding darkness. Dynamic intensity remains shared
and deterministic; hosts cache the fixed shapes through `StaticLayer`.

## Independent shadows and yard activity

`ShadowArt` registers separate near/soft masks for watercraft and fixed scenery.
`ShadowView` blends quay-light influence smoothly with ambient illumination,
then projects masks through `Painter.spriteAffine`. All three native hosts draw
the same normalized affine basis; viewport culling, opacity and screen transforms
preserve that basis. Factory shadows render before scenery and vessels. Vessel
shadows stay on the water plane through bob and heading changes. See
[mask generation and projection limits](art-prompts/shadow-masks-v1.md).

`FactoryActivity` adds deterministic furnace charge cycles, portal sparks,
staggered window-bank surges and intermittent steam at reviewed anchors. Existing
smoke and lights continue independently. Its two small immutable kernels use the
bounded native layer cache. [The activity study](art-studies/factory-activity-v1/review.md)
records anchor choices and measured additional submissions.

`YardActivity` places small electric haulers on contiguous dry road runs. Their
3-D box parts share the isometric camera and raised ground plane. They shuttle,
pause at loading ends, vibrate slightly while moving and carry an amber beacon
and headlight. At most six visible haulers render; their ground depth is merged
with the existing factory/vessel ordering. Entire footprints stay on road cells
and outside water. They are ambient actors, with no collision or scoring impact.
All motion derives from `visualTicks`, so pause freezes the scene and rendering
never advances gameplay.
