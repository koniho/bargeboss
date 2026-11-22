# Figure-eight progress stutter — November 22, 2026

The moving lesson's percentage text was the main source of recurring long draws,
not the circle shader. Each percentage made a new cached image of the whole
instruction sentence. Until it was ready, the renderer submitted all its glyph
polygons again. Nearby offscreen scenery preparation also rebuilt discarded
speculative tiles repeatedly.

Changes:
- Percentage-bearing instruction lines retain fixed words and individual digits.
  Every 0–100% value uses the same 14 layers, with unchanged glyph placement.
- Android retains completed speculative terrain before another frame's neighbor
  requests displace it. The existing 48 MiB layer budget includes worker reservations;
  speculative retention does not evict visible layers.
- Circle shaders, glow, progression, vessel physics and scenery remain unchanged.

## Device comparison

Pixel 11 Pro, Android Vulkan profile APK, `buoy-complete replay`,30 warmup plus 360
measured frames. No builds or method tracing during timing captures. Baseline APK
is 6b7a209; final APK contains the two fixes above. Order: baseline A, intermediate
experiments, final A, baseline B, final B. Baseline B reinstalls the original APK,
so its captured working-tree revision file does not describe that APK's sources.
All four runs match lesson, tick and tug position exactly. Maximum zoom difference
is 0.00000191, from camera settling, below 0.001%.

| Metric | Before A / B | After A / B |
| --- | --- | --- |
| Draw median |8.9 /8.9 ms |8.7 /8.8 ms |
| Draw p 95 |28.8 /29.2 ms |10.3 /10.9 ms |
| Draw-start interval p 95 |33.4 /33.9 ms |21.9 /24.4 ms |
| Mean draw-start interval |19.8 /19.7 ms |16.8 /16.8 ms |
| Intervals above 25 ms |66 /66 |3 /11 |
| Background raster builds |330 /328 |2 /2 |
| Peak retained+pending layer storage |47.22 /47.22 MiB |22.22 /22.22 MiB |

[Structured measurements and local raw capture paths](figure-eight-2026-11-22.json).
Counters show no frame-cache evictions or synchronous layer builds. Native hardware
screenshots were reviewed: rings, soft edges, progress text and scene remain intact.
Full-frame before/after screenshot mean absolute RGB delta is 0.0194/255; small
camera/filtering and separately rasterized text sampling differences remain.
Do not call the native outputs pixel-identical. Shared geometry tests compare
3,904,976 glyph coordinates across 101 values and require exactly 14 reusable keys.

## Rejected experiment and remaining work

Conservative annular GPU meshes reduced empty ring fill but did not improve scene
timing (draw p 95 remained 29.7 ms). That experiment was reverted. Scenery retention
alone reduced builds to 66 but left draw p 95 at 31.7 ms. On that run, frames reporting
completed background builds had median draw 28.54 ms versus 8.25 ms without builds.
Splitting the changing percentage line removed those remaining repeated misses.

A separate ART trace ranks bridge depth rendering 20% inclusive, water 9.8%, and
tutorial UI 5.3%; sampled nested timings overlap and tracing distorts frame times.
Remaining interval spikes require further native scheduling/GPU analysis; this is
not a claim of locked 60 FPS. Android gfxinfo does not cover this direct Vulkan
surface (zero matched recent frames). No physical iOS or desktop performance run
was made; the numeric text optimization is shared, while terrain-worker retention
is Android-specific. Shared gameplay/assets/native encoder checks passed.
