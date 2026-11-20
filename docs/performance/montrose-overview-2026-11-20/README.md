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
