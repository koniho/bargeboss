# Vessel foam: observed behavior and sprite implementation

Reviewed 2026-11-14. Replaces the thick stroked hull rim and procedural attached
wake strips. This is a visual approximation, not a fluid solver.

## References and observations

- [University of Minnesota field study, July 2025](https://www.rpbcwd.org/files/b986166d9/A%2BField%2BStudy%2Bof%2BRecreational%2BPowerboat%2BHydrodynamics%2Band%2Btheir%2BImpacts%2Bon%2Bthe%2BWater%2BColumn%2Band%2BLakebed_UofM_July%2B2025.pdf)
  distinguishes hull-displacement waves from the turbulent propeller jet. Its
  recreational-boat measurements are not tug-specific calibration data.
- [Astilleros Armon, Cap Bougaroun](https://astillerosarmon.com/es/remolcadores/item/2001-cap-bougaroun.html)
  has a useful aerial photo of irregular twin wash patterns: bright filaments,
  swirling patches and substantial clear water between them. This azimuth tug
  is a visual reference, not a claim that our pushboat has identical propulsion.
- [Wärtsilä wake-field](https://www.wartsila.com/encyclopedia/term/wake-field)
  describes nonuniform water flow behind the hull and at the propeller.

Game interpretation: movement creates intermittent foam at the leading shoulders
and short streaks passing aft. Barges have displacement foam but no powered wash.
Tug wash responds to engine loading, including high loading at low speed; astern
thrust reverses jet drift at the stern fittings. Idle whitewater is suppressed.
Released patches expand, drift and fade independently of the source vessel.
The existing expanding wake-wave system remains separate from foam bubbles.

## Asset

`assets/sprites/vessel-foam-v1.png`: generated transparent 2×2 atlas, with
horizontal broken foam streaks in cells 0/1 and dense/dissipating prop-wash patches
in cells 2/3. Final padded image-generation revision retained without repainting or alpha replacement.
Padding validation permits only invisible 1/255 alpha quantization residue.
The four textures are variations, not claimed consecutive fluid-simulation frames.
Animation comes from particle expansion/advection/fade and crossfade of older
wash to the sparse patch. No shadows, lights or smoke anchors: these are flat
water-plane effects, not raised watercraft/scenery geometry.

Generation prompt:

> Create a transparent RGBA sprite atlas, 2 columns and 2 rows of equal cells.
> Each cell contains one different isolated realistic river-boat surface foam
> patch, viewed exactly straight down onto the water plane (the game will
> project it isometrically). Top left: fragmented narrow elongated breaking
> bow-wave foam streak, long axis horizontal, wispy torn ends. Top right:
> another broken narrow wake streak with slightly curved organic eddies, long
> axis horizontal. Bottom left: roughly oval churn patch of dispersed white
> bubbles and turbulent filaments from tugboat propeller wash. Bottom right:
> dissipating older sparse prop wash with more transparent gaps. Natural scale
> variation, irregular lace-like bubble clusters, creamy off-white highlights,
> subdued pale gray-blue semitransparent filaments. Sparse broken foam with
> transparent holes and surroundings, no solid water underneath. Clear padding
> inside each cell. No boat, text, grid, shadow, glow, background, solid discs,
> cartoon outlines or perfect circles. Flat surface decals, not spray or smoke.

Padding refinement prompt: preserve the four textures, shrink each into the
central 60% of its quadrant with empty padding on all sides, remove stray haze
and gray water film, keep irregular pale foam and transparent holes. The final
output is 1254×1254 with four equal 627×627 cells.

## Rendering and maintenance

`SpriteAtlas.FOAM` is atlas 31. `VesselWaterline` uses at most seven small contact
patches, interpolated along the same registered lower silhouette as the hull.
`WaterSurface.foam` owns a bounded 160-slot pool, advanced only at 60 Hz. Sources
are velocity-driven hull shoulders and load-driven tug propellers. Each patch
stores its own position, velocity, orientation, age and lifetime; no render-time
simulation and no source-relative trail rotation. Flat decals use the shared
isometric affine projection; they do not need 32 directional painted frames.

Detached foam draws with water effects before hulls and solid scenery. Contact
foam draws along the lower hull after the sprite. Both must use the all-dry-land
shore cutout. Foam is not a vessel silhouette or a building-fade trigger.
Native painters only execute shared sprite/clip commands on all platforms.

Review `out/waterline/*.png` for quiet idle, cruise, slow loaded push and astern
travel. Preserve pixel shore regressions, source independence, no barge prop
wash, render purity and the dense-scene submission budget.
