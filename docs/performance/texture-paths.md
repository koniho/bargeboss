# Android texture residency and water paths — 2026-11-15

The tutorial barge-arrival replay improved from **22–25 FPS to 55–60 FPS** across
two passes on the development phone. The two changes are cropped frame textures
and individual native water paths. This is a measured improvement in this fixture,
not a claim of locked 60 FPS everywhere: optimized p95 draw intervals remain
21–27 ms, and cold source decoding still stalls.

## What changed

Android previously referenced an entire sprite-sheet bitmap even when drawing
one small directional cell. It now lazily retains exact cell copies in a
**24 MiB LRU**, shared by ordinary sprites, affine foam/shadows and heading blends.
The copies preserve every source pixel and transparent margin. Shared atlas
rectangles, 32 headings, pivots, registration, light/smoke anchors and depth rules
are untouched. Full-image frames reuse their existing source. Evictions release
references without recycling bitmaps that hardware display lists can still use.
Desktop and iOS already use cropped frames; their renderer implementations remain
unchanged. The title sky's thin adjacent fade slices keep their original shared
texture, preserving filtered samples along strip edges.

Android now expands a shared water polygon batch into its individual native
paths. The exact same contours, colors and ordering are retained. Fewer calls
with larger compound paths were slower on this phone's Skia/Vulkan backend.
Java2D and Core Graphics retain their compound-path implementations; the Android
debug switch remains available for measurements on future devices.

## Controlled comparison

Each capture uses `barge-arrival replay`, 90 warmup draws and 240 measured draws,
with a 1320.3539 × 600 logical canvas. All eight captures have matching lesson and
tick sequences. Six have identical zoom sequences; two differ by at most **0.135%**
from the reference. This is reported by the comparison tool, not silently treated
as bit-identical state. Reverse-order repetitions reduce dependence on run order;
thermal/JIT/scheduling variation still remains. A build overlapped the first
whole-sheet/individual-path warmup, but finished before its measured window;
the reverse pass had no concurrent build. No profiling session included the
expensive pixel-review worker.

FPS below is **1000 / mean draw interval**. These debug captures exercise render
throughput; normal gameplay still uses the shared fixed 60 Hz simulation.

| Texture storage / native water paths | Pass 1 FPS | Pass 2 FPS | Median draw interval, passes 1 / 2 | p95 interval, passes 1 / 2 |
| --- | ---: | ---: | --- | --- |
| Whole sheets / compound (previous default) | 22.14 | 24.53 | 46.35 / 41.89 ms | 55.18 / 50.62 ms |
| Whole sheets / individual | 26.19 | 21.93 | 39.24 / 47.09 ms | 47.12 / 54.78 ms |
| Cropped cells / compound | 45.61 | 41.23 | 21.55 / 24.46 ms | 29.17 / 29.59 ms |
| **Cropped cells / individual (new default)** | **60.26** | **54.77** | **16.60 / 17.02 ms** | **20.97 / 27.02 ms** |

The median unique bitmap working-set estimate drops **138.33 → 90.69 MiB**.
This counter totals bitmaps referenced during a draw; it is not a measurement of
GPU residency or transferred bytes. Additional retained cell storage is 17.19 MiB
in the replay, with **zero measured cell-cache evictions**. Decoded source storage
is still about 170.73 MiB; the optimization intentionally trades a bounded amount
of extra CPU storage for smaller textures presented to the native renderer.

For previous versus new defaults, median native synchronization falls from
**7.34–8.21 ms to 0.42–0.56 ms**. Android's whole-run “Slow bitmap uploads” counter
falls from **199–213 to 8–12 frames**, including warmup. Treat that label as a
sync-stage warning, not a byte-transfer measurement: the AOSP
[JankTracker](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/libs/hwui/JankTracker.cpp)
classifies slow synchronization using `SyncStart` to `IssueDrawCommandsStart`.
The controlled texture switch is stronger evidence for the resource bottleneck
than the counter or GPU allocation total alone.

Raw frame CSVs, retained native frame stats, metadata and the reproducible summary
are in [texture-paths-2026-11-15](texture-paths-2026-11-15/summary.json). Preliminary
experiments with per-callback replay advancement or incomplete captures are
excluded. The replay hook now advances once per recorded draw, so extra callbacks
cannot skip compared simulation ticks. The two passes differ only in debug review
support, defaults explicitly overridden by the same switches, and the title-only
sky sampling exception; the measured tutorial render paths are the same.

## Visual and cache validation

- Android software-raster review covers **1,223 atlas/frame cases and 240,451,584
  pixels**, including rotated sprites, filtered affine stamps and partial heading
  blends. There are 1,659 differing pixels, concentrated at sampling boundaries;
  a few stress cases differ by up to 68 channel levels. We do not claim exact
  native raster identity merely because source pixels are unchanged.
- Four full tutorial scenes plus the title compare **3,960,000 pixels**. Only
  **41 pixels differ**, with maximum channel error **2/255**. The title matches
  exactly. Gameplay and title images were visually reviewed, as was an actual
  hardware screenshot from the optimized replay.
- Cache stress reaches exactly **24 MiB**, exercises **259 evictions**, and reloads
  the first tug frame with **zero pixel differences** against a fresh cell copy.
- The shared rules/replay checks and developer fixture checks pass. The gameplay
  and trace signature stays `f9fd76a9:2:1350`.
- A normal Android APK is built and installed after profiling. Native iOS timing
  and raster behavior are not measured here; cross-platform CI remains the gate
  for translated rules and native builds.

## Remaining work toward consistently meeting 16.7 ms

The next targets are long-frame spikes rather than simulation speed: source decode
and graded factory loading at transitions, native water/effect path processing,
and repeated shoreline clipping around foam. Preserve the cropped-texture gain
when evaluating static shoreline caches; extra raster textures can reintroduce
resource pressure. Measure end-to-end intervals as well as UI draw time. A new GPU
backend is not required to obtain the improvement measured in this round.
