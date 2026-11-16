# Montrose waterfront v2 candidate

Four new components match the accepted industrial ground and foundry materials: flat concrete promenade, flat weathered timber, a raised concrete seawall reference, and a raised wooden dock reference. The slate/concrete palette, broad masses and restrained detail replace the previous orange dock look. These are new candidates; no production PNG or shared renderer was changed by this art task.

Generated through built-in imagegen with the exact tracked [prompt](../../../art-prompts/montrose-waterfront-v2.md), concept, accepted ground and accepted foundry as style inputs. Actual output is **1254×1254**, four **627×627** cells, despite requesting 1536 square. See metadata.json for crops, measured alpha bounds, material corners, explicit empty light/smoke/steam inventories, and shadow registration.

## Review findings

- Raw and cool-industrial graded composites were inspected against water, light and dark backgrounds. Recommend applying the same existing shared scenery grade. This preserves a consistent dark slate material response; independently lit pedestal pools should provide the warm accents. No glow or fixtures are baked into either top material.
- The generated flat diamonds have roughly **32.7°** edges, rather than exact 30° screen axes. Their affine-remapped join proof restores exact camera geometry. Use top cells 0/1 as plane textures in root's rigid dock/walkway mesh. Never rotate the dimensional cells 2/3 as painted isometric billboards.
- Straight and corner material joins were inspected. Overall shapes meet, but slab mottling and board rhythm restart and some antialiased seam pixels remain. These are **not certified seamless tiles**. Map an interior sample over each rigid segment, fill the underlying surface opaquely, and clip to exact geometry; do not rely on source alpha for shore collision or cutouts.
- Source alpha has 971,235 fully transparent pixels, 1,269 fully opaque pixels, and 600,012 intermediate-alpha pixels. Alpha was preserved; no thresholding or source recoloring was performed. Transparent compositing does not show the colored fringe that some raw RGBA viewers display.
- All four alpha>=128 silhouettes stay inside their cells. Closest gutter is the wood cell's 10px left margin. Use rounded 627px crops, not nominal requested dimensions.
- Raised references show thick cap/structural rim and tall wall/piles with no detached cast shadow. Their two bright cap corners are material highlights, not emissive fixtures. Receiving-plane pivots are estimates and unsuitable as certified production registrations until rigid geometry is supplied.

## Shadows and lights

`waterfront-v2-reference-shadows.png` supplies independent near-contact and soft-cast proof masks for the two raised modules only, using the existing pipeline settings (128px cells, 16px padding, 96px interior, alpha cutoff64, sigma1.0/3.2). Four masks have transparent gutters. The separate review shows both layers on three backgrounds and three projection directions. Masks are alpha silhouettes, not recovered 3D shapes. The two flat top materials intentionally do not cast shadows.

All source light/smoke/steam arrays are explicitly empty. User-requested lighted dock pedestals must be separate rigid world geometry with local anchors, above their light pools. Root owns exact seawall/dock depth, caps, piles and runtime shadows. West/port entrance rock armor is owned by the park art task; the broad promenade sits above the retaining wall.

## Files and regeneration

- `waterfront-v2-candidate.png`: canonical source RGBA.
- `waterfront-v2-composites.png`: raw/graded appearance review.
- `waterfront-v2-joins.png`: affine flat-material straight/corner join review.
- `waterfront-v2-reference-shadows.png` and `waterfront-v2-shadow-review.png`: independent masks/proof.
- `WaterfrontProof.java`: deterministic review/mask generator; reads source unchanged.

Compile the proof into an isolated build/marina-waterfront directory and run with Java2D headless. Review PNGs were also copied to Downloads using versioned `BargeBoss-Montrose-waterfront-v2-*` names. This candidate-only task made no native rendering or performance claim; full shared/native validation belongs to integration.
