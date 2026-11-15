# Surface-light response masks v1

These are procedural rendering data, not AI-generated replacement artwork.
The original watercraft, cargo and buoy PNGs are unchanged.

Reproduce with `tools/GenerateSurfaceLighting.java` compiled with the shared core
and desktop renderer; `--check` verifies the tracked PNGs pixel for pixel.
`SurfaceLightArt.sources()` is the authoritative opt-in list. Source files and
all 32 heading crops, pivots and half-frame scales come from `SpriteAtlas`.

Recipe: conservatively sample each source cell into 96x96 pixels; retain only
regions whose entire source footprint plus a one-pixel guard has alpha >=245.
Estimate broad rounded directional response from occupied bounds; export right,
down, left and up responses using the original material colors, brightened with
a warm bias. Atlas frame = direction * source-frame-count + source-frame.
Keep pivots and scales identical to the source. Do not pad, recenter, rescale the
object within the cell, add halos, or bake smoke, foam, wakes or shadows into it.

The approximate response is a first pass, not a calibrated normal map. Authored
face/normal/material masks can replace it later under the same runtime contract.
See `docs/rendering/surface-lighting.md` for composition, limitations and selective
scenery rollout. Reviews: `out/surface-lighting-materials.png` and
`out/surface-lighting-harbor.png`.
