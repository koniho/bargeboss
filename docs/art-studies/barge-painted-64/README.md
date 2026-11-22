# Painted 64-pose barge study — not approved for runtime

2026-11-21. Built-in image generation/editing, using accepted painted hull art
and the deterministic 64-heading guide. The painted materials are much closer
to the requested style than the rejected geometric export, but this candidate
still fails rigid-vessel continuity and angle checks.

Run `sh tools/review-painted-barge-64.sh` from the repository. It reproduces
quarter guides, lossless packing, registration estimates, projected anchor
metadata, and `out/painted-barge-64/review.html`. The self-contained interactive
review supports 64 views, guide overlays, raw/registered pivots, light/dark/water
backgrounds and estimated tire anchors. No interpolation or bitmap rotation.

## Measurements and acceptance

- Full-sheet first attempt: only 56 cells; rejected.
- Each quarter: 1254x1254, 4x4. Selected versions: 0-v1, 1-v3, 2-v2, 3-v1.
- Packed candidate: 2512x2512, 8x8, 314px cells. Source cells are copied exactly,
  padding the 313px cells by one pixel; no per-frame scaling.
- All 64 guide quarter cells match the corresponding full-turn guide exactly.
  All 64 packed source cells retain identical pixels.
- All 64 source cells contain opaque and transparent pixels; none touches its
  cell edge at alpha>=32. Low-alpha colored fringe is visible in some raw viewer
  output; review on actual backgrounds before making edge claims.
- One uniform registration scale: 0.9293. Mean silhouette IoU improves from
  0.572 to 0.834; worst registered IoU 0.701. These are alignment estimates only.
- Silhouette principal-axis difference versus the projected guide reaches
  15.664 degrees (frame40). This is a diagnostic of outline disagreement, not
  a measured physical yaw error; fenders and changing shape also affect it.
- Frame15/47 approach end-on too early. Frame15→16 changes apparent hull length
  and end hardware. Quarter2 and quarter3 differ in stern deck fittings.
  Silhouette registration cannot correct these defects. The full-turn contact
  sheet makes the inconsistent proportions apparent.
- Quarter1-v2 background repair failed (alpha>=32 reaches many cell edges).
  Quarter1-v3 fixes that export defect, but does not certify its geometry.

Anchor JSON includes 64 ordered frames, 10 tires, 12 contacts, 1 tow point,
4 mooring points and 1 cargo origin per frame. Lights and smoke arrays are
explicitly empty in this guide. All points remain **guide-projected-unverified**;
visible generated fixtures do not yet match consistently. Do not reuse these
as verified runtime anchors. Guide-registration CSVs describe the guide's own
identity transform; their IoU=1 values are not measurements of generated art.

## Remaining work

Do not promote this candidate by counting cells, fitting silhouettes or adding
blends. The watercraft skill requires a consistent full turn. The next authoring
step needs controlled source geometry with finished painted materials, or smaller
authored pose corrections that demonstrably preserve the vessel and exact angles.
Repeated full-sheet angle-table prompting has not achieved that.

Cargo, final pixel-verified anchors, geometry-derived material maps, separate
contact/cast shadows and runtime derivatives remain pending a stable hull.
Producing derivatives for the rejected shape would not make it production-ready.
The accepted 32-pose painted hull/cargo remains active in title and gameplay;
specialized equipment remains at 64 poses. No runtime/installed art changed in
this study. Native title/gameplay validation is still required before promotion.

## Validation

Java 8 authoring tools compiled. Existing 32-pose tug review still exports all32
frames; new barge review exports all64. Both generated review scripts pass
`node --check`. Quarter/full-guide equality and lossless cell copy checked for
all64 frames. This validates tooling, not asset acceptance or native performance.
