# Montrose travelift lowering

Shared Java owns operator input, clearance, a 240-tick continuous hoist phase,
webbing transforms, smoke emission and water-contact events. The two straps share
one prepared moving topology and reusable pose. Chassis partitions use fixed
prepared geometry. Four hoist cables use the same exact block anchors. The shore
mask is prepared once per visible frame and reused across depth slices. Ripples
use the existing texture in a fixed 32-event pool; exhaust uses the existing
384-puff pool. No new textures, per-phase model caches or native gameplay were added.

## Validation

- Full `check.sh` through `BARGEBOSS_PROFILE=1 ./build-android.sh`: passed, including
  asset, normal, shadow, terrain/chart/portrait and Vulkan transport checks.
- 763 focused travelift checks: explicit radio authorization and standby, canceled
  gestures, refusal, confirmation-time recheck, mid-motion obstruction, retained
  and NPC hulls, all 240 motion ticks, exact fractional/reversed transforms, bounded
  effects, water-contact timing and lifecycle pause.
- Public-input tow/release/push voyage passes at 960x600 and 1333x600 in 1464 ticks;
  1280x720 in 1427 ticks. All finish with hull100, retained disabled boat, reward
  recorded once and a new Dockmaster call for the yacht job.
- JVM/J2ObjC complete gameplay/drawing trace: `dd774209:2:1350`.
- 1,756,523 all-heading production hull/cargo shoreline checks at the well/quay;
  38,121 cached training-wall pixel checks.
- Actual shared-renderer review at raised, intermediate, water-contact and lowered
  phases; corrected webbing overdraw across the foreground quay. Native Metal
  simulator water-contact image confirms ripple placement, exhaust and masking.
- Signed physical iPhone build passed. Installation on the connected iPhone11ProMax
  succeeded after refreshing its development profile. Launch initially blocked by
  the locked device. Simulator remains the development target.
- Android APK installed successfully using the existing debug key. The profiling
  launch was delivered to an already-running topmost activity, so it did not create
  the requested session/CSV. No new Android timing result is claimed. Stopped the
  pending capture and restored the pre-existing display timeout value (2147483647).

## Native simulator timing

Dedicated iPhone17Pro/iOS26.5 simulator, Metal, Debug profile build. CSVs include
startup and the real scene; these are functional captures, not matched optimization
benchmarks. Desktop checks/builds overlapped. They do not establish iPhone-device
performance or 60fps equivalence to a scene without the lift.

| Capture | Frames | Interval median | Interval p95 | Draw median | Draw p95 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Lowering, replay, lighting enabled, no warmup | 240 | 25.7154 ms | 40.9880 ms | 21.4360 ms | 23.6667 ms |
| Water-contact pose, lighting disabled, 30 warmup | 60 | 18.8831 ms | 20.3175 ms | 16.3913 ms | 16.7425 ms |

Motion capture includes camera movement and the replay continuing to the boat
once the straps reach the bottom. The frozen water-contact capture is phase175;
it does not measure moving transforms. Native screenshots remain under ignored
`out/ios-profile/lift-motion-v1` and `lift-water-v1`. The final changes after these
captures were approach assistance and offscreen shore-mask rejection; the full
translated parity check and device build include those changes.
