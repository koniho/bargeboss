# Montrose overview bottlenecks

Compared pre-preset commit 12b881b with lighting commit cb15b0b (01106e8 adds only test records), same iPhone 17 Pro iOS 26.5 simulator, frozen montrose-overview, 45 warmup and 30 measured draws, default dusk, 160 MiB texture budget. Two baseline runs followed by two restored lighting runs, no builds or sampling during these timed runs. Earlier lighting measurements preceded baseline. This baseline still includes the existing dusk grade, lamps and water effects; it is not a renderer with every lighting effect disabled.

| Run | Interval median ms | CPU draw median ms | GPU median ms | Cumulative layer fallbacks | Budget MiB |
|---|---:|---:|---:|---:|---:|
| prelighting-overview-a | 216.69 | 183.99 | 0.67 | 26820 | 160 |
| prelighting-overview-b | 216.69 | 183.62 | 0.61 | 26820 | 160 |
| lighting-overview-after-a | 216.67 | 181.94 | 0.58 | 27024 | 160 |
| lighting-overview-after-b | 216.67 | 182.60 | 0.63 | 27024 | 160 |
| lighting-overview-budget640 | 200.04 | 176.99 | 0.66 | 24781 | 192 |

The branch and baseline are effectively equivalent in steady overview cost in these samples. This does not establish zero overhead at every scene, nor a physical-device guarantee. Branch prewarming adds entry work: cumulative uploads/evictions are 2164/1963 versus baseline 728/527; totals include startup and cannot be interpreted as per-frame uploads. The diagnostic named budget640 was clamped by the existing native maximum to **192 MiB**, not 640. Its modest improvement does not solve the bottleneck.

## Warm CPU sample

Separate 5-second native sample at 2 ms intervals, started 12 seconds after launch, with no build running. 1441 samples under the main-thread display/render call:

- Water drawing (`WaveView.draw`), including shoreline polygon overlap/clipping and temporary array/object churn: 603 samples, about 42%.
- Marina pedestal glow/pool static kernels falling back through `layer` -> `StaticLayer.draw` -> `Kernel.paint` -> polygon triangulation: 269 + 258 samples, about 37%. These are two separate call paths (fixtures and pools), not nested counts.
- Montrose ground-layer draw path: 106 samples, about 7%; 91 descend into `Ground.paint`, rebuilding tile clipping/mapping commands.
- GPU median is only 0.58–0.67 ms; CPU draw is about 182–184 ms. Extra interval time beyond CPU draw is not attributed by this sample.

The initial startup sample overlapped a baseline build and is discarded. The warm raw sample remains untracked at `/tmp/montrose-warm-sample.txt`; the percentages are sampled call-tree shares, not instrumented exclusive durations.

## Cache pressure and source evidence

`StaticLayer.rasterScale` has a minimum density of 2 even when overview camera scale is about 0.215. A reflection-based enumeration of the actual ground layers at a 1333x600 overview found 195 visible chunks needing 207.19 MiB at that density, before sprites/light kernels. This is a representative footprint calculation, not a native exact-residency measurement. The GPU cache is shared and limited to 160 MiB; native captures use about 159.3 MiB. The CPU static-raster cache has a separate 48 MiB limit.

`BBMetalPainter.prepareLayer` queues bounded background work. A missing background-safe layer is immediately expanded through its complete drawing fallback. Cache pressure and pending-job competition therefore propagate into both grass rendering and repeated light-kernel triangulation. The overview reserves 128 MiB vertex capacity in the native metadata, another sign of heavy command generation. Allocation capacity is not per-frame upload size.

## Recommended order

1. Keep small, repeatedly used light/pool kernels resident and give visible preparation appropriate priority. Verify fallback counts reach zero without changing light shapes, density or opacity.
2. Add a bounded lower-resolution raster bucket for distant static terrain, using actual projected/native pixel density. Preserve close-view texture quality, shore clipping and drawing order. Do not merely raise the global cache limit.
3. Reduce water shoreline intersection work and temporary geometry allocation through conservative bounds/culling and reusable buffers. Preserve the accepted ripple floor and clipping output.
4. Re-measure native frames and pixel comparisons after each isolated change. Reassess remaining clip encoding/triangulation costs after cache misses are removed.

No production code, cache limits or artwork were changed in this investigation. The lighting app was restored on the dedicated simulator; no connected phone was used.
