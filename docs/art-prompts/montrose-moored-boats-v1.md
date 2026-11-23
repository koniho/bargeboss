# Montrose moored boats v1

Direction: unlit, muted dusk silhouettes. Three small powered hulls (runabout,
covered cruiser, fishing cabin) and three furled-sail hulls with different lengths,
colors and mast heights. No emissive windows, navigation lights, halos, wakes or
smoke. Player and mission vessels remain the focus.

Reproducible offline authoring: `tools/BakeMooredBoats.java` constructs rigid hull,
cabin, mast, furled boom, rigging and fender geometry, rotates it in world space,
then projects through the shared orthographic camera. Thirty-two uniform rigid headings per design, six designs, with a dark base and
three RGB receiving layers: 768 cells of 128x128 in a 16x48 sheet. Adjacent true
headings blend for limited tether yaw; artwork never rotates as a flat bitmap.
Cell is 96 world units; waterline center pivot (0.5, 0.7). No bitmap rotations.
The PNG is the production asset; model generator is its editable source.

Regenerate with shared core sources and BakeMooredBoats compiled, then run
`com.bargeboss.game.BakeMooredBoats`. Review actual harbor overview and docking
closeup using MontroseReview, not just the atlas. Boat footprints and hidden tether anchors come from shared berth data, with the
mission bay excluded. Lighting layers receive nearby sources but emit no light.

The baker also exports geometric normal cells; see
[normal-map generation](montrose-moored-normals-v1.md). All layers are rasterized
in local cell coordinates before atlas placement to keep antialiased silhouettes
identical across rows. The existing RGB layers remain the older-host fallback.

2026-11-22: removed bimini roofs and support bows from all three sailboat
families, at every heading. Motorboat canvas is unchanged. Regenerated the
color/receiving atlas, normals and registered canvas masks through the existing
rigid Java baker. Sailboat canvas cells are intentionally transparent, and the
runtime skips their canvas submissions. This is a deterministic model edit;
no AI image generation was used.

Nearby hulls now use HarborBoatModel's prepared live geometry. Local dimensions,
waterline pivot, mast and cockpit layout match the sprite source; live models
add a foredeck hatch, sheer trim and stronger glazing. Their lights, smokestacks
and tire collections are explicitly empty. Stable cleat and fender anchor IDs
are in shared Java. Idle hulls receive marina lighting but emit none.
