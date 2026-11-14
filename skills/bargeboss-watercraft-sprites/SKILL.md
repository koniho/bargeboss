---
name: bargeboss-watercraft-sprites
description: "Generate, refine and validate directional watercraft sprites for Barge Boss: pushboats, tugboats, barges and separate cargo layers. Use for vessel artwork, rotation sheets, waterline registration and vessel effect anchors; not static scenery or animated shore equipment."
---

# Barge Boss watercraft sprites

Produce readable, consistently registered vessel artwork that the same shared
Java renderer can use on Android, iOS and desktop. Preserve the user's requested
scope: an asset proof does not authorize replacing the live scene or camera.

## Start with the current vessel contract

Locate the Barge Boss checkout from the current workspace; the development
checkout is commonly `~/dev/bargeboss`. Read its `AGENTS.md`, current
`docs/art-studies/sprite-sheet-learnings.md`, and relevant `SpriteAtlas` / vessel
rendering code before choosing atlas layout or integration metadata. Do not
assume the experimental three-quarter camera has already replaced gameplay's
existing projection.

Read [watercraft geometry](references/watercraft-geometry.md) when creating or
changing a vessel. Establish hull length/beam, waterline datum, draft/freeboard,
bow/stern identity, cabin/stack/winch layout, cargo footprint and attachment
points. Retain the same model at every heading. Separate machinery, cargo and
effects only where the game needs them independently animated.

For current directional vessels, the requested target is **32 actual poses**,
11.25° apart. A 32-cell image or 16 poses duplicated/mirrored does not meet that
target. Other animation dimensions, such as unloading or bobbing, do not count
as additional directions.

## Generate and refine

Use the available `imagegen` skill and built-in image generation for raster art
and edits. Inspect each local reference first. Label inputs by purpose: geometry
guide, exact edit target, material reference, or atmosphere reference. Preserve
true transparency; request no water, wakes, smoke, detached shadow or glow in
the vessel sheet. Use versioned filenames, with prompts in `docs/art-prompts/`.

For a new rotating hull, prefer a deterministic projected pose guide before
requesting final materials. The repository's `tools/TugPoseGuide.java` is a
specific tug experiment, not a universal hull model: adapt the geometry for a
barge, ferry or other requested craft. Preserve a fixed camera and waterline
center while rotating the model, not the finished bitmap. Read
[generation and review](references/generation-review.md) for the tested workflow,
tool commands, and limits of guide fitting.

Target one observed defect per revision. Do not repeatedly submit the same
angle-table prompt after it has already failed. If the guide is ignored, reduce
the problem to cardinal views or a short adjacent sequence and record the
outcome. Do not expand a successful pose into production until the complete
turn and wraparound are checked. If repeated generation still changes physical
geometry, keep it as a documented candidate and explain the remaining need for
a controlled source model or authored correction; do not certify it by counting
cells or hiding transitions with blends.

## Review before integration

Inspect raw frames and a stationary-anchor turntable at gameplay size and
magnified size. Verify the sequence, opposite views, 31→0 seam, hull proportions,
equipment identity, waterline, alpha edges and crop gutters. Composite on dark,
light and water colors; a transparent preview alone can misrepresent edges.

Registration may translate frames around the known waterline datum and use a
consistent overall scale. Avoid per-frame stretching that disguises changing
geometry. A silhouette fit proposes metadata; it does not prove heading,
waterline or attachment-point accuracy. Review the bow knees, cabin and stack
visually even when silhouette overlap is high.

For integration, put accepted PNGs in `assets/sprites/` and source rectangles,
frame ordering, waterline pivots, cargo offsets and effect anchors in shared
Java. Native hosts draw the same art and metadata. Keep experimental sheets and
concept references under `docs/` until accepted for the requested integration.
Follow the checkout's current validation/install workflow when gameplay changes
are part of the task; this skill grants no additional publishing authorization.

Record each experiment's inputs, prompt, actual dimensions, measured results,
manual findings and acceptance status in the repository's art notes. Report
saved asset and prompt paths, generation mode, and material limits to the user.
