# Ambient water detail cost

Measured Montrose's frozen full overview with the same scene/camera and normal
water physics. Render-only switches isolate fine ambient strokes (mode 1 omits
WaveView) and remaining water detail (mode 0 also omits surface crests, foam,
ripples and reflections). Solid base fill and scene content remain in all modes.
No wave density, animation frequency, physics or shoreline mask was reduced.

## Findings and change

On the iPhone 17 Pro iOS 26.5 simulator, baseline median CPU drawing was 57.45 ms.
Omitting fine ambient strokes reduced it to 23.73 ms; solid water measured
22.20 ms. A separate CPU sample showed WaveView spending substantial time in
stroke polygon allocation, coordinate wrappers and native triangulation.

Added a shared convexQuad primitive. Preclipped water strokes keep their exact
four vertices, color and clip scopes, but bypass temporary polygon arrays,
repeated bounds scans and general ear clipping. River, scene, viewport and
opacity wrappers preserve the primitive. Metal submits six vertices directly;
Vulkan reuses eight scratch floats and its existing compact triangle encoder.
Core Graphics/default painters retain equivalent fill semantics. No new textures
or caches; the Vulkan scratch is eight floats per painter.

An initial version missed the passive SceneryOcclusion forwarding method and
re-entered general polygons. Its partial results in out/ are superseded. A shared
regression now checks the complete wrapper chain, coordinates and opacity.

## iOS simulator

Debug profiling build, 45 warmup draws and 90 measured draws, no concurrent host
builds or sampling during capture. Frozen overview, default dusk.

| Capture | Median CPU draw | Mean frame interval | p95 interval |
| --- | ---: | ---: | ---: |
| Original full detail | 57.45 ms | 67.04 ms | 66.72 ms |
| Final direct path A | 46.13 ms | 50.00 ms | 50.08 ms |
| Final direct path B | 47.15 ms | 53.33 ms | 66.68 ms |
| Solid-water repeat | 22.15 ms | 28.52 ms | 37.13 ms |

CPU drawing improves about 18–20%; presentation about 15 to 19–20 FPS, still well
short of 60 FPS. GPU medians remain approximately 0.22–0.25 ms; do not add GPU and
CPU pipeline durations. Final direct triangulation changes 37 of 3,162,132 native
pixels, adding tiny ripple coverage where the general thin-quad path lost it.
No geometry, color or mask changes. Native captures were visually reviewed.

## Android device

Pixel 11 Pro, Vulkan, same signed profiling APK with `bb_water_quads=false/true`.
120 warmup + 180 measured frames each; original, direct, solid, direct, original
order. All captures are complete, tick 120, zoom 0.1113404483, Vulkan enabled,
rotation preparation ready, no measured layer/frame builds or fallbacks.

| Capture | Median CPU draw | Mean frame interval | p95 interval |
| --- | ---: | ---: | ---: |
| Original A | 66.97 ms | 69.26 ms | 77.73 ms |
| Direct A | 63.45 ms | 65.97 ms | 79.38 ms |
| Solid | 12.79 ms | 16.68 ms | 22.85 ms |
| Direct B | 68.66 ms | 70.07 ms | 80.29 ms |
| Original B | 100.90 ms | 101.67 ms | 109.38 ms |

The first pair suggests a modest gain, but the original path's large final drift
prevents a reliable Android speedup percentage. The cause remains unresolved;
a subsequent thermal-service status of 0 cannot rule out earlier clock changes
or scheduling effects. Do not attribute the entire late difference to the code.
The solid comparison still shows water is a major CPU cost on this device.

Original B and direct B screenshots match all 2,602,800 pixels. Pair A differs
only in the Android system status-bar strip (rows 14–52), not game rendering.
Native logs show Vulkan completion; raw CSVs/logs/images remain untracked under
out/profiles/water-quads-phone-final. Summary JSON is retained beside this file.
Simulator raw captures are under out/ios-profile/water-full-a, water-effects-a,
water-solid-a/b and water-forward-a/b; CPU sample was /tmp/water-native-sample.txt.

## Validation and remaining work

Full shared/asset checks, signed Android profile APK, final iOS simulator build,
722 thin-ripple winding/zoom cases comparing direct and polygon Vulkan vertices,
2,526,477 encoder fields and nested clip tests pass. Full wrapper forwarding and
opacity regression passes. Montrose all-heading shore: 2,434,414 hull/cargo plus
32,273 cached-wall assertions. Final JVM/J2ObjC parity: b0bbe1a2:2:1350.

Ambient wave sampling and submission still dominate wide water. This increment
removes avoidable transport work; it does not solve wide-scene performance.
Next isolate shared sampling/math versus remaining submissions on Android before
changing wave density or introducing a new retained representation.
