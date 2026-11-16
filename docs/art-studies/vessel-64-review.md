# 64-heading vessel authoring progress

The runtime remains32 genuine authored frames. A user-requested64-heading
upgrade needs32 new hull poses plus coherent cargo, effects and registered
anchors. Changing direction modulo alone would duplicate or misregister art.

`TugPoseGuide` and `BargePoseGuide` now accept `--64` (64 real model rotations,
5.625 degree steps), `--intermediate` (32 odd poses offset5.625 degrees), and
`--proof` (first four poses). Existing command defaults/output remain intact.
Mode suffixes prevent new experiments overwriting accepted guide files.
The barge tool generates matching steel/container/coal guides using its existing
cargo flags.64-pose guides retain the same mechanical geometry and camera.

Validation: five families' even64-grid poses exactly match the original32-grid
images, pixel for pixel (`VesselGuideCheck`). All64 barge/cargo gutters pass;
empty-hold floor remains visible in every barge direction. Guide metadata
retains named anchors at every direction. These are guide projections, not
reviewed anchors on generated painted assets.

Two full-sheet style transfers returned1774x887, not requested3072x1536.
Both copied the old near-cardinal progression rather than reliably following
new intermediate angles. In particular, first broadside and row-two first
end-on poses remain near the old reference directions. Reject both for runtime.
Tug silhouette registration against intermediate guide: meanIoU.845,min.813,
uniformscale.8783. Barge:mean.887,min.846,scale1.0263. These numbers only describe
silhouette registration, not heading accuracy or physical/artwork coherence.

Generated candidates and prompts are retained separately from runtime assets.
No new runtime shadows/glows/surface lighting are generated for rejected art.
No native performance claim or completed64-frame integration is made.

The guide-only four-pose proof follows the short angular progression more
clearly, but replaces the production tug's layered raised pilothouse/mast
architecture with a simpler guide-shaped cabin. The proof is2172x724 (not the
requested1536x384) with more surrounding transparent space. It cannot interleave
with the current tug without visible geometry/material popping. Retain as an
experiment, not a production substitute. A controlled detailed3D source model
matching the accepted tug and barge is the dependable route to exact additional
poses; the existing blockouts are pose scaffolds rather than production models.
