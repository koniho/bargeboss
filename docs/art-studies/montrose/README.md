# Montrose marina assets and mission

The playable level adapts the official Montrose Harbor dock map:
https://www.chicagoharbors.info/harbors/montrose/ and its linked
https://www.chicagoharbors.info/wp-content/uploads/2013/04/montrose-zoom4.jpg .
The southwest inlet, enclosed broad basin, north-shore finger docks and parkland
inform the layout. The game enlarges the fairway and yacht slip. This is not a
navigation chart or a claim that the real harbor accepts an 80-foot yacht;
the official harbor page lists 25–50 ft accommodations.

## Art provenance and registration

The yacht and scenery sources were created with built-in image generation. The
marina follows `docs/reference/industrial-isometric-concept.png` for chunky,
readable materials and the same dusk grade as the industrial stages. New neutral
concrete/timber sources and quieter parkland replace the earlier marina candidate.
See [waterfront source review](waterfront-v2/README.md) and
[park runtime integration](park-v2/runtime-integration.md) for measured corners,
crops, pivots and shadow inventories. The production marina atlas is 1776×888,
with eight 444px cells. Prompts and original candidates remain alongside the
review history. Flat materials are affine-mapped onto exact world geometry;
raised reference modules are not rotated as billboards.

The yacht follows `YachtPoseGuide.java`'s orthographic rigid model. Its actual
source output is 1774×887; requested dimensions were not delivered. Source alpha
is preserved. The transparent viewer shows bright RGB in near-transparent pixels;
composites on dark water are clean, so inspect composited output before treating
hidden RGB as a visible defect.

The polygon shoreline traces the official hooked peninsula and broad basin.
Fifteen main finger rows, five star docks and the western mooring field reproduce
the recognizable dock arrangement. Tug and yacht start outside both breakwaters.
The two entrance beacons sit at the mapped pier tips, with fixed red/green towers
and three-second flashes. Riprap continues along both entrance banks, including
the port side when returning. A raised concrete seawall and continuous perimeter
walk separate the park from the water. Terrain remains static and cached; upright
trees have distinct heights and a restrained independent shear sway around their
fixed roots. Tree shadows remain anchored. The small lawn inset avoids painted
tile rims; it is a repeated material, not seamless aerial photography.

The yacht has 32 guide-matched headings, including front/rear/opposite views.
`yacht-registration.csv` records uniform scale 0.9690734 and translated pivots:
mean silhouette overlap improves .894 → .939, minimum .904. Bow/stern identity,
window bands, white fenders, mast and 31→0 progression were visually checked on
the contact sheet. Silhouette fitting does not verify every physical feature;
fine rail/fender details still vary slightly. No mirrored or rotated 2-D frames
are substituted for the authored views. World hull length 110 maps to nominal
80 ft, beam 30; radius 15 and disc half-spacing 40 drive collisions, independent
of artwork. Mass 6.5, angular inertia, anisotropic drag and tow impulses use the
same physics as other craft.

## Effect anchors

`yacht-80ft-v1-guide-anchors.csv` contains stable named local XYZ, normals,
projected UV, radius and estimated visibility for every heading. All positions
remain **guide-projected estimates**, not manually verified final-pixel anchors.
The waterline origin is model (0,0,0), guide UV (.5,.72), mapped through the
registration pivot and uniform scale. Model lengths multiply by 1.1 in world
units. Mast `light.mast` is (-12,0,42), nav port/starboard (24,±9,14), stern
(-48,0,11); six `fender.port/starboard.0..2` anchors are at X -30,-12,6,
Y ±14.5, Z 5. `tow.bow` is (46,0,8), `mooring.stern` (-45,0,9).
The generic rope rig currently uses ±(half+7), a nearby gameplay cleat estimate,
and shared deck height 4; it does not claim exact rendered cleat attachment.
Smoke-stack collection: **empty** (engines off; no visible stack). Tire collection:
**empty**; this yacht uses white cylindrical fenders. Runtime nav lamps use
continuous body-local transforms. Mast has a softly animated service-light emitter at the projected model point;
its placement remains an estimate awaiting per-heading pixel review.

Marina atlas inventory: 0 timber top, 1 concrete top, 2/3 lawn variants,
4 tall tree group, 5 lower tree group, 6 limestone pile, 7 directional riprap.
Every raster cell has empty light/smoke/steam/machinery collections. Flat cells
0–3 are noncasting. Trees and rocks have measured ground pivots and estimated
height extents in `MarinaArt`; trees are set back from the shoreline.

The 132 dock power pedestals are separate rigid fixtures, one between each pair
of slip-finger stations and one per star hub, plus the northern service floats. Their warm lights breathe subtly
with independent phases. Light positions share the same rigid model used to draw
the housing. Cached feathered pools sit below vessels; the upright fixtures merge
with hulls, towline spans and markers by ground depth. Nearby watercraft receive
surface-light response through a bounded spatial lamp index. Fixed entrance
beacons use the reviewed land-marker atlas and deck-clipped shadow/light pools.

## Shadow/lighting data

`GenerateShadowMasks` produces independent near-contact and soft-cast masks:
128 px cells, 96 px inner support, 16 px padding; sigma 1.0 / 3.2. Pivot mapping
is `(16 + 96 * sourcePivot) / 128`, scale `sourceScale * 128 / 96`. Yacht masks
cover all 32 headings and follow hull blending; scenery masks cover all eight
cells but only tree/rock masks are drawn. Flat materials do not cast a shadow. These are
alpha-derived silhouettes, not reconstructed 3-D light occlusion. Vessel
shadows stay on clipped water below the bobbing hull, scenery shadows on ground.
Separate gold/mint glow masks and four surface-shading response directions are
prepared offline. `GenerateOcclusionMasks` supplies the yacht's waterline foam
and conservative opaque hull mask; no barge cargo is drawn on this hull.

## Review and reproduction

Compile `tools/MontroseReview.java` with core/desktop sources, then run
`com.bargeboss.game.MontroseReview`. It exports real shared-renderer full-harbor,
inlet and slip views to `out/`. `MontroseHarbor.overview` is a review camera;
ordinary gameplay retains following/transit/docking zoom. `MontroseTest` and
`ReplayInput.marinaTow` use public native-coordinate gestures through the complete
route, without teleporting or modifying velocities. Existing save IDs remain
stable; Montrose is optional and selectable on challenge page two. Goal requires
a connected tow through the inlet plus full-hull, aligned, low-speed settling
for 90 ticks. No global deadline, unloading animation or yacht removal. Native iOS rasterization
requires a device check; shared Java traces alone cannot establish native pixels.
