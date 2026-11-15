# Navigation marks and practice buoys

`NavigationLights` owns named entrance placements and their returning vectors in
world coordinates. It follows the US/IALA-B lateral convention: returning into
harbor or proceeding upstream, green marks port and red marks starboard.
[US Coast Guard Aids to Navigation](https://www.uscgboating.org/images/486.PDF)
is the reference. The fixtures do not switch color when the camera moves or a
boat turns around. When sailing outward, the colors naturally reverse sides
relative to the boat.

The practice basin-to-canal and canal-to-harbor mouths return along world +X;
red therefore sits on +Y. The bridge approach, towing-channel entrance and exit,
and main-shift reaches return along world -Y; red sits on +X. The shift registry
also marks the main river arrival entrance, upstream harbor beyond the transit
bridge, and the two constrained reaches alongside industrial finger piers.
These explicit placements are entrance aids, not progress gates or bridge
permission signals. Bridge stop/go signals retain their own existing behavior.

Each lateral pair uses one regular flash every two seconds, with a longer true
dark interval. The colored float and its triangular red / square green daymark
remain visible between flashes. Practice buoys use numbered yellow daymarks and
yellow lanterns with a steady base glow between pulses, so the two driving targets
stay conspicuous without being confused for lateral navigation aids.

All fixtures use the same code-native isometric steel float, upright post and
lantern, with scale reduced beside the narrow train passage. Float position and
vertical rise come from the shared water wave/wake field. Reflection ripples are
drawn through `WaterEffectsPainter` before hulls, while buoy structure and lantern
halos join vessel, rope, shoreline and factory world-depth order. They are not HUD
overlays and never participate in building fading. Each fixture is culled before
wave sampling or glow work. Static glow kernels are reused; animation depends only
on shared `visualTicks`, with no native or rendering-side simulation.

`NavigationLightsTest` validates returning sides, entrance water placement,
heading/aspect stability, depth ordering, flash/dark phases, culling and render
purity. `NavigationReview` captures practice, canal and harbor entrance scenes
through the actual shared renderer. A new stage entrance must add a named entry
with a normalized returning vector; do not infer it from the current tug heading.
