# Shared architecture and parity

`Game` owns all state, gestures, transitions, the shift clock, incoming cargo,
delivery rules and AI assistance. `Physics` uses 60 Hz semi-implicit integration.
Each hull is three overlapping discs attached to one rigid body; impulses transfer
linear and angular momentum, while low forward drag and higher sideways drag
approximate a boat's inertia. Current adds a small downstream acceleration.

The tug uses a touch-anchored direction/thrust drag with a neutral/braking ring.
Holding cargo gives an assisted stern approach; dragging overrides its heading.
Cargo receives propulsion through contact impulses. Near a berth, fender
alignment and damping help it settle. A matching bollard accepts a rope drop
only when the complete hull is inside its berth and speed is below the limit.
Dragging an armed lasso shows a curved aiming guide; release starts an 18-tick
flight. A hit attaches at impact and flashes for 24 ticks; a miss retracts for 18
instead. These are real-time shared simulation phases. Aiming cancellation
never launches a rope. Tightening emits an 18-tick tension pulse and quiver as
unloading begins.
Attached lines anchor the hull during tightening and three seconds of unloading;
only completed unloading scores. Unloading and crew animation share the same
counter, so pause/background freezes both. Equipment returns to idle afterward.

The 2× pace comes from two original-size motion substeps for each 60 Hz real
step. Steering, thrust, drag, current, fenders and collisions advance together,
keeping original stopping distances and collision resolution. Contract, shift,
gesture, haptic, mooring and unloading clocks advance once per real step.
Crossing boats cover their route twice as fast; warning and arrival cycles keep
their original timing. Visual wakes, wave patterns and bursts do not affect physics.

`Renderer` emits geometry and atlas sprite commands through `Painter`. Lettering
is a shared 5x7 vector alphabet, avoiding different platform font metrics. There
are no native menus overlaying the game. `Viewport` fits the same **960×600
landscape interface** within each host's safe bounds, using letterboxing where
necessary. The entire playable stage remains visible; there is no following
camera or platform-specific crop.

`RiverPainter` turns the source river a quarter-turn into the landscape board:
`screenX = 16 + (worldY - 148) × 1.35` and
`screenY = 84 + (460 - worldX) × 1.35`. The full source rectangle is
`x=140..460, y=148..836`; navigable water is `x=170..430, y=215..817`.
The paired inverse transform maps native pointer coordinates back to that same
world. World-label helpers retain upright text after rotation. Hull dimensions
are 1.25× their earlier values, and collision inertia is recalculated with those
dimensions. These shared layout and geometry choices make the boats larger
while preserving the complete stage on every platform.

Platform hosts forward stable pointer IDs, cancel on interruptions, feed elapsed
time into `Game.advance`, and implement four geometry primitives plus the shared
`Painter.sprite` and `Painter.spriteBlend` operations. One pointer
owns the current tug, cargo or rope drag. Extra fingers cannot replace it.
The original touch point remains fixed as the hull moves. Releasing removes
thrust and coasts; returning to the ring brakes. Rope cancellation never attaches
a line; release launches it and impact checks the correct bollard.
Backgrounding pauses the shift, releases gestures and removes thrust; returning requires an
explicit shared Resume action. Process termination starts a fresh session.

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
registration points. All five rotating assets use 16 views in a 4×4 grid, ordered
clockwise from east in 22.5° steps. Shared Java chooses adjacent frames, their
blend weight, scale, pivots, opacity and decorative animation from the same
phase state on every platform. Both frames place their registered footprint at
the same world point; the stage applies no residual bitmap rotation. The atlas
camera and painted lighting stay fixed. Crossfading smooths transitions between
adjacent drawings, rather than generating skeletal motion or true 3D geometry.
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
frames, pivots, grid dimensions and blend weight; pixel raster output remains a
native validation boundary.

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
| Title | Enlarged shared tug/barge with exhaust and wake | Sunset skyline, clouds, reflections, bridge lights, pulsing start cue |
| Tutorial/results | Same gameplay art and animated focus cues | Lesson progress; result stars reflect quota, losses and hull |
| Pause/help | World frozen in place | Matching cabinet-style panels and existing button hitboxes |

`./preview.sh` exports actual renderer captures, including all phases and menus.
`./preview.sh --animations` exports 12 fps title, mooring and results frames and,
when FFmpeg is available, looping GIFs. The mooring sequence preserves the real
flight, tightening, unloading and celebration durations. It also includes the
rotation export. `./preview.sh --rotation` exports 20 fps turns for isolated
assets, the tug, and empty/loaded barges against stationary crosshairs, with a
contact sheet and `out/concept-rotation.gif` when FFmpeg is installed. Each preview
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
  transparent edges, overall group alpha and exact frame endpoints. These do
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
and resume; inspect landscape notches and home indicators; verify the whole stage and all controls remain visible.
On Windows/macOS/Linux: resize and letterbox the game, move the window between
different pixel-density monitors, lose focus while holding and resume.
Compare corresponding native captures with the shared renderer previews. Feel
gesture, contact, impact and delivery on real phones, and verify the haptic toggle
and OS settings. Simulator events do not establish physical vibration quality.

This first prototype has one stage, no saved progress and no audio. Native
screen-reader navigation is not implemented; letters supplement berth colors.

Turning holds a crisp source view through most of each 22.5-degree sector. A smooth
blend operates only across the central 4.5 degrees, limiting doubled contours.
Per-frame footprint pivots compensate for the source drawings' unequal padding.
