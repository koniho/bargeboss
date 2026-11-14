# Static-layer baseline and constraints

Baseline revision: `820ea8c`. Source was exported with `git archive` before the next layer-cache and geometry changes, compiled separately in `out/cache-perf-baseline-build`, and run against unchanged production assets. The reusable source snapshot is in `out/cache-perf-baseline-source`. This preserves a reproducible comparison independent of ongoing working-tree edits.

## Controlled fixtures

`ScenePerformanceBenchmark` replays real shared tutorial input to canal entry, canal middle and the close harbor lesson at logical widths 960, 1333 and 1920, height 600. Counts are deterministic. JVM timings are diagnostic and do not predict Android Canvas frame rate; thermal scheduling and concurrent work affect them.

| Width | Scene | Full submissions | Factory sprites | Circles | Ground sprites |
| --- | --- | ---: | ---: | ---: | ---: |
| 960 | Entry | 8,499 | 77 | 1,457 | 182 |
| 960 | Middle | 10,635 | 59 | 1,181 | 147 |
| 960 | Close harbor | 8,641 | 19 | 853 | 52 |
| 1333 | Entry | 10,163 | 98 | 2,164 | 252 |
| 1333 | Middle | 12,398 | 84 | 1,826 | 223 |
| 1333 | Close harbor | 9,819 | 28 | 1,131 | 86 |
| 1920 | Entry | 12,201 | 140 | 3,164 | 373 |
| 1920 | Middle | 14,367 | 129 | 2,730 | 331 |
| 1920 | Close harbor | 11,029 | 45 | 1,725 | 127 |

Raw files: `cache-baseline-counts.csv`, `cache-baseline-raster.txt`, `cache-baseline-bands.csv`, and `cache-baseline-sources.csv`. Actual same-state renderer PNGs are under `out/cache-render-before`. Separate component counts bypass the full renderer's final viewport wrapper, so component totals are diagnostic and must not be summed as if they were the full-scene submission count.

Terrain remains a substantial static cost. At 1333 middle, standalone walls submit 3,011 primitives, ground 604 and roads 277. Factories submit only 84 actual visible sprite commands in the complete scene; animated smoke and glow make up much of their remaining work. Static factory caching alone therefore cannot remove most native draw submissions.

## Native source retention

`RetainedSpriteMemoryAudit` records atlases encountered during title and the three tutorial fixtures, then enumerates every source file each host loads when that atlas is first used. Twenty-five unique cached sources occupy 149.98 MiB at decoded ARGB8888 dimensions. The sixteen 1254-square factory sources account for 95.98 MiB. These figures exclude GPU copies, decoded temporary buffers, cropped-image objects, engine state, and new static layers.

Android already decodes each source once and grades mutable scenery in place. Factory frames refer to the same source bitmap and a source rectangle; the hot path performs no PNG decoding. iOS keeps one graded RGBA buffer and source image per source, then cropped frame views. Desktop holds graded source images and subimage views. Additional scene caches need a separate byte budget and explicit eviction; source retention does not become cheaper just because a scene layer is cached.

## Safe grouping constraints

Factory centers lie on fixed ground-depth planes `(worldY-worldX)/32`. Translation and uniform zoom preserve their order. Equal-depth groups are a useful starting point, but a broad layer must not cross a vessel insertion point or include a foreground building whose opacity can change independently.

The exact planes are numerous: the 98 visible factories at 1333 entry span 48 planes; at 1920 entry, 140 factories span 52 planes. Unioning whole plane bounds wastes transparent gaps. Clipped tight plane unions total 15.9 MiB and 32.1 MiB respectively at logical 1× resolution; individual tight source bounds total only 5.9 MiB and 9.3 MiB. A native 2× raster scale multiplies those pixel costs by four. Spatial chunks or bounded contiguous runs avoid enormous mostly-empty layer textures. Cap total bytes rather than only entry count, and avoid retaining a new set for every fractional camera zoom.

Only 11 of 98 entry factories at 1333 and 14 of 140 at 1920 contain conservative opaque-mask samples projecting onto the water plane. Those counts are a lower bound on potentially eligible foreground buildings, not an exact eligibility algorithm: edge pixels and the vessel's above-water silhouette need separate care. None of the factories in these three normal gameplay snapshots was actually faded. Most source sprites can remain opaque and cache-stable, consistent with the requested rule that back/top scenery never fades.

Keep animated lighting and smoke out of immutable layers. Moving every emitter after a single combined factory image changes occlusion: smoke behind a nearer factory can suddenly paint over its roof. Preserve effect depth order or review any intentional ordering change. A sprite-only cache must also preserve individual source order inside overlapping building groups.

## Validation for the new cache

- Compare uncached and cached images at all nine frozen fixtures, including fractional camera translation, multiple zoom scales and native viewport scaling. Allow only explained compositing/antialias rounding differences.
- Force a boat beneath an eligible foreground building and back out again; foreground opacity must update, while back/top buildings remain opaque. Do not reuse a layer containing stale vessel-dependent alpha.
- Check a stationary camera across changing visual ticks: water, smoke and organic lights continue animating while static-layer rebuild counts remain zero.
- Check long scrolling and docking zoom: bounded cache bytes, finite entry count, and no allocation storm as scale eases. Resize, restart and scene transitions must discard incompatible layers.
- Measure first-build cost separately from warmed hits. Count native replay submissions, not only Java-level layer calls, so a cache that silently redraws everything cannot pass the budget test.
- Compare all three hosts' shared command intent and independently review native interpolation, premultiplied alpha, clipping at tile edges and cache invalidation. Shared Java tests alone cannot establish native performance or identical pixels.
