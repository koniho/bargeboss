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

## Validation on 2026-11-16

The iPhone 17 Pro / iOS 26.5 simulator ran a Release Metal `canal-middle` replay,
60 warmup draws and 300 measured draws, twice with no concurrent build or preview
work. Source baseline was `a662f6d` plus the local Metal and water changes. Raw
CSV, metadata and summaries are in [water-shaders-2026-11-16](water-shaders-2026-11-16/).

| Run | FPS | interval p95 | CPU draw p95 | GPU p95 | maximum interval |
| --- | ---: | ---: | ---: | ---: | ---: |
| shader A | 59.80 | 16.708 ms | 5.432 ms | 0.201 ms | 32.510 ms |
| shader B | 59.99 | 16.702 ms | 5.235 ms | 0.195 ms | 26.241 ms |

These are simulator results, not physical iPhone/Android performance guarantees.
There are still isolated long frames; this does not prove locked 60 FPS or attribute
those gaps to any specific cause. An initial texture comparison used a more
expensive crest fallback; it is not retained as evidence for the final fallback.
Shader A/B use the final shader path; the subsequent fallback-only simplification
does not execute in these runs.

Shared direction/phase/wrapper checks passed (1,752 assertions). The all-heading
shoreline test now uses moving hulls to include bow wakes (1,439,937 assertions).
Detached foam transparency/shoreline checks passed (197,334 assertions). Full
preview exports and native Metal gameplay/title screenshots were reviewed.
JVM/J2ObjC drawing parity and the final shared/Android build results are recorded
in `optimization.md`. The user requested that the connected Android phone not be
used; no Android shader runtime or physical-device pixel/timing validation was
performed in this round. The APK was built locally without replacing a signing key.
