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
