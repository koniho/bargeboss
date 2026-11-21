# Painted 64-heading hull revision

Generation: built-in imagegen. Reference1 is the accepted painted hull; reference2
is a pose/registration guide only. Keep the rounded fendered hull, substantial
rubber tires, yellow safety coaming, recessed worn steel well and warm brown/rust
palette of reference1. Do not reproduce the flat-sided primitive appearance of
reference2. Use its heading progression, fixed 35.264-degree elevation, eight
columns/eight rows and waterline origin only.

Render exactly64 real views, yaw0 through354.375 degrees in5.625-degree steps,
row-major. Same rigid vessel at every heading. Bow+X points right at0 degrees;
90 faces viewer. Genuine alpha, no ground/water/wake/smoke/glow/shadows/text.
Square3072px sheet requested, equal square cells, hull waterline anchor at
(0.5,0.72) in each cell, stable size, transparent gutters. Preserve fitted
hardware identities, symmetric four side fenders per side, end fenders, cleats,
rounded bow/stern and empty well. High-quality prerendered painterly game art,
soft bevels, curved tires with recessed centers, restrained textured materials,
legible large forms; no pixel art or raw polygon blockout surfaces.

Review status: candidate only. The accepted runtime art stays in place until
style, true headings, cargo fit and fixed registration have been checked.

## Results and targeted follow-ups (2026-11-21)

The full-sheet request produced 56 cells (8x7), not 64. Retained as
`../art-studies/barge-painted-64/rejected-full-sheet-v1.png`; rejected.

Switched to four 4x4 quarter guides exported by `BargePoseGuide --quarter=N`.
Each request used its quarter guide for geometry/heading only, the approved
32-pose hull for painted materials, and (after quarter0) quarter0 for continuity.
Requested 16 rigid views, 5.625-degree steps, fixed 35.264-degree camera, genuine
alpha, no water/smoke/glow/external shadow, rounded substantial tire fenders,
yellow coaming, worn recessed empty well and the same hardware at every heading.
Quarter ranges were 0–84.375, 90–174.375, 180–264.375 and 270–354.375 degrees.
The actual generated quarter images are 1254x1254, not the requested 1536x1536.

Targeted edits:
- Quarter1 v2: preserve all poses/layout/materials; replace three visible side
  fenders with the specified four. This introduced a partially opaque background
  and was rejected as an export.
- Quarter1 v3: background extraction only; remove backdrop and detached shadows,
  preserve vessel pixels/layout/angles. This is the current review input.
- Quarter2 v2: preserve all poses/layout/materials; replace incorrect near-end bow
  pads with the stern pair of round tires, using quarter3 as hardware reference.

Selected review inputs are quarter0-v1, quarter1-v3, quarter2-v2, quarter3-v1.
`PackVesselQuarters` copies their actual cells without rotation/rescaling into
an 8x8, 2512x2512 candidate. These are generated candidates, not verified rigid
model views. See the study README for failed acceptance checks. No runtime art
was replaced, and no cargo was generated against the unstable hull.
