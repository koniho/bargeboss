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
