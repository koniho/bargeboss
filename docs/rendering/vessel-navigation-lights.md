# Vessel navigation lights

Every vessel rendered by `VesselView` carries the same shared navigation-light
rig: player tug, feeder tug/load, crossing workboat, title convoy, and all cargo
barge variants. Port is red, starboard green and stern white; powered hulls also
have a white masthead lamp at the reviewed `TugLightArt` mast anchor. Running lamps remain steady. Their low-frequency halo variation never turns the
lamp off. Pushed barges additionally carry the eased yellow flasher described below;
harbor entrance lights retain their separate rhythm.

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
submissions and water clipping. `VesselNavigationReview` verifies all 352 deck-anchor positions (including either-end barge fixtures) against opaque production source pixels, then exports 32-heading
sheets for tugs and barges. Native/device antialiasing still requires host review.

Run `VesselNavigationReview --turns` for 128 production-renderer frames (four
samples per heading) in `out/animation-frames/vessel-navigation-turns/`. Review
forward and rear-quarter overlap plus the moving mast/roof/exhaust attachments.
Exact authored heading points stay registered; intermediate attachment positions
are intentionally eased independently of the shorter image crossfade.

## Inland pushed-barge pulse

Following [USCG Inland Rules 21(g) and 24](https://www.navcen.uscg.gov/navigation-rules-amalgamated),
a barge being pushed ahead shows forward sidelights and a yellow special flashing
light; its stern navigation lamp is suppressed. An astern tow keeps sidelights and
its white sternlight, without the pushed-barge flasher. Forward/aft all-round white
lights are not applied universally to every tow. Mooring, vessel-size exceptions,
composite units and multi-barge group lighting remain outside this simplified rig.
Painted service/work lamps are separate from navigation fixtures.

`nav-special-flasher` is a newly modeled deck fitting at the leading end centerline:
local X = `half + radius * .12`, Y = 0, Z = 6. Both ends of a barge can lead. The
rig rotates 180 degrees when the tug pushes from the opposite end, or an incoming
towline is attached at the negative-X end. Port/starboard fixtures and view sectors
rotate with it. Far/near cargo layering uses those actual transformed positions.
Yachts and powered craft keep their existing running-light rigs.

Player push detection requires hull contact and pushing/forward-drive input;
selecting a distant load does not light its flasher. NPC inbound feeder loads use
this rig even while queued. A towline takes precedence over push contact. Releasing
controls restores the ordinary rig; this is a gameplay-state indication, not a
persistent legal declaration of a tow assembly. Title-only decorative barges retain
the ordinary rig unless represented by those gameplay relationships.

The flasher repeats every 60 simulation ticks: 15 ticks of smoothstep rise, 9 at
full brightness, 21 of smoothstep fall and 15 dark. This produces 60 pulses/minute
with zero-slope joins. Lens, cached halo and water reflection share the same pulse
energy and heading visibility. It does not mutate render state or allocate a new
texture/cache family. The 225-degree forward sector retains the documented soft
camera overlap. Review `BargeLightReview` for pulse phases and either-end operation.
