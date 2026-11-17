# Montrose rescue swimmers v1

Generated with the built-in image-generation tool. The first draft was rejected
for a front-facing portrait camera; the revision uses an elevated view.

Source: `docs/art-prompts/sources/swimmers-rescue-v1-source.png` (1448x1086 RGBA).
Runtime: `assets/sprites/swimmers-rescue-v1.png` (512x384 RGBA).
Rebuild with `tools/PackSwimmerSprites.java`. Twelve 128x128 cells, row-major;
shared water-contact pivot (64,92), normalized (0.5,0.71875).
Columns are four adult variants. Cells 0–3 signal, 4–7 alternate the arm pose,
8–11 hold a rescue ring. Pair animations by column, never across variants.
Keep upright in world depth, never rotate the painted sprite in the water plane.
No baked water, glow or scenery. Apply shared dusk grading and shoreline clips.
Packing clears alpha values below 8/255 to remove near-transparent colored residue.
Runtime atlas is 768 KiB decoded; source is authoring-only and must not be decoded
by the game. Actual tiny-scale in-scene readability remains an integration check.

## Initial prompt

Use case: stylized-concept. Asset type: production transparent sprite atlas for Barge Boss, a detailed isometric industrial tugboat rescue game. Create exactly 12 small adult swimmer sprites in a precisely spaced 4-column by 3-row grid, equal rectangular cells, each isolated with generous clean transparent gutters, no grid lines or labels. Same orthographic game camera for every cell: looking down 35.264 degrees, NOT straight overhead, NOT flat front-facing portraits. Painterly realistic miniature 3D-render aesthetic with subtle surface texture, soft bevel-like anatomy shading, realistic human proportions, matching detailed weathered tugboats and dark industrial harbor scenery. NOT pixel art, NOT cartoon, NOT glossy toy people, NOT icons. Dusk palette: cool muted shadows, restrained warm skin highlights, dark wet hair, desaturated navy/teal/burgundy swimwear. Readable silhouettes at tiny game scale. Four distinct adult variants by column with varied skin tones and hair, consistent identity and scale down each column. Row 1: swimmers treading water in distress, heads and shoulders at surface, one arm raised signaling, other arm extended along surface; distinct natural poses. Row 2: same four swimmers and identical water-contact registration with alternate treading/signaling arm poses for a two-frame cyclic animation. Row 3: same four swimmers now securely holding orange rescue lifebuoys, both arms resting across rings, relieved calm body language. People upright in water, ONLY head, shoulders, upper chest and arms above water; lower body is submerged and must NOT be drawn. The imaginary water-contact center must sit at exactly horizontal center and 65 percent down each cell. Keep head position stable between row1 and row2, same figure height across all cells. Composition centered separately within each cell with at least 12 percent transparent padding. Lighting soft diffuse cool dusk with subtle readable skin detail, NO cast shadows outside figures, NO point-light glow. Transparent background with actual alpha. NO painted water, foam, ripples, scenery, text, lettering, shadows on background, panels, borders or checkerboard. Clean antialiased alpha silhouettes. Output one landscape 4:3 sprite sheet.

## Revision prompt

Revise this swimmer sprite atlas. Main correction: CAMERA IS ABOVE THE SWIMMERS looking DOWN at 35 degrees below horizontal, the exact isometric game view used for boats, NOT looking up at their faces. Swimmers look toward the water ahead, not up at the viewer. Show TOPS of heads and shoulders, foreshortened faces, and arms seen from above. Rotate their bodies in world so each swimmer faces diagonally down-right in the image. This is a distant miniature game piece, NOT a portrait or cinematic closeup. Make heads realistically small versus shoulders and arms. Keep gritty painted 3D game texture and muted cool dusk colors. Exactly 4 columns by 3 rows equal cells, no text. Row1 four adults treading water with one arm signaling; row2 the same four in alternate arm positions, SAME fixed head and water-contact positions; row3 same four holding orange life rings viewed from ABOVE so the tops of rings are visibly broad ELLIPSES. Render only head, shoulders and arms ABOVE THE IMAGINARY WATER LEVEL, omit lower body; no water painted. Keep water contact at x=50%, y=70% of EVERY cell. Fit each figure within middle 76% width and middle 80% height of its cell. Crucial: coherent identical scale, camera, anatomy and pivot in all cells. No black background, no checkerboard, no colored fringe, no background pixels, no water, no effects, no shadows outside figures, clean true transparent alpha.
