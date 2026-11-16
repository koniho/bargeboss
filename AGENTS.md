# Barge Boss

Use DDDumpling's shared pure-Java architecture. `core/` is the only home for
gameplay, input rules, timing, layout, menus, lettering and scene composition.
Android Canvas, iOS Metal (with a Core Graphics reference path) via J2ObjC 3.1, and desktop Java2D implement
the same `Painter`. Do not add a second game implementation for any platform.

- Default art direction for both Calumet and Montrose is late dusk, close to night,
  unless the user explicitly requests another ambience. Docks should appear almost
  dark; keep shore/scenery subdued and cool, with warm lamps/windows and navigation
  lights supplying localized contrast. Preserve readable vessels, goals and controls.
- Follow [gameplay highlight guidelines](docs/art-direction/gameplay-highlights.md) for
  quiet guidance: thin translucent strokes, open arrowheads, continuous rings
  and restrained glow. The broad neon/segmented redesign was rejected.
- Push completed, checked development increments as work progresses; do not hold
  unrelated completed changes until a large batch is finished.
- Keep platform APIs out of `core/`. Native hosts own event transport, safe-area
  bounds, bitmap decoding/caching and drawing primitives only. All hosts use `Viewport`, the shared pointer router and `Game.advance`.
  Native haptic adapters consume shared semantic events; they never alter gameplay.
- Use a responsive landscape canvas with a 960x600 reference size. Fill the available
  display using per-game Layout and RiverCamera; never stretch sprites or change physics based on screen size/frame rate.
- Keep stage-aware camera behavior in shared Java: close views for local work,
  wider canal/bridge transit views, and eased docking zoom near a loaded berth.
  Freeze projection during lasso aiming/flight and preserve screen-space drag
  anchors during camera movement. Use paired forward/inverse transforms for input.
- Follow [the drawing-order contract](docs/rendering/drawing-order.md) for depth,
  height, orientation, solid-shore cutouts, rope spans and building-only fades.
  A cap stroke is not a land surface: production hull/cargo pixels must not show
  through foreground ground. Maintain clip support on all three native painters
  and coordinate wrappers; run the all-heading shoreline pixel regression.
- Follow [the cargo authoring contract](docs/art-studies/cargo-authoring-contract.md)
  for every cargo addition or revision: shared hold geometry/perspective, 32 real
  poses, registered whole-load layers, foreground-coaming occlusion, explicit
  effect metadata and fresh shadow/surface-light derivatives. Run its asset
  checks and review actual hull composites; unique pixels do not prove correct angles.
- Keep sprite atlas selection, frame indices, pivots and animation rules in
  shared Java. Native hosts decode the same PNGs and execute `Painter.sprite` / `Painter.spriteBlend`;
  they must not infer gameplay phases or select different artwork.
- Scenery that changes orientation (bridge leaves, booms and similar machinery)
  must be authored as rigid parts in a shared skeleton or 3-D model. Rotate parts
  about mechanical pivots before projection; do not rotate a painted isometric
  bitmap. Parent light/smoke anchors to their rigid parts, with stable IDs and
  local XYZ/normals, so effects follow the same transformation. Match the concept
  and shared camera, and review closed, moving and open/working states.
- Coordinate optimization work through [optimization.md](optimization.md). Read other
  agents’ entries before shared edits and append platform, evidence, changes and
  validation after each meaningful step; do not overwrite their findings.
- Follow [performance development rules](docs/performance/development-rules.md)
  when adding levels, assets, effects, caches or renderer primitives. Keep runtime
  textures frame-sized, caches bounded, and expensive raster preparation out of
  active draws. One cache build per frame is not a time budget. Preserve drawing
  order and anchors; verify end-to-end frame intervals and native pixels rather
  than assuming fewer draw calls are faster. Record evidence and unresolved causes.
- Fixed 60 Hz steps, ordered cargo slots and `StrictMath` keep replays repeatable.
  Rendering must not mutate gameplay or use wall-clock time/randomness.
- `./check.sh` checks rules and bounded play. `./preview.sh` exports the actual
  shared renderer. Review affected frames for visual changes.
- `./ios/scripts/parity.sh` compares translated native and JVM state/drawing traces.
  All shared Java sources are discovered automatically by every build script.
- Keep build artifacts, preview exports, credentials, signing keys and generated
  Objective-C untracked. Production PNG atlases in `assets/sprites/` are source
  assets; track them with their generation prompts in `docs/art-prompts/`.
- When creating concept images or sprite sheets for user review on the development
  phone, also copy them to `/storage/emulated/0/Download` with descriptive versioned
  names (for example `BargeBoss-tug-v5-design.png`). Keep the canonical source in
  the repository; do not leave the only copy in Downloads or the imagegen cache.
- Preserve unrelated work. Do not modify the sibling DDDumpling repository.
- When the user refers to a screenshot, check the phone's photos directory first:
  `/storage/emulated/0/Pictures/Screenshots` (newest files first), then
  `/storage/emulated/0/DCIM/Screenshots` if needed. Do not assume it must be attached
  to the conversation. Inspect the relevant image before diagnosing the report.
- Report native/device validation limits. Shared logic does not prove identical
  antialiasing, touch latency, safe areas or lifecycle behavior on real devices.

After each completed development round, run the relevant checks, build the Android
APK and install it on the connected development phone, then launch it. The user
has requested this as the ongoing workflow. Reuse the existing debug signing key
and authorized Shizuku/rish installation path when adb is unavailable.

During device development and profiling, keep the phone awake with
`./tools/phone-awake.sh on` and restore its previous display timeout with
`./tools/phone-awake.sh off` when the work is finished. The user requested this
as an ongoing workflow. Profiling scripts manage their own awake scope when a
development round has not already opened one. Never change keyguard security.
