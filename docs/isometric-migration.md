# Isometric migration: first playable pass

The shared Java camera now uses 45° ground yaw and 35.264° elevation. Physics
continues in the same world coordinates. Android, iOS and desktop receive the
same projected primitives, atlas frames, registered pivots and effect offsets.

Let `q = 1/sqrt(2)` and `depth = 1/sqrt(3)`. The projection is:

```
screenX = left + q * ((x - worldRight) + (y - worldTop) + 320) * scale
screenY = top + q * depth * ((y - worldTop) - (x - worldRight)) * scale
```

The paired inverse maps touch coordinates to the water plane. A 320 × 688 world
board projects into a `1008*q` by `1008*q*depth` bounding box. Default scale is
`min(canvasWidth / boardWidth, (canvasHeight - 104) / boardHeight)`; the box is
centered. Camera follow and docking zoom share this transform. Moving the camera
reprojects both touch anchors, and lasso aiming/flight freezes the camera.
Directional vessel art shifts by 28 of 32 frames (−45°), with no residual bitmap
rotation. Heave offsets cancel horizontal motion and lift sprites vertically;
vessel overlap uses projected waterline depth rather than world X alone.

## Replay baseline review

An isolated comparison of the previous commit and the camera conversion used the
same canonical gesture script with rendering omitted. The first physical
variation occurs at tick 37: a round-tripped pointer X is `282.09418` versus
`282.09424`, and tug heading is `-2.5737233` versus `-2.5737236`. The new inverse
requires arithmetic on both axes. No mass, thrust, damping or collision constants
were changed for this conversion.

The first two deliveries remain at ticks 837 and 1782 with scores 696 and 1440.
Later contact trajectories change delivery timing slightly. Integer contract-age
rounding adds two points at deliveries 4, 7 and 9: the canonical shift still
completes 11 deliveries, with score 8974 instead of 8968. This is a deterministic
input-projection change, not a scoring-rule change. The drawing hash must also
change because projected geometry and the tug atlas changed. `CoreTest` pins the
final merged signature; iOS parity computes and compares JVM and translated
Objective-C signatures rather than using a separate stored constant.

## Coverage and remaining work

Shared checks exercise forward/inverse mapping, equal ground-axis foreshortening,
projected collision-footprint corners, HUD clearance, zoomed tie targets, stable
drag anchors, heave direction, vessel depth order and complete winding-channel
navigation. Tug checks cover all 32 source cells and smoke attachment through
heading interpolation. Bounded players still complete the introductory, bridge
and heavy shifts; repeated runs and 30/144 Hz checks protect determinism.

The approved v5 tug is the first production watercraft in the new style. Barge
and cargo sheets, some scenery, and shore machinery still need replacement with
art authored for the same camera. Their gameplay positions use the new shared
projection, but this pass does not claim that all artwork matches the concept.
Unverified guide-derived feature anchors remain estimates until checked against
the finished sprites; smoke outlet calibration does not imply every light and
tire has been verified.

JVM checks and shared-renderer previews do not establish native pixel identity,
iOS safe-area behavior, physical touch latency or haptics. Android ART/device
installation and macOS J2ObjC/iOS CI results should be reported separately for the
actual merged build. Native UIKit test gestures use the new projection and live
camera metadata, including during follow and zoom.

## Local verification for this pass

`./check-art.sh` passed the same suite on the JVM and Android ART with canonical
signature `b53f5656:11:8974`. The Android APK built, installed through the existing
Shizuku path, and launched successfully. `./desktop.sh --build-only` passed;
`./preview.sh` exported the shared renderer and passed its sprite pixel checks.
Reviewed the overview, winding channel, bridge and 32 smoke headings. Review
images were also copied to Downloads. Native iOS validation is delegated to CI;
these local results are not a claim that its UI tests passed.

## Verified CI build

[Run 43563701732](https://github.com/koniho/bargeboss/actions/runs/43563701732)
passed for commit `7fa87052aff4eaaa7d7be1a2aa174af439f1bd31` on 2026-11-14.
Android and Windows/macOS/Linux desktop builds passed. The JVM and J2ObjC replay
and drawing traces both produced `b53f5656:11:8974`. The iOS simulator passed all
four native UI tests: saved captain progression, the complete interactive
tutorial, lasso/unloading/lifecycle, and missed-lasso retraction without scoring.
The unsigned iOS device build also passed. This verifies that specific commit;
it does not establish physical iPhone touch/haptic behavior or validate later art
changes. The focused development log is `out/ci-43563701732-ios.log` (untracked).
