# Water polygon CPU work — 2026-11-15

This round reduces CPU work while retaining the same water geometry, animation,
alpha and shore masks. It is a **modest drawing-time improvement, not a demonstrated
fix for all long frames**. Median barge-arrival drawing fell from roughly 9.1 ms to
8.7 ms; canal drawing fell from 7.57 to 7.13 ms. Frame-interval tails remain variable.

## Changes

- `Painter.prefersPolygonBatches()` lets a host skip optional shared grouping.
  Android's default uses individual native paths, so collecting, searching and
  sorting groups only to expand them again was redundant. Painter wrappers forward
  the preference; Java2D and Core Graphics retain batching. Shared code still owns
  every polygon, projection, clip and animation decision. The direct path keeps
  source order; batches only reorder disjoint antialiased coverage.
- Water clipping and viewport polygon bounds now use ordered comparisons rather
  than repeated float `Math.min` / `Math.max`. The Android method sample showed
  float bit-conversion and min/max overhead among the largest exclusive costs.
  Bounds do not alter vertex data; signed zero is equivalent for these overlap
  comparisons. NaN polygons are rejected before native drawing.
- Keep `BARGEBOSS_PROFILE_WATER_GROUPING=true` as a diagnostic switch for the old
  grouping/expansion route. Compound native paths still request shared grouping.
  `bb_water_review` verifies native wave raster output independently of timing.

Skipping grouping alone did **not** establish a frame-pacing improvement. Its
median drawing difference was only 0.04–0.18 ms across the two comparisons. Keep
that finding separate from the subsequent bounds change; do not credit this round
with the earlier 22–25 to 55–60 FPS texture improvement.

## Device comparisons

Development Android phone, Skia/Vulkan, instrumented APK, no method/system tracing
or builds/tests during measurements. Each replay has 120 warmup and 360 measured
draws. Every comparison passed identical scene, dimensions (1320.3539 × 600),
lesson/tick sequence and camera checks; maximum camera delta was zero.

Grouping comparisons ran grouped → direct → direct → grouped. Bounds comparisons
then ran new/new, followed by the earlier APK again. The canal comparison ran new
then earlier APK. `direct` below means grouping disabled **before** the bounds
change. All runs use frame textures and asynchronous terrain preparation.

| Barge-arrival run | Draw median / p95 (ms) | Interval p95 / max (ms) | Draw-start throughput |
| --- | --- | --- | --- |
| Grouped 1 | 9.178 / 10.764 | 19.067 / 24.655 | 59.80/s |
| Direct 1 | 9.141 / 10.788 | 20.253 / 32.036 | 59.75/s |
| Direct 2 | 8.929 / 11.020 | 20.018 / 32.003 | 59.99/s |
| Grouped 2 | 9.112 / 10.874 | 19.941 / 32.324 | 59.33/s |
| Direct + bounds 1 | 8.686 / 10.212 | 19.472 / 25.893 | 59.81/s |
| Direct + bounds 2 | 8.761 / 10.760 | 19.917 / 31.821 | 59.61/s |
| Earlier direct APK, repeated last | 8.915 / 10.355 | 20.595 / 33.543 | 57.70/s |

| Canal-middle run | Draw median / p95 (ms) | Interval p95 / max (ms) | Draw-start throughput |
| --- | --- | --- | --- |
| Direct + bounds | 7.128 / 9.540 | 20.211 / 31.601 | 66.97/s |
| Earlier direct APK, repeated last | 7.565 / 9.163 | 19.202 / 33.330 | 64.44/s |

The canal's median improves but its p95 does not. Do not infer consistently better
tails from these samples. Draw-start throughput counts app draws, not distinct
presented frames; it may exceed 60 with the device's scheduling. Native recent-frame
issue-to-swap p95 still ranges roughly 18–26 ms, including waits rather than pure
GPU execution. Terrain preparation had zero measured async misses in all runs;
small UI-layer fallbacks remain. Preserve the raw evidence for future comparisons.

## Diagnostic traces and remaining work

A separate 1 ms ART sampling run before these changes reports `WaveView.surface`
at 22.33% inclusive main-thread sampled CPU and `WaterBatchPainter.poly` at 11.13%.
Inclusive entries overlap and must not be summed. The largest exclusive entries
include native path drawing, float min/max and float bit conversions. This
motivated the CPU changes; the untraced replays above establish their practical limit.

A separate `atrace gfx view sched freq idle dalvik` capture shows extensive Skia
path preparation/execution and UI `postAndWait` slices. Its ring buffer overwrote
some events, and detailed path tracing adds substantial overhead. Retain the
app-only summary as diagnostic evidence, **not** an unbiased performance result
or a complete explanation of every missed frame. Ftrace used a different clock:
the captured `trace_event_clock_sync` offset was required to align the CSV window.
The 31.78 ms concurrent GC slice is not a 31.78 ms stop-the-world pause; the captured
`ThreadFlipSuspendAll` slice was 0.013 ms. Other suspensions or scheduler effects
cannot be excluded from an incomplete trace.

Next candidates remain repeated effect clipping/projection and native path work.
Use narrower, shorter system traces or a larger buffer to avoid overwrite before
attributing the remaining long gaps. Changes that combine masks, cache geometry
or replace native paths must still preserve alpha order, foreground-shore clipping
and animation. Reducing wake, smoke or light quality was not needed here.

## Validation and reproduction

- Android software raster: 60,480,000 wave pixels identical between grouped and
  direct paths across eight fixtures, three widths and three phases, repeated
  after the bounds change.
- Java2D: 30,333,600 direct-versus-batched water pixels identical. After changing
  bounds, 10,111,200 pixels in exported water PNGs were also byte-identical to the
  earlier bounds implementation. Viewport edge/alpha/culling pixel checks passed.
- Full preview passed, including 1,439,937 production shoreline pixel assertions
  across all 32 vessel headings. Device screenshots retain wakes, smoke, lighting,
  soft indicators and shore masks. No assets or effect densities changed.
- Shared checks and developer fixtures passed; replay remains `f9fd76a9:2:1350`.
  Native iOS execution was not available on the development phone.

Use the profiling commands in [developer profiling](developer-profiling.md).
Raw CSVs, strategy metadata, app logs, native frame summaries and comparison JSON
are in [water-geometry-2026-11-15](water-geometry-2026-11-15/barge-summary.json).
The captures identify the starting revision plus working changes; the named
`bounds` runs include the ordered-comparison loops and the others precede them.
Systemwide raw tracing stays in ignored local output; only an app-filtered summary
is retained here.
