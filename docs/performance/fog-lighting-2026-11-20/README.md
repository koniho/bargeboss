# Fog scattering and entrance beacon receivers

Fog wisps now draw over all scenery, craft and smoke before UI. Background haze
retains its earlier pass. Broad GLOW patches scatter the actual colors of beacon,
work, navigation and dock pedestal lamps. Beacon patches follow the shared
three-second flash including its dark interval; vessel anchors follow heading,
heave and lamp visibility. Fog level increases both diameter and strength.

The foreground pass selects up to 24 visible lamp patches, using fixed per-thread
scratch. Combined weather has at most 33 material submissions, four rectangles
and 384 streaks. No new textures, offscreen blur targets, caches or native APIs.
This is an artistic screen-space scattering approximation, not volumetric ray
marching; it has no new shadow/occlusion volume. Existing water reflections retain
their shoreline masks. Native shader falloff is smoother than reference contours.

Montrose entrance and horn lamps were only visible lens/reflection effects. They
now contribute to the existing directional reception on painted moving vessels
and colored normal reception on moored boats. The three rigid beacon models opt
into per-face Lambert lighting from up to four nearby sources, including their
own lanterns and passing vessel lamps. The upper gallery receives the self light;
back-facing stem surfaces do not receive an invented uniform bloom. No additional
cast-shadow maps. This does not migrate painted moving vessels to RGB normal maps.

The user also requested removal of the tug's movement glow. Removed the gold
silhouette pulse and idle hull bloom for tugs, preserving work/navigation lamps
and cargo targeting. Updated canonical drawing checksum only after JVM/J2ObjC
agreed on 95991121:2:1350. Gameplay result remains two deliveries, score 1350.

Validation: full checks/APK/native encoding regressions pass, including 31,547
weather assertions and 22,978 beacon assertions. Regression covers broad colored
fog scattering, flash-dark intervals, bounded work, deterministic motion, beacon
reception on nearby vessel samples, and pole reception from nearby tug lamps.
All-heading shore regression: 1,461,230 hull/cargo pixels and 32,273 wall pixels.
Actual shared clear/lit-fog/dark-fog previews and unobstructed native Vulkan clear
and fog screenshots reviewed. APK installed on Pixel 11 Pro. No iOS native pixel
or performance validation in this round.

Phone fixture: new montrose-lights, frozen visual tick 15 (peak beacon flash),
120 warmup/360 samples, camera zoom0.65 / projected scale0.7834479. Clear/fog/clear
order, no concurrent builds or method sampling. All samples Vulkan. Native logs
report 480 completed frames and zero replaced packets per run. No measured
sample decoding or layer builds. Raw CSV/screenshots/logs/memory under
out/android-overview-review/<capture-name>/.

| Capture | Mean interval | p95 interval | Mean CPU draw |
| --- | ---: | ---: | ---: |
| beacon-clear-a | 16.670 ms | 23.877 ms | 8.551 ms |
| beacon-fog | 16.651 ms | 23.874 ms | 8.762 ms |
| beacon-clear-b | 16.683 ms | 24.658 ms | 9.349 ms |

No meaningful average frame-interval regression in this warm stationary fixture.
CPU variation across repeats exceeds the isolated fog difference; no precise cost
or speedup claim. p95 intervals exceed16.7ms, so this is not locked60FPS evidence.
Camera traversal, crowded full overview and cold preparation remain unmeasured
for this increment.
