# Full Montrose overview optimization

All native measurements use the iOS26.5 BargeBoss Lighting simulator, Metal,
Release, expanded montrose-overview fixture, 30 warmup and 120 samples. The
baseline is adb0981. Simulator results do not establish real-device performance.

Measured causes and changes:

- Open-lake point queries outside the index grid fell back to every dock.
  Reject points outside the total footprint bounds before querying the grid.
  Broad polygon queries retain their existing conservative fallback.
- Shared scene-kind queries repeatedly loaded object metadata; J2ObjC translated
  those into retain/autorelease traffic. A primitive kind table derives from the
  immutable catalog once. Wave passes also retain their scene flag locally.
- Ripple cells already excluded by the existing detail fade are rejected before
  projection/exposure work. A conservative cell visibility bound precedes wave
  sampling. Ripple density, motion, wave intensity and drawing order are unchanged.
- DockTopLight's packed Long coordinates collided heavily in HashMap. Sampling
  showed collision-tree lookups invoking generic reflection under J2ObjC. Replace
  active-draw map queries with a fixed survey grid built during stage preparation.
  There are63,840 slots,6,336 occupied cells and7,122 polygons. Reference-table
  payload is at most510,720bytes at eight bytes/reference; the construction maps
  are discarded. A262,144-slot guard bounds future survey expansion. Polygons,
  row/column iteration order, falloff and shore blocking remain identical.
- Orbital X/Y displacement shares phase/cosine calculations. Scalar physics and
  paired renderer samples are checked bit-for-bit at all24 directions, powers
  0/1/3 and shoreline/open-lake positions. Smooth interpolation retains endpoint,
  signed-zero and NaN behavior while avoiding general min/max calls.

Intermediate native captures: baseline133.75ms mean, bounds83.75ms, visibility
51.53ms, dock grid50.00ms (CPU35.08ms), paired samples50.00ms (CPU32.69ms).
These guided diagnosis; host validation/build work overlapped some captures.
Final quiet repeats are recorded below separately.

Validation:10,368,000 actual Java2D ripple pixels identical against the original
WaveView at powers0/1/3, three bearings and two ticks. Native intermediate full
frames are identical across all3,162,132 pixels. No production textures changed.
Raw native images/CSVs remain untracked in out/ios-profile; summary JSON retained
here. Full shared checks and final JVM/J2ObjC parity are recorded below.

## Final quiet captures

No builds or tests ran during these captures. Full overview repeats measured
34.44ms /33.47ms mean (29.03/29.88FPS),33.33ms median,49.66/33.40ms p95;
CPU medians29.91/29.65ms. The baseline mean was133.75ms, about four times slower.
Beacon close-up remains60FPS with3.75ms CPU median. Final overview and beacon
frames match their baseline images exactly:0 differing pixels of3,162,132 each.
Full ./check.sh passes and JVM/J2ObjC parity remains da53558f:2:1350.

A cold180-frame overview-to-follow-camera replay (no warmup) averaged37.49ms,
median16.68ms, p95 100.08ms, maximum725.52ms. This is a changing camera and cold
cache test, not a fixed-overview comparison. Initial preparation and newly visible
scenery still hitch; warm overview gains do not establish smooth startup or60FPS
at every scale. Per-frame analytic ripple evaluation remains a substantial warm
cost. No reduction to ripple density or replacement of artwork was made.
All-heading Montrose shoreline regression also passed2,434,414 assertions.

## Android phone validation

Tested revision5631953 on Pixel11 Pro, Android17 (CD1A.261005.003), with the
instrumented APK and existing debug signature. Full build/shared checks passed;
installation used `adb install --no-incremental -r` without clearing app data.
Frozen beach and beacon runs used120 warmup draws and360 samples, with Vulkan
active for every measured draw. Mean intervals were16.67ms and16.65ms (about60FPS);
p95 intervals22.18ms and17.65ms. CPU draw medians7.60ms and6.77ms. Native images
were reviewed: corrugated horn walls and red lattice beacon render, beach planting
is present, and existing lawn tile seams remain visible. These frozen captures do
not validate the beacon's entire flash cycle or interactive touch latency.

**Full overview failed both cold launches.** The initial direct terrain fallback
exceeded VulkanPainter's8,388,608-float vertex limit (32MiB). The logged stack goes
through MontroseView.surface, Ground.paint and VulkanPainter.layer while its
background raster is missing. MainActivity then permanently selects Canvas.
Both processes subsequently exited with native SIGSEGV on RenderThread, with the
first tombstone identifying vendor `vulkan.powervr.so` IMG_vkCmdPipelineBarrier
through HWUI/Skia and a likely stack overflow. No completed overview CSV exists;
there is no valid Android overview FPS claim for this revision. The Java vertex
limit is established; the driver's internal failure mechanism remains unresolved.
This was not corrected as part of the device validation run. Next work should
bound cold terrain fallback/preparation without increasing geometry budgets or
reducing artwork, then repeat cold launches and steady overview measurements.

A360-frame cold inlet camera replay completed: mean18.54ms, median16.45ms,
p95 23.52ms, maximum628.29ms. CPU draw maximum476.92ms. This includes startup
and temporary Canvas draws; replay advances one simulation tick per draw and is
not a live gameplay FPS guarantee. Raw captures, screenshots and failure logs
remain in `out/android-overview-review/`; compact summaries are tracked here.
