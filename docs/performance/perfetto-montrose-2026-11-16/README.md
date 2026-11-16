# Montrose native rendering trace

Captured 2026-11-16 on the connected Pixel 11 Pro, Android Canvas/Skia Vulkan,
revision 98f288a. Three-second Perfetto capture during montrose-docks replay,
60 warmup / 900 requested samples, BB draw markers enabled. 152 complete draw
markers are available. Raw 48.35 MB trace is local at
`out/android-profile/perfetto-montrose-v1/trace.pftrace`; it is intentionally not
committed because system traces include unrelated process metadata.

## Findings

| Work | Median | p95 | Interpretation |
| --- | ---: | ---: | --- |
| App BB draw wall time | 12.348 ms | 19.026 ms | Includes tracing overhead |
| RenderThread DrawFrames wall time | 18.405 ms | 22.824 ms | Native rendering stage |
| RenderThread DrawFrames running CPU | 18.000 ms | 21.075 ms | Predominantly CPU work, not blocked waits |
| Flush commands running CPU | 13.976 ms | 16.451 ms | Included in DrawFrames; do not add |
| Main-thread postAndWait | 3.803 ms | 9.242 ms | Main thread waits for renderer |
| RenderThread dequeueBuffer | 0.005 ms | 0.013 ms | Mostly negligible; maximum 16.597 ms |

Across 152 frames, RenderThread runs for 2773.77 ms inside 2892.30 ms of DrawFrames.
The flush-command region runs on CPU for 2164.67 of its 2179.97 ms. This is strong
evidence that native command preparation/submission is a sustained bottleneck.
The trace alone does not separate Skia CPU work from Vulkan driver CPU work.

Named path operations include 96,334 AAConvexPathOp slices (205.96 ms total) and
12,738 TriangulatingPathOp slices (39.88 ms total). These do not account for all
flush time. Large numbers of rectangle operations and texture-upload-labeled
slices also occur. Such labels do not prove texture residency churn or establish
actual transfer volume. Do not simply add nested slice totals.

Six waitForBufferRelease slices total 47.46 ms, maximum 16.562 ms. Buffer waits
are intermittent spikes, not the sustained 14 ms flush cost. FrameTimeline records
64 late frames with App Deadline Missed + App Resynced Jitter, 4 additional late
App Deadline Missed frames, and 62 on-time frames with App Resynced Jitter + Buffer
Stuffing. Scheduler/frame-pacing effects coexist with CPU rendering pressure.

GPU attribution remains incomplete: gpu_slice has zero rows and the app HWUI
GPU-duration counter reports zero throughout. GPU-completion worker waits exist
(median not analyzed, average waitForever 13.033 ms), but these are waits and
cannot be treated as individual GPU-pass durations. This capture cannot tell how
much GPU execution belongs specifically to overlapping water, foam, or lighting.
Use controlled effect-family comparisons or a supported GPU profiler before
claiming an overdraw/shader bottleneck.

## Capture quality and next target

The earlier eight-second 32 MiB DISCARD attempt filled after about two seconds and
lost metadata; it was rejected. The retained trace uses a 128 MiB ring plus a
separate 4 MiB metadata/frame buffer. There are no nonzero error-severity,
ftrace-overrun, or lost-event stats and no failed patches. Six service chunks are
reported discarded; retain that caveat rather than claiming every system packet
was kept. Trace Processor reports no data-loss warning for this retained capture.

Tracing emits hundreds of thousands of graphics slices and affects timings.
Use the prior untraced comparisons for performance claims. Main/render/GPU work
is pipelined, so their times must not be summed into a frame interval.

Next target: reduce native effect/path command preparation and submission while
preserving drawing order. A controlled water/foam/light-family comparison will
identify which submission stream dominates; lowering artwork quality is not yet
justified. Buffer pacing needs separate follow-up after reducing this pressure.

Config and SQL are retained beside this report. `cpu.sql` intersects each slice
with Running scheduler intervals, rather than mistaking its full wall time for CPU.
Reference: https://perfetto.dev/docs/data-sources/frametimeline
