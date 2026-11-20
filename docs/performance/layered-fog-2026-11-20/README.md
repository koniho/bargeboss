# Layered Montrose fog

The existing Fog control now drives three composition layers: eight slowly
moving soft patches before opaque terrain and boats, background scenery haze
before the river depth merge, and a faint foreground veil before all HUD/UI.
The broad upper haze suggests distance; this is an artistic screen-space effect,
not physical depth/volumetric fog. Wisps maintain fixed screen coverage across zoom.

Nine existing GLOW material submissions plus two rectangles cover fog alone.
Maximum combined weather is nine materials, four rectangles and 384 rain strokes.
No new textures, caches, render targets or simulation state. Motion uses fixed
simulation ticks and continuous sine phases. Native painters reuse their existing
material shader; reference painters use their existing contour approximation.
Fog OFF emits no fog primitives. Default weather remains clear.

Full check/build and native encoder regressions pass. WeatherTest passes 31,159
assertions including deterministic fog, drift and zoom-independent patch count.
All-heading shoreline checks pass 1,461,230 hull/cargo pixels and 32,273 cached
wall pixels. JVM/J2ObjC parity remains 38137f35:2:1350. Shared close/wide fog
previews and unobstructed Android Vulkan screenshots were reviewed: wisps remain
behind shore/boats and HUD is clear. iOS native pixels and performance are untested.

Pixel 11 Pro, Vulkan, frozen Montrose inlet, player zoom 0.65 (projected scale
0.7834479), 120 warmup and 360 measured frames per run. Order below. Old flat fog
was measured before installing the new APK; new fog/clear/fog were measured after
all builds finished. No method sampling. Raw CSV, screenshots, memory and native
logs remain in out/android-overview-review/<capture-name>/.

| Capture | Mean interval | p95 interval | Mean CPU draw |
| --- | ---: | ---: | ---: |
| fog-flat-before | 16.647 ms | 18.549 ms | 8.283 ms |
| fog-layered-a | 16.679 ms | 23.589 ms | 7.893 ms |
| fog-layered-clear | 16.674 ms | 24.217 ms | 7.722 ms |
| fog-layered-b | 16.680 ms | 24.086 ms | 7.708 ms |

All sampled frames use Vulkan; native logs show 480 completed and zero replaced
packets in each new run. No sample decoding/layer construction. Fog versus clear
CPU draw varies from -0.013 to +0.171 ms: the added cost is below the variation
in these short runs. No meaningful mean interval regression observed, but p95
intervals near 24 ms preclude a locked-60-FPS claim. This stationary warm inlet
fixture does not prove cold-cache, full-overview or camera-motion performance.
