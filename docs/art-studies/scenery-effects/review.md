# Scenery lighting and smoke: first pass

The built-in image generation tool produced `chimneys-v1.png` from the accepted
isometric practice-yard props and the industrial harbor concept. The prompt is
`docs/art-prompts/isometric-chimneys-v1.txt`. Requested1536×768; actual1774×887,
with two887×887 cells. Source pixels are unchanged in the production copy at
`assets/sprites/chimneys-v1.png`.

Both assets have one clearly open top rim and one amber maintenance lamp. Source
centers were inspected against the generated image and are recorded in
`chimneys-v1-anchors.json` and shared `ChimneyArt`. Ground pivots are inferred
foundation centers; localXYZ values are fixed-view estimates, not a recovered
3D model. The steel stack uses source rim(443,67), lamp(414,424); the brick stack
uses cell-local rim(444,293), lamp(353,663). Runtime offsets use the same source
pivot and uniform scale as the sprites.

`SceneryEffects` generates smoke independently from those two rim anchors.
Unequal particle lobes expand, rise, drift and disperse deterministically from
`Game.visualTicks`. The stack and its plume share a spatial opacity envelope so
smoke does not obscure a vessel or objective that needs to remain readable.
Warehouse and practice-shed pipes remain smoke-free: they have no open exhaust
outlet in their source artwork.

Window and quay-lamp glow uses three low-amplitude temporal frequencies, soft
concentric falloff and three subtle moving lobes. Fixtures use stable phases so
they do not blink in unison. Bank reflections use the same intensity signal.
Every effect is shared Java and rendering does not mutate gameplay.

`SceneryEffectsTest` passed4812 checks for bounded smooth intensity, deterministic
primitives, opacity-zero suppression and source-rim attachment across responsive
canvas sizes. Isolated Java8 compilation passed. Reviewed the actual shared
renderer at ticks90,180,270 and the labeled source-anchor overlay, exported by
`tools/SceneryEffectsPreview.java` under `out/scenery-lighting/`. Native device
sampling and antialiasing remain parent-round validation work.
