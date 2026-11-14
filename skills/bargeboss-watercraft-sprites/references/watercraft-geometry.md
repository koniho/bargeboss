# Watercraft geometry and attachment contract

## Hull datum and camera

Use the center of the hull's waterline footprint as the vessel datum, aligned
with the existing physics anchor. Do not use the center of visible pixels or
the wheelhouse roof. A tall stack, asymmetric cargo or bow-facing view changes
the visible bounds without moving the physical hull.

The current three-quarter art study uses an orthographic elevation of
35.264° above the water plane. With screen-right world X and world Y toward the
viewer, projection is `screenX = X`, `screenY = Y/sqrt(3) - Z*sqrt(2/3)` before
scale/translation. Rotate the same local geometry in XY, leaving Z unchanged.
Yaw 0 has bow right; 90 has bow toward viewer; 180 has bow left; 270 shows stern
toward viewer. This is an art-study convention, not a claim that existing game
camera transforms are identical. Reconcile conventions explicitly at integration.

Freeze length, beam, draft, freeboard and cabin height. Keep the waterline in the
same vertical location on the hull through the turn. Show only the intended
above-water portion; do not add a full underwater hull to some frames. Heave,
roll/pitch, trim under load and waves are separate decisions, not accidental
frame-to-frame hull changes. Current gameplay heave should move vessel layers
together; shoreline objects should not bob with them.

## Preserve identity through rotation

- Tug/pushboat: declare the bow end, push knees, cabin location, stack, rear deck
  and winch. Tires, railings and lights remain attached to the same hull side.
  Opposite views should reveal opposite sides of one vessel, not mirror a lit
  bitmap or swap bow and stern.
- Empty barge: define deck well, coaming, bow rake, stern treatment and mooring
  fittings. Broadside and end-on views must project the same length and beam.
- Loaded barge: keep base hull and cargo separate when unloading requires it.
  Cargo uses the same yaw, projection, scale, deck elevation and anchor system.
  Containers, steel coils and bulk piles must fit the deck without hiding the
  vessel's usable silhouette or lasso feedback. Use the required 32 headings for
  every rotating cargo layer too.
- NPC feeder craft: same camera, registration and heading convention as player
  vessels. Distinguish by permitted paint/equipment changes, not a new perspective.

## Record attachment points

For each frame (or a shared 3D source projected into it), record the waterline
pivot, bow contact face, stern tow point, barge mooring points, stack opening and
cargo deck origin. Distinguish visible paint features from actual rope endpoints.
The projected stack opening must follow the stack across all orientations;
positioning smoke with one constant screen offset will fail.

Wakes originate on the waterplane, not at the top of the visible sprite. Their
direction follows motion/heading as specified by gameplay; do not bake them into
turntable frames. Smoke emission follows the stack and engine loading. Ropes
follow shared tow/lasso state and their attachment points. Ensure those effects
do not acquire a different pivot or phase when hull art changes.

## Atlas metadata to retain

Store measured image size, grid/source rectangles, heading convention and frame
map, per-frame waterline pivot, uniform scale convention, attachment coordinates,
alpha expectations, prompt/source provenance and review status. For odd image
dimensions, use the same rounded crop boundaries as the native adapters; do not
silently assume equal integer cell sizes or claim requested resolution was met.
