# Tight-circle towing trace

2026-11-16, Pixel 11 Pro Android hardware Canvas/Skia Vulkan. Full-throttle
`tow-tight-circle` replay, 600 warmup + 2400 requested samples, graphics markers
on. Three-second trace captured during ongoing replay after wake accumulation.
Raw 23 MB trace remains untracked at out/perfetto-tow/trace.pftrace because system
traces contain unrelated process metadata. Source revision 11b524b.

49 complete RenderThread frames in the retained interval:

| Region | Median wall time | Median running CPU |
| --- | ---: | ---: |
| App BB draw | 12.275 ms | not measured here |
| RenderThread DrawFrames | 57.735 ms | 30.345 ms |
| Flush commands (inside DrawFrames) | 46.874 ms | 20.246 ms |
| QueueSubmit (inside flush) | 31.715 ms | about 4.95 ms mean |
| Main-thread postAndWait | 43.981 ms | wait, not CPU work |
| dequeueBuffer | 0.007 ms | not material |

QueueSubmit spans 1583.78 ms over 50 slices: 1297.82 ms sleeping, 247.50 ms
running, 35.48 ms runnable and 2.55 ms uninterruptible sleep. This is substantial
submission/driver waiting alongside substantial renderer CPU work. It is not
proof that a specific shader or GPU pass takes 31 ms. GPU slice table is empty
and Reported gpu duration remains zero. Main/render work is pipelined and slice
times nest; do not add these numbers together.

The trace records 129,250 FillRectOp slices, 6,365 AAConvexPathOp slices and 2,398
TriangulatingPathOp slices. Texture-upload-labelled slices are numerous but tiny;
labels alone do not prove texture allocation/residency churn. The preceding
untraced full-throttle run had zero sampled frame builds/decodes/evictions.
49 FrameTimeline entries report App Deadline Missed + App Resynced Jitter.
No nonzero error-severity/lost/overrun stats were reported by inspect.sql.

Tracing adds overhead: use the untraced approximately 26 FPS replay for the
baseline performance claim, not this trace's slower intervals. Simulation was
below 1 ms p95 in the untraced capture and physical speed stayed stable. The next
experiment is a matched foam-drawing isolation using developer intent
`--ez bb_skip_foam true`; production defaults retain all effects. This skips only
Painter.waterFoam submissions, not physical wake forces, stamped under-hull rings,
all arc geometry, lighting, smoke or tow physics.

## Matched foam-drawing isolation

Same profiling APK, full-throttle circle, 600 warmup + 900 sample frames, trace
markers off. Run A used `--ez bb_skip_foam false`; run B used `true`. The 1500 rows
match exactly in tick, tug/barge heading and speed, tow count/tension, tug position
and camera zoom. This is a renderer-only comparison, not a change to the route.

| Mode | Mean interval / effective FPS | Median / p95 interval | Median / p95 draw |
| --- | --- | --- | --- |
| Normal foam | 42.044 ms / 23.78 FPS | 51.063 / 58.193 ms | 10.601 / 14.012 ms |
| Foam submissions suppressed | 16.656 ms / 60.04 FPS | 16.656 / 17.050 ms | 5.921 / 7.150 ms |

This strongly isolates Painter.waterFoam rendering as a major contributor in
this turning-tow workload. It does not isolate shader arithmetic, overdraw,
RuntimeShader/driver setup or clip interaction from one another. The test skips
all waterFoam calls, so a narrower bow/propwash/crest mode split is the next useful
measurement. Preserve physical wakes and the accepted visual style when optimizing;
turning effects off is not a production fix. One sequential A/B pair was collected,
not a randomized repeated trial. No 60 FPS headroom claim follows from a 17.05 ms
p95, nor does this establish Montrose performance.

Heading dissolve alone is less strongly implicated: in the earlier 2400-sample
full-throttle run, frames without sprite blends averaged 38.855 ms versus 39.144
ms with blends. This is an observational grouping, not an isolated blend test.

Reproduce the isolation with a profiling APK and this launch, changing only the
last boolean between runs (force-stop the app and remove the old CSV first):

```sh
adb shell am start -W -n com.bargeboss.game/com.bargeboss.game.MainActivity \
  --es bb_scene tow-tight-circle --es bb_mode replay \
  --ei bb_warmup 600 --ei bb_frames 900 --ez bb_skip_foam false
```
