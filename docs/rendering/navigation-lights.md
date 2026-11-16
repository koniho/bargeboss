# Navigation marks and practice buoys

`NavigationLights` owns named entrance placements and their returning vectors in
world coordinates. It follows the US/IALA-B lateral convention: returning into
harbor or proceeding upstream, green marks port and red marks starboard.
[US Coast Guard Aids to Navigation](https://www.uscgboating.org/images/486.PDF)
is the reference. The fixtures do not switch color when the camera moves or a
boat turns around. When sailing outward, the colors naturally reverse sides
relative to the boat.

Physical marker pairs belong only at mouths between distinct regions. Practice
marks the basin/canal, canal/harbor and both ends of the narrow towing channel.
The basin-to-canal and canal-to-harbor mouths return along world +X (red on +Y);
the towing channel and shift harbor mouths return along world -Y (red on +X).
The transit shift also marks entry to its upstream harbor. Bridge approaches,
interior piers and ordinary bends do not receive extra pairs. Bridge stop/go
signals retain their own behavior. The driving tutorial shows one temporary
next-turn cue while navigating; repeated gate posts are not scenery.

Each lateral pair uses one regular flash every two seconds, with a longer true
dark interval. The colored float and its triangular red / square green daymark
remain visible between flashes. Practice buoys use numbered yellow daymarks and
yellow lanterns with a steady base glow between pulses, so the two driving targets
stay conspicuous without being confused for lateral navigation aids.

Floating markers use the [screenshot-based sprite family](../art-studies/buoys-v1/README.md).
Tutorial entrance pairs instead use [fixed shore beacons](../art-studies/land-markers-v1/README.md),
with square concrete foundations entirely on dry ground. They stand 18 world units
back from each canal bank and 26 units inside the narrow reach at its mouths.
The two yellow practice buoys remain floating. Regular shifts retain red/green
floating markers. Fixed beacons have no heave, roll, submergence or building fade;
their ground pivot sits on the raised quay, and their lamp pulses remain animated.
Their independent contact/cast shadows and soft light spill draw after cached
terrain, before depth-sorted vessels, shore caps, beacons and buildings. A deck-height
water cutout keeps those ground effects off the channel.

## Floating collision and moorings

`Buoys` owns one lightweight circular body per floating marker, initialized on stage
or tutorial reset. `NavigationLights.anchorX/anchorY` remain fixed seabed mooring
locations; `x/y` return the simulated waterline position. The numbered tutorial
course stays centered on the fixed moorings so float drift cannot award orbit
progress or move the player's target. Land beacons have no floating body.

Buoy radius is 27% of displayed world size, matching the float base rather than
the tall tower. Tugs and barges exchange linear impulse and off-center torque
with the float. Secured hulls and scripted feeder craft push it without moving
off their berth or path. Buoy impacts give contact haptics without hull damage.
Pickup assistance leaves clearance on the open-water side of entrance floats;
direct player steering can still drive into them.

Each float has a damped spring toward its mooring, progressive chain resistance,
and a finite excursion of 55% of its displayed size. The footprint remains on
water, and collision corrections reapply vessel bank constraints. Contact also
excites a damped roll oscillator. Positions, velocities and contact roll advance
in the same two fixed motion substeps as vessel physics; they freeze on pause
and never depend on camera visibility or rendering frequency.

`WaterSurface.buoyForce` samples the arriving bands of existing rear wake fans
for horizontal push/drawback, with ambient wave drift and lock current where
applicable. An intervening dry bank/quay screens that horizontal force. Existing
wave-height sampling supplies vertical bob and slope-driven roll; it remains the
shared visual wave field, not a full fluid/shore diffraction simulation. No
additional wake particles or per-step lighting textures are allocated.

Every displaced float uses its current position for visibility, depth, shadows,
waterline, reflections and lighting. The sprite and lamp rotate together about
the registered waterline pivot; the water meniscus stays independent of roll.
Contact roll is bounded separately and combines with wave roll to at most 0.26
radians. No sprite is rotated to invent a different isometric heading.

For floating fixtures, `BuoyArt` registers each unchanged
PNG to its waterline pivot and lantern center. Shared wave slope drives bounded
sprite roll, with delayed heave for bounce. A curved moving meniscus hides
the lower float independently of sprite rotation; soft sprite foam and faint
outward crests make the wet contact visible; the lamp and glow follow the
same rigid transform as the tower. Separate contact/cast shadow masks flatten on
the water before hulls, with shoreline clipping.

Reflection ripples draw through `WaterEffectsPainter` before hulls, while buoy
structure and lantern halos join vessel, rope, shoreline and factory depth order.
They are not HUD overlays and never participate in building fading. Each fixture
is culled before wave sampling or glow work. Static glow kernels are reused;
animation depends only on shared simulation wave state and `visualTicks`.

`NavigationLightsTest` validates returning sides, entrance land/water placement,
heading/aspect stability, depth ordering, flash/dark phases, culling and render
purity. `NavigationReview` captures practice, canal and harbor entrance scenes
through the actual shared renderer. A new stage entrance must add a named entry
with a normalized returning vector; do not infer it from the current tug heading.

`BuoyPhysicsTest` covers all 32 tug/barge contact headings, momentum, off-center
torque, chain limits, settling, bank constraints, fixed NPC/secured hulls, wake
arrival and screening, pause/reset, render purity and 30/144 Hz parity. It also
guards nearest-keel barge alignment after contact: either end of a barge may
lead, without an unnecessary half-turn in a narrow slip.
`BuoyCollisionReview` exports an actual drive-in/back-away/settle sequence to
`out/buoy-collision-motion/` and a comparison board to
`out/buoy-collision-review.png`.

Floating buoy/marker bodies also implement `Watercraft` and report collisions
through the shared [directional splash-stamp pipeline](collision-splashes.md).
Their actual relative contact velocity determines the spray direction and
strength; a tether reaching its limit does not emit a collision splash.

## Montrose breakwater beacons

Montrose's entrance pair uses the same fixed tower assets as the tutorial, at
its two traced breakwater tips. The west/port tower is green and east/starboard
is red. Its flash repeats every 180 simulation ticks (three seconds), matching
the published USCG light characteristics; ordinary shifts/tutorial retain their
existing 120-tick cadence. The separate mooring-can field remains unlit and
floating. See the [Montrose source audit](../gameplay/montrose-layout-audit.md).

`NavigationLights.land` identifies both entrance towers, so `Buoys.reset/get`
create no `Watercraft` body for them and no tether, wave roll, buoy contact or
buoy splash applies. The physical breakwater remains the collision target.
`LandMarkerArt.baseRise` selects `MontroseHarbor.DOCK_RISE` for these towers;
tutorial beacons still use `GroundArt.LAND_RISE`. Lanterns, foundation pivots,
contact/cast shadows and deck light all use that same receiving elevation.

Two immutable local ground cutouts are prepared once from the actual breakwater
quads. Per-frame work only projects these small masks: it does not triangulate
or subtract the complete harbor. Ground shadows/light draw after cached decks;
tower sprites/lanterns join the existing unraised-ground-depth merge. Animated
reflections are separate water-only effects before hulls. Never bob the tower or
use its raised lantern as the depth key. No fixed beacon fades like a building.
