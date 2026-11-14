# Factory scenery set 1 — sixteen accepted modules

Generated with the built-in image generator, one call per distinct asset or revision.
The visual reference was `docs/reference/industrial-isometric-concept.png`.
All sixteen accepted transparent PNG originals are preserved unchanged in
`assets/sprites/factory-*.png`; their source candidates, exact prompts and geometry
guides remain in this study and `docs/art-prompts/factory-v1/`.

## Accepted inventory

| Index | Module | Reserved cells | Source version | Visible height above ground pivot | Lights | Exhausts |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | Blast furnace | 2 × 2 | v2 | 190.8 | 5 | 2 |
| 1 | Boiler house | 2 × 2 | v3 | 187.1 | 4 | 2 |
| 2 | Coking plant | 2 × 2 | v2 | 189.1 | 3 | 2 |
| 3 | Foundry | 2 × 2 | v2 | 185.8 | 6 | 1 |
| 4 | Rolling mill | 2 × 1 | v3 | 98.8 | 6 | 1 |
| 5 | Assembly hall | 2 × 1 | v3 | 100.1 | 4 | 1 |
| 6 | Power station | 2 × 1 | v3 | 158.7 | 3 | 1 |
| 7 | Pipework gallery | 2 × 1 | v3 | 107.5 | 2 | 1 |
| 8 | Tank farm | 1 × 2 | v2 | 92.5 | 2 | 0 |
| 9 | Filter bank | 1 × 2 | v2 | 171.2 | 2 | 1 |
| 10 | Coal bunker | 1 × 2 | v2 | 85.4 | 1 | 0 |
| 11 | Rail warehouse | 1 × 2 | v2 | 73.8 | 4 | 0 |
| 12 | Pump house | 1 × 1 | v2 | 100.7 | 2 | 1 |
| 13 | Substation | 1 × 1 | v2 | 59.2 | 4 | 0 |
| 14 | Compressor house | 1 × 1 | v2 | 101.2 | 2 | 1 |
| 15 | Gatehouse | 1 × 1 | v3 | 99.5 | 3 | 0 |

Heights in this table are projected visible extents in world display units,
**not measured local Z**. A stack behind the footprint center changes that extent.
The guide-authored height is recorded separately in the anchor manifest.

## Camera and registration

Text-only camera prompts produced attractive but shallow 17–27° foundations and
some edge clipping. Those v1 images are rejected studies, never runtime art.
The successful pass used exact programmatic orthographic guides from
`tools/FactoryGeometryGuide.java`: yaw 45°, elevation 35.2643897°, ground edges
±30°, upright verticals and no perspective convergence. Guide image 1 controls
geometry. Material-reference images were explicitly assigned style only. The
four long v3 modules and gatehouse v3 used the guide alone to remove the earlier
material reference's camera bias. Boiler v3 used the guide plus approved foundry
materials. Other v2 modules retain their prompt's guide/style-reference roles.

Every final foundation was inspected and measured using the bottom alpha
silhouette on explicit straight edge ranges. `grounding-audit.json` records the
fits, ranges, actual bounds, pivot and inferred rear vertex. Both visible axes
are within 1.5° of the shared camera. This is the accepted tolerance for
**freestanding structures**, not proof of seamless terrain joins.

One cell reserves 64 × 64 world units. Actual inverse-projected foundations are
uniformly fitted inside their reserved cells; some have inset service apron
space rather than filling a mathematical 2:1 rectangle. Runtime rendering never
stretches or warps the source. The ground pivot is the inferred foundation center,
not the image bottom or the lowest visible corner. `FactoryArtData` exports the
measured per-image pivot and uniform scale. Raised tutorial terrain shifts both
sprite and every emitter upward by `GroundArt.LAND_RISE` exactly once.

## Lights, smoke and review

`factory-anchors.json` inventories 53 light fixtures/window banks and 14 open
exhaust rims. Window banks are one fixture each; mullions are not separate
emitters. Closed tanks, filters, cabinets and coal bins explicitly have no smoke.
Every source UV point was inspected on labeled overlays of the final artwork.
The three coke doors and two foundry window centers were refined during overlay
review. All points remain attached through shared camera projection and fading.

Local XYZ is an explicitly inferred, non-unique single-view estimate, using a
documented local Y=0 assumption. Normals are inferred upward exhaust directions
or nominal light-facing directions. These are **not pixel-verified 3-D geometry**;
runtime uses the pixel-reviewed UV attachment points. A future camera or model
must re-author XYZ and normals. Stable IDs, radius, color, visibility and review
status are present for every emitter; empty categories are recorded.

`SceneryEffects` supplies organic multi-frequency light breathing and drifting,
expanding smoke from shared visual ticks. Smoke and light are separate from the
PNG; no smoke or halo was baked into the source. The whole structure and its
plume share the gameplay-obstruction fade envelope. The existing tutorial layout
places permanent factory clusters inland and keeps full footprints behind the
raised quay apron; the same factories remain as the player travels to the harbor.

Review outputs are in `reviews/`: light, dark and water-color contact sheets and
four labeled anchor pages. These are inspection composites only, not atlases.
Copies are in Downloads as `BargeBoss-factory-16-v1-contact-{0,1,2}.png` and
`BargeBoss-factory-16-v1-anchors-page-{1,2,3,4}.png`.

## Validation and practical limits

All sixteen actual outputs are 1254 × 1254 RGBA despite the 1024-square prompt.
Total compressed production PNG size is 10,069,211 bytes; full RGBA decode is
about 96 MiB for all sixteen, before host cache overhead. Source pixels were not
resized to hide this cost. Actual opaque bounds have clear gutters. Ten PNGs
contain isolated border alpha=1 quantization noise; no border alpha>=2 occurs.
Preserving those original pixels is deliberate, with no source masking.

Core Java compilation passed. `tools/FactoryArtPixelTest.java` passed 87,313
checks covering the actual PNG bytes, reviewed foundation angles, ground pivots,
uniform occupancy fit, alpha gutters, light anchors and smoke-feature proximity.
This numeric test cannot prove an opening is an exhaust; the labeled source
overlay inspection provides that semantic review. Native renderer/device checks
and the combined tutorial render are performed by the main integration workflow.

Rebuild metadata with `python tools/export_factory_metadata.py`; this copies
accepted immutable PNGs and emits the JSON and `FactoryArtData.java` arrays.
Regenerate inspection composites with `tools/FactoryArtReview.java` after
compilation. A source revision requires fresh edge ranges, registration and
labeled-anchor review before promotion.

## Reusable learnings

- Exact geometry guides outperform numeric camera words alone. A style reference
  can pull the camera off-axis even when the prompt says otherwise; remove it
  when repeated short-edge drift persists.
- Measure both visible axes, especially the short edge of a rectangular module.
  A visually convincing long edge can hide a 2–3° short-edge mismatch.
- Requested image size, footprint ratio and guide height are not guaranteed.
  Register what was actually generated, preserving one uniform scale.
- Reserve logical grid occupancy separately from the actual freestanding base.
  A clean service apron absorbs small ratio differences without pixel warping.
- Ground-center pivots can lie visually on a front wall because the building
  occludes its own footprint center; bottom-center pivots cause sliding bases.
- Review every actual warm fixture and open rim after generation. Do not reuse
  emitter coordinates from a superseded candidate or emit from capped pipes.
