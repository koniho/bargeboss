# Montrose sprite canopy depth correction — 2026-11-23

No generative prompt: deterministic bake from the existing accepted boat geometry
and palette in tools/BakeMooredBoats.java. Use a 2x supersampled depth buffer,
shared winning faces for color, receiver bands, octahedral normal/height and
canvas masks. World camera depth is y-x+z, consistent with the shared projection.
Average face sorting is forbidden: it erased roofs at down-left headings.
Keep frame registration/crops/palettes, no sails/canopies added to sailboats.
BiminiReview checks every motorboat canopy center and exports all32 headings.
