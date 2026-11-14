# Static harbor art contract

## Current art-study target

- Fixed orthographic camera at 35.264 degrees above the ground; a world grid
  rotated 45 degrees yields screen axes approximately 30 degrees up/down from
  horizontal. Vertical structures remain vertical. Do not assume any particular
  canvas yaw from the live game's older river projection.
- One scale and fixed light direction across a set. Vessel, bollard and door
  sizes provide useful visual scale checks; image canvas size alone does not.
- Readable chunky forms with smooth prerendered shading. The industrial concept
  supplies materials and atmosphere, not trustworthy geometric specifications.
- Transparent surroundings; no baked water, reflections, detached ground shadow,
  smoke, people, moving machines or contextual background.

These are current project art choices, not universal requirements for every
isometric game. Honor a user's explicit change of camera or style.

## Asset boundaries

| Static scenery | Separate animated shore elements |
| --- | --- |
| Quay slab, pile wall, fixed bollard, warehouse shell | Crane boom, hook, hoist cable, conveyor belt |
| Machine housing and stationary base | Forklift, truck, worker, unloading motion |
| Furnace shell and fixed pipework | Flames, smoke, glow pulses and particles |
| Bridge abutment or tower | Opening bridge span and moving barriers |

If a fixed structure and moving component overlap, define attachment coordinates
and draw ordering before generating their layers. Do not bake a moving object
into the base and then draw a second copy over it.

## Ground and tile registration

Specify the footprint as world-plane vertices independently from tall artwork.
Record the anchor at the footprint center or a named connection corner. For
quays, distinguish ground/deck anchor from the bottom of the visible pile wall.
The wall can extend below the deck without changing the collision boundary.

Connection metadata should identify the two endpoints of each join, surface
height, edge material, and whether that edge faces water or adjoining land.
Matched projected endpoints plus matched scale are necessary for a clean seam.
An exposed end wall may need a separate cap or to be suppressed at an internal
join; do not hide arbitrary seam failures by overlapping tiles deeply.

The initial three-asset trial requests a 2×1 straight quay, a 2×2 beveled corner,
and a warehouse on a 2×2 foundation. Those are desired footprints until measured
against a geometry guide. The warehouse's foundation thickness is not proof its
floor is at the same world elevation as the adjacent quay deck.

## Occlusion and read-through

Store water/land boundaries separately from scenery pixels. Tall structures that
can obscure the tug, barge, berth, bollard or interaction guide need a removable
foreground layer or a whole-sprite alpha treatment controlled by shared Java.
Keep the playable shoreline visible while tall visual mass becomes translucent.
Do not make collision or lasso eligibility depend on the generated alpha mask.

Review full opacity and a translucent state over a vessel silhouette. Check that
roof overhangs, walls and foundations fade together where intended and that baked
shadows or glow do not remain as opaque visual obstacles.

## Useful prompt structure

State the grid and exact asset list, then each input image's role. Lock camera,
world scale, deck height, light direction and footprint. Describe connecting faces
and exclusions. Give the ground anchor in normalized cell coordinates and leave
padding. Restrict texture detail to what survives gameplay scale.

Keep a visual geometry guide separate from the finished material reference when
the generator confuses footprints or projections. Camera words and numeric
angles are requests, not geometric validation. Record an unresolved seam or
projection difference plainly when the trial remains conceptual.

The first three-element trial returned mostly alpha 252–253 inside solid
structures rather than 255. Treat transparent gutters and opaque interiors as
separate checks; an RGBA file alone proves neither. Do not silently threshold
soft alpha to hide an observed defect or destroy edge antialiasing.
