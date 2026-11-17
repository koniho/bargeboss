# Single-barge tight-circle replay

`tow-tight-circle` attaches one steel-coil barge with the actual tow gesture, then
holds a full-throttle forward steering gesture (90-unit drag) around a 110-world-unit-radius target circle in
Indiana's open basin. Physics determines the actual path, heading, speed and rope
tension. Bodies are positioned only during setup; replay never teleports them.
No additional cargo, feeder arrivals or marina fleet are involved.

Build/install a profiling APK, then run with an authorized, unlocked Android device:

```sh
BARGEBOSS_PROFILE=1 ./build-android.sh
adb install --no-incremental -r build/bargeboss-debug.apk
./tools/profile-tow-circle.sh
```

The runner records 360 warmup frames and 2400 measured frames. Every replay frame
advances exactly one 60 Hz physics tick, independent of display refresh. Thus replay
wall time is not gameplay time. Heading, speed, tow count/tension and tug position
are appended to the Android CSV alongside draw, simulation, frame interval,
rotation preparation and cache counters. Inspect both interval spikes and actual
boat speed before attributing a perceived slowdown to rendering or tow physics.

`./tools/check-profile.sh` verifies two independent 6000-tick executions remain
identical, keep exactly one tow attached, remain in water, and complete at least
two laps. The current trajectory completes approximately twelve laps. This is an
isolated reproduction workload, not a diagnosis or a worst-case Montrose benchmark.

## Initial evidence

The preliminary moderate-throttle run (52-unit drag) recorded 2400 samples with
40.076 ms mean frame interval (24.95 effective FPS), 49.512 ms median and 57.056 ms
p95. Tug speed stayed between 23.76 and 24.02 world units/second and the single tow
remained attached. Shared advance p95 was 0.943 ms and draw submission p95 13.678
ms. This reproduces a frame-pacing slowdown with steady physical speed, but does
not distinguish RenderThread work, GPU work and presentation waits. Five native
frame builds occurred during sampling; no cache evictions were recorded.

This is not a matched straight-line/turn A/B. Use a system trace before attributing
the slowdown to heading changes, accumulated wake overlap or buffer waits.
The default test was subsequently increased to full throttle at the user's request.

Full-throttle follow-up: 2400 samples, one persistent tow, mean tug speed 45.93
world units/second. Mean interval 38.962 ms (25.67 effective FPS), p95 57.412 ms;
advance p95 0.893 ms and draw p95 13.706 ms. No sampled frame builds, decodes or
cache evictions. The faster workload still reproduces poor frame pacing.
