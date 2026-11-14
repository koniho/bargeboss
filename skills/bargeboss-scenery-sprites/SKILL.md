---
name: bargeboss-scenery-sprites
description: "Generate and validate modular static scenery sprites for Barge Boss, including quays, shoreline corners, warehouses and industrial background structures. Use for fixed harbor art; use the separate watercraft or animated shore equipment workflows for vessels, cranes, vehicles and unloading animations."
---

# Barge Boss static scenery sprites

Produce a small coherent set of reusable harbor elements before expanding the
scene. Preserve the user's requested asset count and stage scope. Creating art
does not itself authorize replacing level geometry or implementing new gameplay.

## Establish the shared scene contract

Locate the Barge Boss repository and read its current `AGENTS.md`. Inspect the
requested concept and nearby accepted artwork before generating. The working
reference is `docs/reference/industrial-isometric-concept.png`; the latest
scenery trial and findings are under `docs/art-studies/scenery-trial/`.

Read [the scenery contract](references/scenery-contract.md) when selecting camera,
tile connections, ground anchors or occlusion layers. Treat its camera values as
the current art-study target, not evidence that the live game camera already uses
that projection. Read the actual shared camera before integrating anything.

Define each asset's ground footprint, connection edges, deck elevation, visible
foundation depth, scale and light direction. A building's bottom-center pixel is
not automatically the center of its ground footprint. For modular joins, match
the projected connection endpoints and elevations, not equal image dimensions.

## Generate the candidate

Use the available `imagegen` skill and built-in image generation for raster art.
Identify input roles explicitly: style reference, exact geometry guide, or edit
target. A dramatic full-scene reference should not be copied as the sprite's
background. Preserve genuine alpha, leave gutters, and keep prompts tracked.

Favor smooth prerendered materials, readable industrial masses, restrained wear,
dark steel, slate concrete, rust colors and limited safety-yellow accents. Keep
the quay/water boundary clear at gameplay size. Separate water, foam, smoke,
lights that animate, vehicles and unloading machinery from static scenery.

Save versioned candidates under `docs/art-studies/` and prompts under
`docs/art-prompts/`. Only accepted runtime PNGs belong in `assets/sprites/`.
Inspect every generated result before deciding to iterate. Change the observed
defect specifically; a prompt's exact dimensions, camera or seamlessness claim
does not establish that the output satisfied it. If repeated generations ignore
the same structural constraint, improve the geometry guide or report the
candidate's limits rather than declaring it production-ready.

## Review and record

Measure actual PNG dimensions, rounded source rectangles, alpha bounds and edge
clipping. The existing read-only `tools/AtlasBounds.java` can analyze an atlas at
the supplied column/row count. Composite candidates on water-colored, light and
dark backgrounds; transparent-image viewers alone can misrepresent edge colors.

Review a contact sheet and a join test: straight-to-straight, straight-to-corner,
and the building's ground footprint against the intended deck. Keep the source
pixels unchanged while drawing these inspection previews. Record gaps, double
walls, wrong elevations, mismatched scale and occlusion risks. A visually pleasing
standalone tile is not yet a seamless modular tile.

Maintain a short experiment note with exact inputs, prompt, tool mode, actual
resolution, source crops, observed improvements, remaining defects and acceptance
status. Share asset and prompt paths. For requested integration, keep anchors,
depth ordering and dynamic translucency rules in shared Java, and use the
repository's usual cross-platform checks and installation workflow. Do not add
host-specific scenery layouts or treat art opacity as collision geometry.
On the development phone, also copy concept/review images to
`/storage/emulated/0/Download` with descriptive versioned names, as requested in
the repository workflow. Preserve the canonical files and prompts in the repo.

## Required lighting and smoke anchors

Inventory every visible lamp, furnace opening, chimney, exhaust and steam outlet.
Record stable named IDs, emitter kind/role, color, local XYZ and outward normal,
normalized source-cell position, radius, visibility and review status alongside
source crops and the ground pivot. Explicitly record empty categories for assets
without lights or outlets. Keep glow, smoke and steam independently animated;
a painted bright spot is not a substitute for an emitter anchor.

For static scenery, store anchors per asset/layer and fixed view. Keep the same
local origin as the ground footprint; state outlet height above the deck and
light spill direction so effects remain attached when the building fades.

Review labeled emitter overlays on the actual source pixels and at gameplay size.
Distinguish guide-projected estimates from pixel-verified positions; generated
art can add or relocate fixtures. Verify visibility, phase transitions and any
interpolation before integrating effects. Keep the runtime metadata and emission
rules in shared Java so Android, iOS and desktop use identical attachment points.
