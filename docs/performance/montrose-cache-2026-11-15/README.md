# Montrose cache regression captures

Android captures on the development phone, same native Canvas renderer and assets.
The frame CSV column named `zoom` records actual camera scale, not the gameplay
zoom multiplier. Interval means time since previous draw start. Frozen rows have
no advancing world; live rows include production simulation, camera and effects.

- Baseline: commit4b6237c gameplay plus new debug-only Montrose fixtures.
- Scale-fix: correct display-scale preparation plus exact shore-mask reuse.
- Docks-trim-oldzoom: additionally tight geometry bounds/empty-chunk exclusion;
  frozen override2.15 preserves the old working zoom for comparison.
- Final-live: all fixes and new2× desired Montrose zoom (working-hull fit retained).

No sampling trace overhead was enabled. Warmup60/sample120 for frozen scenes;
120/180 for live. Captures were sequential, not simultaneous with compiler/pixel
proof workloads. This is short-run evidence, not a thermal soak or guarantee of
60FPS at every position/platform. Full-harbor overview still has a large working
set and is not covered by the local-view improvement claim. No iOS device run.

See [summary](summary.json), per-capture frame data/native summaries and the
repository optimization.md for decisions and code-level findings.
