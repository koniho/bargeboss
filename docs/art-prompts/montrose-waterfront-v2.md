# Montrose waterfront v2

Built-in image generation; references inspected before generation. New candidate only, no production atlas replacement.

## Exact prompt

Use case: stylized-concept
Asset type: production-oriented Barge Boss waterfront sprite candidate sheet, transparent RGBA.
Create ONE coherent four-asset sheet in an exact 2 columns x 2 rows grid, square 1536x1536 canvas, equal cells, with wide fully transparent gutters. No labels.
Input reference 1 is the game's industrial concept: copy its chunky prerendered proportions, smooth restrained materials, readable masses and consistent three-quarter camera, NOT its background, lighting glow, water, vessels or text.
Input reference 2 is the accepted plain industrial ground diamond: material/texture restraint reference.
Input reference 3 is accepted foundry: smooth stylized dimensional rendering and broad edge bevel reference.
All objects have orthographic isometric camera elevation 35.264 degrees and yaw45, ground diamond edges at +/-30 degrees. Same upper-left soft studio illumination; surfaces neutral lit for later dynamic lighting. Colors slate cool gray concrete, dark desaturated gray-brown timber, charcoal support steel; no orange wood or yellow grass. High resolution smooth prerendered 3D look, no pixel art and no line-art outline.
Each cell fills 76% of width; top flat diamonds occupy x=.12..88, y=.29..73, with top=.51,.29 left=.12,.51 right=.88,.51 bottom=.50,.73 approximately. Use ample padding. Keep symmetric projection.
TOP LEFT: one perfectly FLAT concrete promenade material diamond. No thickness or sides. Four broad concrete paving squares, very subtle joints, quiet mottled slate-gray aggregate, sparse subtle edge wear. No border curb. Edges must reach the full straight diamond silhouette so this can be affinely mapped into exact curved walkway geometry. No fixtures.
TOP RIGHT: one perfectly FLAT weathered wooden marina decking material diamond. No thickness, posts or side faces. About eight broad gray-brown desaturated boards, very restrained grain, slightly bevelled subtle seams; no orange hue, no bright nails, no border frame. Full diamond silhouette for affine world-plane texturing.
BOTTOM LEFT: one substantial raised concrete waterfront promenade and tall concrete retaining seawall module on a square diamond footprint: broad flat upper walkway slab, thick clean pale-gray cap, dark cool gray vertical wall about 1.5m high on two front faces, large readable panel joints and restrained waterline stains, no corrugations. Same restrained real-world stylized material as the foundry foundation. No railing, trees, lamps or bollards. Whole isolated module including front wall bottom visible, nothing else.
BOTTOM RIGHT: one substantial wooden marina dock module on a square diamond footprint: broad flat gray-brown plank top, thick charcoal timber/steel structural rim, four short thick dark timber supporting piles extending beneath it, two visibly nearest corners. No ropes, tires, cleats or lamps. No decorative complexity; broad quiet readable masses.
Genuine transparent background throughout, no water, cast shadow, ground plane, floating haze, reflections, glow, lamps, vegetation, text or checkerboard. Ambient shading ON objects only; no shadow blob below. No perspective distortion. Finished isolated game assets, not a full scene.

Output: `docs/art-studies/montrose/waterfront-v2/waterfront-v2-candidate.png`.

No lamps are baked into the material surfaces. Dock pedestal fixtures and their lighting remain rigid world geometry and dynamic effects owned by the shared renderer.
