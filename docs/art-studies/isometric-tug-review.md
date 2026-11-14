# Isometric tug style proof

This records the earlier v2 study. See [v4 refinement](isometric-tug-v4-review.md)
and [accumulated learnings](sprite-sheet-learnings.md) for the current result.

Status: **review candidate, not approved for gameplay**. This round validates a
single tug before converting the full scene. The existing live vessel assets,
camera, physics, input and cross-platform rendering remain unchanged.

The [new industrial concept](../reference/industrial-isometric-concept.png) was
found in the user's Pictures directory. Its dark hulls, cream wheelhouses,
orange paint, warm industrial lighting and smooth prerendered material treatment
guide this study. The original concept is a supplied reference; no claim is made
about its original authorship or license.

The [candidate atlas](tug-isometric-v2-32.png) contains 32 populated cells in an
8-column, 4-row grid. Actual generated resolution is **1774 × 887**, with cells
approximately 222 pixels square; the requested 3072 × 1536 was not delivered.
It was generated with the **built-in image_gen tool**, using
[this prompt](../art-prompts/isometric-tug-v2-32.txt). No source pixels have been
programmatically edited, rotated or blended. Smoke and wakes are excluded so
they can eventually remain separate runtime effects.

## Review

- The rounded dark hull, orange/cream silhouette and smooth materials read well
  over a dark teal background. The simplified tug is less detailed than the
  reference; this is a candidate for discussion, not an approved final design.
- All 32 cells contain opaque vessel pixels and fully transparent background
  pixels. None touches its cell edge at alpha 128 or above. This checks packing,
  not correctness of the physical model or every semitransparent edge pixel.
- **32 cells do not yet mean 32 accurate 11.25-degree headings.** Heading spacing
  is uneven, with near repeats and changes in bow/stern fittings between views.
- The requested fixed hull anchor was not honored consistently. Translation,
  scale and silhouette changes are visible against the stationary crosshair.
  Registration cannot correct changing geometry or missing orientations.
- The fixed camera elevation and lighting were requested but have not been
  geometrically verified. This sheet must not be used to calibrate world
  projection, collisions, wake direction or smoke attachment points.

The earlier 16-view generation and cleanup/intermediate prompts are retained in
`docs/art-prompts/isometric-tug-v1-*.txt` as experiment provenance. The attempted
half-step sheet repeated too many existing poses and was rejected. Discarded
images are not bundled into the game.

## Reproduce the review

The current `./preview-tug-concept.sh` now defaults to v4. To reproduce this v2
study, first run that script to compile the tool, then run
`java -Djava.awt.headless=true -cp build/tug-concept com.bargeboss.game.TugConceptPreview docs/art-studies/tug-isometric-v2-32.png out/isometric-tug-v2 - docs/art-studies/tug-isometric-v4-pose-guide.png`.
The tool reads the candidate
without modifying it and exports:

- `out/isometric-tug/review.html`: self-contained interactive turntable with play,
  pause, previous/next buttons and a frame slider. Works without a server.
- `out/isometric-tug/all-32-frames.png`: contact sheet.
- `out/isometric-tug/turntable.gif`: looping preview when FFmpeg is available.
- `out/isometric-tug/measurements.csv`: alpha and source-cell measurements.

The v2 raw review uses the requested source anchor `(0.50, 0.72)` rather
than automatically centering each silhouette. The crosshair remains fixed so
artwork drift is visible. No blend conceals frame changes. This is an isolated
asset inspection tool, not a separate platform implementation of the game.

## Gate before integration

Approve the vessel's proportions/materials first. A production sheet then needs
consistent bow/stern geometry and 32 evenly spaced views of the same vessel,
with measured waterline pivots and exhaust openings. A single reusable 3D source
model rendered at locked camera, lighting and rotation settings would make those
constraints controllable; the generated sheet alone has not achieved them.
Review the result at gameplay size and on a fixed-pivot turntable before replacing
the live atlas. The eventual camera and input conversion still belongs in shared
Java, with identical asset metadata consumed by every native host.
