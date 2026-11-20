# Montrose wave experiment: Android Vulkan

The experiment adds no assets or texture caches. Wave forces run once per 60 Hz
tick, heave uses four hull samples, and waterline points sample the same field.
Visible sea crests have a 512-candidate limit; wakes retain 192 packet slots.

Build: `BARGEBOSS_PROFILE=1 ./build-android.sh`, existing debug signing key.
Captures: `python3 tools/profile-gpu-host.py NAME --scene SCENE --mode replay
--zoom ZOOM --warmup 120 --frames 180`. Inlet uses `montrose-inlet` at 1.5;
wide uses `montrose-overview` at 0.65. These are fixed scenario cameras, not a
claim about every camera reached through normal play. Command reuse is off.

Wide comparison order: wave build, original HEAD core, wave build again. The
baseline uses the same Android/native renderer, resources, assets and profile
harness, with the archived pre-wave core/tests from commit `1057b8f`. Captures
use identical fixtures and frame counts. State will differ slightly because
wave forces intentionally change boat motion. No instrumented method tracing or
other phone workload was introduced. Two post-install activity-launch races
were rejected by the harness and retried; their partial captures are excluded.

Per-run folders retain the frame CSV, native timing log, presentation timestamps
and summary. `draw_ms` measures CPU scene submission; compositor intervals are
reported separately. Neither medians nor this short sequential comparison prove
steady 60 FPS or isolate the causes of wide-scene cost. The baseline already
has a large CPU draw cost. Temperature/scheduler variation is not controlled.

The default Vulkan inlet rendering was inspected directly. Shared JVM/J2ObjC
trace equality is not iOS GPU/pixel/performance validation. Canvas fallback and
other devices were not profiled in this round. Generated screenshots and desktop
review sequences remain untracked in `out/montrose-waves/`.

| Capture | Mean displayed ms | Median displayed ms | p95 displayed ms | Median CPU draw ms |
| --- | ---: | ---: | ---: | ---: |
| inlet | 17.31 | 16.65 | 24.97 | 10.90 |
| wide | 85.88 | 83.28 | 91.63 | 81.80 |
| wide-before | 89.94 | 91.61 | 99.94 | 87.41 |
| wide-repeat | 92.50 | 91.61 | 99.95 | 88.65 |

Wide before/after/repeat variability overlaps; this does not establish a wave
regression or improvement. All measured frames used Vulkan without fallback.
