# Thin wave geometry across zoom

Vulkan polygon winding previously accumulated absolute screen-coordinate products
in float precision. At small projected sizes those products cancelled, sometimes
reversing the winding and causing ear clipping to emit no triangles. Winding now
uses coordinates relative to the first vertex and double accumulation; vertex
positions, colors, triangle order for unaffected polygons and shared wave density
remain unchanged.

The new encoder regression fails against e447d8b at scale 0.1 and passes all 722
zoom/winding cases after the fix. The existing 2,526,477-field golden command
comparison and nested clipping checks also pass. Full Android build/checks pass.

An additional host probe passed actual WaveView polygons to the old/new encoders
at nine camera scales (0.1 through 2.0), tick 120, centered at world (0,700).
The old encoder dropped 31, 7 and 3 quads at scales 0.1, 0.11 and 0.12 respectively;
the fixed encoder drops none at any sampled scale. Counts are in the adjacent
text files. This proves a geometry loss bug; it does not establish that all visible
flicker has the same cause. The single-sample native renderer can still alias
subpixel geometry. No global MSAA or wave density change is included.

Pixel 11 Pro validation: 120 warmup plus 360 frozen overview samples all stayed
on Vulkan, mean interval 76.13 ms (13.14 FPS), p95 84.19 ms. This single run does
not establish a performance change. Native overview screenshot reviewed; 41 of
2,602,800 pixels differ from the preceding compact-encoding screenshot, mean RGB
channel delta 0.000249. Raw PNG/CSV/logs are retained under
`out/android-overview-review/wave-zoom-fixed-overview/`. This is a frozen device
capture, not proof that every moving-zoom visual artifact is resolved.
