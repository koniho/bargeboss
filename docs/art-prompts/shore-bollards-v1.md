# Shore bollards and mooring fittings v1

Generated with built-in imagegen on 2026-11-16. New source art, not wired into the runtime atlas.

References: user-supplied Barge Boss harbor gameplay concept (`codex-clipboard-Gq2brQ.png`) and industrial isometric tileset (`codex-clipboard-zTSHm0.png`). The second reference supplied the primary material/detail direction. Both were passed as style references, not edit targets.

Camera target: the shared RiverCamera contract, 45-degree ground yaw and 35.264-degree elevation, projected 30-degree ground axes. This is not a literal 2:1 diamond. Rusted iron, chipped yellow paint, upper-left light, fixed isometric view.

## Sheet layout

`assets/sprites/shore-bollards-v1.png`: 1536 × 1024, 8-bit RGBA; 3 columns × 2 rows of 512 × 512 cells. Reading order:

| Frame | Fitting | Visible bounds in cell (alpha > 2, exclusive right/bottom) |
| --- | --- | --- |
| 0 | Dark twin-horn bollard | 92,147–447,450 |
| 1 | Mushroom-head bollard | 111,127–399,455 |
| 2 | Heavy double cylindrical bitt | 61,117–462,450 |
| 3 | Chipped yellow twin-horn bollard | 91,135–446,432 |
| 4 | Low mooring cleat | 81,155–428,429 |
| 5 | Mushroom bollard with rope and coil | 83,86–439,433 |

The initial generation was followed by one targeted alpha/gutter edit. The final PNG has 1,138,563 fully transparent pixels (72.39%); alpha range 0–254. Cell boundary alpha is at most 1/255. Colored RGB remains beneath transparent pixels, so viewers ignoring alpha can show a brown backdrop; it is not opaque scene geometry. Preserve alpha and use premultiplied-safe texture handling.

Visual review: all six subjects are readable, with consistent weathered metal and camera character; the rope is separate enough to read. No text, water or ground slabs. The sheet is source art, not an automatic production registration. The requested precise 358-pixel maximum and (256,365) base anchor were not obeyed exactly: frame 2 is 401 pixels wide, and bases differ vertically. All substantive silhouettes have at least 50 pixels of gutter. Source-alpha residue outside silhouettes is at most 2/255 at the measured boundaries. Do not infer pivots from transparent canvas center.

Before runtime integration, crop retained frames, register each mounting-plate ground center to its shared world anchor, review at actual game scale, and author collision/rope fitting positions in shared Java. A provisional per-cell pivot near (0.50,0.78) is a starting point only, not a verified attachment point. These are fixed-orientation static fittings; do not rotate the painted bitmap to fabricate new views. No animated light, smoke or cast shadow is baked as a separate effect.

## Initial prompt (exact)

Use case: stylized-concept. Asset type: transparent game sprite sheet for Barge Boss. Create a NEW 3-column by 2-row equal-cell atlas containing SIX isolated small dock mooring fittings. The supplied images are STYLE references only: image 2 industrial tileset is the primary material/detail reference; image 1 game scene supports harbor character. Match their detailed painted/prerendered industrial isometric appearance, charcoal cast iron and steel, orange-brown rust in seams, worn edges, warm upper-left lighting, restrained weathered safety yellow. Orthographic camera at 45-degree ground yaw and 35.264-degree elevation, parallel ground edges project at 30 degrees; no perspective convergence. Layout on a 1536x1024 canvas, six equal 512x512 cells. Reading order: (1) squat twin-horn mooring bollard in dark rusted iron on its metal mounting plate; (2) mushroom-head bollard; (3) heavy double cylindrical bitt on single riveted steel mounting plate; (4) twin-horn bollard with heavily chipped yellow safety paint exposing dark rusty iron; (5) low heavy mooring cleat on steel mounting plate; (6) mushroom bollard with tidy thick beige hemp rope looped around it and a compact rope coil resting beside its base. Each object fully inside its own cell with at least 15% transparent gutters. Common apparent scale and same camera and lighting. Center the object's base contact near x50%, y73% within its cell. Bolt heads and rust must be readable, silhouettes bold at game size, not overcomplicated microtexture. Mounting plates are part of the objects; NO concrete ground chunks, no surrounding ground, water, terrain, scenery, labels, text, numbers, borders, grid lines, checkerboard, UI, glow, smoke, or cast shadow painted onto the background. Actual fully transparent alpha everywhere outside objects. No objects crossing cell boundaries. This is an isolated sprite sheet, not a composed scene.

## Cleanup prompt (exact)

Edit this sprite sheet only. Preserve the EXACT six mooring fittings, rust, paint, rope, camera, material detail and 3x2 layout. Remove every background halo, glow, gradient, smoke or soft ground shadow: pixels outside the physical metal/rope silhouettes must have alpha exactly zero. Keep antialiasing only within 1-2 pixels of the physical edge. No brown haze or lighting bloom outside objects. Scale each complete object down uniformly to fit within a 358x358-pixel box centered in its existing 512x512 cell on the same 1536x1024 canvas, preserving proportions. Anchor base center at local (256,365) within each cell. Actual transparent background, no checkerboard artwork, no ground, no text. Do not redesign objects.


Canonical reference copies: `docs/art-studies/industrial-shore-v1/tileset-reference.png`
and `docs/art-studies/industrial-shore-v1/scene-reference.png`.
