# Cargo hold art — built-in image generation, 2026-11-16

Mode: built-in `image_gen`, transparent background. No CLI fallback or raster
cleanup scripts. Three initial independent requests; two targeted extraction
edits. Sources copied unchanged from generated_images into this repository.

Geometry references: `docs/art-studies/cargo-{steel,container,coal}-isometric-v1-pose-guide.png`.
Style-only reference: `docs/reference/industrial-isometric-concept.png`.

## Shared initial prompt

Production Barge Boss cargo sprite atlas. First image is a STRICT 8-column by
4-row geometric pose/mask guide; second image is STYLE REFERENCE ONLY. Render
exactly the same objects at exactly those 32 positions and 11.25 degree heading
increments, orthographic elevation 35.264 degrees. Preserve the guide's
silhouette, empty gutters, waterline datum and near-coaming cutoffs: the lower
chopped edges are intentional because these will sit behind a barge rail. No
hull, no background, no water, no smoke, no cast shadows or halos, no text, no
additional props. Genuine transparent background. Maintain an 8x4 grid with
generous gutters. High-resolution prerendered painted 2D industrial arcade art,
broad readable material shading, warm rusty details, cool dusk shadows, matched
to the concept's barges; not pixel art, not black cartoon outlines or noisy
microtexture. Give the objects realistic three-dimensional faces consistent
across the full turn. Do not rotate or mirror one bitmap to fill cells. Use the
same dimensions and camera for every pose.

Steel addition: Six silver steel coils in two parallel rows of three on dark low
wooden saddles. Preserve each coil's hollow center and exact guide dimensions.
Clean broad steel faces, restrained rolled-metal rings and warm edge glints.
NOT pipes.

Container addition: Three rust ochre shipping containers end to end. Each fills
the width of the hold. Readable broad corrugated panels and subtle roof ribs,
muted ochre/orange palette, realistic low contrast wear, no labels or writing.
Respect guide proportions and all lower cutoffs.

Coal addition: One full broad coal load, dark charcoal with broad irregular
lumps and restrained sparse cool edge highlights. Preserve the broad low guide
footprint and raised center; avoid small isolated heap, glitter, silver rocks
and excessive fine detail.

## Container and coal v2 correction prompt

Edit the FIRST image only: it is an 8x4 transparent cargo atlas. Remove ALL of the
surrounding barge/hull/deck/base/rail/rim/coaming from every cell. Preserve only
the [coal pile / three orange shipping containers]. This is an overlay for a
separately rendered existing barge: there must be NO second hull or railing in
this image. Preserve the exact 32 poses, object identity, paint style, placement
and canvas; no new details. The SECOND image is the precise desired isolated
cargo silhouette/near-rail cutoff guide: reproduce its lower cut edge, NOT a
support/platform under it. Match that silhouette and clip away pixels hidden
behind an invisible near rail. Clean alpha, transparent gutters and no shadows
or glow. Strictly remove supporting hull/rim/rail pixels, leave their areas
transparent, do not fill with dark rectangles. Maintain 8 columns and 4 rows.

## Results and limits

All candidate sheets measured 1774x887, rather than guide resolution3072x1536.
Initial container/coal included unwanted rim pixels despite the exclusion prompt;
v2 extraction removed those structures. Reviewed candidates: steel v1, container
v2, coal v2. Source alpha at some cell edges is1–2/255; opaque content has clear
gutters. The contract records this tolerance, not a false claim of perfect alpha.

Registered uniform scales / mean silhouette IoU / minimum:

| Cargo | Scale | Mean | Minimum |
| --- | ---: | ---: | ---: |
| Steel v1 | .7967 | .799 | .657 |
| Container v2 | .9248 | .914 | .848 |
| Coal v2 | .7949 | .873 | .809 |

Fitting uses one scale per entire sheet and translation only per pose. These are
shape-registration measurements, not proof of exact3D reconstruction. Coil rim
and fine texture details vary across generated views; review source cells and
actual vessel composites rather than claiming pixel-perfect geometry.

Shared CargoView clips source color and dynamic shading below the two foreground
coaming edges. All layers retain the hull waterline datum and heave. No frame is
stretched independently or rotated as a flat bitmap. Unloading uses a whole-load
quantity fade over the existing180ticks; hull and dock operations persist.

`GenerateCargoShadows` derives separate contact/cast masks from accepted alpha
using the same128px cell,96px inner source,16px padding and sigma1.0/3.2 kernels
as hull shadows. Metadata declares the hold floor as receiving plane. These
reusable masks are delivered under art-studies; runtime retains the existing
hull-only water shadow to avoid double-darkening water. `GenerateSurfaceLighting`
regenerates all three active cargo response sheets. Both are derived effect data,
not repainting or synthetic replacement of the generated color sprites.

## Coal v3: fuller source geometry

The v2 in-game review still looked underfilled. Changed the model, not bitmap
stretching: intermediate coal ring height7.5→12, inner ring11.1→17 and peak16→20,
while retaining the same hold footprint/floor and camera. Saved the new guide as
`cargo-coal-isometric-v2-pose-guide.png`, leaving the original guide available.

Prompt: Create a final transparent8x4 cargo sprite atlas by painting the FIRST
geometric guide with the dark coal material from the SECOND image. The first
guide has a taller, fuller load than the old second image: FOLLOW THE FIRST
GUIDE'S SILHOUETTE AND PLACEMENT in all32cells, including its lower cutoffs for an
invisible foreground barge rim. Material only from second: dark charcoal lumps,
broad readable shading, sparse subdued cool highlights. Precisely32distinct
11.25degree poses, fixed35.264degree orthographic elevation. Preserve generous
cell gutters and entire canvas grid. Cargo ONLY: NO hull, NO rim, NO orange frame,
NO rails, NO support platform, NO surrounding glow/shadows/water. Transparent
background. Keep fixed datum and silhouette of first guide. Do not shrink back
to the old second reference's low mound; make the higher broad pile from the first
guide, with chunky dark coal surfaces. No text.

Final coal v3 measured1774x887; uniformscale.8464, meanIoU.889, minimum.829.
Final accepted color families are steelv1, containerv2 and coalv3. Earlier coalv2
measurements above are retained as experiment history, not the integrated source.
