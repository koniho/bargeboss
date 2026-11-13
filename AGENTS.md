# Barge Boss

Use DDDumpling's shared pure-Java architecture. `core/` is the only home for
gameplay, input rules, timing, layout, menus, lettering and scene composition.
Android Canvas, iOS Core Graphics via J2ObjC 3.1, and desktop Java2D implement
the same `Painter`. Do not add a second game implementation for any platform.

- Keep platform APIs out of `core/`. Native hosts own event transport, safe-area
  bounds and drawing primitives only. All hosts use `Viewport` and `Game.advance`.
- Keep the stage at 600x960 and fit it inside available bounds. Never crop a
  platform's playable area or change its physics based on screen size/frame rate.
- Fixed 60 Hz steps, ordered cargo slots and `StrictMath` keep replays repeatable.
  Rendering must not mutate gameplay or use wall-clock time/randomness.
- `./check.sh` checks rules and bounded play. `./preview.sh` exports the actual
  shared renderer. Review affected frames for visual changes.
- `./ios/scripts/parity.sh` compares translated native and JVM state/drawing traces.
  All shared Java sources are discovered automatically by every build script.
- Keep artifacts, credentials, signing keys and generated Objective-C untracked.
- Preserve unrelated work. Do not modify the sibling DDDumpling repository.
- Report native/device validation limits. Shared logic does not prove identical
  antialiasing, touch latency, safe areas or lifecycle behavior on real devices.
