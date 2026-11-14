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

**Track effect anchors while making the sprites.** Every vessel sheet needs
named light points, smokestack outlets and tire fender/contact points alongside
its waterline, tow and mooring anchors. Keep stable identities across headings
and record visibility/occlusion. See the anchor contract in
[watercraft geometry](references/watercraft-geometry.md). An absent feature is
an explicitly empty collection, not missing metadata.

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

Check the guide against the actual concept before generating a whole sheet.
Do not remove a raised pilothouse, rounded hull, red lower deckhouse, railings,
mast or prominent tires simply to improve registration. v4 fitted its guide
well but the user rejected its squat, boxy design; silhouette/material fidelity
to the intended vessel is a separate acceptance check from pose consistency.

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

Export an anchor overlay for each heading and check it against generated pixels.
Distinguish guide-projected estimates from points verified on finished art.
Tire side/visibility, stack lip height and lamp centers can change despite a
good hull silhouette fit. Keep unverified points labeled until reviewed; do
not report them as production-ready effects metadata.

For integration, put accepted PNGs in `assets/sprites/` and source rectangles,
frame ordering, waterline pivots, cargo offsets and effect anchors in shared
Java. Native hosts draw the same art and metadata. Keep experimental sheets and
concept references under `docs/` until accepted for the requested integration.
Follow the checkout's current validation/install workflow when gameplay changes
are part of the task; this skill grants no additional publishing authorization.

Record each experiment's inputs, prompt, actual dimensions, measured results,
manual findings and acceptance status in the repository's art notes. Report
saved asset and prompt paths, generation mode, and material limits to the user.
For review images on the development phone, follow the repository's standing
request to copy versioned exports to `/storage/emulated/0/Download` as well.
Keep repository sources canonical and report the review-copy filenames.

## Required separate shadow layers

Deliver independent near-contact and soft cast-shadow masks with new or revised
shadow-casting sprites. Keep cast shadows out of the color artwork; do not remove
its intentional surface shading. Generate masks deterministically from accepted
source alpha or the source model, excluding smoke, glow, wakes and other effects.
For an asset proof, keep the masks and previews with the candidate; generating
shadows does not authorize runtime integration by itself.

Before integration, read `docs/art-prompts/shadow-masks-v1.md`,
`tools/GenerateShadowMasks.java`, and `ShadowArt` / `ShadowView` in the checkout.
Use or extend that pipeline for the requested asset. Record source asset/frame,
receiving plane, ground/waterline anchor, source-to-mask scale/padding/pivot
transform, softness variants and whether the mask is alpha-derived or model-based.
Preserve existing lighting, smoke and contact anchor IDs. Do not describe an
alpha silhouette as reconstructed 3-D height or fully light-occluded geometry.

Validate transparent mask gutters, exact pivot registration and frame coverage.
Review contact and cast layers independently on light, dark and gameplay
backgrounds, under several light directions and strengths. Shadows must stay on
the receiving surface and below gameplay sprites, not paint across roofs or UI;
they must not count as opaque sprite pixels for scenery fading. Cache masks or
pose results; animate transforms/opacity in shared Java rather than blurring or
allocating full-size bitmaps each frame. For integrated assets, regenerate masks,
run their freshness checks in `preview.sh`, and verify native affine rendering.
Save the shadow-generation settings and review outputs alongside the sprite's
art notes, and copy versioned review images to Downloads.

For watercraft, include all 32 actual headings and the 31→0 seam. The shadow's
heading selection/blend must match the hull's registered frames. Keep its anchor
on the water plane while the vessel heaves; review shadow strength/separation
through the bob cycle. Document whether cargo contributes another shadow so
hull/cargo overlap does not produce accidental double darkness.
