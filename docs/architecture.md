# Shared architecture and parity

`Game` owns all state, gestures, transitions, the shift clock, incoming cargo,
delivery rules and AI assistance. `Physics` uses 60 Hz semi-implicit integration.
Each hull is three overlapping discs attached to one rigid body; impulses transfer
linear and angular momentum, while low forward drag and higher sideways drag
approximate a boat's inertia. Current adds a small downstream acceleration.

The playable interpretation exaggerates steering authority and adds a latched forward/reverse throttle with a spring-centered wheel.
Low-speed vectored thrust helps the tug turn; backing reverses the rudder.
The optional tap-assist mode gives the tug an assisted stern approach. Cargo receives propulsion through contact impulses.
Near a target berth, fender alignment and mooring damping make precision docking
possible on a phone. Tap navigation does not pathfind around other traffic;
the player must choose a clear route. This is an arcade model, not a marine simulator.

`Renderer` emits every primitive through `Painter`. Lettering is a shared 5x7
vector alphabet, avoiding different platform font metrics. There are no native
menus overlaying the game. `Viewport` fits the same full portrait interface to each
host, including desktop landscape windows and tablet screens with letterboxing.

`RiverPainter` uniformly fits the entire original river above the helm. Its
inverse transform is used by the shared pointer router, preserving world physics
and showing the same whole stage on all devices.

Platform hosts forward stable pointer IDs, cancel on interruptions, feed elapsed
time into `Game.advance`, and implement four drawing primitives. `Helm` owns one
throttle pointer and one wheel pointer independently. A separate pointer can
select cargo or use tap assist. Throttle release latches its value; cancellation
neutralizes it. Wheel release/cancellation springs it to center.
Backgrounding pauses the shift, releases gestures and neutralizes thrust; returning requires an
explicit shared Resume action. Process termination starts a fresh session.

`Tutorial` runs a separate shared `Game` instance, leaving any suspended shift
intact. Five lessons require actual motion, turning, stopping, reverse and
physical cargo delivery. There is no expiry, damage or traffic in practice.
Resetting a lesson restores its fixture; completion or exit returns through the
same input path as normal menus.

Haptic events are generated in `Game`/`Helm`, never inferred by native hosts.
Detents have a short cooldown, contact is edge-triggered with a longer cooldown,
and delivery/impact take priority over ticks. Android uses View haptic feedback
and respects system settings; iOS uses selection, impact and notification
feedback generators. Desktop drains events without physical output. Turning
haptics off never changes simulation state.

## Verification boundaries

- Shared tests cover two-finger helm ownership, latched/cancelled thrust, wheel
  return, reverse, haptic priority/cooldowns/toggle, tutorial completion/restoration,
  navigation, hold activation/cancellation, help/resume,
  docking containment/speed, score ownership, shift expiry, collision momentum,
  bounded catch-up and identical 30/144 Hz state.
- A bounded touch player must meet the quota during a complete shift.
- `check-art.sh` runs the suite and canonical signature on Android ART from
  Termux, using the same D8 compilation path as the APK.
- `Parity` runs a scripted shift and hashes state plus emitted drawing commands,
  quantized to 0.01 logical units. CI compares JVM and translated Objective-C
  output (including helm practice and haptic event traces) and also pins the JVM signature to catch accidental replay changes.
- Native builds use `-ffp-contract=off` so compiler-specific fused arithmetic
  does not silently alter the physics path.
- CI compilation proves native API wiring; it does not prove visual pixel
  identity or physical-device responsiveness. Native antialiasing can differ.
- The iPhone simulator smoke test delivers cargo using the throttle, completes all tutorial lessons, and
  verifies pause/resume and background lifecycle transitions through UIKit.

## Device acceptance before release

On Android and iPhone/iPad: complete a shift; test throttle, wheel, neutral, reverse, brake and tap assist;
add/remove/cancel either finger while both helm controls are active; cancel via an OS gesture; background
and resume; inspect notches and home indicators; verify no controls are clipped.
On Windows/macOS/Linux: resize and letterbox the game, move the window between
different pixel-density monitors, lose focus while holding and resume.
Compare corresponding native captures with the shared renderer previews. Feel
detents, contact, impact and delivery on real phones, and verify the haptic toggle
and OS settings. Simulator events do not establish physical vibration quality.

This first prototype has one stage, no saved progress and no audio. Native
screen-reader navigation is not implemented; letters supplement berth colors.
