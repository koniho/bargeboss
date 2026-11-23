# Spotlight receiver derivatives

These are deterministic albedo extracts, not newly generated artwork.
Run tools/SpotlightReceivers.java against the approved source atlases listed in
SpotlightArt.SOURCES. Crop each registered cell using SpriteAtlas source bounds,
retain its full transparent margins, and downsample proportionally with bicubic
filtering to at most 256 pixels per edge. Preserve original alpha and colors.
Output: assets/sprites/frames/spotlight-<source-atlas>/<frame>.png.

Runtime aliases inherit source pivots and scale corrections exactly. They draw
with bounded cone-dependent opacity over the dusk source at the same depth.
For animated trees, use the identical affine sway transform. Do not regenerate
receiver images per frame or change the underlying accepted scenery art.
