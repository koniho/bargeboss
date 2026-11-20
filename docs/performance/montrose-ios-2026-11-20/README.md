# Montrose simulator refinement checks

Host: BargeBoss Lighting simulator, iOS 26.5, Metal, 1206x2622 native capture
(landscape game viewport). These are simulator measurements, not iPhone results.
Baseline source is a61b84d, the expanded beach and corrugated seawall increment.
All comparisons below use Debug, frozen fixtures, 30 warmup and 120 measured
frames. Some host compilation occurred during initial captures, so these are
bottleneck evidence rather than controlled final performance claims.

The wide overview baseline took 649.3 ms mean / 650.0 ms median per frame,
535.5 ms median CPU draw. A live process sample attributed most main-thread work
to WaveView -> WaterEffectsPainter -> MontroseHarbor.paintWater, including repeated
polygon clipping and translated array/autorelease allocations for each ripple.
The close beacon fixture remained near 60 FPS.

Ambient Montrose ripples now install one retained water-only shoreline mask per
pass. The bounded mask follows the same policy as existing wake clipping. The
polygon stroke geometry, density floor, colors, wave phase and order are retained.
The shared painter skips individual shore clipping only under this installed mask.
The cache is owned by WaterSurface, bounded to 2 MiB source/projected float payload,
and invalidated for camera bounds or clip-union capability changes.

After that change, overview mean was 183.3 ms, CPU median 151.2 ms. Metal also
stops asking shared code to build polygon batches that the native host immediately
expands back into individual polygons. Mean then measured 167.9 ms, CPU median
139.0 ms. This remains slow in the Debug simulator. Further point-containment,
translated allocation and wide-scene work remains; this is not a 60 FPS claim.

Native frozen pixel comparison: beacon unchanged across all 3,162,132 pixels;
overview changed 3 pixels, each by one channel unit. Disabling redundant shared
batching changed zero pixels. Existing all-heading shoreline checks passed
2,434,414 assertions. JVM/J2ObjC intermediate shared trace matched 4975c859:2:1350.

The developer iOS host now accepts --ios-live-scene=NAME for interactive fixture
review without restoring or writing career progress. Profiling output also creates
Documents on a fresh simulator install before writing CSV/metadata. The initial
fresh-install capture timed out because this directory did not exist.

Raw screenshots/CSV are untracked under out/ios-profile; retained JSON summaries
and pixel counts are beside this file. Final combined refinement checks follow.

Final combined Release simulator captures: horn16.66ms mean, inlet16.62ms,
beach16.66ms, full expanded overview133.75ms (7.48FPS). Release is not a
like-for-like comparison with the earlier Debug numbers. The wide overview
still needs optimization; local scenes do not establish full-level60FPS.
Final shared/native trace matches da53558f:2:1350. New lighthouse dark-phase
native geometry and corrugated horn faces were visually confirmed. On/off
lighting and both inlet structures were also reviewed in shared-renderer exports.
