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
never launches a rope. Tightening emits an 18-tick tension pulse and quiver before
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

`Renderer` emits every primitive through `Painter`. Lettering is a shared 5x7
vector alphabet, avoiding different platform font metrics. There are no native
menus overlaying the game. `Viewport` fits the same full portrait interface to each
host, including desktop landscape windows and tablet screens with letterboxing.

`RiverPainter` uniformly fits the entire original river above the control panel. Its
inverse transform is used by the shared pointer router, preserving world physics
and showing the same whole stage on all devices.

Platform hosts forward stable pointer IDs, cancel on interruptions, feed elapsed
time into `Game.advance`, and implement four drawing primitives. One pointer
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

The user-supplied reference is retained under `docs/reference/`. `VisualStyle`
sets the shared palette; `VesselView`, `HarborView`, `DockView`, `MooringView`,
`ArcadeView` and `ScreenView` emit the same primitives on every platform.
Raised cabins/decks, quays and equipment use decorative screen-up depth while
keeping the original world coordinates and collision footprints.

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
flight, tightening, unloading and celebration durations.

## Verification boundaries

- Shared tests cover drag direction/ownership/neutral/release, cancelled input,
  lasso eligibility/aim/release/flight/miss retraction/cancellation, anchoring, three-second unloading,
  one-time scoring, tutorial completion/restoration, haptic timing and toggle,
  2× motion with real-time clocks, shift expiry, collision momentum,
  bounded catch-up, pure rendering, title/results clocks and identical 30/144 Hz state.
- A bounded touch player must meet the quota during a complete shift.
- `check-art.sh` runs the suite and canonical signature on Android ART from
  Termux, using the same D8 compilation path as the APK.
- `Parity` runs a scripted shift and hashes state plus emitted drawing commands,
  quantized to 0.01 logical units. CI compares JVM and translated Objective-C
  output (including drag/lasso practice and haptic event traces) and also pins the JVM signature to catch accidental replay changes.
- Native builds use `-ffp-contract=off` so compiler-specific fused arithmetic
  does not silently alter the physics path.
- CI compilation proves native API wiring; it does not prove visual pixel
  identity or physical-device responsiveness. Native antialiasing can differ.
- The iPhone simulator smoke test delivers cargo by holding and lassoing, completes all tutorial lessons, and
  verifies pause/resume and background lifecycle transitions through UIKit.

## Device acceptance before release

On Android and iPhone/iPad: complete a shift; test tug dragging, neutral, release, brake, cargo steering and lasso drops;
add/remove/cancel an extra finger while holding a drag; cancel via an OS gesture; background
and resume; inspect notches and home indicators; verify no controls are clipped.
On Windows/macOS/Linux: resize and letterbox the game, move the window between
different pixel-density monitors, lose focus while holding and resume.
Compare corresponding native captures with the shared renderer previews. Feel
gesture, contact, impact and delivery on real phones, and verify the haptic toggle
and OS settings. Simulator events do not establish physical vibration quality.

This first prototype has one stage, no saved progress and no audio. Native
screen-reader navigation is not implemented; letters supplement berth colors.
