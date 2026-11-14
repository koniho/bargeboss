# Tutorial industrial prop set

The tutorial uses four new static isometric props alongside the accepted
warehouse: stacked wood crates, a steel-pipe rack, twin storage tanks and a small
brick utility shed. All harbor and tutorial channel decorations now avoid the
old front-facing SCENERY atlas. Working unloading machinery remains procedural.

Built-in image generation produced the initial atlas using the existing warehouse
sheet as material/camera reference and the industrial concept for atmosphere.
The first candidate looked coherent but its pipe-rack foot crossed a cell border.
A targeted second built-in edit reduced prop scale and restored transparent cell
gutters while preserving their designs. Both prompts are tracked under
`docs/art-prompts/tutorial-isometric-props-v1.txt` and `-v2.txt`.

The accepted unchanged PNG is `assets/sprites/tutorial-isometric-props-v2.png`:
**1254 × 1254 RGBA, 2 × 2 cells of 627 × 627**. Runtime atlas ID17 is
`TUTORIAL_SCENERY`. The actual resolution differs from the requested2048square;
source crops use measured dimensions. `alpha-bounds-v2.csv` confirms all four
alpha≥128 silhouettes stay inside their cells. No source pixels were edited by
code. Atlas cropping and anchor overlays are read-only inspections.

Ground pivots were inspected against visible pallet, rack-foot and foundation
corners. Hidden corners remain inferred, not surveyed geometry. The painted
camera is an approximate coherent three-quarter view, not a geometry-guide proof
of exact35.264degree elevation. Conservative world footprints drive decorative
placement; scenery does not change collision geometry.

The November 14 [grounding audit](../scenery-grounding-audit.md) measured visible
foundation-edge mismatches against the actual camera, especially on the pipe
rack and shed. It also corrected the pipe rack's runtime/JSON Y pivot from
439/627 to451/627, matching the center of its existing recorded corner set.
This registration correction preserves source pixels and does not repair the
painted perspective. Calibrated replacement art is still needed for exact tile
alignment.

`isometric-props-v2-anchors.json` records per-cell ground pivots, source rectangles,
world sizes, conservative footprints, visibility and feature inventories. The shed
contains two windows with four tracked glowing panes. Their painted source centers
are pixel-inspected; localXYZ values are approximate wall-plane reconstruction.
Crates, pipe rack and storage tanks have empty light inventories. All four have
explicitly empty smoke inventories: the stored pipe mouths are cargo, tank pipes
are closed, and neither building nor crate prop has an exhaust outlet.

`SceneryAnchorPreview --tutorial` renders the four isolated assets, ground pivots
and window anchors against dark water and shore colors. Runtime windows pulse on
the shared visual clock and fade with the full sprite. The rectangular training
canal uses the exact shared bank geometry; no painted quay edge is used to imply
a different navigable boundary. Every prop's conservative footprint is placed
outside the union of water rectangles, including the inside of right-angle bends.

The tutorial scenery is less dense than the prior practice set, depth-sorted across
both banks and faded when it covers the tug or next gate. Initial driving, cargo
pushing, tying and towing lessons use the same new prop family as canal practice.
The remaining shore machinery can be replaced by the separate animated-equipment
workflow without changing these static decorations.

The same accepted prop replacements are also used in the main harbor, preserving
the distinct crate, pipe, tank and building roles while removing bright legacy
front-facing decorations. Gameplay-phase dock equipment remains in place.

Two separate chimney landmarks use `ChimneyArt` on the inland side of canal
practice, between storage-prop positions. Their genuine open outlets and amber
lamps are documented independently in `../scenery-effects/chimneys-v1-anchors.json`.
They do not turn the four smoke-free storage props into emitters.
