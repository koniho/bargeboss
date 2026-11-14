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

### Required effect anchor collections

Each vessel must declare `lights`, `smokestacks` and `tires`, even when a
collection is empty. Give each physical feature a stable ID across all 32
headings; frame number must not change its identity. Carry these with the hull
through rotation, registration, camera zoom and heave.

- **Lights:** lens/emitter center, role (navigation, mast, deck/work), intended
  color and base intensity, height above water, and facing/visibility. Keep the
  fixture in the hull sprite; animate glow and water reflection separately.
- **Smokestacks:** outlet center at the top lip, opening radius and exhaust
  direction. Smoke quantity follows engine load. A stack opening behind a cabin
  still emits in world space; use depth/occlusion ordering rather than deleting
  its emitter or drawing every puff in front of the boat.
- **Tires:** center and contact radius, hull side/normal, height relative to the
  waterline, and visibility. Record a contact point or derive it from center,
  radius and outward normal for compression/scrape/splash effects. Port and
  starboard tires must not swap IDs when the boat turns. Hidden tires remain
  declared but should not paint foreground contact effects through the hull.

Prefer local vessel-space XYZ definitions when the guide/source model supplies
them, plus projected coordinates for every source frame. Record whether image
coordinates are normalized to the full atlas cell or pixel crop; do not mix the
two. Retain source-cell dimensions and pivot so the renderer applies the same
transform to the sprite and its points. Specify radii in a declared coordinate
space rather than silently mixing pixels and world units.

A compact interchange record can use this shape (values illustrative only):

```json
{
  "schemaVersion": 1,
  "coordinateSpace": "normalized-source-cell",
  "frame": 0,
  "waterlinePivot": [0.5, 0.72],
  "anchors": [
    {"id": "stack.main", "kind": "smokestack", "position": [0.4, 0.3],
     "localXYZ": [-12, 0, 50], "radius": 0.02,
     "visibility": "visible", "reviewStatus": "guide-projected"},
    {"id": "light.mast", "kind": "light", "position": [0.5, 0.2],
     "color": "#ffca74", "visibility": "visible", "reviewStatus": "unverified"},
    {"id": "tire.port.0", "kind": "tire", "position": [0.3, 0.7],
     "radius": 0.04, "side": "port", "localNormal": [0, -1, 0],
     "visibility": "occluded", "reviewStatus": "unverified"}
  ]
}
```

Generate an overlay with IDs, centers and radii for all headings, including the
wrap seam. Verify points against actual generated lamps, stack lips and tires
after registration. Guide-derived coordinates are starting estimates: the
generator can move equipment independently of the hull. Save review status and
manual corrections rather than presenting mathematical projection as a pixel
measurement. Check anchor motion with the same frame/blend selection as the
hull so effects do not snap to the neighboring pose early.

For shared Java integration, keep the metadata and effect rules in core; native
hosts must not rediscover emitter locations or tire contacts from image pixels.
Missing frames, duplicate IDs, nonfinite positions, undeclared coordinate spaces
or silently omitted anchor categories are validation failures.

### Other atlas metadata

Store measured image size, grid/source rectangles, heading convention and frame
map, per-frame waterline pivot, uniform scale convention, attachment coordinates,
alpha expectations, prompt/source provenance and review status. For odd image
dimensions, use the same rounded crop boundaries as the native adapters; do not
silently assume equal integer cell sizes or claim requested resolution was met.
