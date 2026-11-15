# Directional surface shading v2

Procedural effect data; no AI image generation and no edits to original sprite art.
Run `tools/GenerateSurfaceLighting.java` with the core classes; `--check` verifies
all six tracked `surface-shade-*-v2.png` files pixel for pixel.

Retain v1's 96x96 conservative source support, source pivots and 32 heading frames.
At each supported pixel, calculate all four broad directional responses and
normalize them into a partition whose total alpha matches the supported source.
Tint the response pixels dark navy #081220. Runtime reduces each directional
shade layer as lamps reach that face, instead of painting extra light onto it.

Exclude reviewed `TugLightArt` lantern cores, `BuoyArt` lens cores, and bright warm
pixels in the upper tug-window region. Feather anchor exclusions. Runtime fixture
halos and navigation lights draw after shading. Do not add wakes, smoke, exterior
halos or cast shadows to these masks. Original hull/cargo/buoy PNGs stay unchanged.

These are broad surface estimates, not calibrated normal maps. Authored face or
normal maps may replace them under the same opt-in receiver contract later.
Review source, away-from-light, left-lit and right-lit views in
`out/surface-shading-materials-v2.png` and the actual game composition in
`out/surface-shading-harbor-v2.png`. The runtime/rendering contract is documented
in `docs/rendering/surface-lighting.md`.
