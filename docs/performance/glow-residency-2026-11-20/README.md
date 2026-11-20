# Resident marina glow kernels

Merged origin/main d257bcf before comparison. The baseline includes its beach, weather, water-mask and renderer optimizations. Do not compare the new results directly against the old 217 ms overview and attribute that improvement to this patch.

Shared LevelAssets declares the two immutable marina kernels. The iOS stage-entry host prepares their 2x and 3x raster buckets. Metal protects these four entries from terrain/sprite eviction within the existing total texture-cache budget; reservation is bounded to 1 MiB and actually consumes 402,688 bytes (393.25 KiB). Changing profile clears the reservation; changing stage replaces the manifest. Core Graphics also receives the entry preparation hint.

Pending speculative layer preparation is promoted when the layer becomes visible. The main-thread publication callback checks the current promotion state, allowing eviction of unprotected entries. Generation checks still reject retired-profile jobs. No native game/scene inference or changes to ring geometry, colors, opacity, anchors, animation or drawing order.

## Simulator evidence

Dedicated iPhone 17 Pro, iOS 26.5, Debug Metal; 45 warmup and 60 measured frozen draws. Builds and shared tests stopped during timed runs. Normal budget 160 MiB; pressure test 16 MiB. These are simulator measurements, not physical-device claims.

| Run | Mean interval ms | CPU draw median ms | All layer fallbacks (cumulative) | Resident kernel fallbacks | Vertex capacity MiB |
|---|---:|---:|---:|---:|---:|
| glow-merged-before | 67.22 | 59.09 | 9070 | untracked | 256 |
| glow-after-a | 66.67 | 56.94 | 3129 | 0 | 64 |
| glow-after-b | 66.67 | 56.69 | 3129 | 0 | 64 |
| glow-after-pressure | 150.00 | 128.21 | 26892 | 0 | 64 |
| glow-after-daylight | 16.67 | 4.66 | 117 | 0 | 16 |
| glow-after-night | 16.68 | 4.61 | 118 | 0 | 16 |

All five updated runs retained exactly four kernel textures, 402,688 bytes, with zero resident-kernel fallbacks. The 16 MiB run forced over 11,000 texture evictions while preserving those entries; retained cache bytes stayed within its configured limit. In-flight/frame references can exceed the LRU cache accounting, so this is not a total GPU-memory bound. Promotion counters were positive, including 420 under pressure.

Normal overview vertex capacity fell from 256 MiB to 64 MiB. Mean intervals remain about 67 ms; CPU draw improved only modestly. No 60 FPS overview claim. Main's smaller terrain caches already allow eventual warm kernel residency; this update removes entry fallback work and makes residency reliable under eviction pressure. Remaining wide-scene costs need separate investigation.

The complete dusk overview screenshot is pixel-identical before/after: zero differing pixels across 3,162,132 pixels. Screenshots were visually reviewed with no system overlays. Daylight and night beach captures confirm the newly merged sand/path polygons use the selected profile (their previous fixed dusk call was updated). Generated images remain untracked under out/ios-profile/glow-* and out/lighting-review/.

Validation: full shared/asset checks and signed Android APK build passed, including Vulkan encoding regressions. All-heading Montrose shore regression passed 2,434,414 production hull/cargo pixel assertions plus 32,273 wall pixels. JVM/J2ObjC parity passed b0bbe1a2:2:1350. Shared manifest checks cover the exact bounded kernel footprint and defensive copying; metadata assertions cover all five native captures, zero kernel fallbacks, four resident entries, budget bounds and visible promotion under stress. Android was built but not installed or visually validated in this round.
