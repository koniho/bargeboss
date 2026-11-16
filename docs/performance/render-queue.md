# Render-thread queues and surface effects — 2026-11-15

Android's remaining long frames include **active native drawing work and buffer
release waits**. The previous summary omitted `SyncStart - SyncQueued`, hiding time
when the UI had finished recording but was waiting for RenderThread. This round
adds that metric and a short-trace analyzer, simplifies ambient crests, and shares
one shoreline clip across the surface-foam pass.

The practical result is lower drawing cost, not a claim that every hitch is fixed.
Java draw medians improve roughly 12–15%; median native issue-to-swap time after
subtracting the recorded dequeue duration improves about 0.5–1 ms. That remainder
can still include other waits/driver work: it is **not pure CPU or GPU time**.

## Complete warm trace

`out/queue-warm-trace.txt` contains 700,067 of 700,067 written events, with no ring
buffer overwrite. Explicit `BB draw N` markers identify 88 complete game draws,
frames 543–630, spanning 1.99 seconds. RenderThread spent 1,855.54 ms running,
132.34 ms blocked and 2.16 ms runnable. Native path preparation/execution accounts
for substantial work; buffer-release slices reach 12.37 ms.

In the largest captured 38.14 ms gap, Java drawing took 12.29 ms and the UI's
`postAndWait` covered 24.81 ms. During that whole interval, RenderThread was running
for 26.70 ms and blocked for 11.39 ms. These threads overlap; **do not sum their
times**. This trace points to active rendering plus occasional buffer stalls,
rather than scheduler starvation in the captured window.

Detailed tracing adds considerable work and reduces throughput. These durations
are diagnostic, not estimates of ordinary gameplay cost. An initial trace caught
startup frames 0–8 because a readiness poll saw the old process's SAMPLING log;
that capture was excluded. Require a new app PID and its own SAMPLING marker.

## Changes and comparisons

All geometry/animation decisions remain shared Java:

- Ambient wave crests use three segments instead of five. A stable per-cell hash
  retains 55% of their locations, independently of time and camera movement.
  This intentionally reduces background detail. Shore laps, boat wakes, foam,
  collisions, light effects and gameplay are not reduced.
- `VesselFoam.draw` builds one shore mask over the union of visible surface-decal
  bounds. It preserves the existing oldest-first texture/alpha order and restores
  the clip after the pass. Hulls, ropes and airborne splashes retain separate masks.

Untraced barge-arrival replays use 120 warmup and 360 measured draws at the same
1320.3539 × 600 logical dimensions. All comparison checks passed identical replay
lesson/tick sequences and zero camera delta. Builds and tests were kept out of
these measurements. Baselines were repeated after the candidates.

| Variant | Java draw median (ms) | Native issue excluding dequeue median (ms) | Draw interval p95 / max (ms) |
| --- | --- | --- | --- |
| Baseline first | 8.653 | 15.430 | 19.679 / 33.442 |
| Three-segment crests, pass 1 | 7.988 | 14.515 | 20.153 / 29.139 |
| Three-segment crests, pass 2 | 8.057 | 14.759 | 18.373 / 27.896 |
| + 55% retained cells, pass 1 | 7.664 | 14.147 | 19.703 / 42.521 |
| + 55% retained cells, pass 2 | 7.597 | 14.202 | 18.541 / 23.042 |
| Baseline repeated | 8.773 | 15.028 | 20.318 / 32.992 |
| + shared foam clip, pass 1 | 7.585 | 14.487 | 20.124 / 31.229 |
| + shared foam clip, pass 2 | 7.554 | 14.422 | 18.412 / 21.533 |
| Baseline repeated last | 8.854 | 15.441 | 19.788 / 28.217 |

Median polygons fall 2,734 → 1,858.5; clip scopes fall 40.5 → 15. The shared foam
clip removes repeated mask generation/state changes but does not independently
establish a native speedup beyond the wave change. Keep that distinction when
prioritizing further work. Queue-wait medians can increase when Java finishes
sooner; a shorter CPU draw alone does not prove less presentation latency.

The developer replay forces redraws to advance one tick per recorded frame;
production `FrameGate` avoids drawing unchanged simulation states. Treat replay
throughput as workload capacity, not a direct measure of distinct presented game
frames. Remaining pipeline waits need further profiling under production pacing.

## Validation and next candidate

Shared checks and developer fixtures pass. The intentional combined drawing/state
signature is `ad11a073:2:1350`; deliveries and score remain 2 / 1350. Foam/shore
raster checks passed 197,334 assertions; the 32-heading hull/cargo shoreline test
passed 1,439,937 assertions. Native screenshots retain readable wakes, reflections,
smoke and shore boundaries with less ambient ripple clutter. Native iOS execution
is delegated to the concurrent iOS work; no iOS host file changed in this round.

The user proposed **colorized wave stamps**. A small shared grayscale/alpha atlas
with a few shapes, cached color variants and shared shoreline clipping could
replace multiple polygon/path operations with one or two textured quads per
crest. Avoid per-draw bitmap generation or expensive recoloring. Measure the trade
between path preparation and transparent overdraw; vary shape/scale/phase to avoid
obvious repetition. This is recorded for follow-up, not implemented in this change.

Raw comparisons and the app-filtered trace summary are in
[render-queue-2026-11-15](render-queue-2026-11-15/comparison.json).
Cross-agent progress is maintained in [optimization.md](../../optimization.md).
