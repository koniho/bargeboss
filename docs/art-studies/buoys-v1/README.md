# Screenshot-based lantern buoys

The user requested buoy sprites from the latest Photos screenshot, then confirmed
that the red/green region-mouth markers should use the same sprite treatment.
The source is `reference-screenshot.png` (phone screenshot dated 2026-11-14 20:36:47).
It supplies the stout riveted float, rusted open braced tower, ladder, railed
platform and enclosed lantern. The surrounding factory, water, glow and phone UI
are reference context only and are absent from the sprites.

Built-in image generation produced a red buoy, then edited that source into
matching green and yellow variants. See [prompts](../../art-prompts/buoys-v1.md).
All three outputs are 1254×1254 RGBA, copied unchanged to `assets/sprites/` as
`buoy-red-v1.png`, `buoy-green-v1.png`, and `buoy-yellow-v1.png`. The matching
`*-source.png` files here preserve the generator outputs. Red retains a triangular
daymark; green and yellow use square dayboards. Blank plates allow shared-runtime
practice numbers. No new directional sheet is needed: the user explicitly chose
small sprite rotations for rocking motion.

Read-only `AtlasBounds` results are saved beside this note. Alpha>=32 extents:

| Variant | Left/top | Right/bottom |
| --- | --- | --- |
| Red | 274,25 | 964,1198 |
| Green | 272,22 | 968,1198 |
| Yellow | 273,22 | 965,1198 |

None touches an image edge. The requested 12% top gutter was not delivered; actual
clearance is 22–25 pixels. Rotation is applied to the entire image by the native
painter rather than baking tilted frames into that source canvas, preserving the
silhouette. Fixed isometric camera language guided generation; the image is a
fixed-view artistic approximation, not a geometrically calibrated 3-D model.
Each variant is registered separately from measured source pixels.

`BuoyArt` is the authoritative runtime metadata for waterline pivots, lantern
anchors, source scale and wave pose. Every colored beacon shares the sprite's
rotation and bob. Smoke outlets: none. Steam outlets: none. Tires: none; the
metal side rings are painted fittings, not smoke/light emitters. Dynamic glow,
reflections, receiving-plane shadows and waterline masking are separate layers.

The lower float is intentionally painted complete in the source. Runtime water
masks its submerged portion while the sprite rocks around its waterline pivot.
The water surface mask does not rotate with the artwork. Shadow masks derive from
accepted source alpha through the existing deterministic shadow pipeline; they
are approximations, not reconstructed 3-D height or occlusion maps.

Placement remains sparse: only region mouths receive navigation pairs. Repeating
bend/gate posts and extra bridge/pier pairs were removed. The two numbered gold
buoys remain because they are the opening tutorial's circling objectives.
