# Montrose moored boats v1

Direction: unlit, muted dusk silhouettes. Three small powered hulls (runabout,
covered cruiser, fishing cabin) and three furled-sail hulls with different lengths,
colors and mast heights. No emissive windows, navigation lights, halos, wakes or
smoke. Player and mission vessels remain the focus.

Reproducible offline authoring: `tools/BakeMooredBoats.java` constructs rigid hull,
cabin, mast, furled boom, rigging and fender geometry, rotates it in world space,
then projects through the shared orthographic camera. Thirty exact dock-row/side orientations plus eight star-dock orientations per
design, six designs, 228 cells of 128x128 in a 19x12 sheet.
Cell is 96 world units; waterline center pivot (0.5, 0.7). No bitmap rotations.
The PNG is the production asset; model generator is its editable source.

Regenerate with shared core sources and BakeMooredBoats compiled, then run
`com.bargeboss.game.BakeMooredBoats`. Review actual harbor overview and docking
closeup using MontroseReview, not just the atlas. Boat footprints and line anchors
come from the same shared berth data, with the mission bay excluded.
