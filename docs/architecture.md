# Shared architecture and parity

`Game` owns all state, gestures, transitions, the shift clock, incoming cargo,
delivery rules and AI assistance. `Physics` uses 60 Hz semi-implicit integration.
Each hull is three overlapping discs attached to one rigid body; impulses transfer
linear and angular momentum, while low forward drag and higher sideways drag
approximate a boat's inertia. Current adds a small downstream acceleration.

The playable interpretation exaggerates steering authority and gives the tug an
assisted stern approach. Cargo receives propulsion through contact impulses.
Near a target berth, fender alignment and mooring damping make precision docking
possible on a phone. Tap navigation does not pathfind around other traffic;
the player must choose a clear route. This is an arcade model, not a marine simulator.

`Renderer` emits every primitive through `Painter`. Lettering is a shared 5x7
vector alphabet, avoiding different platform font metrics. There are no native
menus overlaying the game. `Viewport` fits the same full portrait stage to each
host, including desktop landscape windows and tablet screens with letterboxing.

Platform hosts translate the primary pointer, cancel on interruptions, feed
elapsed time into `Game.advance`, and implement four drawing primitives. Android
and iOS retain one pointer ID/reference while secondary touches are ignored.
Backgrounding pauses the shift and releases gestures; returning requires an
explicit shared Resume action. Process termination starts a fresh session.

## Verification boundaries

- Shared tests cover navigation, hold activation/cancellation, help/resume,
  docking containment/speed, score ownership, shift expiry, collision momentum,
  bounded catch-up and identical 30/144 Hz state.
- A bounded touch player must meet the quota during a complete shift.
- `check-art.sh` runs the suite and canonical signature on Android ART from
  Termux, using the same D8 compilation path as the APK.
- `Parity` runs a scripted shift and hashes state plus emitted drawing commands,
  quantized to 0.01 logical units. CI compares JVM and translated Objective-C
  output and also pins the JVM signature to catch accidental replay changes.
- Native builds use `-ffp-contract=off` so compiler-specific fused arithmetic
  does not silently alter the physics path.
- CI compilation proves native API wiring; it does not prove visual pixel
  identity or physical-device responsiveness. Native antialiasing can differ.
- The iPhone simulator smoke test delivers cargo using a real long press and
  verifies pause/resume and background lifecycle transitions through UIKit.

## Device acceptance before release

On Android and iPhone/iPad: complete a shift; test tap, hold, drag and release;
add and remove a second finger during a push; cancel via an OS gesture; background
and resume; inspect notches and home indicators; verify no controls are clipped.
On Windows/macOS/Linux: resize and letterbox the game, move the window between
different pixel-density monitors, lose focus while holding and resume.
Compare corresponding native captures with the shared renderer previews.

This first prototype has one stage, no saved progress and no audio. Native
screen-reader navigation is not implemented; letters supplement berth colors.
