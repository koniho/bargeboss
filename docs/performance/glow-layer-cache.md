# Cached scenery glow and terrain review

The shared glow renderer caches its immutable outer shells in small color-specific layers. Full glow uses one layer and four live accents instead of fourteen circle submissions; distant glow uses one layer and a live fixture core instead of four circles. Smoke is unchanged.

The full kernel key is `0x40000000 | RGB`; the distant kernel uses `0x41000000 | RGB`. Both are 44×44 logical units, with at least one pixel of geometry gutter. The existing host density policy makes a kernel about 30 KiB at 2× or 68 KiB at 3×. All positions, radius, pulse energy, breathing, drift, source phase and building opacity remain live draw parameters. The hot fixture core stays fixed on its source anchor. Animated lobe accents remain outside the cache.

The cached group uses a small 3% shell-energy adjustment to preserve warmth after group-opacity compositing. Sixteen actual Java2D comparisons against the frozen `820ea8c` algorithm cover close/distant lights, opacity 1/.28 and four animation ticks. Full-opacity integrated RGB energy is 2.7–7.8% higher. Faded energy ratios range 0.966–1.273, reflecting the previous per-circle integer alpha truncation versus group opacity. The glow remains clearly faded and does not lose its warm impact. Comparison image: [cached glow review](../art-studies/glow-cache-v1/BargeBoss-cached-glow-review-v1.png); raw results: `glow-cache-brightness.csv`.

## Validation

`SceneryEffectsTest` passes 4,847 assertions, including immutable kernel identity across phase/size/opacity changes, distinct cache namespaces, bounded raster geometry, antialias gutters, fixed source attachment, smooth intensity and deterministic smoke. `LayerCounterTest` passes 12 assertions covering cold replay, separate offscreen build work, the one-build-per-frame allowance, warmed hits, changed density keys, invisible layers, LRU byte eviction and oversized fallback.

The profiler now distinguishes `EXPANDED`, `CACHE_COLD` and `CACHE_WARM`. It simulates the actual layer byte budget, density keys, LRU behavior and build allowance. Offscreen construction primitives are reported separately from target blits; cold fallback work stays visible. Translucent oversized multi-tile fallback composites are approximated, so rows with nonzero `cacheFallbacks` must not be described as fully warm native measurements. These are command estimates, not Android FPS measurements.

The tutorial fixtures now include the new basin entry, the actual transition to the channel lesson, canal middle and close harbor at widths 960/1333/1920. Because the basin changes camera poses and factory population, compare expanded and cached rows from the same current fixture rather than treating old-level totals as a pure optimization delta.

| Current fixture | Expanded commands | Warm target commands | Warm cache | Warm-up frames |
| --- | ---: | ---: | ---: | ---: |
| 1333 canal middle | 11,636 | 7,248 | 24.6 MiB | 17 |
| 1920 canal middle | 14,130 | 8,263 | 34.6 MiB | 22 |
| 1333 close harbor | 11,464 | 8,104 | 25.0 MiB | 18 |
| 1920 close harbor | 14,197 | 8,717 | 40.6 MiB | 25 |

All twelve stationary fixture caches settled with zero warm rebuilds/fallbacks. The largest retained cache was 40.646 MiB, below the 48 MiB host cap. Raw results are in `cache-current-counts.csv`. The raster benchmark now warms 80 frames, rather than three, to avoid measuring the one-raster-per-frame population period as steady state.

## Actual terrain and scene comparisons

`TerrainLayerReview` compares the original terrain calls directly against warmed `StaticTerrain` layers at the same current camera and geometry. Across twelve fixtures the mean RGB-channel difference is 0.172–0.548/255; pixels differing by more than 60 summed RGB units occupy 0.028–0.236%. Reviewed comparisons at all three widths retain aligned banks, roads and quay edges without missing chunks or broad seams. Differences follow primitive antialiasing and source-texture resampling. The tool guards mean channel error ≤1/255 and large-change pixels ≤.5%. Raw output: `terrain-layer-difference.csv`; direct/cached/difference PNGs: `out/terrain-layer-review/`.

Actual whole-scene cold-versus-warm captures at the same twelve poses differ by only 0.040–0.263/255 mean channel error, with large changes ≤.12%. Warmed previews are in `out/cache-current-warm`; cold captures are in `out/cache-current-cold`. Selected [canal](../art-studies/glow-cache-v1/BargeBoss-cached-canal-v1.png) and [harbor](../art-studies/glow-cache-v1/BargeBoss-cached-harbor-v1.png) reviews preserve the darker industrial setting, bright vessels and strong factory lighting. Copies were also placed in Downloads.

Native platform checks, cold device hitches, interpolation and real frame timing remain separate from this JVM review. The shared layer contract does not establish identical native pixels or touch latency.
