# Title breakwater surf

`TitleRockWaves` clips the bottom of the painted title rockwall against a
traveling water edge. The underlying title water remains visible below that
edge; no opaque water-colored rectangle is painted over the scene. Only the
rock sprite participates in this clip, so the industrial background and later
vessels keep their existing depth order.

The edge rises and falls on a four-second cycle driven by `visualTicks`. Its
three wavelengths per 960-unit rock repeat join continuously at the scrolling
seam. Two triangles per 40-unit span exclude the submerged rock toes. The clip
is restored before drawing foam or vessels.

Existing collision-splash artwork supplies compressed contact foam and a few
larger spray stamps. Spray starts while the crest rises, follows a short upward
arc, then fades back toward the moving water edge. There are no circular spray
particles, new atlases, extra raster caches, or native timing rules. Each repeat
has at most 24 contact stamps and 12 spray candidates; offscreen patches are culled.

The registered harbor opening at 68.2–83.6% of the rock panorama stays free of foam.
Green left/red right navigation lamps and the original painting are unchanged.
This title-only surf does not restore the removed animated shoreline bands in
playable levels.

Review with `tools/TitleRockWaveReview.java`: actual title frames and a six-phase
fixed-wall contact sheet export to `out/title-rock-waves`. `TitleRockWavesTest`
checks repeat continuity, triangle winding, bounded work, opening clearance,
clip lifetime and render purity. Native visual/performance review remains part
of the parent integration round.
