# Shared water foam shaders

The waterline, bow pressure wave, detached shoulder foam, prop wash, expanding
wake crests and title wakes use `Painter.waterFoam`. All positions, source
headings, speed response, ages, phase and clipping remain in Java. The shader
cannot move a vessel, apply a force or change the camera.

`assets/shaders/water-foam.inc` is the single shading kernel, compiled into Metal
and loaded by Android's API 33+ `RuntimeShader`. It uses local patch coordinates,
warped filaments, irregular foam coverage and soft edges. It needs no scene-color
read, offscreen water texture, fluid simulation or fullscreen effect. Metal batches
these patches with its existing vertex/stencil pipeline. Android creates one
program before active drawing and reuses the paint, matrix and uniform storage.

Java sends two bow patches only when forward hull speed exceeds 3 world units/s;
they strengthen smoothly with speed. Sideways drift and reverse motion retain
existing displacement foam without a false forward bow wave. Contact foam remains
registered to the authored, heading-blended lower silhouette, including heave.
Released foam remains independent of later vessel motion. Wake packets keep their
existing lifetime, source direction and force behavior. No new wake forces were
added to barges.

Software Canvas, Android before API 33, Core Graphics and Java2D use the authored
foam textures and bounded crest geometry. Short expanding-crest segments stay one
line each; only the two bow ribbons need eight curved fallback segments. The
fallback deliberately has less fine detail. No pixel equivalence is claimed.
Android API documentation: https://developer.android.com/reference/android/graphics/RuntimeShader

## Drawing and cost boundaries

- Water-plane effects stay below hulls and use shared raised-shore/dock exclusion.
  Expanding crests share one visible-region clip; contact retains its hull-local
  water-plane scope. Title shader patches clip at the title water plane.
- Affine projection, opacity, offscreen rejection and batch flush order are
  forwarded by painter wrappers. Native code does not choose artwork or heading.
- Existing 160 foam decals / 192 wave packets remain bounded. Shader patches add
  no texture cache. Metal's phase field fits the existing aligned vertex stride.
- `--ios-water-textures` selects the fallback for controlled comparisons. The
  profile script accepts `--water-textures`; metadata records `water_shaders`.

## Routing correction and validation on 2026-11-16

The first captures labeled “shader A/B” actually used the fallback: the outer
`SceneryOcclusion` wrapper did not forward `waterFoam`. Those old files remain
historical evidence but cannot establish shader cost. The wrapper and screen
transform now forward native effects. Tests exercise the actual Renderer root;
Metal counters confirm 532 foam patches and three reflection patches in the
canal replay. Counters reset at beginFrame, before the previous completed frame
is reported on the following callback.

Release iPhone 17 Pro / iOS 26.5 simulator, `canal-middle` replay, 60 warmup and
300 measured draws per run, no concurrent builds:

| Run | FPS | interval p95 | CPU draw p95 | GPU p95 | maximum interval |
| --- | ---: | ---: | ---: | ---: | ---: |
| routed shader A | 60.01 | 17.255 ms | 5.457 ms | 0.219 ms | 30.239 ms |
| texture fallback | 60.00 | 17.249 ms | 5.152 ms | 0.206 ms | 29.313 ms |
| routed shader B | 60.00 | 17.092 ms | 5.397 ms | 0.200 ms | 23.495 ms |

These runs include stronger arcade foam, prop turbulence and light reflections,
before the smoke/collision extension. They establish actual shader execution;
they do not establish a speedup. Raw CSV, metadata and summaries are in
[effect-shaders-2026-11-16](effect-shaders-2026-11-16/).

The kernel also supports rippling tinted light reflections and dark rolling smoke.
Shared Java supplies light anchors, exhaust transport, ages and palettes. Smoke
uses the existing 384-puff pool and two small fixed cached fallback materials.
Collision spray uses the same foam primitive with shared ballistic paths; a short
impact flash and six heavy-hit spark streaks remain depth sorted and shore clipped.
No fullscreen pass, additional particle pool or physics change is involved.
`--ios-water-textures` disables all these procedural material modes for comparison.

Simulator timing does not prove physical-device headroom or locked 60 FPS; isolated
long frames remain. Android was built locally, but the user requested no phone use.
AGSL runtime, native Android pixels and physical-device timings remain unvalidated.

Final smoke/collision builds repeated the same replay twice: 59.80/60.00 FPS,
CPU draw p95 4.965/4.630 ms, GPU p95 0.243/0.238 ms, maximum interval
33.267/24.673 ms. Final metadata records 295 smoke patches. This fixture is not
a worst-case collision stress test. See optimization.md for checks and limits.
