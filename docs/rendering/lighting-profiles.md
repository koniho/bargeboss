# Stage-entry lighting presets

Issue #17 first phase: dawn, daylight, dusk and night. Dusk remains the default.
Choose a preset on the title or location screen before sailing or starting the
practice tutorial. The selection is session-local and applies on the next entry;
a live voyage retains its immutable snapshot. Practice owns its own snapshot and
does not recolor its parent voyage. The painted title composition stays dusk.

`LightingProfile` is immutable shared Java data: scenery/material RGB grade,
water and glint colors, ambient shade, sun vector/color, cast-shadow strength,
lamp/glow and reflection multipliers, haze and smoke tint. Shared views supply
those values to existing painter primitives. Gameplay, collision geometry,
projection, UI colors, navigation-light colors and emitter anchors are unchanged.
Active hull source colors and teacher livery remain readable; ambient surface
shade changes separately. This is approximate relighting, not a new physical
lighting engine or a claim that baked sprite shadows follow the sun.

## Cache and preparation boundary

- `SpriteGrade` accepts an explicit snapshot. Its source key includes profile
  identity for graded scenery; ungraded hull sources and teacher livery retain
  their own identities. Dusk retains the established grading version/key.
- Canvas stops and joins retired rotation preparation before replacing its
  profile, clears decoded sources/cells/static rasters/retained surface commands,
  and starts the selected manifest. Terrain workers own a final profile snapshot;
  a retired worker cannot publish into the replacement rasterizer. No referenced
  hardware bitmap is recycled.
- Vulkan clears texture and retained-command caches at the profile boundary.
  Sprite/frame uploads still use the existing frame-sized 160 MiB budget.
- Core Graphics invalidates its source, atlas, decoded-frame and static-layer
  caches. Metal additionally keys sprite and static-layer textures by profile,
  invalidates its texture LRU, and rejects asynchronous publication using a
  generation token, including a switch back to an earlier profile. Serial worker
  jobs capture immutable profiles; the worker's retired cache is cleared on its
  queue. The existing 160 MiB Metal budget remains in force.
- Java2D retains one profile's images per rendering thread. Child layer painters
  inherit the parent snapshot. Source/frame/static-raster round trips are checked
  through actual shared-renderer pixels, not just key comparisons.

Hosts prepare the upcoming level manifest at entry before advancing gameplay.
Preparation is an explicit loading boundary, not work repeated for each time
step. Native clocks exclude that boundary from the first simulation delta.
Current loading is synchronous at entry; simulator preparation time is recorded
separately from steady frame intervals. An asynchronous loading screen and
continuous transitions are follow-up work. Background terrain preparation and
all native cache limits remain bounded. Four complete profile families are not
retained at once.

## Art follow-ups

- The title backdrop has a painted dusk sky, smog and warm horizon. It deliberately
  remains dusk. A daylight title needs separately authored sky/horizon artwork.
- `FACTORY` and `STEELWORKS` sprites contain bright warm windows and baked halos.
  Daylight reduces the warm-window exemption and dynamic glow, but cannot remove
  illumination painted into adjacent brick. Separate diffuse/window/emissive
  layers are the targeted revision.
- Active hulls and cargo have baked highlights/shadows. Shared directional shade
  masks preserve their registration and readability; moving sun highlights need
  registered normal/material maps, not stronger whole-sprite color grading.
- Approximate cast-shadow offsets are bounded. Long dawn shadows and continuous
  moving-sun relighting belong to the later phase/#13.

## Reproduce validation

`./check.sh` includes LightingProfileTest and the existing material/alpha,
lighting, depth, input and replay checks. `./ios/scripts/parity.sh` compares the
shared JVM/native drawing and gameplay trace.

Compile `tools/LightingReview.java` with core, desktop, ReplayInput and
`developer/shared/com/bargeboss/game/ProfileScenario.java`, then run
`com.bargeboss.game.LightingReview` from the repository root. It exports sixteen
actual renderer frames and checks exact cache round-trip pixels for tutorial,
Montrose, Calumet and Indiana.

Build with `BARGEBOSS_IOS_PROFILE=1 ./ios/scripts/build.sh`, install only on the
chosen simulator, then use `SIMULATOR_ID=<id> python3 ios/scripts/profile.py <name>
--scene=tutorial-yard --lighting=1 --warmup=30 --frames=60`. Profiles are 0 dawn,
1 daylight, 2 dusk, 3 night. `--coregraphics` and `--water-textures` exercise native
fallback paths. Screenshots must be inspected for system alerts before treating
a capture as evidence. See the accompanying performance evidence for limits.

Marina entry manifests also declare two small resident static layers (pool and
halo). iOS prepares both density buckets before advancing gameplay. Metal pins
these four textures inside its existing budget (402,688 bytes, with a 1 MiB
reservation ceiling), replaces the reservation on stage changes, and clears it
on profile changes. Pending speculative layers are promoted when visible so
publication can evict unprotected entries. This leaves dynamic position/opacity
and the original kernel geometry unchanged.
