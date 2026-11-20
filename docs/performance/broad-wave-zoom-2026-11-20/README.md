# Broad wave fronts and zoom

Follow-up to the remaining disappearance reported on the Android phone after
`d2d36d4`. Inspected the live Montrose inlet with the wave lab at power 3.0 and
captured the screen, then exercised two-finger camera pinches. Broad textured wave fronts are emitted
by SeaWaves, separately from WaveView's small ripple polygons.

The previous broad-front renderer derived segment length directly from camera
scale and an integer visible-crest count. Each zoom change therefore moved every
segment midpoint. Broken-front brightness, shelter tests and curvature sampled
those moving midpoints, replacing bright pieces with gaps. A shared 512-iteration
cutoff could also stop before rendering the second wave family completely.

The renderer now uses nested world grids with power-of-two segment lengths,
blending neighboring grids with a smooth opacity weight. Desired detail depends
on continuous view extents, so a crest entering the viewport cannot globally
change the grid. Both wave families finish drawing; the continuous spacing
estimate controls work instead of an early return through the scene. Wave
physics, wave direction, intensity and the small ambient ripple system are
unchanged.

SeaWaveZoomTest fails on the previous code at scale 0.11. The new code passes
14,857 world-anchor/opacity comparisons across nine scales and all 24 bearings,
with a maximum-submission guard. This is a shared geometry regression; native
visual inspection is required separately. The early slider captures in `out/wave-zoom-phone-before/` changed only the
overview map and are not evidence of scene zoom behavior. A subsequent actual
scene pinch was inspected before installation.


Installed APK checked on the Pixel 11 Pro with four actual two-finger camera
pinches at power 3.0. Native screenshots and the 15-second recording are retained
in `out/wave-zoom-phone-after-live/`. Reviewed sampled transition frames and the
full-size zoomed-out frame: broad fronts remain present through the inspected
zoom-out/zoom-in sequence. An earlier recording made on the title screen was
excluded. This is not an exhaustive visual test of all positions/ticks/bearings.

Full Android checks/build pass, including the existing encoder regressions.
JVM/J2ObjC gameplay and drawing parity matches `38137f35:2:1350`; the drawing
baseline changes with crest segmentation, while delivered count and score stay
unchanged. All-heading shoreline checks pass 1,461,230 production hull/cargo
assertions, plus 32,273 cached training-wall pixel assertions.

Overview performance check:120 warmup/360 samples all Vulkan, mean78.81ms,
median74.08ms,p95 101.25ms (12.69FPS). Prior single capture was76.13ms mean,
75.65ms median,p95 84.19ms. Median is similar but tails are worse in this run;
this was not a thermally controlled A/B, so no performance improvement or exact
cost attribution is claimed. No runtime texture or cache budget was increased.
