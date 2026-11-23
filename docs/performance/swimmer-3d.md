# Montrose continuous 3-D swimmers — 2026-11-22

`SwimmerModel` replaces the mission's sprite frame selection with a shared Java
3-D upper-body skeleton: tapered/octagonal torso, head and cap, shoulder/elbow
segments, four skin palettes and two readable swim colors. All twenty swimmers
have independent deterministic animation phases. Waiting swimmers tread and wave;
secured swimmers lower their arms toward the orange rescue ring over 30 ticks,
then continue bobbing, turning slightly and treading. There is no frozen idle pose.

The model intentionally stays on the existing rescue anchor. Small visual bobbing
and limb motion do not move hit targets or alter buoy range, collision radius,
scoring, mission completion or the surveyed safe-water placements. No gameplay
state is updated by rendering. Animation uses `visualTicks` and `StrictMath`.

Each rescue owns one scratch model, reused serially for visible swimmers. Eighty
bounded face slots hold projection, clip and sorting buffers (about 8 KiB raw array
payload); there is no atlas decode, runtime texture build or pose cache. Rigid
segments are transformed continuously, clipped against z=0, backface culled and
sorted by transformed 3-D depth. Existing actor depth ordering and solid-shore
clips remain in MontroseRescueView. The orange secured ring stays on the water
plane. Water effects add three small primitives per swimmer; existing target cues
remain readable. Submerged limbs are clipped instead of painted over with water.

Validation:

- Existing `MontroseRescueTest`: 77 mission/input/geometry/camera/safety assertions,
  plus 681 existing cruiser-light assertions, pass without changed gameplay.
- `SwimmerModelTest`: 9,600 poses (240 consecutive ticks × 20 swimmers × 2 states)
  all change each tick, remain finite, produce deterministic redraws and leave
  gameplay unchanged. Maximum 32 mesh draw calls per swimmer (640 for all twenty),
  plus three motion-water calls each. View culling occurs before mesh evaluation.
- Warm JVM diagnostic on the development phone: all twenty models to a counting
  painter cost about 0.08–0.20 ms/frame across 1,000 frames. This excludes native draw,
  blending and GPU work; it is not an end-to-end FPS claim.
- `tools/SwimmerReview.java` exports actual DesktopPainter pose details and the
  complete Renderer scene to `out/swimmer-3d-poses.png` and
  `out/swimmer-3d-scene.png`. Reviewed visible distress/secured silhouettes,
  submerged-body cutoff and world-scale placement. Outputs are untracked.

Run after compiling core/desktop/tests and the review tool:

```
java -cp build/swimmer-test com.bargeboss.game.SwimmerModelTest
java -cp build/swimmer-test com.bargeboss.game.MontroseRescueTest
java -Djava.awt.headless=true -cp build/swimmer-test com.bargeboss.game.SwimmerReview
```

Native Android/iOS timing, antialiasing and final device pixels still need combined
integration validation. Root agent owns the Android build/install round. Remove
SWIMMERS from level preparation only once all remaining sprite callers are audited;
the legacy source asset need not be deleted. Future swimmer actions should extend
this skeleton/state input, keeping clipping and effects on the same world anchor.
