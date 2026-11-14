# Three static scenery candidates

The user requested a small scenery trial while watercraft refinement continues.
This is **one atlas containing three static elements**, not a new level or an
approved seamless tileset. Nothing from this trial is integrated into gameplay.

- [Source atlas](static-harbor-v1.png)
- [Interactive background/opacity review](review.html)
- [Exact prompt](../../art-prompts/scenery-trial-v1.txt)
- [Style reference](../../reference/industrial-isometric-concept.png)

The built-in `image_gen` tool generated the atlas using the industrial concept as
a material/color reference. The requested resolution was 3072×1024; the measured
output is **2172×724 RGBA**, a 3×1 grid with 724-pixel square cells. Source pixels
have not been edited, extracted or recolored with code. No baked water, smoke,
vehicles, workers or moving machines appear in the three elements.

| Cell | Asset | Source rectangle x,y,w,h | Alpha≥128 bounds inside cell |
| --- | --- | --- | --- |
| 0 | Straight industrial quay | 0,0,724,724 | 80,239–635,684 |
| 1 | Beveled outside quay corner | 724,0,724,724 | 38,174–682,661 |
| 2 | Compact warehouse | 1448,0,724,724 | 49,110–668,689 |

Bounds endpoints are inclusive. None of the three opaque silhouettes touches a
cell boundary. Detailed read-only measurements are in
[alpha-bounds-128.csv](alpha-bounds-128.csv). The generic analyzer's
`intended_degrees` and `axis_error_degrees` columns do not apply to these static
assets; it assumes an evenly spaced turntable when given a grid.

Alpha is real, with 366,593 / 300,065 / 269,427 fully transparent pixels in the
three cells. Most visible interior pixels have alpha 252–253 rather than 255;
mean alpha among pixels at least 128 is 252.08 / 252.22 / 252.04. Thus the source
is nearly opaque internally, not mathematically fully opaque. Preserve that
measurement and inspect against actual game backgrounds before accepting it;
the image has not been thresholded or silently cleaned.

## What worked

- The set has coherent concrete, dark pile walls, warm rust metal and large
  yellow bollards. Major structural forms remain readable without dense pipes or
  machinery clutter.
- The corner has a substantial bevel and matching quay materials, giving a way
  to break up a straight shoreline.
- The camera appears consistent across the three elements. This is a visual
  assessment, not a geometric verification of the requested 35.264-degree
  elevation or exact 30-degree projected axes.
- Static geometry remains isolated from effects and moving shore equipment.

## What remains before integration

- No joining test has yet proven straight-to-straight or straight-to-corner seams.
  The straight piece has an exposed end face which must not become a doubled
  wall at an internal join. Matching colors alone does not establish modularity.
- The prompt requested one world scale and deck elevation. Those quantities need
  measurement against a known projected footprint; generation is not proof they
  match. The warehouse's thin base and the quay's deep visible wall represent
  different vertical spans and cannot simply be aligned by their bottom pixels.
- The requested normalized footprint anchor `(0.50,0.72)` remains a **requested,
  unverified anchor**. Opaque-pixel centers in the CSV are not ground pivots.
- Bollards are baked into these concept quays. If they become interactive lasso
  targets, extract/regenerate them as separate sprites so their position and hit
  effect can be shared gameplay data. Do not infer interaction points from paint.
- The warehouse is a whole sprite. Before a tall building can obscure a vessel,
  decide whether to fade the whole structure or split tall mass from its base;
  keep the navigable shoreline visible. The review's opacity slider is only an
  artwork inspection aid, not an implementation of dynamic gameplay fading.

## Learnings to carry forward

For a small style trial, generating adjacent static elements in one sheet helped
keep the materials visually coherent. Equal cells simplified source crops, but
did not establish equal world scale or valid joins. The next geometry-focused
iteration should supply a projected footprint/connection guide and inspect
assembled seams before expanding the asset library. Keep game-interactive props
out of static terrain artwork when independent placement or animation is needed.

The focused reusable workflow is
`skills/bargeboss-scenery-sprites/SKILL.md`. Watercraft turntables and animated
shore equipment belong to their separate skills.
