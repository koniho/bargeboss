# Registered watercraft glow effects

These are procedural effect textures, derived from existing artwork rather than
new painted vessels. `tools/GenerateVesselGlow.java` reads
`tug-isometric-v5-32.png` and `barge-isometric-v3-32.png` and exports:

- `tug-glow-gold-32.png`
- `barge-glow-gold-32.png`
- `barge-glow-mint-32.png`

Each 1536×768 atlas has 32 directional cells. A 192px cell contains the source
alpha resampled into a 144px square plus 24px padding on each side. Pivots are
mapped through that exact padding transform; draw scale compensates by 192/144.
The original 32-view blending, heave and camera transform apply unchanged.

Source alpha below 80 is excluded to prevent soft painted shadows glowing.
A dilated mask produces the bright rim; separable Gaussian filters produce a
small halo and broad bloom. A faint interior tint keeps the hull luminous while
preserving its painted detail. Gold uses a saturated amber halo and cream edge;
mint uses a green halo and pale mint edge. Each frame is processed independently,
and generation asserts transparent cell boundaries to prevent neighboring-view
bleed. No wakes, smoke, lamps or effects are included in the source mask.

The shared renderer draws glow with the vessel, before nearer scenery and UI.
All watercraft, including title and NPC craft, get a quiet warm rim; active tug
control/tutorial targets pulse more strongly over 90 simulation ticks. Lasso-ready
barges use mint, and successful attachment removes the readiness pulse immediately.
Glow sprites never count as opaque hull pixels for scenery fading.

All hosts decode the same images through the existing source-image cache. There
is no runtime blur or per-frame bitmap allocation. The three RGBA atlases add
13.5 MiB decoded source memory. `preview.sh` regenerates in memory and verifies
all 96 stored frames against current source artwork. To rebuild after changing
hull art, compile `tools/GenerateVesselGlow.java` with the shared core and run
`com.bargeboss.game.GenerateVesselGlow` without `--check`.
