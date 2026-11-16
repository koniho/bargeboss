# Industrial shore tiles v1

Generated with the built-in imagegen tool from the supplied industrial tileset reference. New source artwork; not yet registered or wired into runtime terrain.

## Original prompt

Use case: stylized-concept. Asset type: transparent production-source sprite sheet for Barge Boss industrial harbor game.
Input image is a STYLE REFERENCE ONLY, not an edit target. Create new modular shore tiles matching its detailed painterly industrial realism: worn gray concrete slabs, charcoal steel sheet-pile quay walls, orange-brown rust streaks, chipped safety-yellow railings, restrained grime and bolt detail.
Camera: fixed orthographic isometric, 45 degree ground yaw, elevation 35.264 degrees, ground axes at 30 degrees to horizontal. No perspective convergence. Consistent scale, upper-left neutral warm lighting.
Sheet: exactly 3 columns by 2 rows of equal cells, six separate complete modules, large transparent gutters, no touching or overlapping cells. All square dock top footprints same dimensions and wall depth. Row 1: (1) plain concrete square quay tile with two visible vertical sheet-pile walls, fine seams/drain grate, no railings; (2) same tile with worn yellow railing on the back-left top edge; (3) same tile with railing on the back-right top edge. Row 2: (4) same tile with railings on both back edges forming a corner; (5) same tile with short rusty service pipes laid on supports along back-left edge, exposed top for foot traffic; (6) same tile with a recessed ladder on a visible vertical wall and a short broken/worn railing segment on back-right top edge. Industrial details must belong to the tile, not float outside it.
True transparent alpha everywhere outside the six object silhouettes and between railing bars. No dark background, checkerboard pixels, water, boats, buildings, trucks, people, text, labels, borders, UI, lamps, colored glow, cast shadows on an outside ground plane. Do not duplicate the reference banner. Preserve complete lower wall edges and railing tips with at least 8 percent empty gutter in every cell. Crisp readable silhouettes and material detail suitable for a mobile game. This is a modular source sheet, not an assembled scene.

## Transparency edit

Remove only the background and broad colored haze/halos/shadows outside the six tile silhouettes and between railing bars. Preserve objects, layout, scale, camera, rust and details. No exterior glow, text or checkerboard pixels. Generated output inspected visually and alpha audited independently; the inline preview exposes background RGB even where alpha is zero.

## Layout and integration

`assets/sprites/shore-tiles-v1.png`: 1536 × 1024 RGBA, 3 × 2 cells, each 512 × 512. Row-major cells: plain quay, back-left rail, back-right rail, two-rail corner, pipe support, ladder and worn rail. Intended top-surface centers must be measured per cell; do not assume a shared artwork pivot or exact seamless edges.

These are authoring sources. Preserve real raised-ground occlusion independently of railing strokes. Separate foreground walls/railings from top surfaces before integrating behind/in front of boats. Terrain collision geometry must not be inferred from these images. Crop retained textures to frames and declare them in the shared asset manifest before display.

The generator produced square platform blocks rather than guaranteed seamlessly tiling terrain. Repeated drain grates and wall ribs need a seam/scale review in a real shared-renderer composite. Current production shore tiles remain unchanged.

## Alpha and cell audit

All six cell borders have alpha 0. Significant bounds (alpha > 8), inclusive and
relative to each 512-pixel cell: `(44,65)-(482,449)`, `(36,15)-(477,449)`,
`(29,34)-(468,449)`, `(43,15)-(483,449)`, `(35,30)-(477,449)`,
`(29,62)-(468,449)`. Source maximum alpha is 254/255. Gutters exist but are not
uniform and do not meet the requested 8% on every edge.

Canonical style reference: `docs/art-studies/industrial-shore-v1/tileset-reference.png`.
