# Vessel navigation lights

Every vessel rendered by `VesselView` carries the same shared navigation-light
rig: player tug, feeder tug/load, crossing workboat, title convoy, and all cargo
barge variants. Port is red, starboard green and stern white; powered hulls also
have a white masthead lamp at the reviewed `TugLightArt` mast anchor. These are
steady running lamps. Their low-frequency halo variation never turns the lamp
off; the separate harbor entrance lights flash.

The color and viewing sectors follow [USCG Navigation Rule 21](https://www.navcen.uscg.gov/navigation-rules-amalgamated):
112.5-degree sidelights, a 225-degree forward masthead sector and a 135-degree
stern sector. The elevated game camera deliberately extends these nominal sectors with a
22.5-degree smooth fade (two authored heading steps). This is an artistic visibility
overlap, not a change to the real navigation sectors: forward poses show red and
green together, powered craft also show the masthead, and rear-quarter poses can
show a side lamp with the stern lamp. Broadside views still hide the opposite
colored lamp; directly astern hides both sidelights and the forward masthead.
Each lamp evaluates its own visibility rather than selecting one lamp per pose. This is a deliberately simple game rig on every barge, rather than a
complete model of lighting requirements for vessel lengths, mooring conditions,
towing assemblies or special operations. Entrance markers use a different rule:
red to the right when returning; the red light aboard a vessel is on its port side.

`VesselNavigationLights` owns stable `nav-port`, `nav-starboard`, `nav-stern` and
`nav-masthead` IDs. New deck lamps are explicitly added fixtures with body-local
X/Y and Z=6, not guesses at existing painted lamp pixels. Positive local X points
to the bow; positive Y is starboard. The masthead reuses the reviewed source-cell
anchor and full-sector `SpriteAtlas.attachmentBlend` position easing. The same
position easing serves roof work lamps and exhaust outlets. Hull imagery keeps its
short crossfade, while painted roof-lamp visibility still follows those image weights
so a hidden fixture does not glow through its occluder. Body-local deck lamps already
move continuously with heading; they do not snap to a frame or accumulate temporal lag. Every fixture shares the hull's existing
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

Run `VesselNavigationReview --turns` for 128 production-renderer frames (four
samples per heading) in `out/animation-frames/vessel-navigation-turns/`. Review
forward and rear-quarter overlap plus the moving mast/roof/exhaust attachments.
Exact authored heading points stay registered; intermediate attachment positions
are intentionally eased independently of the shorter image crossfade.
