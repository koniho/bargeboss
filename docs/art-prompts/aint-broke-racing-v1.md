# Ain't Broke — red offshore racer

A mission-only56×14-unit racing hull, larger and lower than the existing39/44-unit
cabin cruisers. Long tapered foredeck, red fiberglass shell, cream racing flash,
low windshield, twin bucket seats, open cockpit, engine hatch vents, sterndrives
and a removable anchor-light pole. No cabin, canvas roof, mast sail or exhaust stack.

`RacingBoatBake` authors one fixed model and renders32 real11.25-degree headings
through the same128px/96-world-unit camera as the accepted marina sprites. Uses
the existing2× supersampled depth buffer, not average face sorting. The model is
an offline authoring source; production has no live3-D boat path.

The built-in imagegen material request: seamless opaque vermilion painted
fiberglass, subtle hand-painted variation/scuffs, no objects/panels/words, no
perspective or directional light. Canonical texture:
`docs/art-studies/aint-broke/red-fiberglass-v1.png`. Texture is sampled in hull-local
coordinates, so paint stays attached through rotations. It is not a generated
turntable with independently changing hull shapes. The accepted six existing
marina models are retained pixel-for-pixel by the append installer.

Source exports beside the texture: color, octahedral world-normal/height and
roughness/material maps with identical alpha/cells/pivots. Legacy marina normal
encoding remains RG octahedral normals, B=height0..64; do not confuse it with the
separate signed-height prototype. Material R=roughness, G=emission0, B=reserved0.
Runtime uses existing normal-map point-light reception; roughness is authoring
metadata for the future full-material renderer, not a new shader claim.

Color/normal runtime frames192..223 extend the existing marina receiver family.
Four color/receiver bands now have224 frames each. Bimini palette stride explicitly
stays192 because the racer has no canvas; do not remap existing1536 canvas files.
`RacingBoatArt` contains named local XYZ light/cleat/tow anchors and explicit empty
smokestack/tire/fender collections. Frame pivot remains(.5,.7), no per-pose stretch.
Navigation-light state follows the same anchored/underway rules as rescued cruisers.

Independent contact/cast masks derive from registered source alpha, flattened onto
the water plane, with1/3-pixel blur radii. They are stylized silhouette shadows,
not reconstructed physical self-shadowing. Cast offset is fixed to the authored
ambient direction; point-light cast-shadow occlusion remains unsupported. Masks
stay at the waterline while the hull bobs; no glow/wake/smoke enters the masks.
64 runtime shadow PNGs are frame-sized128×128 with transparent gutters.

Reproduce with shared classes plus `BakeMooredBoats`, `RacingBoatBake` and
`InstallRacingBoat` compiled together. Running `BakeMooredBoats` regenerates the
legacy fleet and then appends the racer; `InstallRacingBoat --check` verifies
runtime colors, normals, receiver bands and every shadow frame. `RacingBoatReview`
checks32-pose map coverage/gutters and exports actual renderer reviews. Review
opposite views and31→0 transition, not only distinct pixels.
