# Isometric barge v1 geometry and registration

This is deterministic pose guidance, not finished game artwork. The concept is
`docs/reference/industrial-isometric-concept.png`. `tools/BargePoseGuide.java`
creates one rigid empty barge through 32 yaw angles, 11.25 degrees apart, at an
orthographic camera elevation of 35.264 degrees. Frame 0 points bow right,
8 toward the viewer, 16 left and 24 away. The yellow chevron belongs only to the
+X bow, while the central tow fitting and two end tires belong to the stern.

The fixed world dimensions are length 77.5, beam 35, waterline Z 0, hold floor Z 3,
walkway Z 5.8, and coaming rim Z 11. Source cells are 384×384 in an 8×4 atlas,
3072×1536 total. The waterline pivot is (.5,.72), scale is 384*.78/77.5 pixels per
world unit. The broad flat bow has two push pads. Cargo is separate from the
charcoal hull and rusty red coaming; the floor is actually recessed rather than
painted on a solid elevated deck. Per-pixel depth preserves the well and cleats.

Every frame has 10 stable tire identities,12 contact points (10 tire contacts
and 2 bow pads), 4 mooring points, 1 stern tow point, and 1 hold origin. `lights` and
`smokestacks` are explicitly empty because this unpowered design has no such
fixtures. Positions include local XYZ, normals, radius, side, role and projected
visibility. Guide-derived visibility is an estimate, not a measured property of
future generated sprites. The ten tire contacts lie at each torus's outer normal
surface; their radius describes contact thickness, not the tire's full radius.

## Reproduce and register

```sh
mkdir -p out/barge-guide-check
javac -Xlint:-options --release 8 -d out/barge-guide-check tools/BargePoseGuide.java tools/VesselRegistration.java
java -Djava.awt.headless=true -cp out/barge-guide-check com.bargeboss.game.BargePoseGuide
python tools/project-vessel-anchors.py docs/art-studies/barge-isometric-v1-guide-anchors.csv docs/art-studies/barge-isometric-v1-guide-registration.csv docs/art-studies/barge-isometric-v1-pose-guide.png docs/art-studies/barge-isometric-v1-guide-anchors.json
python tools/preview-vessel-anchors.py docs/art-studies/barge-isometric-v1-pose-guide.png docs/art-studies/barge-isometric-v1-guide-anchors.json out/barge-guide-check/review.html
```

`guide-registration.csv` is an identity transform for the deterministic guide,
not a fit to a generated candidate. After generation, measure the actual image
size, inspect heading order and body identity, then run:

```sh
java -Djava.awt.headless=true -cp out/barge-guide-check com.bargeboss.game.VesselRegistration docs/art-studies/barge-isometric-v1-pose-guide.png CANDIDATE.png REGISTRATION.csv
python tools/project-vessel-anchors.py docs/art-studies/barge-isometric-v1-guide-anchors.csv REGISTRATION.csv CANDIDATE.png CANDIDATE-anchors.json
python tools/preview-vessel-anchors.py CANDIDATE.png CANDIDATE-anchors.json out/barge-guide-check/candidate-review.html
```

The reusable exporter now preserves contact, tow, mooring and cargo-origin
collections as well as lights, smoke and tires. Existing tug anchor records were
compared before/after the exporter extension: all 512 retained identical values.
The standalone review page displays every category and stable ID without editing
source pixels. Do not use per-frame stretching or judge orientation by silhouette
IoU alone. Recheck cleats and fenders against the generated pixels.

Validation: all 32 empty-hold center checks and transparent source-cell gutter
checks passed. All 896 anchor records have consistent identities, finite normalized
coordinates and explicit visibility. Guide sheet visually inspected, including
opposite directions and the bow-only marker.

## Separate bulk guide

`BargePoseGuide --bulk` creates `bulk-isometric-v1-pose-guide.png` and matching
CSV metadata using the exact same world/cell transform. The asymmetric angular
coal heap has an approximately 48×22 footprint, floor Z 3, and peak Z 13. It has no
hull, glow, water, lights, smoke or tires. It retains `cargo.origin` at the barge's
hold floor. Shape asymmetry is fixed in local coordinates for all 32 frames.
Use the same exporter/review commands with the `bulk-isometric-v1` prefix.

The coal guide includes a depth-only hull/coaming pass. Only visible coal pixels
survive, with deliberate cutout edges where the near coaming occludes the pile;
the hull itself is fully transparent. A material generation must preserve these
cutouts rather than reconstructing a whole free-standing heap. Fade the coal
at its fixed scale/position during unloading: moving or shrinking a pre-masked
pile invalidates the coaming alignment. The generated barge may differ from the
geometry guide, so the mask remains approximate until the painted composite is
reviewed in all headings.

Registered barge candidate anchors were exported to
`barge-isometric-v1-anchors.json` using the generated candidate and its own
registration CSV. All 896 points remain guide-projected and the actual fixture
inventory remains unverified. Do not assume image generation retained exactly
ten tires or four cleats because the projected metadata does.

### Painted v1 packing rejection

The first painted v1 sheet is not approved for runtime: the parent's packing
review found edge pixels in 13 cells and neighboring-cell fragments in the
contact sheet. The projected v1 anchors are study estimates only. Keep the
candidate as provenance and re-run registration/anchor projection against the
corrected v2 sheet; do not reuse v1 source pivots after the artwork is shrunk
or recentered. A good silhouette fit does not establish clean atlas gutters.
