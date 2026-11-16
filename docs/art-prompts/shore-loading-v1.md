# Shore loading equipment v1

Created with the built-in imagegen tool on 2026-11-16. Source: `assets/sprites/shore-loading-v1.png`.

References: [industrial tileset](../art-studies/industrial-shore-v1/tileset-reference.png) is primary; [harbor scene](../art-studies/industrial-shore-v1/scene-reference.png) is secondary. Both were supplied by the user. Camera requested: orthographic 45° yaw, 35.264° elevation, ground axes ±30°; not a literal 2:1 projection.

## Asset layout and registration

1536×1024 RGBA PNG; 2 columns × 2 rows; 768×512 cells, row-major:

| Frame | Prop | Visible bounds in sheet, alpha >16 | Provisional ground pivot in cell |
| --- | --- | --- | --- |
| 0 | Empty low bulk hopper | (238,78)–(556,413) | (398,354) |
| 1 | Fixed conveyor transfer station | (949,77)–(1326,428) | (375,352) |
| 2 | Two-coil cradle | (205,620)–(586,916) | (397,340) |
| 3 | Raised grated staging platform | (940,605)–(1335,917) | (370,336) |

Pivots are visual estimates of the ground footprint center, not validated runtime anchors. Preserve native aspect ratio; set world dimensions per prop instead of stretching to a common square. Coil storage is a static shore prop, not a new vessel cargo atlas. The conveyor is a fixed station; any future belt animation must be a separately registered material. No rigid machinery part should be animated by rotating this bitmap.

## Validation

- Inspected all four complete silhouettes and corrected initial tight row spacing with a layout-only generation pass.
- Alpha range 0–254; 1,277,514 fully transparent pixels and 295,350 nonzero pixels.
- All outer-edge pixels have alpha 0. Center row/column has 20 residual pixels at alpha 1 only; no opaque art crosses cell boundaries.
- Significant art bounds (alpha >2): (203,75)–(1337,918).
- Background RGB retains colored render values beneath alpha zero; image viewers ignoring alpha show a false backdrop. Preserve alpha when decoding/exporting.
- No runtime references, cache entries, collision footprints, native builds or device validation added in this asset round. Requires actual shared-renderer composite review before enabling. Fine grating/railings may need scale-specific review.

## Exact generation prompt

Create a NEW production-oriented sprite sheet derived from the attached industrial harbor references. Use case: stylized-concept. Asset type: transparent isometric game sprite sheet for Barge Boss. The industrial tileset reference is PRIMARY for style; the gameplay harbor reference is secondary for rust, steel and yellow safety fittings. Do not recreate either scene. Produce exactly FOUR separate static loading equipment props in a clean 2 columns by 2 rows equal-cell grid, large generous transparent gutters, each complete and uncropped. Top left: a low open bulk-material receiving hopper on four charcoal steel legs, rusty orange sloping metal funnel, fixed access ladder, small chipped yellow safety handrail, no contents. Top right: compact fixed conveyor transfer station with black rubber belt, rusted housing, steel supports, yellow maintenance railing, no moving boom. Bottom left: heavy dockside steel-coil loading/storage cradle carrying two metallic silver steel coils, rusted orange support saddles and charcoal frame. Bottom right: static raised cargo staging platform with iron grated deck, a short staircase, chipped yellow guardrails along two edges, rusted steel legs and crossbracing; empty deck. Style: highly legible detailed painterly realistic game art matching the reference, warm edge highlights, charcoal shadows, orange-red rust streaks, chipped industrial yellow paint, bolts, seams, restrained grime. Orthographic isometric camera at 45 degree ground yaw and 35.264 degree elevation; projected ground axes at plus/minus 30 degrees to horizontal, no perspective vanishing points. Same lighting, camera and material scale for all four. Each prop centered in its own equal cell, modest contact shading only, no broad cast shadows. All structural feet visible. These are separate static station assets, NOT crane animation or rotating machinery. True transparent background with transparent gaps beneath and between structural members, no backdrop, no water, no shore slab, no floor, no landscape, no text, no labels, no borders, no UI, no people. Keep silhouettes clear enough to read at game scale.

## Exact transparency correction prompt

Edit this sprite sheet: remove EVERY background pixel and ALL broad colored glow/haze/cast shadows surrounding the four objects. Make background genuinely alpha zero transparent, including all gaps between legs, under machinery, between guardrails and inside the coil openings. Keep the four objects, rust textures, yellow railing shapes, exact poses, arrangement, dimensions and object shading unchanged. The transparent gaps must be clean cutouts. Do not add black, brown, checkerboard or any other backdrop. Also preserve a clean transparent horizontal gutter separating the two rows: upper props must fit completely above center and lower props below center, with no cropping. This is a production transparent PNG asset sheet, not a studio render with backdrop or ambient glow.

## Exact final layout correction prompt

Preserve these exact four loading equipment designs, textures, orientation and transparent cutouts. Change ONLY the sheet layout: scale each of the four objects down by about 25 percent within its own quadrant, centered, so none touch or cross cell boundaries. Use a strict 2x2 equal-cell grid. The entire horizontal stripe from 45% to 55% image height MUST be empty alpha-zero, and the entire vertical stripe from 45% to 55% image width MUST be empty alpha-zero. Each object must fit completely inside its quadrant with generous transparent margins on ALL sides. Top left hopper, top right conveyor, bottom left two-coil cradle, bottom right stair platform. True transparent background everywhere outside silhouettes, no glow or cast shadows, no checkerboard, no text or labels. Preserve the same industrial weathering, small-scale detail and exact object shapes. Do not crop any feet, railings, chimneys or ladders.

