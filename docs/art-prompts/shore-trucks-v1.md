# Industrial dock trucks v1

Generated with the built-in imagegen tool on 2026-11-16. Source asset:
`assets/sprites/shore-trucks-v1.png`. This is a new static-prop candidate sheet;
it does not replace or register any existing runtime sprite.

## Reference and camera

The user's industrial tileset (`codex-clipboard-zTSHm0.png`) is the primary
material/style reference. The harbor game scene (`codex-clipboard-Gq2brQ.png`)
is secondary. Both were supplied directly to imagegen after inspection.
The requested camera matches `RiverCamera`: 45° ground yaw, 35.264° elevation,
30° projected ground axes. Warm upper-left light, cool fill, ochre paint,
charcoal chassis, rust seams, dark rubber and worn wooden beds.

## Layout and validation

Actual output is **1254 × 1254 RGBA**, a 2 × 2 sheet of **627 × 627** cells.
The requested 2048-square resolution was not returned; use actual dimensions.
Cells are row-major:

| Index | Truck | Cab direction |
| --- | --- | --- |
| 0 | Empty flatbed | Lower left, front/side visible |
| 1 | Empty flatbed | Upper right, rear/opposite side visible |
| 2 | Three strapped steel coils | Lower left |
| 3 | Three strapped steel coils | Upper right |

Visual inspection confirms four isolated trucks, consistent industrial materials,
three coils per loaded truck, and distinct front/rear views rather than mirrored
art. No UI, lettering, broad ground patches or detached shadows are present.
There are 931,916 fully transparent pixels, 877 fully opaque pixels and 639,723
partially transparent pixels in the generated source. All four cell boundaries
contain at most alpha 1/255 residue. Preserve the source alpha; any future packing
check should explicitly allow that residue or request a clean regeneration.

The prompt requests ground-footprint pivots at normalized **(0.50, 0.65)** in
each cell, approximately **(313.5, 407.55)** pixels. These are intended anchors,
not measured production registrations. Generated truck placement, wheelbase,
exhaust and cab details vary slightly between views. Empty/loaded pairs must be
registered against tire contact points before any in-place load transition.
Use uniform scaling and retained frame-sized textures. Do not rotate these
painted views in screen space or claim a continuous 32-heading animation.
For animated trucks, author a rigid model or a properly registered directional
family first. Coils here are static scenery cargo, not replacements for the
registered barge cargo family. No native rendering, scene occlusion or device
validation has been performed for these new assets.

## Exact generation prompt

```text
Use case: stylized-concept. Asset type: Barge Boss transparent on-shore truck sprite atlas.
Use the attached industrial tileset as the PRIMARY style and material reference, and the attached harbor game scene as SECONDARY style reference. Create a new truck sheet; do not reproduce either scene or its labels.
Exactly FOUR isolated sprites in an evenly spaced 2 by 2 grid, equal square cells, square 2048 by 2048 image. Top-left: a weathered six-wheel dock flatbed truck, empty, cab pointing toward lower-left of screen. Top-right: the SAME truck empty, turned 180 degrees on the ground so cab points upper-right; show the real rear/right view, NOT a mirrored front. Bottom-left: the same lower-left-facing truck carrying three heavy steel coils strapped securely into wood chocks on its bed. Bottom-right: the same loaded truck facing upper-right, its actual opposite view. Keep each truck at precisely the same scale and center its ground footprint at 50% cell width and 65% cell height. No object may cross its cell; broad transparent gutters.
Camera fixed orthographic isometric: ground yaw 45 degrees, elevation 35.264 degrees above horizontal, ground axes slope 30 degrees. Vertical vehicle posts remain vertical. No perspective convergence, no camera changes. Render genuine 3D opposite poses instead of rotating a painted bitmap.
Match the reference's detailed industrial miniature rendering: chipped ochre-yellow steel cab, charcoal chassis, orange rust seams and patches, dark rubber tires, worn wood flatbed, dark blue-gray window glass, subtle metal highlights, small ladders/steps and robust mudguards. Steel coils gray galvanized metal with dark visible bores, no glow. Painterly realistic industrial game asset, readable chunky masses and restrained fine surface grain, not pixel art. Consistent soft warm upper-left lighting and cool fill in every cell, lighting does not mirror between poses.
Genuinely transparent alpha outside all four truck silhouettes. No ground plane, foundation, dock, water, smoke, people, environment, broad cast shadows, vignettes, checkerboard, text, numbers, brands, logos, grid, caption, UI or border. Preserve clean antialiased edges and all wheels. These are isolated STATIC scenery sprites for a later shared-Java composition pass, not a full illustration.
```

Canonical reference copies: `docs/art-studies/industrial-shore-v1/tileset-reference.png`
and `docs/art-studies/industrial-shore-v1/scene-reference.png`.
