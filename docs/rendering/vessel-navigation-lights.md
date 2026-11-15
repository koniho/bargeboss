# Vessel navigation lights

Every vessel rendered by `VesselView` carries the same shared navigation-light
rig: player tug, feeder tug/load, crossing workboat, title convoy, and all cargo
barge variants. Port is red, starboard green and stern white; powered hulls also
have a white masthead lamp at the reviewed `TugLightArt` mast anchor. These are
steady running lamps. Their low-frequency halo variation never turns the lamp
off; the separate harbor entrance lights flash.

The color and viewing sectors follow [USCG Navigation Rule 21](https://www.navcen.uscg.gov/navigation-rules-amalgamated):
112.5-degree sidelights, a 225-degree forward masthead sector and a 135-degree
stern sector. A six-degree soft edge avoids sudden disappearance during sprite
rotation. This is a deliberately simple game rig on every barge, rather than a
complete model of lighting requirements for vessel lengths, mooring conditions,
towing assemblies or special operations. Entrance markers use a different rule:
red to the right when returning; the red light aboard a vessel is on its port side.

`VesselNavigationLights` owns stable `nav-port`, `nav-starboard`, `nav-stern` and
`nav-masthead` IDs. New deck lamps are explicitly added fixtures with body-local
X/Y and Z=6, not guesses at existing painted lamp pixels. Positive local X points
to the bow; positive Y is starboard. The masthead reuses the reviewed source-cell
anchor and its frame interpolation. Every fixture shares the hull's existing
waterline pivot, scale and heave; fixtures do not alter vessel pose or simulation.

Far barge lamps draw before cargo; near lamps draw after cargo. All fixtures stay
inside `VesselView`'s existing solid-shore clip and world depth order. Colored
water shimmers draw before the hull, with the existing water-only and raised-shore
clips. Halos use the cached scenery glow kernel and a small saturated lens.

`VesselNavigationLightsTest` checks sectors, wraparound, all 32 headings and both
hull types, bob, deterministic rendering, player/NPC/cargo coverage, bounded
submissions and water clipping. `VesselNavigationReview` verifies all 192 new
deck anchors against opaque production source pixels, then exports 32-heading
sheets for tugs and barges. Native/device antialiasing still requires host review.
