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
basin and harbor. Ordinary stages use the river banks; shared-slip and deep-channel
levels also use `PhysicalBerths.solids(challengeId)`. These same footprints drive
collision, route planning, water-effect clipping and opaque shore cutouts. Do not
substitute the legacy `HarborGeometry.PIERS` in a career level. Unloaded barges
remain active vessels: they retain normal hull depth, water effects, collision and
shore-line rendering until a successful tow catch transfers the attachment.
Title artwork has its own open-water composition and is
explicitly excluded from gameplay shore clipping.

`VesselView` clips hull, cargo, damage flash and hull glow together. Its
water-plane effects use a nested, stricter clip. Both scopes must restore before
subsequent vessels, buildings and UI. Moving smoke is airborne and is not put
inside the hull's ground clip.

### Animated vessel waterline and foam

Collision surface stamps join the water pass below hulls. Airborne collision
stamps join the same depth merge as vessels, buoys, rope spans, shore sections,
factories and lock gates; use each stamp's water-plane position, then apply its
ballistic height only to its drawn position. They are never a late global
overlay. See [directional collision stamps](collision-splashes.md).

`VesselWaterline` derives contact positions from the 32 registered lower-hull
silhouettes with the same sector blend as the hull. Small transparent foam
sprites appear intermittently along that contour; do not restore a continuous
thick outline. `WaterSurface.foam` holds detached displacement and propeller-wash
sprites in world space, advanced at 60 Hz. Speed drives bow/shoulder foam;
engine loading drives tug wash. Unpowered barges have no propeller wash.
Released patches drift, expand and fade independently of vessel heading/position.

Detached foam draws with water beneath hulls; contact foam draws at the lower
hull after its sprite. Both use the all-dry-land clip, including background banks,
and neither contributes to building-fade silhouettes. Flat texture decals use
affine projection onto the water plane, unlike upright vessel artwork. See
[foam references, asset prompt and source behavior](../art-prompts/vessel-foam-v1.md).

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

Each `Towline` link keeps its own leader/follower fittings and catch/release
state; see [barge trains](../gameplay/barge-trains.md). The tug keeps the stern fitting at `-(tug.half + 7)` along the tug's heading.
The selected barge end is `+(barge.half + 7)` or its negative, and rotates with
the barge. `TowView.Segments` constructs 24 spans per link between those fittings, adding
deck height, each hull's heave and vertical sag. Merge spans from **all** links by their unraised
world midpoint depth alongside vessels and scenery. Thus far-side fittings and
spans can pass behind a hull while near-side spans remain visible in front.

Each span also obeys the solid-ground clip. A towline does not become visible
through a quay because it is an interaction object. Release recoil uses the
same span/depth rules. Catch indicators and release labels belong to the cue
pass and are distinct from the physical rope.

Shore lasso aiming/flight uses `LassoPath`, including the hull-edge origin and
elevated throw arc. Do not replace it with the towline's low, sagging path.
Attached shore-mooring lines and airborne casts now join the same depth merge.
Their shared ground/height paths distinguish water spans from lines resting on
the raised quay; deck fittings and bollard coils retain their own local placement.
See [rope materials](rope-material.md).

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

The optional joystick is a screen-space HUD overlay drawn after tutorial cues.
It never participates in vessel/rope/shore depth sorting or scenery fades. Its
visible hit circle owns the native pointer before water-plane projection; see
[joystick input and settings](../gameplay/joystick.md).

Entrance navigation markers and the two practice buoys join the same depth merge
as vessels, rope spans, shore caps and factories; lantern halos stay with those
fixtures. Their water reflections draw before hulls and use water-only clipping.
Floating fixtures use their simulated `Buoys.Buoy` position for this entire pass,
including culling and depth; their fixed mooring anchors are not drawing pivots.
Contact roll combines with wave roll without rotating the water plane.
Buoy contact/cast shadows also draw before hulls. Sprite roll and delayed heave
share a waterline pivot with the lamp anchors; a separate curved meniscus clip
hides the submerged float, with translucent sprite foam along the contact edge.
This clip follows the water height, never the sprite roll, and nests inside the solid-shore cutout. Buoys do not use building fading.
Vessel navigation fittings stay inside each vessel's shore clip and cargo layering.
See [entrance marks](navigation-lights.md) and [vessel navigation lamps](vessel-navigation-lights.md)
for fixed returning directions versus hull-local port/starboard anchors.

Tutorial entrance beacons stand on dry concrete foundations at the shared raised
quay elevation. Their sprite/lamp transforms never sample waves. Ground light
and separate shadow masks draw after cached ground, with a deck-height water
cutout, before the ordinary world depth merge. Only the yellow practice targets
remain floating in this scene. Fixed beacons never use building fades.

## Lock machinery and water-level cues

The third career level uses two `LockView` gates; the opening bridge remains in
the tutorial. Each lock gate is an `ArticulatedModel` with fixed bank machinery
and independently hinged rigid leaves. Their steel faces, braces and top rails
rotate in XYZ about the bank pivots before projection. Lantern anchors belong
to their fixed machinery or moving leaf nodes, and model self-occlusion controls
their visibility. Never rotate a painted isometric gate bitmap or leave a lamp
at its closed-position screen coordinate.

`LockDoors` supplies the same bank hinges, leaf length and opening angles to
`LockView` and watercraft collision. Folded leaves remain solid at their visible
bank-side locations; see [lock-door collision](../gameplay/lock-door-collision.md).

`Renderer.river` merges each gate at its unraised world-ground depth among
vessels, individual rope spans and navigation markers. Gate mechanisms are solid
equipment, not building-fade candidates. The model sorts its own faces; the
world merge orders the gate assembly relative to nearby boats and towlines.
Water tint, wall wet-height marks, gauge and filling turbulence draw before
hulls. The water-level cycle is a visual abstraction on the fixed navigation
plane: it does not independently lift hull sprites, shift towline attachments,
or change the projection of emitted wakes. See the
[phase and goal contract](../gameplay/goal-levels.md) before changing that rule.

### Surface-light response passes

Dynamic surface lighting follows each watercraft/cargo base sprite immediately,
inside the same shore clip and before nearer cargo or scenery. Floating markers
also retain their moving waterline clip for the lighting pass. Response layers
use identical pivots, directional frames and blending; they are not new hulls for
building-fade detection. Existing glows, cast shadows, foam and navigation lamps
keep their own order. Scenery receivers remain explicit opt-ins. See
[surface lighting](surface-lighting.md) for the response data and rollout contract.

## Water batches and overlay-only text

`WaterBatchPainter` groups screen-space polygons of exactly the same ARGB color
only when their padded bounds are disjoint. It cannot move a polygon across an
intersecting later group. The two-logical-pixel padding per shape protects native
antialias fringes; overlapping translucent water stays in painter order.
Non-polygon draws and clip changes flush pending water. The buffer is bounded to
256 polygons and a 48-group search window. This changes submission grouping,
not geometry, wave phases, alpha, wake interaction or shoreline clipping.

`Painter.polygons` may fill separate same-color contours in one native path.
Java2D and Core Graphics use compound paths. Android expands the contours to
individual paths by default: device comparisons found this faster end-to-end
than fewer, larger native paths. A debug switch retains the compound-path
implementation for future device comparisons. Trace painters also expand the
contours. Arrays are borrowed only for the call; hosts must not retain them.
Only proven disjoint shapes may use this primitive. `WaterBatchReview` verifies
native multi-contour fills against individual fills at three widths and multiple
animation phases.

Visible text belongs to screen UI: menus, HUD, tutorial coach panels, fixed
notifications and offscreen navigation overlays. Vessels, berths, machinery,
buoys, ropes and world control cues use lighting, colors, arrows and animations
without captions. Do not add text by projecting a world point onto the screen:
that is still a world label. Feeder captain calls now use fixed HUD lanes;
gate-clear messages use a fixed overlay panel. Existing world caption helpers
suppress `RiverPainter` text to enforce this boundary.

`Lettering` caches the unchanged authored glyphs in immutable `StaticLayer`
rasters, keyed by content, RGB and source size. Position and opacity remain live.
Its bounded 128-entry source cache uses unique keys in namespace `0x60000000`;
evictions never reuse an old native raster identity. Host raster caches retain
the existing 48 MiB limit and one-build-per-frame allowance. Sheared/fallback
painters still expand the exact glyph geometry. Test both cold and warm UI and
retain text legibility when adjusting source density or caches.


### Android sprite texture storage

Android retains cropped frame bitmaps (24 MiB LRU) instead of referencing an
entire directional sheet for every sprite draw. Desktop and iOS already use
cropped frame images. This is a native cache policy: `SpriteAtlas` remains the
only source of cell rectangles, heading counts, size corrections and pivots.
Do not trim alpha margins, rescale source pixels or move effect anchors as part
of this optimization. Full-image frames reuse the decoded bitmap. The title sky
fade strips retain their shared source texture to preserve filtered edge samples.
Eviction drops references without recycling bitmaps that recorded hardware draw
commands may still use. Atlas metadata, gameplay, clipping and depth order are
unchanged. Native software pixel review and hardware screenshots cover sampling
rounding differences; shared trace parity alone cannot establish raster parity.


### Prepared terrain rasters

Terrain may be rasterized before visibility using the optional painter cache
hints. Preparation has no drawing position or opacity and never changes the
visible chunk pass, depth merge, raised shore geometry or building fades. Only
immutable `StaticLayer` implementations explicitly marked background-safe may be
painted off the UI thread. A prepared image uses the same chunk key, trimmed
bounds and raster density as the synchronous path. Publish only complete images;
late results use the existing direct fallback. Never replace a missing terrain
raster with transparent pixels or flatten foreground vessels/ropes into a cache.
See [terrain preparation](../performance/terrain-preparation.md).


## Shared surface-foam clipping

`VesselFoam.draw` computes the union of visible decal bounds, builds one
`ShoreOcclusion.region` at negative-infinite depth, and applies that clip around
the existing oldest-first surface-decal pass. This avoids rebuilding native clip
state for every stamp. Foam stays on the water plane and retains its source
pivots, animation, alpha order and solid raised-shore cutout. Empty/offscreen
passes do not push a clip. The clip is restored in `finally`.

Do not extend this scope over hulls, ropes, buildings or airborne collision
splashes: those use different depth/height rules. A common clip is appropriate
only for elements that share the same surface plane and shore coverage. Keep the
foam shoreline raster checks when changing the bounds or mask generation.

### Montrose marina

`MontroseHarbor.WATER_POLYGON` minus the oriented `DOCKS` quads is the shared
water footprint. Triangulated `LAND_POLYGON` and dock geometry supply foreground
shore/dock cutouts for hulls, foam and ropes; a dock's color
sprite never defines collision. Cached grass/wood chunks draw before hulls, with
those cutouts preserving foreground solid coverage. Tree/rock placements are
inland and cannot overlap the navigable water, so the stage does not run building
fade queries. Dynamic water, navigation lights, wakes, ropes and watercraft never
enter static chunk caches. Yacht heading/blend/pivot selection is shared across
color, glow, shadow, surface shading and opaque waterline masks.

Montrose's two entrance skeleton-tower sprites are fixed at the breakwater
endpoints. Their pivots, lamps, deck spill and shadow receiver share
`MontroseHarbor.DOCK_RISE`; their merge depth remains unraised world depth.
`NavigationLights.landShadows` belongs after cached decks and before the object
merge. Its two local precomputed cutouts exclude everything outside each narrow
pier from deck shadows/light, while reflections separately use the water-only
pass. These fixed fixtures have no `Buoys`/`Watercraft` bodies or wave motion;
solid breakwater collision supplies the physical obstruction. Tutorial fixed
beacons retain their raised-quay receiver and existing clipping behavior.

Montrose raised-wall masks clip the physical ground edge against the vessel's
unraised depth **before** extruding its vertical face. Clipping an already
projected face at `depth-rise` incorrectly retains the lower pixels of a rear
wall. Only camera-facing wall normals may add vertical cutouts. Top receivers
retain their own land/dock rise, with both sides excluded for water-plane
passes. World bounds clip each edge before extrusion, so a local hull mask
cannot pull in the entire length of a distant wall.

Intersecting finger docks, gangways, land tops and raised wall faces must be
combined into a disjoint union before calling `pushClipOutside`. Montrose
subtracts bounded original triangle blockers from each incoming piece, rejects
nonoverlapping bounds and prunes zero-area fragments. Do not use accumulated cut
fragments as new blockers: that introduces unnecessary numerical subdivisions.
`PolygonWater.area` uses origin-relative double accumulation to keep thin-piece
orientation stable; clipping buffers allow additional roundoff crossings.
`MontroseGeometryTest` checks raised front/rear coverage, exact-edge hull
separation, camera/heading invariance, positive-area triangle overlap and
sampled even-odd/native coverage at dock junctions. Native raster review remains
necessary for antialiasing and actual device behavior.

## Procedural water-foam patches

`Painter.waterFoam` shades a small centered affine patch. Shared Java owns mode,
phase, hull registration, speed response and lifetime; native Metal/AGSL execute
one shared kernel. Bow pressure patches draw in the existing below-hull water
scope. Vessel water contact is a continuous animated intersection curve derived from the
registered lower silhouette, independent of hull heave. Disjoint clip trapezoids
hide hull, cargo and surface-light/glow pixels below it. A thin, constant-color
translucent strip follows the exact boundary in a single composite, avoiding
brighter overlapping endpoint dots. Moving vessels add a water-side foam ribbon
using continuous arc-length UVs and the shared shoreline material; speed controls
its width and opacity, with no foam at idle. This also applies to the title voyage.
No marching contact stamps or dotted dash pattern are drawn. Detached
wakes, bow pressure and propwash remain separate water effects.
Expanding wake crests share a water-plane shore/dock exclusion scope, bounded by
the visible world region. Title patches also exclude pixels above their water
plane. Painter wrappers must forward the primitive and preserve clip/opacity/order.
Fallbacks use authored textures or short crest geometry inside the same scopes.
See [water shaders](../performance/water-shaders.md) for costs and validation limits.

## Rope materials and attached lines

Tow, recoil, attached mooring and airborne cast spans now share the world-depth
merge. Native rope shaders only shade bounded strips with shared continuous strand
coordinates. Vessel-specific visual fittings follow hull heading and heave;
shore roots remain on the same selected local fitting through winching. Mooring
land sections ride raised quay tops while water sections retain shore cutouts.
See [rope material and attachment contract](rope-material.md) for bounds, tests
and the remaining limits of sprite-based depth.

Detached wake crests use the original expanding rear-facing arcs, anchored to the
packet's recorded position and travel direction. Origins/headings remain fixed
after emission; turning or stopping the source cannot rotate or stop old waves.
The V-arm and translating-wavelet experiments have been reverted. Original foam
animation, packet pools, aging, shoreline clipping and gameplay forces remain.

### Native union clips for surface water

`Painter.supportsUnionClips()` permits a clip made of overlapping, consistently
wound triangles. Android Canvas and desktop Java2D use nonzero winding and support
this directly. The foam-decal and detached-wake passes request that representation,
so Montrose does not repeatedly subtract overlapping receiver triangles for them.
The shoreline outline, world bounds, projection and receiver heights are unchanged.

Other painters default to false and receive the existing disjoint representation.
In particular, Core Graphics' even-odd inverse path and Metal's finite stencil
counter retain that fallback until their union strategy is separately validated.
Painter wrappers forward the capability; projected-mask cache keys include the
strategy, within the existing 32-entry / 256 KiB payload bound. Do not send raw
union triangles to a painter that has not opted in.

### Moored Montrose boats and vessel shore spill

`MooredBoats` adds tethered hulls in surveyed finger/star slips, excluding the mission
bay. The shared depth merge includes their unraised waterline centers. Six rigid
models are baked offline at 32 uniform headings with RGB receiving layers; native
painters only draw registered cells. `MooredFleet` owns fixed-step spring/damping,
wake/contact response, 2.5-unit tether reach, limited physical yaw and vertical
heave. Visible lines are omitted. NPC lights, wakes and smoke are absent; nearby
lights illuminate their surfaces. Moving hull footprints block active craft; buoy-contact tests reject
approaches already occupied by a hull. Clear water and mission approach geometry
remain unchanged.

`VesselShoreLight` draws colored shore wall/lip receivers after ground, before
vessels. Active craft supply warm work lamps and red/green/white sector lights;
receiver bearing, not camera visibility, controls illumination. Short distance
falloff and shore/dock line-of-sight checks restrict spill. This is stylized local
illumination, not a shadow map or a whole-scene physical lighting simulation.

Low Montrose dock tops also receive boat light over their full footprints when the
lamp is above the deck. `DockTopLight` precomputes disjoint eight-unit receiver
cells during level preparation; overlap subtraction uses original local blockers,
never accumulated fragments. Raised land can block low rays while intervening
lower docks do not incorrectly block an above-deck light. RGB hull reception is
incoming light only, not a light-emitting NPC fixture. The lightweight adjacent
heading dissolve preserves opaque-overlap color but may retain a narrow outgoing
silhouette fringe until the next heading; no painted isometric bitmap is rotated.

Collision spray uses water-foam modes 8 (tapered airborne liquid fingers) and 9
(broken water-plane impact crowns). These are separate from propwash modes 2/3.
Detached droplets share each packet's ballistic transform and depth clip. The
existing bounded contact pool, impact-strength response and heavy-hit sparks are
unchanged. Fallback painters use tapered geometry and broken rim segments within
the same clips; native shaders add fine gaps and spray texture.

### Stamped collision spray

Collision splashes now use `SpriteAtlas.SPLASH`, four padded irregular water-sheet
stamps. Three visible airborne clusters retain fixed-step ballistic positions,
stable cell selection and a fixed launch-relative orientation with small variants.
One lower-opacity, expanding stamp supplies surface residue. The packet pool and
contact response stay unchanged; geometric droplets and the radial splash flash
are removed. Heavy-hit sparks remain separate. Native painters all draw the same
registered pixels inside the existing water/depth clips. The earlier procedural
spray modes are no longer used by collision effects.

## Direction arrows after the visual rollback

WorldArrows retains its bounded queue and midpoint depth ordering with vessels.
Arrow appearance is restored to thin, open feathered strokes. The filled neon
planes and segmented guidance materials were rejected and removed. See the
[current highlight policy](../art-direction/gameplay-highlights.md).
