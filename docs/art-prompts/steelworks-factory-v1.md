# Steelworks factory v1

Created with the built-in imagegen tool on 2026-11-16. Source: `assets/sprites/steelworks-factory-v1.png`.

Primary reference: [industrial tileset](../art-studies/industrial-shore-v1/tileset-reference.png), especially its central steelworks. Secondary: [harbor scene](../art-studies/industrial-shore-v1/scene-reference.png). This is the requested large factory aesthetic: red-brown brick, rusty stacks, dense steel framing, yellow catwalk railings, corrugated roofs and a bright furnace interior.

## Registration and future effects

Single 1448×1086 RGBA frame. Intended orthographic camera: 45° yaw, 35.264° elevation, ground axes ±30°. Preserve aspect ratio and derive a world-space footprint in shared Java before integration. Provisional ground pivot: source (730,865), normalized (0.5041,0.7965); this is a visual estimate, not a verified sprite registration.

Suggested source-pixel effect anchors, all provisional:

| Anchor | Source pixel | Intended effect |
| --- | --- | --- |
| Furnace core | (715,707) | Shared orange glow, with occlusion at door/recess |
| Left stack opening | (319,104) | Shared dark exhaust |
| Right tall stack opening | (970,38) | Shared dark exhaust |
| Central small outlet | (636,57) | Optional shared exhaust, separate stable ID |

The furnace core and nearby reflected orange illumination are baked into the artwork. No plume or broad exterior bloom is baked in. Any live effect must inherit this fixed architecture's world transform and shared visual clock; native hosts must not choose anchors or animate it independently. The factory is fixed architecture. No existing bridge or lock form was modified.

## Validation and limits

- Inspected complete architectural silhouette, both rusty stacks, furnace, brick side buildings, pipes and catwalks.
- Alpha range 0–255; 654,466 fully transparent pixels and 918,062 nonzero pixels.
- Significant art bounds (alpha >2): (49,16)–(1412,1059). Alpha >16 bounds: (51,18)–(1410,1057).
- Outer border has 71 pixels at alpha 1; no border pixel exceeds 1/255. Chimney headroom is 16 significant pixels, less than the requested 10% margin; retain source extent and add runtime crop gutters if needed.
- Tiny alpha residue is documented rather than destructively thresholded; preserve thin railings.
- Not runtime integrated. Ground footprint, foreground-shore clipping, building-only fade, effect anchors, world scale and neighboring scenery overlap require shared-renderer review.
- Do not upload this full source repeatedly during active draw; prepare/cache the actual frame before it becomes visible, following the existing bounded texture budget.

## Exact generation prompt

Create a NEW isolated game sprite of one LARGE STEEL FACTORY complex for Barge Boss, derived from the center furnace factory in the primary industrial tileset reference. Secondary gameplay image supplies rust, dockside material wear and yellow safety fittings. Use case: stylized-concept. Do not recreate the sheet or the gameplay scene. Subject: a cohesive towering red-brown brick steelworks furnace building with a large recessed orange-hot furnace mouth, two tall rusty exhaust chimneys, dense charcoal steel exterior catwalks and bracing, chipped yellow guardrails and ladders, huge dark rust-orange industrial pipes, a small corrugated-metal side shed, weathered doors and grates. Dense believable heavy industrial architecture with readable silhouette. Match the reference's painterly realistic isometric game art, warm highlights, dark charcoal steel, rusty orange seams, aged brick, bolts, soot, worn yellow paint. Camera MUST be orthographic: 45 degree ground yaw, 35.264 degree elevation, ground axes projected at +30/-30 degrees; no perspective vanishing points. Show roof, both front sides and all architectural feet. Single complete static factory centered within the image with at least 10 percent TRANSPARENT margin on all sides, especially above chimneys. True transparent PNG background, transparent holes between exposed supports and railings. NO environmental ground slab, NO shore terrain tile, NO water, NO scenery behind it, NO text/UI/borders/labels, NO people, NO vehicles. Furnace mouth may have baked bright orange interior core but no exterior bloom, haze, beams, smoke, particles or broad cast shadow; runtime effects will be separate. All architectural parts are fixed, no crane boom, no rotating machinery. This is an isolated game sprite ready for separate world placement, not a scene illustration.

