# Montrose canvas tops, v1

Reference: the user's Montrose Harbor Google Maps view, supplied 2026-11-22,
showing neighboring slips with cream, navy, blue, burgundy, green, tan, charcoal
and teal canvas. Preserve the existing six hull/cabin designs, subdued late-dusk
harbor, mooring positions and mast/rigging. Add an aft-cockpit canvas bimini on
thin stainless bows; fabric color belongs to the berth, not the hull type.

Source: `tools/BakeMooredBoats.java`, explicit fabric faces on each rigid model.
`BiminiArt` owns the palette and registration. This is procedural geometry,
not image-generation output. Rotate the model through all 32 headings before
projection. Rasterize fabric in the same sorted face order, clearing behind
non-fabric foreground geometry so cabins, booms and rigging occlude the canvas.

Regenerate by compiling shared core and BakeMooredBoats, then running
`com.bargeboss.game.BakeMooredBoats` from the repository root. This updates the
base/RGB-receiver atlas and packed world-normal/height atlas together, plus
`assets/sprites/frames/montrose-bimini-v1/{0..1535}.png`.

Each fabric frame is 52x32, cropped at (38,64) from the original 128x128 cell;
world dimensions 39x24, pivot (0.5, (128*0.7-64)/32). Frame index is
`palette*192 + model*32 + heading`. All palettes share geometry/alpha. Base and
normals remain 128x128 with pivot (0.5,0.7). Never recolor hull/window pixels.
Canvas source colors are daylight albedo; shared SpriteGrade handles time of day.
Normal lighting uses the rebaked actual canopy surfaces, with no emissive canvas.

`BiminiReview` checks all 1536 frame registrations, identical palette masks and
containment in all 192 base silhouettes, and exports day/dusk/night palette boards
and actual harbor views. `NormalBoatReview` checks exact normal/base alpha.
