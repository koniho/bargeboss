# Chicago Police M3, v1

Reference: the two photographs supplied by Alexander on 2026-11-26, preserved
in `docs/art-studies/police-m3/reference-{overhead,profile}.png`. The profile
reference retains its stock-photo watermark. References are authoring inputs,
not game textures.

Requested change: replace Wake Patrol's generic civilian cruiser with the M3
model in those photographs. Preserve the deep blue wraparound collar, black
lower hull, aluminum wheelhouse, dark glazing, open working decks, bow push
pad, stainless rails, radar mast, antennas, orange aft life ring, stern platform,
American flag and white CHICAGO POLICE / M3 markings. Keep the existing dusk
ambience. No image-generation service was used: geometry and lettering are
shared Java source in `PoliceBoatModel`, exported offline by `PoliceBoatBake`.

## Registration

32 true rigid headings at 11.25 degrees; +X bow, +Y starboard, +Z up, origin at
waterline. Cells are 128 square, world size 96, pivot (.5,.7), matching the
existing marina receiver family. Frames 224-255 belong exclusively to the M3.
Normal RG is octahedral world-space normal after rotation; B is height 0-64.
This is the existing marina receiver format, not the experimental 256-square
watercraft material format. Albedo, normal/height and explicit roughness maps
have identical coverage. Material G and B are zero: lamp emission is shared
fixture state, never inferred from pixel colors. Roughness is authored by
surface class; the current marina shader does not consume roughness.

Tow eyes, navigation lamps, strobes and explicit empty stack/tire collections
are model-local. PoliceBoatArt projects strobes with boat heading and heave
inside the vessel's clipping/depth scope. The boat remains moored with its
existing gameplay collision dimensions; this is an artwork change.

The authoring sheets are offline references. Runtime uses the existing cropped
marina texture path, plus 64 frame-sized 128-square shadow textures. New color,
light bands and normals add 10 MiB of possible cropped RGBA storage; shadow
frames add 4 MiB. Actual cache residency depends on the scene and bounded native
cache. Legacy civilian and racing cells are copied pixel-for-pixel.

Rebuild with core classes plus BakeMooredBoats, RacingBoatBake,
InstallRacingBoat and PoliceBoatBake; run PoliceBoatBake. `./check.sh` verifies
fresh output, registration, borders, roof light rejection and mission assignment,
and exports the actual dusk scene and all-heading review. No approximation of
normals from RGB. The existing surface receiver lacks cast self-shadowing and
uses its established diffuse response; material-channel export is not a claim
that the experimental material shader is enabled.

Rotating red/blue searchlights use the player's extracted `SpotlightBeam` mesh,
220-unit range and .23-radian half-angle. A revolution takes 240 fixed ticks.
This draws colored water pools and airborne volumes; it does not extend colored
spotlight response to other vessels/land receivers. Android native validation
and remaining backend limits are recorded in optimization.md (2026-11-27).
