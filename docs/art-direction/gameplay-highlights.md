# Gameplay highlights: arcade clarity over dusk scenery

Default policy for guidance, selections, destinations, interaction ranges and
progress indicators in Calumet and Montrose. Read this before changing `SoftCue`,
`WorldArrows`, `EffectMaterial`, or their shared shaders. Updated 2026-11-16.

## Source and attribution

The verified Valve source used here is Jason Mitchell's
[Connecting Visuals to Gameplay at Valve, MIGS 2008](https://cdn.fastly.steamstatic.com/apps/valve/2008/MIGS08_ConnectingVisualsToGameplay.pdf).
This is a presentation about gameplay readability and art direction, not a
particle-authoring manual. The user's particular “Valve visual FX guide” has not
been positively identified; do not label these project rules as quotations from
that unspecified guide.

Valve principles, paraphrased (PDF page numbers are one-based):

- Establish an order of recognition: broad identity before fine detail; concentrate
  contrast where the player needs to look (page 9).
- Make silhouettes recognizable before adding interior detail (pages 16–18).
- Keep environments less saturated than important gameplay subjects (page 27).
- Let visual treatment communicate game state (page 41).
- Use lighting to aid navigation and simplify competing areas of contrast
  (pages 57–59).
- Separate foreground and background so important silhouettes remain legible
  (page 62).

The following rules are Barge Boss adaptations and explicit user art direction,
not numerical requirements or prescriptions from Valve.

## Visual identity

Highlights are unmistakable arcade graphics placed over the world. They are not
painted dock markings, steel fixtures, ropes, water foam or scenery lights.
Keep the near-night setting, almost-dark docks and muted NPC boats. Reserve bold,
saturated cue colors for information the player can act on.

Use a strong colored core, a softer shoulder of the same hue, and a broad low-alpha
halo. Do not push the core toward white. Do not add concrete, rust, material
lighting, surface noise, drop shadows or metallic bevels to guidance. A recognizable
shape and clear gaps must survive when bloom is disabled or the scene is reduced
to a small thumbnail.

Use approximately twice the previous thin-guide core width: current starting
range is 5.6–9 reference-screen units, with zoom compensation. These are tuning
values, not a reason to stretch artwork or alter physics. Small cues need fewer
segments, not a dense dotted ring. Preserve an empty center in target rings/boxes
so the player can see hulls and docking clearance.

## Shared shape and color language

| Meaning | Shape | Palette family |
| --- | --- | --- |
| Next objective / destination | Broken rectangle or ellipse; clear approach arrow | Electric yellow |
| Steering / forward movement | Broad filled arrow in the water plane | Cyan / blue |
| Ready / valid interaction / completion | Open ring or target outline | Vivid green |
| Reverse / caution | Direction arrow or warning outline, distinct from forward | Yellow / orange |
| Invalid / damage / urgent danger | Distinct warning shape and brief response | Red |
| Secondary interaction | Smaller or quieter cue; never competing with the main goal | Violet when already semantically assigned |

Color supplements shape, direction and timing; it is never the only distinction.
Do not map a pale blue to purple simply because it contains a large red channel.
Keep cargo/berth identity consistent with existing game rules; palette changes
must not silently reassign the meaning of a destination.

Use broken outlines for ranges, hold-progress rings, destination squares and slip
rectangles. Leave clear gaps; start near a two-thirds-on, one-third-off pattern.
The current ring implementation uses 16 segments for large targets and eight
for compact indicators, with proportional width caps that preserve the center. Long straight borders use
roughly 32 screen-unit cells; short corner strokes need not contain extra gaps.
Arrows stay broad and filled, with one obvious head. Do not make a thick white
wireframe arrow or an arrow that resembles a length of dock.

## Motion, hierarchy and depth

- Reserve hull guidance glow for actionable targets and active controls; ambient
  illumination belongs to vessel lighting. Idle or already-delivered boats must
  not keep a guidance rim. Avoid a permanent ring around every towed cargo.
- Keep one dominant current objective. Reduce redundant tutorial prompts while the
  player is steering, dragging cargo or aiming a rope.
- Use a gentle shared-clock pulse. Preserve visible color through the trough;
  avoid rapid flashing, jittering dash counts, or shapes that crawl when paused.
- Progress changes the occupied arc or state, not the apparent collision boundary.
  Completion may briefly brighten the same hue, then settle or disappear.
- World arrows are flat planes: project the shaft, head, width and glow together.
  Merge their ground-depth anchors with watercraft/scenery. Far arrows must be
  covered by nearer hulls; near arrows may cover farther hulls. Never render all
  arrows last as a HUD layer. The current sorter uses each arrow's midpoint;
  long arrows crossing several depths may need splitting, as towlines already do.
- Screen-edge labels and actual HUD controls stay screen-aligned. Do not tilt text
  into the water or let a cue move touch targets.
- Keep guidance separate from physical effects. Splashes, smoke and wakes may be
  organic and textured; action cues should be clean, regular and immediately read.

## Implementation and review

Shared Java owns meaning, placement, pulse, color, dimensions and ordering.
Native painters execute the same material parameters. Prefer bounded shader
patches for dashed borders/rings rather than one native draw per dash. Avoid
full-screen offscreen layers and per-frame texture generation. Keep the reference
fallback usable, but report shader/fallback edge differences honestly.

Review the actual shared renderer on these cases:

1. Montrose's crowded slips: lamps, moored boats, wakes, a selected yacht and a goal.
2. Calumet: yellow industrial railings and warm windows behind a destination cue.
3. Forward and reverse arrows, both ahead of and behind a hull, at close/wide zoom.
4. A ring and rectangle at partial/full progress, including the pulse trough.
5. Pause/resume, narrow/wide landscape displays, and color-independent readability.

Reject white cores, thin decorative lines, excessive bloom that closes dash gaps,
scenery-like arrows, or a cue that obscures the collision edge it explains. Verify
render purity, depth order, shoreline clips and native pixels. Measure Montrose
frame intervals and native cost; fewer submissions alone do not prove 60 FPS.
Record measured results and unresolved causes in `optimization.md`.

See [drawing order](../rendering/drawing-order.md) and
[performance rules](../performance/development-rules.md) for the existing contracts.
