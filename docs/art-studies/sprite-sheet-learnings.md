# Barge Boss sprite-sheet field notes

This is an experiment log, not a claim that image generation guarantees accurate
turntables. Record observed results separately from techniques still being tried.

## Lessons from tug v1 and v2

1. **Frame count is not rotation coverage.** A populated 8×4 atlas can still repeat
   angles, reverse the turn, or change bow/stern fittings. Inspect a sequential
   turn and the 31→0 wrap, not just a contact sheet.
2. **Text angles alone were insufficient.** v2's explicit 11.25-degree heading
   table did not enforce those angles. Its silhouettes and fittings drifted.
3. **“Halfway between these poses” repeated source poses.** The v1 half-step
   edit did not establish sixteen reliable new headings. Do not count edited
   near-copies as additional rotational resolution.
4. **Requested resolution is not measured resolution.** v2 requested 3072×1536
   but returned 1774×887. Record actual PNG size and each rounded source crop.
5. **A fixed draw anchor does not make the artwork stable.** Do not center on
   visible-pixel bounds: tall cabins and stacks move that center. Use the hull's
   waterline footprint, and measure fittings relative to that anchor.
6. **Inspect alpha on a real composite.** The inline transparent image preview
   showed alarming colored edge pixels that did not appear in the Java2D teal
   composite. Do not diagnose a halo from one viewer alone. Check dark, light,
   and water-colored backgrounds before regenerating for an alpha defect.
7. **Separate renderable art from development references.** Candidates and
   reference images belong in `docs/`; only accepted atlases belong in runtime
   `assets/sprites/`. Do not inflate every native build with rejected experiments.
8. **Do not hide defects with blending.** Use discrete source frames first.
   Blending may soften stepping after registration, but cannot repair different
   vessel geometry, wrong headings, or unstable camera elevation.

## Guide experiment: observed results

Hypothesis tested: a deterministic projected blockout can constrain headings, silhouette,
fixed hull anchor, and the positions of bow knees/cabin/stack more effectively
than a text-only angle table. The generated bitmap still provides the final
prerendered material treatment; the guide is not replacement game artwork.

The guide uses a fixed orthographic camera with elevation 35.264 degrees,
screen-right bow at heading 0 and bow toward the viewer at heading 90. Ground
depth projects at `sin(elevation) = 1/sqrt(3)`; vertical height projects at
`cos(elevation)`. Every pose rotates the same local 3D vertices by 11.25 degrees.
Hull waterline center stays at `(0.50, 0.72)` of each cell. A ground-plane outline
and its center are known independently of the cabin's visible bounds.

The v3 and v4 outputs follow the guide's heading sequence much more closely than
v2. Bow knees remain at the bow and the turn proceeds through the four cardinal
views in order. These are visual assessments, not independent measurements of
exact 11.25-degree camera yaw in each generated frame.

| Candidate | Comparison guide | Mean raw silhouette IoU | Mean registered IoU | Minimum registered IoU |
| --- | --- | --- | --- | --- |
| v2 text-only | v3 blockout | 0.461 | 0.695 | 0.447 |
| v3 guide-based | v3 blockout | 0.875 | 0.915 | 0.897 |
| v4 corrected guide | v4 blockout | 0.878 | 0.929 | 0.909 |

These are silhouette intersections-over-union on 128×128 alpha≥128 masks, not
percentages of physical accuracy. Each sheet receives one uniform scale and
per-frame translations only. v2's vessel design was also less like the blockout,
so this comparison does not isolate the effect of prompting alone. No source
pixels are edited, rotated or stretched. v4 scale is 0.9671; measured pivots are
saved in `tug-isometric-v4-registration.csv`. These remain candidate anchors.

**Validate the guide itself.** v3's guide sorted whole faces by average depth.
That incorrectly covered the stern winch with the deck at some angles, and the
generated art copied the missing winch. v4 uses per-pixel depth, with a regression
check for visible winch tops at known unobstructed headings. The resulting v4
sheet restores the winch across those views while retaining the improved turn.
High silhouette overlap alone did not reveal the interior equipment defect.

**Detailed equipment still needs a manual pass.** v4 is a stronger registered
candidate, not a proven 3D model. Tires, roof fittings and the winch drum can vary
slightly between painted frames. Smoke/rope attachment points have not yet been
authored, so this revision is not ready for automatic live-atlas replacement.

Both guide-based generations still returned 1774×887 rather than the requested
3072×1536. Geometry guidance improved pose consistency but did not enforce output
resolution. Keep measured sizes in metadata and avoid calling the output 3K.

The reusable workflows now live in `skills/bargeboss-watercraft-sprites/`,
`skills/bargeboss-scenery-sprites/` and `skills/bargeboss-shore-animation/`.
Their scopes deliberately separate floating vessels, static harbor construction,
and moving land machinery/crew.

## Review checklist for future vessels

### v5: design fidelity and effects

The user rejected v4's squat shape because it did not resemble the concept.
Its high guide-overlap score did not establish that the guide represented the
right boat. v5 restores a rounded hull, red lower deckhouse, cream band, raised
pilothouse, tall stack, mast, rails and large fenders. A new single-vessel design
reference supplies materials while a new projected guide supplies the turn.
The standalone design reference has atmospheric glow and is not a runtime cutout.

The v5 sheet is 1774×887. Raw silhouette IoU is 0.699, registered mean 0.854,
minimum 0.809; its single sheet scale is 0.8958. This is a lower geometric fit
than v4 but a closer design match. Report these separately rather than optimizing
the wrong hull to a high score. Pose and detailed fittings remain review work.

The user also requires light, smoke-outlet and tire effect points while making
vessel art. The v5 guide exports 16 stable authored features per pose (5 lights,
1 stack, 10 tires), 512 records across 32 headings. Source-cell estimates are
derived through registration and retain XYZ, radius, normal, identity, side,
visibility and review status. The HTML preview overlays these by category.
Generated extra cabin/roof fixtures and tire details mean the inventory is not
yet pixel-verified; metadata explicitly records that limitation.

Review images are now also copied to the phone's Downloads folder with versioned
`BargeBoss-...` filenames. Repository originals and prompt provenance remain the
canonical files. This delivery preference is recorded in `AGENTS.md` and the
three art skills.

### Checklist

- Lock a vessel specification: bow/stern, length/beam, cabin placement, stack,
  winch, tire spacing and attachment points. Avoid contradictory reference views.
- Keep camera, light, scale and all geometry fixed while changing only heading.
- Generate separate smoke, wakes, ropes, lighting pulses and cargo layers.
- Measure actual output dimensions, transparent gutters and clipping.
- Review the four cardinal views and opposite pairs before all intermediate views.
- Review at gameplay size plus magnified inspection; check dark/light composites.
- Inspect source frames without blending and compare the wraparound seam.
- Keep per-frame hull pivots and effect attachment coordinates in shared Java
  when integrating; all native hosts must consume the same metadata.
- Record prompt, input roles, output, observed failures and acceptance decision
  for each attempt. Do not silently replace the previous candidate.

### Runtime v5 smoke registration

Guide-generated effect positions need their own pixel review even after a hull
fit is accepted. The v5 main stack rim is substantially higher than several
projected guide points. Visually calibrated 31 rim centers; frame 10 has no
reliable visible rim and remains explicitly interpolated/occluded. Keep this
calibration as a separate authored CSV so re-exporting guide metadata cannot
silently overwrite it. Runtime smoke must share the hull's source-frame offset,
short blend, registered pivot and uniform scale. Review these through the actual
native drawing contract, not only the art-study HTML. Square source cells need
square runtime rectangles; retaining the old tug's 1.05 height factor deforms
the approved isometric design.

### Empty barge and cargo guide contracts

The empty barge needs a real recessed floor in its guide: constructing a full
solid deck before adding coaming conceals the well. Render the low hull top as
the floor, then build the walkway/coaming as a ring. Keep a bow-only paint marker
and stern-only fittings so a nearly symmetric barge still has a verifiable yaw.
Track rope attachment and cargo-floor points alongside light/smoke/tire arrays;
unpowered barges explicitly declare empty emitter arrays. Barge v1 exports28
stable anchors per heading, including actual tire contact surfaces.

Separate cargo must share the barge's world-to-cell scale and waterline pivot.
A coal pile's smaller opaque bounds are not a reason to resize it to fill its
cell. Registration is relative to the whole hull's datum, not the pile's ink
center. A complete cargo sprite drawn after the hull can incorrectly cover the
near coaming; inspect this compositing relationship before runtime integration.

The bulk v1 guide now performs a depth-only hull/coaming pass before rendering
coal. This removes coal pixels that should sit behind the near coaming without
including hull pixels in the separate cargo layer. Preserve the resulting cutout
silhouette during material generation. Masked cargo cannot be lowered or resized
during unloading without invalidating the mask; a fixed-position scoop fade is
compatible. Painted-hull differences still require composite inspection.

## Scenery ground-plane alignment (continuous tutorial)

The phone screenshot `Screenshot_20261114-011816.png` exposed a different kind
of registration error: several generated foundations used ground-axis slopes
around 20–27 degrees while the shared camera uses 30 degrees on both axes.
Grid-snapping a world origin and correcting a ground pivot cannot repair that
baked-in perspective. The pipe-rack pivot also had a separate twelve-source-pixel
vertical discrepancy, which can be corrected in metadata without changing art.

For new factory modules, supply an orthographic geometry guide with a measured
footprint diamond and explicit height, not only a prompt saying “isometric.”
Check both visible foundation edges, verticals, the ground datum, and transparent
gutters before accepting the material-rendered result. A tall stack touching
the source edge is a clipped candidate even if the rest of the building fits.
Keep rejected appearance studies separate from production assets.

Whole-cell placement means snapping footprint edges to the planning grid;
centers differ by half a tile for odd/even footprint dimensions. Footprint and
height are separate metadata: a narrow tall stack and a wide low production
hall should retain their distinct silhouettes and setbacks. Use one uniform
source-to-world scale, never a bitmap shear to conceal projection errors.

## Montrose materials, parkland and anchored tree motion (2026-11-16)

The first marina set was visually disconnected despite using the requested camera words: yellow-green grass detail, small foliage lobes and bright diagonal tile rims dominated the harbor overview. Review the **actual shared renderer** with the game's dusk grade, not only the isolated transparent sheet. The replacement park source retains neutral daylight exposure, broad cool-green lawn variation, readable tree masses and gray limestone. Shared scenery grading supplies dusk once. Baking darkness into the source as well would reduce material separation and make future lighting harder to tune. The final overview `out/montrose-overview-v2.png` shows grounded, consistently graded trees and a quieter shore; a residual repeated lawn diamond pattern remains visible at wide views.

### Flat materials require measured projected corners

The waterfront generator returned approximately32.7-degree ground edges despite a35.264-degree orthographic elevation /30-degree screen-axis prompt; lawns measured about29.4degrees. Placement pivots alone cannot correct this. For **flat** timber/concrete/lawn material planes, record actual three-corner source coordinates and solve an affine basis to the exact shared-camera world quad. Clip to that quad and retain an opaque receiving surface underneath. The vertical quay walls are separate shared rigid geometry with their own height. Never use this planar-material method to shear an upright factory, rotate a painted isometric structure, or invent a new camera angle. Small tree bending below is an animation deformation, not a perspective repair.

### Quieter lawns still need repetition control

Replace individual bright tufts and flowers with broad subdued variation. Even a smooth standalone diamond can reveal borders or alternating color blocks after repetition. Inspect a multi-tile join study before integration. In this round the shared renderer samples inside the lawn diamond to omit the generated perimeter rim, maps that sample to exact land corners, and blends it over a solid cool-green ground surface. Use one primary lawn variant rather than strict checkerboarding. Roads, paths, tree groups and shoreline geometry break up large fields. Do not hide seams by changing water collision boundaries, widening land into navigation routes, or preparing new per-frame texture patches. Static material mapping belongs in the existing bounded cached terrain layers.

### Roots, height and wind are separate from camera projection

Tree ground pivots come from visible trunk roots, not canopy bounds or the bottom-center of the sheet. Grouped trees need an explicitly estimated common footprint pivot; individual trunks at different projected ground depths will not all share one source Y coordinate. Preserve those authored ground-depth differences. Record upright silhouette height separately from footprint size, and vary group heights/placement to avoid a flat row of identical canopies.

`MarinaArt.drawAnimated` uses the shared deterministic visual clock and a stable placement seed to shear the vertical image basis gently around the registered ground pivot. The tall/low groups have bounded2.7%/1.9% cell-width shear. Horizontal basis, root anchor and shared camera remain fixed; it does not rotate the entire isometric billboard. This gives readable canopy/trunk motion without a new animation atlas, image allocation or frame-rate-dependent state. A raised land camera and the shadow's explicit receiving-plane rise must represent the same elevation. In Montrose both subtract18 screen-vertical world units before scaling.

Contact and cast shadows remain independent alpha-derived masks at the unsheared root pivot; flat lawns, concrete and timber tops are explicitly noncasting. Keep all receiving-plane masks below all overlapping upright sprites: interleaving each tree's shadow and color artwork can cause a later tree shadow to darken an already painted neighbor's crown. Sort upright props by the shared ground-depth expression after the receiving-plane pass. Root-grounding review alone does not prove correct building/boat occlusion or shadow receiving-surface clipping.

### Output dimensions and transparency are measured contracts

The park generator returned1774×887, not an even cell grid, and its first result was opaque white despite the requested transparent output. Retain that failed source for provenance; request a targeted alpha correction through imagegen and inspect the corrected result on water, light and dark backgrounds. A transparent viewer's colored edge artifacts alone are not proof of a visible halo. Preserve the tool's antialiased source alpha rather than silently thresholding it.

The runtime sheet uses exact444px cells at1776×888. Park crops are copied bit-for-bit into those cells;443px source edges receive1px transparent padding, and pivots compensate by443/444 on the affected axis. The waterfront627px material cells are uniformly downsampled offline to444px. Keep this distinction explicit: source-preserving repacking and uniform resampling have different pixel guarantees. Regenerate corresponding shadow masks from the actual production atlas, verify transparent mask gutters, and keep fixtures/outlet inventories explicitly empty for natural unlit assets. Detailed prompts, bounds, pivots, source mappings, candidate composites and shadow proofs live in `docs/art-studies/montrose/park-v2/` and `waterfront-v2/`.

## Cargo hold refresh — 2026-11-16

- A new hull does not automatically migrate legacy cargo. Repeated pipe/container
  sprites with independent sizes/pivots produced oversized steel, undersized boxes
  and foreground rail overdraw. Define one shared hold first, then author loads
  at its floor height and origin. See [cargo contract](cargo-authoring-contract.md).
- Requesting "no hull" still produced container/coal rim fragments when the style
  concept included loaded barges. A targeted second edit with only the candidate
  and isolated guide removed them. Keep rejected inputs for provenance.
- Whole-load32-pose layers reduce repeated submissions and sorting, but trade away
  indexed per-unit removal: these loads use the existing180-tick quantity fade.
  Future independently lifted units need explicit per-unit geometry/occlusion;
  do not reintroduce loose sizes and offsets.
- Model-derived foreground coaming clipping protects the painted rim in every
  orientation; clip lighting together with cargo. Hull, cargo and clip share heave.
- Source boundary alpha can be1–2/255 without visible contamination on gameplay
  composites. Record measured tolerance; derived shadow gutters stay exactly0.
  Distinct32-cell hashes cannot certify correct3D poses. Low steel silhouette fit
  at a few poses remains an honest generated-art limitation.

## 2026-11-20: specialized equipment and the 64-heading target

All new/revised barge families and corresponding cargo should target **64 genuine
poses**, 5.625 degrees apart, per the latest user instruction. This supersedes
the previous 32-pose authoring target for barges. Never count duplicated views,
interpolation or an unload animation as extra headings. The accepted legacy
painted hull/cargo family is not yet migrated.

The equipment prototype uses real rigid geometry to obtain 64 registered views,
plus normal/height/material exports and separate alpha-derived shadows. Keep
live aiming barrels out of baked decks: they rotate continuously around local
monitor anchors, not as flattened bitmap rotations. The same anchor supplies
the water-stream muzzle. Keep pump exhaust separate from tug stack metadata.
The runtime now supports mixed 32/64 heading families; projection offset is a
fraction of the full turn, never a hard-coded four-cell offset on a 64 sheet.

## 2026-11-21: existing barges and loads at 64 headings

Use a rigid source model for the extra angles. The hull and all three ordinary
loads now share a fixed datum instead of fitting each painted pose separately.
Render the hull into depth first for cargo, clear color only, then draw the load:
this keeps foreground coaming occlusion identical across all 64 angles. Preserve
coverage identically across albedo, normal-height and material exports.

Do not reuse a generic palette classifier when exporting an authored palette:
it flattened the recessed floor into charcoal and erased hull/tire contrast in
the first candidate. The fleet exporter preserves explicit face colors and uses
restrained object-space wear. Surface complexity is simpler than the older
painted assets; predictable geometry is not evidence of identical art quality.

Increasing pose count requires more than the source sheet: use one source
heading-count helper for camera offsets, shadow/glow/shade selection and
waterline interpolation. Regenerate opaque-interior masks and test every new
heading at upper/lower shores. Registration CSVs record real angles and the
fixed datum; do not fill unmeasured IoU columns with invented perfect scores.

Ship frame-sized textures. Full-sheet plus cropped-frame caches can double
resident memory and crowd out scenery. Preserve per-level manifests, including
later spawns, and retire previous-level decoded sources as well as cache pins.
Measure source plus crop storage together, and validate packed pixels against
canonical sheets so stale effects cannot silently ship.
