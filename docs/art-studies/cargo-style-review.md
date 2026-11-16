# Cargo style and proportion review — 2026-11-15

Resolved in the 2026-11-16 refresh: see [current cargo contract](cargo-authoring-contract.md), [updated comparison](cargo-style-after-v3.png), and [generation record](../art-prompts/cargo-hold-v1.md). Findings below describe the previous assets.

The newest accessible user-captured screenshot is
`Pictures/Screenshots/Screenshot_20261115-194921.png` (coal tutorial). Later saved
Montrose captures show a yacht rather than cargo barges. Filesystem and Android
MediaStore searches did not reveal a newer user screenshot during this review.
Do not claim this is a review of an unavailable later capture.

Compared that screenshot, `docs/reference/industrial-isometric-concept.png`, and
an actual shared-renderer comparison of every current cargo type at four headings.
Reproduce with `tools/CargoStyleReview.java`; output `out/cargo-style-current.png`,
review copy `Download/BargeBoss-cargo-style-current-v1.png`.

## Observed problems

- Steel: three overlapping pipe bundles overwhelm the hull and cross its near rim.
  Heavy outlines and tight, high-contrast detailing differ from the current hull.
  The concept shows proportioned steel coils with readable spacing and supports;
  current pipes are a different cargo silhouette, not simply a lighting mismatch.
- Containers: three small, thin boxes occupy too little of the hold width and are
  rendered over the foreground coaming. Their detailed corrugation/edge treatment
  looks sharper and older than the surrounding vessel art.
- Coal: best fit and closest material treatment, but the isolated narrow heap
  leaves more exposed hold than the concept's broad, nearly full load. Bright
  individual chunks add texture noise at close zoom.

## Runtime cause

`SpriteAtlas` uses `steel-16.png` / `steel-half.png` and `container-16.png` /
`container-half.png` with their older pivots, alongside the newer isometric barge
and registered 32-pose coal layer. These are 32 direction samples across paired
sheets; the problem is not a missing frame count.

`VesselView` repeats steel at size 58 with lateral offsets -8/0/8 and containers
at size 26 with longitudinal offsets -17/0/17. Those independent sizes and legacy
pivots are not derived from the current hold. Both draw after the hull without a
foreground coaming mask. Coal already uses the hull's registered origin/scale
and a silhouette cut for its foreground rim. Scaling alone cannot fix all of the
style, deck-height registration and occlusion differences.

## Recommended asset correction

Author steel coils, container loads and a broader coal heap against one fixed
hold footprint, cargo-floor height and the current 3/4 camera. Match the reference
palette, materials and readable larger forms. Keep all 32 actual poses, including
the wraparound; use fixed registration and explicit foreground-rim occlusion.
Retain removable cargo units for unloading. Review all types on the same hull at
normal gameplay size before integration. New art must carry the watercraft skill's
anchor metadata, shadow layers and matching surface-lighting derivatives.

No cargo art or gameplay placement was changed in this review. Keep the comparison
as a regression/reference tool when replacing those assets.
