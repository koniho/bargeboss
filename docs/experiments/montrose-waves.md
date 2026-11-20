# Montrose waves, first experiment

Shared Java now gives waves a visible crest, signed surface height and horizontal
orbital flow. `SeaWaves` owns the ambient field; `WaterSurface` adds bounded vessel
wake packets. This is a gameplay approximation, not a hydrodynamic solver.

## Wave field

Montrose has two directional components: amplitudes 2.4 and 0.8 sprite-rise units,
wavelengths 220 and 95 world units, periods 3.4 and 2.1 seconds. They travel toward
world -Y, with a slight crossing direction on the second component. Parameters
are authored game values, not surveyed Lake Michigan conditions. These units do
not establish a physically calibrated gravity/dispersion relation.

Height and horizontal orbital flow share a phase. The rendered strips lie at
that phase's positive crests. Crest opacity has spatial variation, but its radius
or propagation phase does not jitter independently of physics. Rendering reads
fixed-step time and never advances it. Other stages retain quiet ambient water.

Exposure is an authored smooth field in Montrose source-map coordinates. It reaches
full strength on the outer lake, reduces around the inlet, and leaves 12% residual
chop inside shelter. It is deliberately approximate: no bathymetry, diffraction,
reflections or wind-direction-dependent shelter. Water/shore clips still use the
real harbor geometry, including docks.

Wake packets now share their radius (9 + age * 0.55), rear-facing fan, signed
crest/trough profile, lifetime and shoreline visibility across height, flow,
shore response and rendering. The profile goes smoothly to zero 26 units either
side of the crest. Wake sampling rejects out-of-range packets before distance or
shore checks. A packet behind a solid shore cannot lift a boat through it.

Buoys and moored boats consume this field instead of separate ambient formulas.
Buoy artwork no longer evaluates an artificial ten-tick delay against wake
packets of the current age. Hull waterline points sample the same surface rather
than adding an unrelated animated ripple. Vessel wakes do not drive their own
source hull.

## Vessel response

Bow, stern, port and starboard samples supply average heave and water-relative
forces. Filtered heave responds faster on small cruisers and more slowly on
barges/yachts. Keel-aligned and broadside drag contributions differ; forces at
sample offsets also produce bounded yaw torque. This first experiment changes
heave, translation and yaw only. No painted sprite is tilted to fake pitch/roll.

Wave-induced linear acceleration is capped at 5 world units/s² per axis and yaw
acceleration at 0.12 radians/s². These are additional orbital drag contributions;
ordinary drag, thrust, collision and tow constraints retain their existing paths.
Secured/pickup hulls do not receive free-body wave forces.

Packet age advances once at the start of each real 60 Hz game tick. Wave forces
are applied once, before the two existing motion integrations; heave is sampled
after movement. Buoy and moored-fleet constraints retain their existing integration
cadence. All consumers use that tick's field, independent of host draw frequency.
Newly emitted packets become available after that tick's hull response.

## Rendering and cost

No new images, shaders, textures or raster caches. Crests use existing shared
`Painter.waterFoam` inside the water-plane shore clip, below vessels. Visible
camera bounds limit work; a hard cap permits at most 512 candidate strips per
draw across both wave components. Sampling and force integration allocate no
objects. The wake pool retains its existing 192-packet limit.

`tools/MontroseWaveReview.java` exports actual-renderer sequences for the same tow
in head seas, beam seas, the entrance and sheltered water. Outputs belong in the
untracked `out/montrose-waves/` directory. Wave tests cover signed crest/return
flow, rear fan, shore blocking, smooth shelter, heading response, secured hulls,
repeatable host cadence and background pause.

## Follow-up

Tune the experiment on-device before increasing force or amplitude. True pitch
and roll require geometry-backed vessel poses with matching cargo, lamps, tow
anchors and shadows. A more accurate shelter model should be justified by a
specific entrance behavior rather than adding a full fluid simulation.

## Validation, 2026-11-20

- Full `check.sh` passed through the signed Android build, including 863 wave
  assertions, the playable Montrose tow/rescue checks and 490,273 moored-fleet
  placement/tether assertions.
- JVM/J2ObjC gameplay and drawing signature: `be7ee63a:2:1350`.
- All-heading production hull/cargo shoreline pixel regression: 1,461,230
  assertions; cached wall coverage also passed.
- Actual-renderer lake/head, lake/beam, entrance and shelter sequences reviewed;
  Android Vulkan inlet pixels reviewed on the development phone.
- `preview.sh` stops at its existing 9,000-tick tutorial-completion guard. The
  unchanged HEAD fixture also remained at lesson 5 after 9,000 ticks. Targeted
  wave sequences and shoreline tests were run separately.
- Native timings and reproduction details are in
  [the performance record](../performance/montrose-waves-2026-11-20/README.md).
  Wide Montrose remains expensive in the pre-wave baseline as well as this build.
  No claim of sustained 60 FPS, iOS-device parity, or improved rendering speed.
- The phone APK was built and checked before concurrent instructor-radio tutorial
  edits appeared in the working tree; those edits are outside this increment.

### Combined tutorial integration

At the user's request, the final normal APK also includes the current instructor
radio/skip flow, physical instructor contact, edge-aligned reply buttons and
synthesized splash/fender audio. The radio replay helper now taps the already
positioned choice center directly, fixing its missed response at 960×720.

Full checks passed on this combined source, including 189 instructor-radio
assertions and the continuous tutorial at all three tested canvas sizes.
Isolated JVM/J2ObjC parity matched `faf08a7e:2:1350`; all core files were checked
against that validation snapshot before installation. The normal signed Android
APK was installed and launched on the phone. The earlier wave performance
captures precede this integration and are not measurements of the final tutorial.

### Live wave controls and surface response

The Android developer build (`BARGEBOSS_PROFILE=1`) exposes **WAVES** in Montrose.
Open it to change power from 0 to 3× in 0.25 steps and propagation bearing in
15-degree steps. Reset restores 1× / 0 degrees. These experiment settings last
for the session and do not alter saved career progress. Normal release builds
hide the panel. The shared Java implementation owns its rendering and input.

Crests are now bent and broken into shorter sections. Power increases their
height, width and contrast, plus ambient ripple density and hull-ring spread,
count and contrast. Hull rings and expanding wake strokes follow the local
surface height and tilt. Ambient detail moves with analytic orbital displacement.
Wake packet centers themselves are still fixed at emission positions; horizontal
advection of wake positions and their force field is not implemented.

Free hulls sample wave flow at bow, stern and both sides. Broadside resistance
exceeds longitudinal resistance; unequal forces produce yaw torque. The existing
bounded force integrator now consumes the adjusted direction/intensity and bent
surface normal. Secured gameplay boats remain secured. Ambient heave uses spatial
averaging without the old temporal lag; arriving local wakes remain damped.

Shared checks cover orientation-dependent push, nonzero turning, calm force,
rotated flow, strong-wave height/phase, ripple growth, control cancellation and
bearing wrap. Final JVM/J2ObjC signature is `906722f:2:1350`. Full Android build
checks and 1,461,230 all-heading shoreline pixel assertions passed. The developer
APK was installed; live power/direction taps were exercised on the phone.
No physical iOS-device validation was performed.
