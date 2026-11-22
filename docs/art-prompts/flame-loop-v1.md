# Flame loop v1 — 2026-11-22

Generated with the built-in image generation tool using the user screenshot
`/storage/emulated/0/Pictures/Screenshots/Screenshot_20261122-170725.png` as a
**style reference**, not an extraction target. The screenshot shows a bright,
wispy yellow-white campfire with orange edges. No scene pixels are reused.

## Exact generation prompt

Create an original production game sprite animation sheet for Barge Boss. Reference image is style inspiration only: the wispy bright fire in the lower portion of the screenshot. Do not reproduce its scene, masonry, logs, watermark, UI or composition. Exactly 16 sequential flame animation frames in a precise 4 column by 4 row grid, read left to right top to bottom. Square 1024x1024 transparent PNG, each cell exactly 256x256. Every frame is a single upright narrow burning flame plume, same fixed bottom-center anchor at cell coordinate (128,232), generous transparent margins on all edges. Height about 200px, width about 110px. Genuinely transparent background, no painted checkerboard, no labels, no borders. High resolution painted arcade/isometric game effects style, wispy fluid tapering tongues, luminous pale yellow-white interior, hot yellow then orange-red thin translucent edges. Organic curved tongues with fine splitting tips, little transparent gaps inside. Avoid geometric solid triangles, thick outlines, pixel art, smoke, sparks, ground glow, shadows or any solid objects. Continuous looping turbulent flame: base remains planted, tongues stretch upward, curl, separate and vanish then re-form in a coherent sequence, frame 16 blends naturally back to frame 1. Constant overall brightness/scale, variations in flame silhouettes only. Transparent alpha edges suitable for layering several semi-transparent copies over dark water and industrial scenery.

Tool option: `transparent_background: true`.

## Preparation and lessons

- Canonical generated source: `docs/art-studies/flame-v1/generated-sheet.png`.
  The generator returned 1254 square, not the requested 1024: never infer cells
  from prompt dimensions. `tools/PrepareFlameSheet.java` extracts actual quarters.
- Bright base registration scans the bottom-quarter yellow-white core; all source
  bases were within 2 px horizontally and 3 px vertically. A constant .68 scale
  preserves the authored height changes. It places every base at (128,232) in a
  256-square cell with transparent gutters. Runtime is `assets/sprites/flame-loop-v1.png`.
- Registration is essential even for flames: the tongues may move but the fire's
  foot must not hop around. Crossfade adjacent frames, including 15 to 0; do not
  assume a generated sheet is a mathematically seamless fluid simulation.
- Five translucent copies, 60–78% opacity, loop periods 66/73/81/92/107 ticks,
  staggered initial phases. Shared `FlameArt` selects frames and controls opacity.
  Zero runtime texture generation; 16 cropped frames total 4 MiB RGBA, prepared
  by the upcoming tutorial's shared asset manifest.
- No cast/contact shadow masks: flames are non-solid emissive effects. Existing
  timber shadows remain independent. No embedded smoke or ground glow.

## Effect anchor metadata

All anchors are on the existing firewood pile relative to FireworksLesson.X/Y.
Their fixed base height is GroundArt.LAND_RISE; normal is world +Z. Frame base
is normalized (0.5,0.90625). Anchors apply to every frame, with no hidden poses.

| Stable ID | XY offset | Sprite canvas world size | Smoke projected rise above ground |
| --- | --- | --- | --- |
| flame-0 | -18,-10 | 142 | 59.64 × strength |
| flame-1 | 14,-12 | 174 | 73.08 × strength |
| flame-2 | 0,0 | 150 | 63 × strength |
| flame-3 | -16,13 | 124 | 52.08 × strength |
| flame-4 | 16,12 | 154 | 64.68 × strength |

Each has its own smoke outlet, emitting every 16 ticks with a 3-tick stagger.
Puffs inherit their own world position and drift through the bounded VesselSmoke
pool. The existing aggregate FireLight emitter remains at the pile center,
cycling orange/amber/yellow with 210-world-unit receiver radius. Its glow, timber
lighting and water reflection are separate from the sprite. No lamps or machinery.
Heat scales flame height/smoke rise; zero heat stops flames, light and new smoke,
while existing smoke continues drifting. Rendering never advances these states.
