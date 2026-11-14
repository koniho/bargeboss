# Isometric barge and coal integration

Built-in image generation used the existing industrial concept for atmosphere,
the approved tug design for material treatment, and deterministic geometry guides
for 32 poses. Sources remain unchanged; registration is shared Java metadata.

The empty barge is 77.5 by 35 world units with a shallow recessed hold, rusty
coaming, yellow rim, bow push pads and stern tow fittings. The separate coal pile
uses the same waterline origin. Its guide is depth-tested against the barge so
foreground coaming cuts are already represented in the cargo silhouette. This
allows quantity fading during unloading without sliding cargo over the rim.

| Candidate | Actual PNG | Uniform scale | Mean registered silhouette IoU | Minimum | Opaque pixels on cell edges |
| --- | --- | --- | --- | --- | --- |
| Empty barge v1 | 1774 × 887 | 0.9444 | 0.905 | 0.867 | 165 |
| Empty barge v2 | 1774 × 887 | 1.0668 | 0.898 | 0.876 | 32 |
| Empty barge v3 | 1774 × 887 | 1.0491 | 0.896 | 0.874 | 0 |
| Coal v1 | 1774 × 887 | 0.8442 | 0.892 | 0.834 | 0 |

V1 was rejected for clipping and neighboring-cell remnants despite a high fit
score. A uniform gutter edit improved packing; a targeted follow-up moved the
two remaining crowded first-row cells upward. V3 has clear cell boundaries and
is integrated as `assets/sprites/barge-isometric-v3-32.png`. Coal is integrated as
`assets/sprites/bulk-isometric-v1-32.png`. Both use all 32 source cells, one uniform
scale per sheet and square runtime rectangles. No per-frame stretching, bitmap
rotation or pixel cleanup scripts were used.

Silhouette scores do not establish exact physical fidelity. Fine cleat, tire and
rust details still vary between generated views. The barge's 896 guide records
track ten tires, twelve contact points, four mooring points, one tow point and
one cargo origin per view. Lights and smokestacks are explicitly empty because
the barge is unpowered. These detailed points remain guide-projected estimates;
this integration does not promote them as pixel-verified effects emitters.

`tools/export-barge-runtime.py` reproduces runtime pivot and scale arrays from the
reviewed registration CSVs. `tools/preview-vessel-anchors.py` supports an
interactive source/guide overlay including the expanded rope/contact categories.
Prompts: `isometric-barge-v1-design.txt`, `isometric-barge-v1-32.txt`,
`isometric-barge-v2-gutters.txt`, `isometric-barge-v3-gutters.txt`, and
`isometric-bulk-v1-32.txt` under `docs/art-prompts/`.
