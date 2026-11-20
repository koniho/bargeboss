# Zoom-aware rain and independent fog

Rain retains the original192 maximum close-view streaks, gradually adding detail
as camera scale falls from1.6 to0.6. The fixed limit is384 streaks. Distant streaks
are55% shorter,30% narrower and25% softer; additional identities fade in over a
24-streak overlap. Existing positions/animation phases do not change with zoom.
Below scale0.6 a subtle rain haze increases instead of adding more geometry.

Fog is independently adjustable OFF/LIGHT/MEDIUM/DENSE, including with rain off.
It shares one translucent pass with distant rain haze, below guidance and HUD.
All weather combined is bounded at384 streaks and three rectangles, with no
new textures, caches or simulation state. Weather panel rows were compacted to
fit fog above the existing steering control; RESET ALL clears fog too.

31,014 weather assertions cover deterministic draws, stable existing streak
positions, monotonic zoom density, finer geometry, fixed budgets, fog without
rain, cancelled controls and reset. Full checked Android APK and native encoder
regressions pass. JVM/J2ObjC parity remains38137f35:2:1350. Shared renderer
fog-only and wide-rain/fog previews were visually reviewed using WeatherReview.

Phone comparison: Pixel11 Pro, same frozen Montrose inlet at player pinch
zoom0.65 (projected scale0.7834479),120 warmup and360 measured frames per run.
Order: clear, heavy rain+dense fog+peak lightning, clear. No concurrent builds or
sampling. The phone was unlocked for these runs; the earlier locked-phone
attempts from the initial weather increment are excluded.


| Condition | Mean draw interval | Mean CPU draw | p95 interval |
| --- | ---: | ---: | ---: |
| Clear A |16.667ms|7.981ms|23.409ms|
| Heavy rain + dense fog + peak flash |16.680ms|8.685ms|23.425ms|
| Clear B |16.659ms|8.589ms|18.781ms|

All480 frames per run completed on Vulkan, with no replaced packets in the
native logs. The observed CPU draw increase is0.10–0.70ms versus the clear
repeats; do not infer a more precise effect cost from these runs.

Validation limitation: the phone reported unlocked, but native screenshots
were black. Inspection found a system BiometricPrompt as the focused window,
with Barge Boss still the focused app. The user was asked to complete/dismiss
it. These are preliminary rendering timings under that overlay, not verified
unobstructed presentation or native visual acceptance. Shared renderer fog-only
and zoomed-out rain/fog frames are readable; native visual confirmation remains
pending. No biometric/keyguard setting was changed.
