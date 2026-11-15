# Tutorial barge performance investigation — 2026-11-15

The slowdown is primarily in scene drawing and Android render submission, with a
separate large cold asset-load cost. The measured simulation cost is much smaller.
This round adds measurement/developer hooks; it deliberately does not change
artwork, water animation, lighting, sprite resolution, scenery density or gameplay.

Renderer/gameplay baseline: `983fcef`. Measurements use the same renderer in an
opt-in debug profiling APK on the development phone, at 1320.354×600 logical
canvas size. See [developer hooks](developer-profiling.md) for reproduction and
limitations. Each untraced run below uses 90 warmup draws and 240 sample draws.

## Controlled native measurements

| Scene / mode | Simulation p50 / p95 | Draw submission p50 / p95 | Draw interval p50 / p95 | Warm layer rebuilds / evictions |
| --- | --- | --- | --- | --- |
| Barge arrival / replay | 0.52 / 0.77 ms | 11.20 / 18.77 ms | 31.76 / 50.44 ms | 6 / 0 |
| Barge push / live | 1.28 / 1.65 ms | 12.96 / 15.06 ms | 43.73 / 47.37 ms | 0 / 0 |
| Close harbor / live | 0.80 / 1.63 ms | 10.38 / 16.88 ms | 28.17 / 45.16 ms | 0 / 0 |

Replay does one real fixed step per callback. Live mode includes normal real-time
catch-up, so its simulation milliseconds are not a per-tick comparison. Camera,
wake and vessel movement are different between these scenes; these are baseline
captures, **not before/after performance gains**. Temperature, scheduling and
JIT state also varied. The live push capture releases the pointer on entry;
the arrival replay continues the actual steering/push inputs.

The moving arrival fixture rebuilt six newly encountered layers and had one
fallback as the camera moved. Stationary harbor and push scenes had no warm
fallbacks, decoding or evictions. Their retained layer caches were approximately
21.18 and 7.62 MiB. Raising the existing 48 MiB cache cap is therefore not supported
as the first fix by these captures.

The recent Android frame records correlate with the CSV timestamps, but only the
last 120 measured frames remain in `gfxinfo`. Median issue-draw-to-swap spans were
28.10 ms (arrival), 41.71 ms (push), and 26.25 ms (harbor). These include native
command processing and synchronization; they are not pure GPU shader time. The
push run's whole-run GPU histogram was about 3 ms median/p95. This supports
investigating CPU-side drawing/submission and render-thread work before reducing
image quality. A native system trace remains useful to split command replay from
waiting more precisely.

The initial rendered frames spent roughly 593–1032 ms synchronously decoding
assets, plus layer construction and uploads. These are cold fixture launches;
they do not establish an equivalent stall every time a barge appears during a
continuous play session. Constructor preloads occur before these counters, so
this is not the complete application startup cost.

## Sampled Java/Canvas hotspots

A separate harbor live run used 1 ms ART sampling for 180 measured frames after
120 warmup frames. The trace had no overflow or unmatched stack transitions.
Approximate shares of sampled main-thread CPU:

| Inclusive path | Share |
| --- | ---: |
| Entire renderer | 86.1% |
| Water surface (`WaveView.surface`) | 21.2% |
| Text (`Lettering.draw`) | 12.9% |
| Vessel drawing (`VesselView.draw`) | 10.1% |
| Additional water/effects (`ArcadeView.water`) | 8.2% |
| Bank reflections (`HarborAtmosphere.reflections`) | 7.6% |
| Bridge drawing | 6.4% |
| Frame update callback | 6.1% |
| Factory drawing | 5.8% |

Inclusive paths overlap and must not be added. For example, water-plane line
clipping includes polygon projection and Canvas submission. `CanvasPainter.poly`
accounts for about 24.4% inclusive; native `nDrawPath` alone accounts for 9.7%
exclusive. The native captures submit roughly 3,200–4,400 polygons per frame,
plus hundreds of circles and many line/rectangle calls. The bottleneck is not
explained by the number of barge sprite draws alone.

The earlier sampling run used CSV formatting after each measured draw; the final
hooks instead store preallocated numeric rows and format after the run. Treat the
sampled percentages as hotspot ranking, not exact uninstrumented production
costs. Frame timing rows above come from the final low-overhead untraced hooks.

## Optimization order while retaining the image

1. **Cache unchanged tutorial text and HUD text geometry/raster layers.**
   `Lettering.draw` submits every lit 5×7 glyph cell separately, every frame.
   Instruction text, labels and most HUD values remain constant for long periods.
   Use shared immutable layers with bounded keys, rebuilding only when content or
   relevant color changes. Preserve current authored glyph shapes, placement,
   opacity, clipping and animation. Pixel-compare at all supported scales.
2. **Reduce water geometry and clipping overhead without removing waves.**
   Wave crests are rebuilt as many individual clipped quadrilaterals. Precompute
   cell-invariant noise, use spatial water/viewport rejection before allocating
   geometry, and reuse scratch clipping buffers. Then investigate native batching
   for non-overlapping equal-paint geometry. Preserve current phase, alpha overlap,
   shoreline clipping, wake interaction and every visible crest. Batching must
   not change compositing where translucent shapes overlap.
3. **Cache static foreground shoreline sections at their current depth boundaries.**
   Ground already has cached chunks, but depth-sorted foreground sections are
   still expanded. Cache independently sortable dry sections while preserving
   vessels below lower shore, upper shore behind vessels, building-only fades and
   rope attachment ordering. Do not flatten the complete scene into one image.
4. **Move cold decode/preparation outside active play; investigate lossless crops.**
   The current Android atlas constructor decodes every frame/source of an atlas
   when first requested; requesting factory art can prepare all 16 source images.
   Decode/source-cache work can be staged ahead of entering the harbor. Trimming
   transparent borders with matching source rectangles and pivots can reduce
   retained/uploaded bytes without dropping resolution or directional frames.
   Preserve the 32-heading registration, effect anchors and shading masks.
5. **Measure texture pressure and blend layers before redesigning lighting.**
   The push snapshot reported about 78.6 MiB of image surfaces and roughly
   180 MiB total GPU allocations including scratch buffers. That alone does not
   prove texture thrashing. Warm retained-layer evictions were zero, and moving
   harbor blends were occasional (p95 nine). Keep the dynamic surface shading,
   glows, smoke and wakes while measuring uploads and native render-thread work.

The first two items target measured recurring work. Factory removal, lower sprite
resolution, fewer directional frames and fewer water effects are not necessary
first experiments. Each optimization should be run against the same captured
poses/replays with image comparisons and native timings, keeping the existing
shoreline/foam/depth regression tests.

## Evidence and validation

Raw measured frame CSVs, summary JSON, and sampled hotspot ranking are in
[tutorial-barges-2026-11-15](tutorial-barges-2026-11-15/summary.json). Full native
logs, screenshots and method trace stay under ignored `out/profiles/`.

All eight developer fixtures passed repeatability and single-step checks (17
assertions). The full shared suite passed with replay signature
`b3fcab9e:2:1350`. Instrumented and ordinary Android builds compile; ordinary
builds omit the developer scenario and replay classes and retain a non-debuggable
manifest. Actual device captures use the production renderer. This round does
not add iOS device profiling or establish release/iOS performance.
