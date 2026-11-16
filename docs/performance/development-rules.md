# Performance rules for new development

Target smooth 60 FPS (16.7 ms between frames) while preserving the shared game,
artwork and drawing-order contract. Apply these rules when adding levels, assets,
effects, caches or native drawing primitives. These are defaults grounded in the
development phone's measurements, not guarantees for every device/backend.

## Start with the right asset and cache structure

- Keep directional sheets as authoring assets; draw retained frame-sized textures
  at runtime. One small sprite referencing a whole sheet can force a much larger
  native texture working set. Desktop/iOS already crop frames; Android uses a
  160 MiB cropped-frame cache. At startup, prefetch and retain complete
  watercraft rotation families needed by the upcoming level (including cargo,
  glow, shadow and every lighting variant); remaining space is LRU-managed for other artwork. Preserve all authored cell pixels, transparent margins,
  32 headings, pivots, size corrections and light/smoke anchors. Reuse full-image
  frames. The title sky fade strips deliberately keep their shared source because
  cropping changed filtered edge sampling.
- Bound every added cache and measure both retained CPU storage and native texture
  pressure. Existing Android terrain/UI raster storage is capped at 48 MiB;
  additional frame storage is capped at 160 MiB (complete rotation sets currently
  consume 53 MiB for the tutorial, 86 MiB for the entire catalog). This is CPU bitmap storage, not an Android GPU-residency limit. Eviction must not recycle images
  still referenced by recorded hardware drawing commands. Exercise eviction and
  reload, not just a stationary warm cache.
- Declare upcoming assets in shared `LevelAssets.Plan` manifests, including future
  feeder deliveries and later tutorial phases. Expand each listed watercraft into
  every heading/effect variant with `SpriteAtlas.rotationAtlases(sources)`. Native
  hosts only prepare the supplied list; they must not infer requirements from
  visible objects or choose different sprites. Replace obsolete preparation on a
  level change and unpin old-only frames. Keep shared frames reusable.
- Prepare expensive static content before it becomes visible. Account for likely
  camera movement and zoom/density changes. Split preparation into bounded work,
  or prebuild during loading. Raster workers need private drawing state; bitmap-only
  preparation can share immutable source images/crops with safe publication and
  short cache locks. Never share a live Canvas/Paint or mutate a published bitmap.
  Cancel preparation when its owning painter closes. Do not move shared game mutation to a worker.
- A count limit is not a time budget. The current one-layer-build-per-frame limit
  still permits a single 19 ms raster build. A direct-drawing fallback can add work
  during the same frame. Avoid promising that a cache removes stutter merely
  because its steady-state hits are cheap.
- Keep terrain chunks independently sortable. Do not flatten foreground shores,
  buildings with animated fades, ropes, watercraft or dynamic lighting into one
  cached scene. Follow [drawing order](../rendering/drawing-order.md).

## Keep native rendering affordable

- Fewer draw calls are not automatically faster. Android's compound water paths
  reduced submissions but were slower than separate contours on the measured
  Skia/Vulkan backend. Android skips optional shared grouping and submits paths
  directly; Java2D/Core Graphics retain compound paths. Forward host preferences
  through painter wrappers so core does not pay to group work a host expands
  again. Re-measure before changing a backend's strategy.
- Reject dry/offscreen/transparent water work early. Reuse fixed geometry and
  constants, skip clip planes a polygon cannot cross, and preserve shoreline
  masks and translucent overlap order. Share effect clipping only when the masks,
  bounds and compositing order are equivalent; this remains follow-up work.
- Keep static UI lettering cached. Visible text belongs to UI overlays; do not
  reintroduce world labels or per-glyph polygon work every frame.
- Keep animated effects separate from cached static artwork. Optimize drawing,
  sampling and clipping before reducing smoke density, wake life, lighting,
  rotation frames or physics fidelity. Changes to appearance need explicit review.
- Measure physics before optimizing it: the current replay spends about 0.5 ms
  per simulation tick. Rendering and synchronous preparation dominate this case.

## Verify the improvement, not just the implementation

Use [developer profiling hooks](developer-profiling.md). Keep the phone awake
with `tools/phone-awake.sh on` and restore it with `off`; the capture script manages
its own scope when needed. Keep builds/tests and unrelated work off the phone
while collecting timed samples. Use separate runs for method sampling/pixel review.

1. Compare the same fixture, mode, dimensions, warmup and sample count. Replay
   advances once per recorded draw, not per callback. Reject interrupted runs;
   compare lesson/tick sequences and report any camera differences.
2. Change one factor at a time, and repeat in reverse order. Include cold starts,
   camera movement, new scenery entering view, heading changes and cache pressure.
3. Record mean throughput, median and p95 draw intervals, UI draw time, simulation,
   decode, layer/frame construction, fallbacks and evictions. Include native sync,
   render timing and memory evidence. Do not add pipelined stage times as though
   they were a single serial frame; native issue-to-swap includes more than GPU work.
4. Treat “Slow bitmap uploads” and allocation totals as clues, not proof of GPU
   cache thrashing. `texture_mib` is referenced bitmap storage, not resident GPU
   bytes or transferred bytes. A controlled rendering switch is stronger evidence.
5. Verify native pixels, actual screenshots, depth/clipping, pivots and cache reload
   behavior. Shared trace parity does not establish identical native rasterization.
   Report small sampling differences rather than calling source-preserving changes
   pixel-identical without checking.
6. Retain raw evidence and document the measured device/backend, capture limits,
   selected defaults, rejected options and next unknowns. A faster median does not
   prove locked 60 FPS; do not extrapolate one tutorial capture to every platform.

## Established results and remaining causes

The [texture/path comparison](texture-paths.md) improved this tutorial fixture from
22–25 FPS to 55–60 FPS. Referenced bitmap storage dropped 138.33 → 90.69 MiB;
median native sync fell 7.34–8.21 → 0.42–0.56 ms. Extra cropped-cell storage was
17.19 MiB with no measured evictions. An independent stress check exercised 259
evictions within the 24 MiB limit and reloaded the tug without pixel differences.

The next reproducible spike is **synchronous cached-layer rasterization**. In both
optimized runs, frames 114, 115 and 319 construct large layers during drawing:

| Capture frame | Layer build, passes 1 / 2 | Total UI draw, passes 1 / 2 | Following draw interval, passes 1 / 2 |
| --- | --- | --- | --- |
| 114 | 18.99 / 17.89 ms | 29.63 / 30.52 ms | 36.14 / 44.62 ms |
| 115 | 12.39 / 15.12 ms | 20.73 / 24.92 ms | 23.13 / 30.67 ms |
| 319 | 18.03 / 16.80 ms | 26.53 / 25.74 ms | 33.35 / 34.72 ms |

Each of those builds adds 2.25 MiB, consistent with a 256×256 terrain chunk
rasterized at 3× density. The trace does not yet record layer identity, so the
specific terrain key is an inference. Frames 114–116 also record direct fallback
draws. **This cause is now addressed by [bounded background terrain preparation](terrain-preparation.md)**
on Android. Shared offscreen hints, visible-work priority and layer-key/size
telemetry support verification. Keep the synchronous switch for future comparisons;
preparation must fit the cache budget and retain a complete drawing fallback.

Other long gaps remain unresolved: pass 2 has a 63.18 ms draw-start interval after
a preceding 11.97 ms UI draw, without a source decode or layer build in that draw.
Do not label this GC, thermal throttling or GPU work from these counters alone.
An incomplete native trace now records path processing and render-thread waits;
see [water geometry findings](water-geometry.md). Its buffer overwrite and tracing
overhead prevent attributing every gap. A narrower complete trace is still needed.
Steady native issue-to-swap p95 is still 21–22 ms; repeated effect clipping and
path processing are candidates, not yet isolated causes. Cold source decoding
also remains expensive and should be measured separately from warm gameplay.

Raw input for the table is the two `cell-single-*` frame CSVs in
[texture-paths-2026-11-15](texture-paths-2026-11-15/summary.json). `interval_ms` runs
from the **previous** draw start to the current one; attribute a long draw's effect
to the following interval, rather than correlating only values on the same row.

The [water geometry round](water-geometry.md) removes grouping that Android would
immediately expand and simplifies polygon-bound scans. Median drawing improved
modestly, with unchanged pixels, but interval tails did not improve consistently.
Do not equate less Java work with guaranteed presentation at 60 FPS. Keep native
path processing, effect clipping and scheduler/presentation waits measurable.

## Static-layer preparation and sparse scenery

Pass the actual display scale to `Painter.prepareLayer`, matching the subsequent
`layer` call. Do not pass `StaticLayer.rasterScale(scale)`; native hosts perform
that quantization themselves. Double quantization can continuously prepare3×
images while the visible draw asks for2×, starving the intended cache entries.

Calculate visible layer byte cost including transparent pixels and pending worker
reservations. Skip empty chunks and bound sparse content tightly before raising
cache budgets. A small harbor dock in a large transparent tile still pays for
the entire bitmap. Verify warm frames have no persistent fallbacks/evictions,
then examine native issue-to-swap timing. Keep light kernels shared and allow both
raster buckets when a subtle animation straddles a density threshold.
