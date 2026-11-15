# Prepare terrain before it enters view

Terrain raster construction now runs on a dedicated Android worker. The shared
renderer hints the four nearest chunks outside the viewport; visible misses take
priority. A finished bitmap is consumed by the UI painter only when its layer is
actually drawn. Late or cold results use the existing direct drawing fallback,
so solid land remains visible and correctly occludes watercraft.

`StaticLayer.backgroundSafe()` is an explicit opt-in. Only immutable terrain
command lists opt in; UI lettering retains its existing small synchronous cache.
The worker has its own CanvasPainter, Canvas, Paint objects and source-image cache.
It never accesses a live Game, live painter, mutable camera or gameplay clock.
Commands, raster density, chunk keys, dimensions and source pixels are unchanged.

The worker admits at most four jobs, including queued, active and completed
results. Their bitmap reservations count against the existing **48 MiB layer
budget**, together with retained terrain/UI rasters. Speculation cannot evict
retained layers to admit itself; under pressure, unused speculative jobs are
trimmed before visible cache entries. A visible miss can make space in a full
cache, avoiding a permanent fallback loop. Full-size 3× chunks reserve 2.25 MiB
each. The worker additionally retains about 6 MiB of decoded ground artwork on
the CPU. Speculative bitmaps are not submitted to the GPU before use.

Queue changes and completed-result publication use short synchronized sections;
raster work happens outside the monitor. Native resources are never shared across
painters, no bitmap is recycled while draw commands may reference it, and Activity
destruction closes the worker without waiting on rasterization. Late results from
a closed worker are discarded. Queue timing affects cache readiness only.

The optional `Painter.preparesLayers` / `prepareLayer` methods are cache hints,
not drawing commands. Coordinate wrappers forward them without viewport rejection
(the point is preparing offscreen work). Desktop and iOS retain their synchronous
cache and identical visible geometry; iOS explicitly implements the optional no-op
methods. Native iOS performance is not claimed from the Android measurements.

## Validation

The shared terrain test verifies bounded offscreen hints, immutable commands,
wrapper forwarding, unchanged visible placements and no game/camera mutation.
The replay signature remains `f9fd76a9:2:1350`; the full shared preview suite passes.

`AndroidTerrainReview`, enabled by `--ez bb_terrain_review true` in a profiling APK,
checks pending-result publication, duplicate requests, reservation bounds, visible
priority, progress after filling the cache, and nonblocking shutdown. It also
compares fully warmed synchronous and background versions of four complete native
scenes. **All 3,168,000 pixels match exactly.** Both terrain and UI caches must be
warm before comparison; an early test stopped too soon and detected only UI text
cache differences. That incomplete comparison is excluded.

## Repeatable measurements

Use `BARGEBOSS_PROFILE_ASYNC_LAYERS=false` and `true` with the same profiling
fixture, dimensions and counts. The default is true. Record source decode separately
from background raster work and from synchronous UI-layer construction. Worker
elapsed time is not UI blocking time, and must not be added to UI draw duration.

The added CSV fields are `pending_mib`, `worker_source_mib`, `background_build_ms`,
`background_builds`, `async_misses`, `layer_key`, `layer_width` and `layer_height`.
Layer identity/dimensions identify synchronous builds directly instead of inferring
them from allocation size. `pending_mib` includes completed speculative results,
so a nonzero value does not mean the worker is still busy.

## Device results — 2026-11-15

Three matched pairs use 90 warmup draws and 240 measured draws each. The phone
reported two different landscape viewport widths across the session, so the
comparison tool correctly rejected combining all arrival runs. Each pair below
matches dimensions, lesson/tick sequence and camera scale exactly. Compare within
a row only: the 1243-wide arrival pair ran synchronous then background, the
1320-wide pair ran background then synchronous, and the canal pair ran synchronous
then background. Builds, previews and native pixel review did not overlap measured
windows. Pair 2 also includes the finalized full-cache admission/trim handling;
none of these measured runs exercised layer eviction.

| Replay / logical width | Largest UI draw, sync → background | Largest synchronous layer build | p95 draw interval | Longest draw interval |
| --- | --- | --- | --- | --- |
| Barge arrival / 1243.3334 | 25.83 → 11.55 ms | 17.16 → 0.32 ms | 20.03 → 18.23 ms | 31.10 → 20.28 ms |
| Barge arrival / 1320.3539 | 26.78 → 15.01 ms | 18.81 → 0.31 ms | 21.27 → 20.31 ms | 35.55 → 30.36 ms |
| Canal middle / 1320.3539 | 20.37 → 12.44 ms | 9.79 → 0 ms | 19.40 → 17.99 ms | 40.36 → 33.88 ms |

The remaining sub-millisecond synchronous builds are UI lettering. All background
runs have **zero measured terrain misses/fallbacks**, and no retained-layer cache
evictions. Terrain rasterization still costs time on the worker; the difference
is that it completes before those chunks enter view instead of blocking drawing.
The newly recorded keys confirm that the large synchronous builds are 256×256
terrain chunks, rather than inferring that from the previous allocation sizes.

Completed/preparing terrain reservations peak at **9 MiB**; the worker's decoded
ground source is about **6 MiB**. Retained raster plus reserved bitmap storage stays
below 48 MiB throughout the captures, and the worker does not draw speculative
rasters on the hardware canvas. These are retained-storage limits, not a cap on
all process/GPU memory or in-flight native display lists. In the canal comparison,
median referenced bitmap storage is unchanged at 88.71 MiB in both runs.

The shared replay signature, full preview suite, developer fixture checks, native
pixel review and queue/cache tests pass. This round removes the confirmed terrain
build stalls; it does **not** eliminate every long interval. Native rendering,
scheduling and cold source decoding remain separate follow-ups. The observed
20–34 ms worst background-mode gaps should not be attributed to a cache build
without further native/system tracing. Profiling replay also renders every sample;
its throughput is not a guarantee of normal-gameplay FPS on all devices.

Raw CSVs, native frame stats, metadata, native pixel-test output and separate
per-viewport summaries are in [terrain-preparation-2026-11-15](terrain-preparation-2026-11-15/arrival-1320-summary.json).
