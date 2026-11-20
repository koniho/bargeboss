# Wide tutorial: static shore paths, not just factories

Measured on Pixel 11 Pro, Android Skia/Vulkan, 2026-11-19. Source baseline
`5ff02b7`; the initial fetch/merge was already up to date. Profile fixture is
`barge-push`, frozen at tutorial tick 8509, lesson 6 (displayed as lesson 7).
Player pinch minimum is zoom 0.65. At this device/layout that produces reported
camera scale 0.7834479. The close reference reports scale 2.5593519.

An exploratory 0.25 override is outside normal player zoom limits. It produced
extreme delays, but was interrupted and is not included as a completed app CSV.
The subsequent 0.65 measurements reproduce the player's problem within limits.

## Isolation

Two sweeps alternate normal rendering with eight omissions in 45-frame blocks.
The second sweep includes reverse order. Ignore the first eight frames of each
block and associate draw work with the following draw interval. These are
marginal whole-scene changes, not additive GPU costs.

| Rendering | First sweep median interval | Second sweep median interval |
| --- | ---: | ---: |
| Normal wide tutorial | 68.8 ms | 67.9 ms |
| Omit factory artwork | 60.5 ms | 55.2 ms |
| Omit factory art, smoke and lights | 62.3 ms | 63.3 ms |
| Omit ambient water | 67.1 ms | 66.7 ms |
| Omit shore geometry | 33.8 ms | 31.3 ms |
| Omit foam submission | 60.6 ms | 66.4 ms |
| Flat surface materials | 54.1 ms | 48.1 ms |

Warm terrain/frame builds, fallbacks and evictions were zero. Increasing the
raster cache cannot explain/fix this warm-case bottleneck. The close reference
was near 16.65 ms intervals with about 7.9 ms shared/native drawing submission.

A separate CPU sampling run is retained in `method-hotspots.txt`. Its overhead
means its frame intervals are not used for the A/B result. About 31% of sampled
main-thread time was under BridgeView.draw, and about 26% under the native
surface material path adapter. Uniform updates, path submission and affine
matrix setup are prominent. Inclusive percentages overlap and must not be added.
The factory-sprite result from the earlier dense scene does not generalize to
this sparse-factory tutorial.

## Change

On API 34+, record static terrain surface polygons into immutable triangle meshes
inside the existing independently ordered retained chunks. Reuse the existing
surface shader, material UVs, colors and wetness. Flush at every clip boundary;
keep nonopaque/emissive cases on their original path. Keep roads/cap markings in
their existing raster pass. No gameplay, physics or shore geometry is removed.
Older Android and shader-construction failures retain the existing path backend.

Mesh construction happens when a retained chunk is first recorded, not on every
draw. Track vertex storage as well as native command storage within the existing
8 MiB / 96-entry retained-surface bound. The renderer keeps recorded resources
alive across LRU eviction. Native lifecycle checks include oversized mesh charges.

## Paired result

Both runs use 120 warmup frames and 300 frozen samples at zoom 0.65, with all
scene contributors present. Mesh ran first; paths ran immediately afterward.

| Metric | Path baseline | Static shore meshes |
| --- | ---: | ---: |
| Median draw interval | 50.16 ms | 31.27 ms |
| p95 draw interval | 59.67 ms | 36.53 ms |
| Median drawing submission | 22.55 ms | 21.82 ms |
| p95 drawing submission | 27.04 ms | 23.93 ms |
| Retained surface storage | 0.453 MiB | 1.008 MiB |
| Warm layer rebuilds/fallbacks | 0 | 0 |

The paired result is more conservative than comparing against the earlier
67–69 ms sweep baseline. Do not claim a universal 2x improvement or locked 60 FPS.
Large interval improvement with a smaller submission change supports a native
replay-cost reduction; it is not a direct measurement of pure GPU execution.

At identical scene state, 3.219% of screenshot pixels changed at all; only 0.070%
changed by more than 8/255 in any RGB channel, and none by more than 32/255.
Mean maximum-channel error was 0.096/255. Visual review preserved shoreline cuts,
material scale, roads, vessels and order. Mesh/path edge rasterization is not
pixel-identical. Full-resolution images and native timing dumps remain in
`out/profiles/tutorial-wide-20261119/`; compressed raw app CSVs accompany this
report. No iOS rendering changes are included.

Enabled-build repeat (300 samples): median interval 32.36 ms, p95 39.61 ms;
native retained-cache lifecycle checks completed before sampling.

Remaining target: live bridge/machinery surface submission and other water/effect
work. This fixture still runs around 30 FPS with meshes; it does not meet the
60 FPS with headroom target, and freeze does not measure live simulation or
camera-motion behavior.

Close-view enabled-build check (manual zoom 2.123, near the automatic reference):
300 samples, median interval 16.65 ms, p95 18.26 ms. Normal play was relaunched
and the phone's previous display timeout restored after captures. This checks
steady rendering, not input latency or camera-motion cache churn.
