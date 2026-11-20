# Geometry material prototype, v1

This is an offline study for issue 13, not replacement production artwork.
The canonical source is `tools/materials/WatercraftModels.java`, the existing
barge/yacht pose-guide geometry and the existing moored-boat rigid models.
No image-generation model or painted-image normal inference is used.

After compiling shared Java with `./check.sh`, run `./tools/materials/check.sh`.
It writes sheets, frame packs, format metadata and a lighting proof to
`build/materials/export/`. These reproducible intermediate files are untracked.
The retained proof here records the current visual review, not acceptance.

Coverage: tug (also the geometry basis for teacher/feeder liveries), barge,
yacht, three motorboat models (including the two rescue-cruiser shapes), and
three sailboat models. All nine have 32 material poses. Production resolution
for the smaller boats remains a budget decision; the study uses 256-square
cells uniformly to expose defects.

Known incomplete work:

- Tug fittings are more developed than the pose guide, but its shape and finish
  still differ noticeably from the accepted painted tug. Yacht and barge remain
  guide-quality. These are not approved art replacements.
- Guide materials are imported from palettes, not fully authored material IDs.
  Emission currently has one prototype group. Real fixture-state groups, local
  anchor manifests, production shadows and cargo composites remain required.
- Review shading is an offline reference, without self-shadowing or native GPU
  validation. No runtime renderer or production asset has been switched over.
- No Android/iOS/desktop GPU speedup or 60 FPS headroom is established by this study.

See the [fleet-wide authoring contract](../watercraft-material-contract.md).
