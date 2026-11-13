# Barge Boss

Use DDDumpling's shared pure-Java architecture. `core/` is the only home for
gameplay, input rules, timing, layout, menus, lettering and scene composition.
Android Canvas, iOS Core Graphics via J2ObjC 3.1, and desktop Java2D implement
the same `Painter`. Do not add a second game implementation for any platform.

- Keep platform APIs out of `core/`. Native hosts own event transport, safe-area
  bounds, bitmap decoding/caching and drawing primitives only. All hosts use `Viewport`, the shared pointer router and `Game.advance`.
  Native haptic adapters consume shared semantic events; they never alter gameplay.
- Use a responsive landscape canvas with a 960x600 reference size. Fill the available
  display using per-game Layout and RiverCamera; never stretch sprites or change physics based on screen size/frame rate.
- Keep stage-aware camera behavior in shared Java: overview for short stages, tug
  follow for the longer bridge route, and eased docking zoom near a loaded berth.
  Freeze projection during lasso aiming/flight and preserve screen-space drag
  anchors during camera movement. Use paired forward/inverse transforms for input.
- Keep sprite atlas selection, frame indices, pivots and animation rules in
  shared Java. Native hosts decode the same PNGs and execute `Painter.sprite` / `Painter.spriteBlend`;
  they must not infer gameplay phases or select different artwork.
- Fixed 60 Hz steps, ordered cargo slots and `StrictMath` keep replays repeatable.
  Rendering must not mutate gameplay or use wall-clock time/randomness.
- `./check.sh` checks rules and bounded play. `./preview.sh` exports the actual
  shared renderer. Review affected frames for visual changes.
- `./ios/scripts/parity.sh` compares translated native and JVM state/drawing traces.
  All shared Java sources are discovered automatically by every build script.
- Keep build artifacts, preview exports, credentials, signing keys and generated
  Objective-C untracked. Production PNG atlases in `assets/sprites/` are source
  assets; track them with their generation prompts in `docs/art-prompts/`.
- Preserve unrelated work. Do not modify the sibling DDDumpling repository.
- Report native/device validation limits. Shared logic does not prove identical
  antialiasing, touch latency, safe areas or lifecycle behavior on real devices.

After each completed development round, run the relevant checks, build the Android
APK and install it on the connected development phone, then launch it. The user
has requested this as the ongoing workflow. Reuse the existing debug signing key
and authorized Shizuku/rish installation path when adb is unavailable.
