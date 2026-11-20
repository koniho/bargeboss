# Live machinery surface submission — 2026-11-19

Android Pixel 11 Pro, Skia Vulkan. Frozen `barge-push` tutorial tick 8509,
lesson index 6, minimum player pinch multiplier .65 (camera scale .7834479).
Static shore meshes enabled in both variants. No artwork, geometry, effects or
camera simplification. Android API 34+ batches opaque articulated material faces
within each ordered draw range; other primitives and clip boundaries flush.
Fades and emissive surfaces retain their existing path rendering.

## Controlled comparison

120 warmup frames followed by 480 samples in one process, eight 60-frame blocks:
path, mesh, mesh, path, path, mesh, mesh, path. Discard first eight frames of each
block to reduce transition/pipeline effects. 208 retained samples per variant.
`live-bulk-sweep.csv.gz` includes all raw frames, including discarded samples.

| Renderer | Median interval | p95 interval | Median CPU draw | p95 CPU draw |
| --- | ---: | ---: | ---: | ---: |
| Paths | 32.04 ms | 36.17 ms | 25.30 ms | 27.46 ms |
| Mesh | 24.00 ms | 33.79 ms | 17.27 ms | 18.75 ms |

Block median intervals: 28.84, 25.01, 25.11, 31.85, 32.93, 22.52, 23.22,
32.62 ms. Both halves favor meshes. This is about 42 FPS by median interval,
not locked 60 FPS. Draw intervals are not GPU-only timing. A separate preliminary
pair measured 48.51 vs 33.87 ms; thermal/warmup variation makes that less useful
than the within-process comparison above.

## Cost and correctness

Two bounded staging helpers (static and live) each retain a 16380-vertex direct
buffer and equally sized heap float array: about 2.5 MiB combined, compared with
about .625 MiB before this round. Heap staging permits one bulk buffer copy per
flush. Live meshes are transient frame commands, not an unbounded pose cache.
Existing retained shore storage remains about 1.008 MiB, bounded to 8 MiB/96 entries.

Native frozen screenshot versus the previous path rendering: .684% pixels differ
at all, .215% exceed 8/255 channel error, .007% exceed 32/255; mean maximum-channel
error .051/255. Material appearance, bridge shape and shore clipping reviewed;
triangle-edge antialiasing is not pixel-identical. Shared scope tests cover wrapper
forwarding, moving model poses and cleanup on failure. Full Android checks and
JVM/J2ObjC parity passed (266553ab:2:1350).

The iOS Metal renderer already appends these surfaces as ordered triangles.
Its new scope hooks are no-ops; no equivalent iOS speedup is claimed. No fresh
native iPhone timing or raster comparison was performed in this round.

## Reproduction

Use a developer Android build and launch with `bb_scene=barge-push`,
`bb_mode=freeze`, `bb_camera_zoom=.65`, `bb_warmup=120`, `bb_frames=480`,
`bb_live_surface_sweep=true`. CSV adds `live_surface_mesh` to identify each variant.
For standalone comparisons use `bb_live_surface_mesh=true/false`. Production
API 34+ defaults to mesh when shader creation succeeds; older devices retain paths.

Final enabled-build smoke checks: normal-zoom frozen median interval 17.63 ms,
p95 27.21 ms; CPU draw 12.53 ms. A 300-frame moving replay at .65 zoom also
completed. These are smoke checks, not paired comparisons or a 60 FPS guarantee.
