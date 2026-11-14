# Drawing order and occlusion contract

This is the shared rendering contract for Android Canvas, desktop Java2D and iOS
Core Graphics. Read it before changing composition, vessel art, shoreline
geometry, ropes, scenery fading or camera projection. Last reviewed 2026-11-14.

## Coordinates, orientation and direction

`RiverCamera` supplies the only gameplay projection: 45-degree ground yaw and
35.264-degree elevation. Increasing `y - x` means nearer the viewer. Camera pan
and positive zoom change screen coordinates, not that ordering.

Keep three different values separate:

- **Ground depth:** `camera.screenY(worldX, worldY)`, before any height offset.
  Use this for ordering upright vessels, factories, road vehicles, shore caps
  and individual tow spans.
- **Projected height:** subtract `height * camera.scale` for sprite-space rise.
  The tutorial ground uses `GroundArt.LAND_RISE = 18`; hull heave and rope deck
  rise have their own values. Articulated XYZ models instead use
  `ArticulatedModel.Z_SCALE`; do not substitute sprite-rise units for model Z.
- **Heading:** `Body.angle` selects the vessel's 32 authored directional frames.
  `SpriteAtlas.draw` blends only around sector transitions; keep its registered
  waterline pivot fixed. Never rotate an isometric bitmap to invent a view.

Velocity is not heading or depth. A vessel reversing, sliding sideways or
coasting keeps its real heading; neither its sprite order nor the shore changes
because the sign of `vx` or `vy` changes. Use velocity for wake direction and
motion effects. Attachment offsets rotate with the owning vessel's heading.

## Solid shore is a surface, not an edge stroke

The failure reproduced by the 12:58 screenshot was a cap drawn across the tug
while its lower hull remained visible over the land behind that cap. Sorting
only the cap cannot solve this: most of the land is already in a cached layer.

`ShoreOcclusion` subtracts navigable water from a bounded world region, projects
the resulting dry ground at the actual shore elevation, and creates disjoint
triangles for native clipping. This hides vessel pixels while preserving the
existing ground texture, lights and shadows. It does not repaint a flat patch
of ground over buildings.

For an upright vessel, a dry ground pixel is in front when its **unraised**
ground depth is at least the vessel's ground depth. With tutorial land this is
`pixelY + LAND_RISE * scale >= vesselGroundDepth`. This is a surface comparison,
not a comparison with the vessel's roof or the top of its sprite rectangle.

| Situation | Required result |
| --- | --- |
| Lower/foreground bank | Solid land hides the portion of hull/cargo projected behind it. The cap remains opaque. |
| Upper/background bank | Land behind the vessel does not erase upright cabin/cargo artwork. |
| Water opening or canal corner | No land clip may cross the exact navigable-water polygon. |
| Hull bobbing or turning | The artwork moves/changes frame; the shoreline stays fixed. |
| Reflections, wakes and water-contact shadows | Clip against **all** dry ground, including background banks; these effects lie on the water plane. |
| Foreground building covering a visible vessel | Building may animate toward translucency, subject to eligibility below. |
| Hull portion already hidden by land | Those hidden pixels cannot independently trigger building fading. |

The tutorial uses `TrainingChannel.WATER_RECTANGLES`, including its open mouths,
basin and harbor. Ordinary stages use the river banks and, in the pier stage,
`HarborGeometry.PIERS`. Title artwork has its own open-water composition and is
explicitly excluded from gameplay shore clipping.

`VesselView` clips hull, cargo, damage flash and hull glow together. Its
water-plane effects use a nested, stricter clip. Both scopes must restore before
subsequent vessels, buildings and UI. Moving smoke is airborne and is not put
inside the hull's ground clip.

## Scene composition

### Continuous tutorial

1. Water surface and water-plane effects.
2. Cached land, raised walls, roads and base cap artwork.
3. Factory ground shadows, gates, berths, haze and quay lighting.
4. One stable far-to-near merge of factory bases, vessels, road vehicles, short
   foreground cap sections and tow spans. Vessel/span rendering applies the
   solid-ground cutout. Cap sections never become a late full-scene overlay.
5. Berth equipment in its reserved dock area.
6. Airborne vessel smoke, interaction cues, world notifications, HUD and coach UI.

Factory depth is its registered ground pivot, not the lowest pixel of smoke,
a shadow, a glow or its image rectangle. At equal depths the existing stable
merge supplies deterministic ordering; do not introduce platform-specific sorts.

### Ordinary stages

Water and ground/piers are drawn before shore structures. Background props
precede the river's depth-sorted vessels and tow spans; foreground props follow.
The articulated bridge uses its elevated pass. Piers must not be painted over
nearer building roofs at the end of the scene. Shore cutouts handle solid-ground
coverage of vessels without requiring that late pier pass.

### Title

Sky, distant skyline/bridge and industrial shore remain background layers.
Title water effects are constrained below the horizon and behind the coast;
the foreground convoy and its independent smoke use the title camera. Never
reuse the gameplay banks to clip the title's open water.

## Ropes and attachment points

Do not place an entire towline permanently above or below both vessels.

`Towline` keeps the stern fitting at `-(tug.half + 7)` along the tug's heading.
The selected barge end is `+(barge.half + 7)` or its negative, and rotates with
the barge. `TowView.Segments` constructs 24 spans between those fittings, adding
deck height, each hull's heave and vertical sag. Order spans by their unraised
world midpoint depth alongside vessels and scenery. Thus far-side fittings and
spans can pass behind a hull while near-side spans remain visible in front.

Each span also obeys the solid-ground clip. A towline does not become visible
through a quay because it is an interaction object. Release recoil uses the
same span/depth rules. Catch indicators and release labels belong to the cue
pass and are distinct from the physical rope.

Shore lasso aiming/flight uses `LassoPath`, including the hull-edge origin and
elevated throw arc. Do not replace it with the towline's low, sagging path.
The existing shore-mooring rope/coil is a separate elevated deck-to-bollard
presentation; future changes to its shoreline crossings must test the deck,
bollard and intervening shore heights explicitly.

## Building fading

Only buildings may fade. Quay tops, retaining walls, caps, road surfaces,
bridge mechanisms, vehicles and watercraft are solid.

A building must have a painted silhouette that overlaps navigable water, be in
front of a vessel, and overlap actual visible opaque vessel/cargo pixels.
Proximity, transparent sprite padding, wake, smoke, rope, arrows, glow and
screen-space bounding rectangles alone do not qualify.

`SceneryOcclusion` tests the masks; `BuildingFades` stores opacity by world
placement and advances at 60 Hz. Full fade-out takes 18 ticks (0.3 seconds),
return takes 27 (0.45 seconds). Pause freezes it; reset clears it. Rendering
reads the state and must not advance it. Building lights, steam and other
attached effects inherit the same opacity.

The fixed-step silhouette snapshot and rendered silhouette must agree on
heading blend, crop, pivot, heave, remaining cargo and the solid-ground cutout.
When changing `VesselView` cargo layout, update `SceneryOcclusion.register` and
its tests in the same change.

## Shadows, lighting, machinery and air

| Element | Placement/occlusion rule |
| --- | --- |
| Vessel contact/cast shadow | On the water plane beneath the hull; only water can receive it. Uses independent shadow masks. |
| Factory shadow | On raised land before vessels/buildings. Never part of a vessel silhouette or fade trigger. |
| Hull glow/flash | Same frame registration and shore clipping as the hull; no rectangle-based occlusion. |
| Quay lights | Anchored to raised shore. Diffuse water lighting is separate from solid geometry. |
| Building light/smoke | Attached to reviewed sprite anchors; follows building opacity and its scene placement. |
| Vessel exhaust | Emitted from heading-specific stack anchors, then independent world-space particles. Air pass intentionally allows plumes over roofs. |
| Articulated bridge | Real shared XYZ nodes, parent transforms, face ordering and emitter normals. Opaque; not a building-fade exception. |
| Dock crew/equipment | Shore-height anchors in reserved clear dock areas; if new placements overlap other props, insert them into the world-depth merge. |
| Interaction guides | Soft, translucent UI feedback, not physical occluders and never building-fade triggers. |

A moving bridge leaf/boom requires a skeleton or XYZ model, not rotation of an
already painted isometric sprite. Its lights/smoke attach to the same transformed
node. Respect face occlusion and light normals. The bridge has internal 3-D face
ordering, but its scene placement is still an elevated pass, not a general
3-D depth buffer shared with every vessel.

## Native clip contract and caching

`Painter.pushClipOutside(float[])` saves the existing clip and excludes
**disjoint** triangles (six floats each). `popClip()` restores that exact clip.
Core constructs the geometry; Android, iOS and desktop only execute it.
Coordinate/opacity wrappers must forward the clip, transforming coordinates
where required. Nesting is valid and must be balanced with `try/finally`.

- Desktop: subtract from the saved Java2D clip area.
- Android: save Canvas and apply the difference path.
- iOS: save CGContext and use an even-odd path containing the existing clip
  bounds and the disjoint holes. Overlapping triangles inside one call are not
  supported; core partitions them first. Separate nested scopes may overlap.
- Diagnostic painters may record without rasterizing. `Parity` hashes clip
  commands, so cross-platform traces cover the shared clipping geometry.

Keep terrain raster keys independent of the camera. Ground clipping must not
create per-vessel terrain bitmaps, new source textures or a per-frame full-map
redraw. It must not advance simulation or modify collision boundaries.

## Audit findings and validation

Fixed in this review:

- Foreground land now hides the full overlapping hull/cargo area, not just a
  narrow cap stroke. This preserves the original ground artwork.
- Water shadows/reflections and physical tow spans respect shore surfaces.
- Building fading ignores hull pixels already hidden by solid terrain.
- Regular-stage piers moved out of the late pass that could overpaint buildings.

Explicit limits to retain in future reviews:

- Upright painted hulls/buildings are sprites with a ground pivot, not per-pixel
  3-D meshes. New elevated decks, stacked navigable levels or tunnel ceilings
  need explicit receiver planes and tests; do not infer Z from sprite height.
- Tutorial guidance shares the final coach pass. World markers can overlap HUD
  areas at very close zoom. A future tutorial-layout change should separate or
  constrain those markers rather than changing physical-object depth rules.
- Airborne smoke uses a deliberate air-overlay pass, not volumetric occlusion.
- Native clipping/antialias edges require device review; JVM pixel checks do not
  prove iOS/Android rasterization or touch latency identical.

Required regression matrix for order/asset changes:

- Both foreground and both background banks; quay corners and canal mouths.
- All 32 tug and barge headings, three cargo types, bob trough/crest and zoom.
- Forward/reverse/sideways/stopped motion with unchanged heading where relevant.
- Near/far tow attachments, crossing hull projection, slack/tension/release.
- Solid land under the hull, scenery behind it and scenery actually hiding it.
- Animated fade entry/exit, pause/reset and repeated rendering without ticks.
- Bridge closed/open/moving and its attached lights; title separate from gameplay.

Run `./check.sh`, `./check-art.sh` on the development phone and `./preview.sh`.
`ShoreOcclusionTest` checks geometry/direction and clipped fade masks;
`ShoreOcclusionPixelTest` checks production hull/cargo pixels at every heading;
`SceneCueTest`, `BuildingFadeAnimationTest`, `BridgeModelTest` and render-budget
checks protect ordering, timing and cost. Review `out/shoreline-hulls-*.png` and
the fade sequence, then validate the native build. CI runs translated iOS/JVM
parity. Copy requested visual reviews to Downloads.
