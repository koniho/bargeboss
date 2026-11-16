# Registered watercraft glow effects

These are procedural effect textures, derived from existing artwork rather than
new painted vessels. `tools/GenerateVesselGlow.java` reads
`tug-isometric-v5-32.png`,  `barge-isometric-v3-32.png` and the shared yacht atlas and exports:

- `tug-glow-gold-32.png`
- `barge-glow-gold-32.png`
- `barge-glow-mint-32.png`
- `yacht-glow-gold-32.png`
- `yacht-glow-mint-32.png`

Each 1536×768 atlas has 32 directional cells. A 192px cell contains the source
alpha resampled into a 144px square plus 24px padding on each side. Pivots are
mapped through that exact padding transform; draw scale compensates by 192/144.
The original 32-view blending, heave and camera transform apply unchanged.

Source alpha below 80 is excluded to prevent soft painted shadows glowing.
A dilated mask produces the bright rim; separable Gaussian filters produce a
small halo and broad bloom. A faint interior tint keeps the hull luminous while
preserving its painted detail. Gold stays electric yellow (`#ffee00`); mint stays vivid green (`#22ff72`)
through the halo and core. No cream or white edge. Each frame is processed independently,
and generation asserts transparent cell boundaries to prevent neighboring-view
bleed. No wakes, smoke, lamps or effects are included in the source mask.

The shared renderer draws glow with the vessel, before nearer scenery and UI.
Idle, delivered and unrelated vessels do not receive a guidance rim. Active tug
control/tutorial targets pulse over 90 simulation ticks. The mission yacht remains
a goal highlight. Physical lamps and surface lighting are independent. Lasso-ready
barges use mint, and successful attachment removes the readiness pulse immediately.
Glow sprites never count as opaque hull pixels for scenery fading.

All hosts decode the same images through the existing source-image cache. There
is no runtime blur or per-frame bitmap allocation. The five RGBA atlases add
22.5 MiB decoded source memory. `preview.sh` regenerates in memory and verifies
all 160 stored frames against current source artwork. To rebuild after changing
hull art, compile `tools/GenerateVesselGlow.java` with the shared core and run
`com.bargeboss.game.GenerateVesselGlow` without `--check`.
