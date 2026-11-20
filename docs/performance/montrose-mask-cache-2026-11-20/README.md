# Montrose shoreline mask cache

The development phone's wide Montrose Vulkan path spent most sampled CPU time
rebuilding a disjoint union of static shoreline triangles for WaterSurface.draw.
The ordinary 32-entry shoreline cache was churned by other masks and camera
changes. Vulkan currently requires disjoint masks; Canvas accepts their union.
This explains a current backend-specific CPU cost, not which historical commit
introduced it, and is not evidence of a slow GPU.

The new shared WaterClip retains one unit-projection mask per WaterSurface with
96 world units of guard band. A camera change only projects its vertices while
that region covers the view. Stored source and projected floats are capped at
2 MiB; oversized masks fall back. It preserves tops, walls, cutouts, and drawing
order. The first build and a view leaving the guard band still do synchronous
work; cold-start/panning spikes remain a follow-up.

This round also removes ambient swell inside the sheltered harbor and rotates
the overview clockwise. The timing comparison therefore measures the complete
increment, not a cache-only switch. Open-water wave rendering remains intact.

## Phone captures

Untraced replay, 120 warmup + 180 measured frames, wide scene
`montrose-overview`, zoom .65. Inlet uses `montrose-inlet`, zoom 1.5. Same phone,
assets and renderer settings, no Vulkan command reuse. Before build is 8c32d62.

| Capture | Mean presented ms | Median presented ms | p95 presented ms | Median CPU draw ms |
| --- | ---: | ---: | ---: | ---: |
| Vulkan before | 87.00 | 83.28 | 91.63 | 81.77 |
| Canvas before | 22.92 | 16.66 | 33.33 | 15.48 |
| Vulkan after | 19.82 | 16.66 | 24.99 | 16.42 |
| Vulkan inlet after | 16.70 | 16.65 | 24.99 | 8.90 |
| Vulkan before, repeated last | 87.28 | 83.29 | 91.63 | 82.35 |

The old-build repeat restored the slow timing after the optimized capture; the
optimized APK was then reinstalled for the user.

Presentation intervals are SurfaceFlinger samples, not UI submission duration.
The improved wide-view mean is about 50 FPS; a 16.7 ms median does not mean locked
60 FPS. Thermal/scheduler variation is not controlled. The separate sampled
trace had no overflow or unmatched transitions; its timings include profiling
overhead and must not be compared directly with these untraced runs.

Raw traces, CSVs and native logs remain under ignored `out/profiles/montrose-*`.
Small timing summaries and the method ranking are retained here. Native wide
and inlet screenshots were reviewed; sheltered water is quiet and the rotated
map retains its markers. No physical iOS-device or new Canvas-after measurement.

## Validation

Full build/checks passed, including 12,648 retained/direct mask coverage probes,
1,113 wave checks, moored fleet checks and overview pixels/zoom/input. The chart
pixel fixture now excludes samples within .75 chart units of a contour: sparse
world-space probes could miss a narrow dock tip after rotation. Interior fill
checks remain strict. All-heading shoreline regression: 1,461,230 assertions.
JVM/J2ObjC drawing/state parity: `c71edb61:2:1350`.
