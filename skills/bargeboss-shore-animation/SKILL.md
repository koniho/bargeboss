---
name: bargeboss-shore-animation
description: "Create and validate animated shore elements for Barge Boss, such as cranes, unloading equipment, conveyors, bridge mechanisms and work crews. Use for gameplay-phase animation sheets, mechanical pivots and layered machinery; not watercraft turntables or static scenery tiles."
---

# Barge Boss animated shore elements

Create machinery and crew animation that belongs to the same industrial scene
and responds to shared gameplay phases. Locate the Barge Boss checkout and read
its `AGENTS.md`, current art direction and the relevant unloading/bridge/render
code. A scenery or asset trial does not require changing gameplay.

Use the available `imagegen` skill for raster generation/edits, inspect local
references first, and record exact prompts and input roles. Match the scene's
current orthographic camera, light direction, scale and material treatment.
Keep transparent backgrounds and separate water, smoke, moving loads and lighting
effects where they need independent runtime behavior.

## Define motion before drawing frames

Read [mechanisms and gameplay phases](references/mechanisms-and-phases.md) for
the requested element. Define its ground anchor, mechanical pivots, motion path,
moving/static layers, cargo contact points and phase endpoints. Use a projected
geometry/pose guide for precise booms, cables, doors and bridge leaves rather
than relying only on a text description of successive angles.

Choose frames to support the requested motion and existing game timing. The
watercraft requirement for 32 headings does **not** mean every crane or worker
needs 32 frames. Do not generate redundant yaw views for fixed-camera equipment.
Keep rigid parts the same length and volume; a moving boom is not a deforming
crane. Preserve crew counts, outfits and scale throughout a sequence.

## Validate the sequence

Review frames in a fixed-anchor player, then transitions between phases and
the idle loop seam. Check mechanical endpoints, load attachment, contact with
the barge and release, cable connection, body proportions and changing occlusion.
Inspect at gameplay size and on light/dark/water backgrounds. Use the actual
gameplay clock when integrating; a GIF preview alone does not establish timing.

The current unload behavior draws the tied barge parallel to shore before
equipment works for three seconds and returns to idle. Verify the current code
before mapping new frames to those phases. Keep animation deterministic and
render-pure in shared Java; native hosts only draw the same layers/frames.
Scenery that covers playable vessels, ropes or targets must use the existing
shared occlusion/translucency policy, including moving booms and loads.

Keep candidates in `docs/art-studies/`, prompts in `docs/art-prompts/`, and only
accepted runtime atlases in `assets/sprites/`. Store source crops, pivots,
attachment points, phase/frame maps and transition rules alongside shared
metadata. Record observed failures and unresolved timing/geometry limits in the
art notes. Report saved assets/prompts and generation mode; follow current repo
checks/install instructions when integration is part of the authorized task.
On the development phone, also copy concept/review images to
`/storage/emulated/0/Download` with descriptive versioned names, following the
repository workflow; keep source atlases and prompts in the repository.

## Required lighting and smoke anchors

Inventory every visible lamp, furnace opening, chimney, exhaust and steam outlet.
Record stable named IDs, emitter kind/role, color, local XYZ and outward normal,
normalized source-cell position, radius, visibility and review status alongside
source crops and the ground pivot. Explicitly record empty categories for assets
without lights or outlets. Keep glow, smoke and steam independently animated;
a painted bright spot is not a substitute for an emitter anchor.

For animated equipment, store anchors for every phase/frame, including idle,
working and return transitions. Identify the parent rigid part and its pivot;
move lamps and exhaust with that part. Keep IDs stable across frames, marking
hidden points occluded rather than deleting them. Document phase-driven emission
(e.g. engine load or steam release) separately from anchor geometry.

Review labeled emitter overlays on the actual source pixels and at gameplay size.
Distinguish guide-projected estimates from pixel-verified positions; generated
art can add or relocate fixtures. Verify visibility, phase transitions and any
interpolation before integrating effects. Keep the runtime metadata and emission
rules in shared Java so Android, iOS and desktop use identical attachment points.
