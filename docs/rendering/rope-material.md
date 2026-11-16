# Rope material, fittings and depth

`Painter.ropeSegment` is a bounded strip with continuous along-rope strand
coordinates. `assets/shaders/rope.inc` supplies rounded shading, diagonal braid
and fine fibers to Metal and Android API 33+ AGSL. Colors are weathered hemp with
muted highlights, matching the industrial painted hulls. Core Graphics, Java2D
and older/software Android retain bounded layered strokes and short braid marks.
Native hosts never choose fittings, sag, rope paths or gameplay state.

`RopeAttachment` defines a body-local visual rig for powered tugs, cargo barges
and yachts. Each family has its own deck height and end inset. Barge fittings
sit beyond the shared CargoArt hold ends and above its rim; cargo sight-ray
solids use that same hold length/width. These are readable
model estimates, not claims of surveyed cleats in every authored pixel. Cleats
and rope roots use the same coordinates and heave. Deck fittings draw after hull
lighting and before cargo. Shore casts and their previews share the same chosen
end/side fitting; the stored local root follows the hull during flight. A caught
mooring keeps the side/end chosen at its initial position while the winch turns
and translates the boat. Tow spring/torque lever arms remain unchanged.

`TowView.Segments` includes active towlines, release recoil, attached shore
moorings and airborne cast tethers. All join the existing ground-depth merge
with vessels, cargo, buoys, scenery and shore sections. Tow/mooring curves have
24 spans each; the one active cast has 32. The flying loop travels with its last
span instead of becoming a late world overlay. Aiming arcs and hit rings remain
interaction cues. Recoil preserves its captured endpoint heights.

Each water-side span retains the raised shore/dock cutout. A mooring's land-side
section rides the quay top, so it is not incorrectly removed by a water-only
mask. Its ground position still determines its ordering against hulls and
scenery. Four bounded sight-ray samples per span also test shared hull, cabin and cargo
solids. Contiguous visible pieces merge back into one submission; hidden sections
cannot paint through a nearby cabin even when the span's ground point sorts in
front of the vessel center. Those body-local solids cover all active player,
cargo, yacht, feeder and traffic hulls. They are conservative model estimates,
not per-pixel reconstruction of every railing or partially unloaded cargo gap.

No new rope simulation, additional textures, random render state or wall-clock
animation is introduced. `rope_patches` verifies native submissions. All native
and shared wrappers preserve geometry, opacity and strand coordinates. Fallback
braiding is coarser and is not claimed pixel-identical to shaders.

`RopeMaterialTest` covers every heading and both ends across all cargo/yacht
families, continuous strand coordinates, transformed/opacity wrapper delivery,
heave, balanced clips and release attachment continuity. `SceneCueTest` checks
actual root render ordering for those families in ordinary and tutorial stages,
connected and recoiling. `LassoFeedbackTest` checks preview/flight agreement and
moving roots. `tools/RopeReview.java` exports the actual shared renderer for all
32 headings of each towable hull family. Physical iOS/Android pixels still need
on-device review; the Android phone was not used in this round.
