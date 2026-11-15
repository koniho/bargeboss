# Buoy sprite registration v1

The three accepted 1254×1254 RGBA source images are copied unchanged into the
production assets directory. No resizing, cropping, recoloring, painted foam,
wake, glow, or shadow is baked into these color sprites. Transparent bounds
below use alpha ≥32 and inclusive coordinates.

| Variant / frame | Source bounds | Waterline pivot px | Lantern lens center px |
| --- | --- | --- | --- |
| Red / 0 | 274,25 – 964,1198 | 619,1109 | 627,270 |
| Green / 1 | 272,22 – 968,1198 | 620,1109 | 627,270 |
| Yellow / 2 | 273,22 – 965,1198 | 619,1109 | 627,270 |

`BuoyArt` stores these coordinates normalized by1254. Each sprite has one stable
light anchor, `lantern`, on the neutral glass lens; color comes from the marker's
role. Smoke anchors: `[]`. Tire anchors: `[]`. The solar panel, daymark,
mooring rings and lifting eye are not emitters. The nameplate center is
normalized `(0.5,0.783)` for the optional practice number.

A square source size48 represents a practice buoy (~26.5 world-unit base width),
28 a harbor/canal entrance buoy (~15.4), and16 a narrow-channel mark (~8.8).
Buoys are visual navigational marks; their art does not add collision obstacles.
All three variants share one view and the same motion, rather than watercraft
heading turntables. The user explicitly requested sprite rotation for sway.

The registered waterline pivot follows delayed wave heave. Across-wave slope
and a small ambient term determine bounded ±0.105-radian (~6°) roll. Nearby
vessel wakes amplify the sampled water field. Render queries do not change
simulation, use random numbers, or sample wall-clock time. Lantern position is
transformed by the exact same rotation as the source image.

The water surface excludes all sprite pixels below a curved front meniscus at
surface elevation minus0.022 source sizes. This places the lower float visibly
underwater. The curve follows the cylindrical float's water contact ellipse,
with a small traveling surface undulation; it is independent of sprite roll.
Heave samples the water ten ticks earlier at1.1 amplitude, making the float lag
the waves. The water never tilts with the sprite. Soft teal wet-contact strokes
and five translucent animated patches from the existing foam atlas explain the
moving edge without an opaque band or sharp rectangular water overlay. Two faint,
broken front crests spread outward and fade. Foam maximum opacity is0.6; crest
maximum opacity is0.18. No new particles or persistent simulation state are added.
The ground-depth cutout separately prevents shore pixels being overwritten;
only buildings follow the building-fade rules.

`shadow-buoy-v1.png` is a separate six-cell128px atlas: red/green/yellow contact
masks followed by red/green/yellow cast masks. Each unchanged source alpha is
resampled to96px with16px gutters, thresholded and feathered using the existing
sigma1.0/3.2 generator. Pivots compensate exactly for padding. Both shadows roll
with the sprite then flatten onto the receiving water plane, remain below hulls,
and are clipped off dry land. No painted shadows are added to the color PNGs.

Validation: `BuoyArtTest` covers deterministic wave motion, wake response,
registered lamp transforms, stationary pivots, clip balancing and unchanged
simulation state. `BuoySpriteReview` checks source dimensions and opaque lantern
anchors, then checks actual rendered pixels below the curved waterline at −6°,0°,+6°
for all three variants. `ShadowActivityTest` includes every padded buoy mask;
`GenerateShadowMasks --check` compares decoded masks against source alpha.

Reviews: `out/buoy-sprite-review.png` shows all three variants at extreme and
neutral rolls, with glow and unlit glass. `BuoyReview` exports the actual opening
exercise and both completed orbits. `NavigationReview` exports red/green harbor,
canal and narrow-channel mouth views. Native/device visual checks are separate
from desktop pixel and shared command tests.
