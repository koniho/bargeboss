# Collision splash stamps v1

Generated with the built-in image-generation tool, then packed with
`tools/PackSplashStamps.java`. The selected source is
`docs/art-studies/collision-splash-stamps-source-v1.png`; runtime artwork is
`assets/sprites/collision-splash-stamps-v1.png` (256×256 RGBA, four 128px cells).
The packer downsamples cells without replacing or deriving the generated alpha.
It rejects cells with visible pixels in their four-pixel boundary padding.

## Initial generation prompt

Create a production game VFX sprite atlas: a square transparent PNG with exactly
FOUR separate water splash stamps in an evenly spaced 2 by 2 grid. Each sprite
fully contained in its own quarter with generous transparent padding. No text,
no labels, no borders, no ground, no background, no checkerboard. Semi-realistic
painterly arcade maritime game, pale cool gray-white water foam with translucent
ragged edges. NOT round bubbles, NOT rings, NOT spiral propwash, NOT geometric
triangles. Four different short bursting sheets of water with ragged branching
fingers, torn holes, delicate mist and only a few tiny elongated flecks integrated
into each stamp. Each stamp's general jet travels LEFT to RIGHT, asymmetrical broad
fractured spray opening to the right, compact dense base on left; variants differ
in fan shape and curvature. High contrast internal foam texture and irregular
transparency, readable at 128 pixels per stamp. Avoid photographic shiny glass
look, avoid blue water blocks, avoid large circular droplets. These are airborne
impact splash clusters for a tugboat game, not separate particle diagrams. Uniform
lighting and scale.

## Selected padding revision prompt

Edit this four-stamp water-splash atlas for safe game sprite packing. Keep the
textured torn water-sheet style and 2x2 layout. Make each of the FOUR splash
clusters considerably smaller within its quarter: each occupies at most 65% of its
cell width and height, with at least 15% fully transparent padding on EVERY side
of EACH cell. No strand, fleck, or alpha may touch an outer image edge or cross the
central vertical/horizontal cell boundaries. Each splash must be fully complete,
isolated and centered in its quarter. Keep 4 different silhouettes, left-to-right
jet direction, actual transparent background, no labels, no grid. Slightly softer
fine spray edges. No round beads.

## Shared placement

Three airborne stamps per contact select stable variants and small orientation
variations from the contact slot and packet index. Their positions retain the
existing fixed-step ballistic trajectories; orientations do not flip on descent.
One lower-opacity expanding stamp supplies water-plane residue. Heavy-hit sparks
remain separate. No geometric droplets or radial splash flash are drawn.
