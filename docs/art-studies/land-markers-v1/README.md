# Fixed tutorial channel beacons v1

Accepted runtime sources: [red](../../../assets/sprites/land-marker-red-v1.png)
and [green](../../../assets/sprites/land-marker-green-v1.png), generated with the
built-in image tool and copied unchanged. The floating red buoy was the style
reference; the accepted red land variant was the green edit target. Exact
[prompts](../../art-prompts/land-markers-v1.md) are tracked separately.

Both sources are 1254 × 1254 RGBA. At alpha >=32, red bounds are
`278,33–974,1236`; green bounds are `277,30–976,1236`. Neither visible silhouette
touches an image edge. The requested 10% vertical margins were not achieved:
the narrowest visible margin is 17px. Red has 28 negligible border samples at
alpha 1/255; green borders are fully transparent. No source pixels were edited
or thresholded. The shadow pipeline has its own padded cells.

Each tower is bolted to a square concrete plinth, with no floating drum, water,
cast shadow, smoke or glow baked into the color sprite. The camera and broad
materials match the buoy family at gameplay scale. The square is an image-model
approximation of the requested isometric projection, not measured 3-D geometry;
this is a freestanding prop, with no modular tile connections.

## Registration and effects

Both frames use the same measured source registration:

| Item | Source pixels | Runtime / interpretation |
| --- | --- | --- |
| Ground pivot | 627,1103 | Estimated center of bottom foundation footprint, not its front corner |
| `lantern` | 626,315 | Pixel-verified neutral lens center; red or green navigation flash |
| Source canvas size | 1254 square | 36 world-unit billboard size |
| Foundation | conservative 18 × 18 world units | Entire footprint must be dry |
| Receiving plane | raised quay | `GroundArt.LAND_RISE`, currently 18 |

The lamp's approximate local XYZ is (-0.0203,-0.0203,22.62) relative to the
foundation pivot, with Z up. Those coordinates are inferred from the fixed-view
registration, not a reconstructed model. Its orientation reference normal is
(0,0,1); the enclosed lens emits omnidirectionally, without a directional cone.
The source UV is `(626/1254,315/1254)` and the halo radius is 0.15 source sizes.
The glass stays visible during the dark portion of the flash. The runtime sprite
and lamp share a rigid fixed transform; neither samples waves or building fades.
Smoke/steam/exhaust anchors: `[]`. Tire anchors: `[]`. Solar panel and roof
lifting eye are not emitters.

Independent masks are in `shadow-land-marker-v1.png`: red/green near-contact,
then red/green soft-cast. The existing deterministic alpha pipeline resamples
each source to 96px inside a 128px cell with 16px gutters, sigma 1.0/3.2 blur.
Shadow pivots are `(16+96*pivot)/128` and source size scales by `128/96`.
They represent source silhouettes, not 3-D occlusion. A water cutout projected
at deck elevation keeps the flattened shadows and diffuse lamp spill on land.
Ground effects draw after cached terrain and before depth-sorted objects.

## Placement and review

Only the tutorial's four entrance pairs use these land variants. Canal markers
sit 18 units behind the banks and 26 units inside the canal ends. Narrow train
channel markers sit 18 units behind its banks and 26 units inside each end.
Their complete conservative foundation footprints are tested for dry ground.
The two yellow orbit targets still float; regular shift entrance markers still
use the floating red/green sprites. Gameplay water geometry is unchanged.

`LandMarkerReview` checks source/lamp registration, transparent gutters,
stationary transforms and shadow placement, and exports `out/land-marker-review.png`.
`NavigationReview` exports the actual canal and narrow-channel entrance scenes.
These reviews show both foundations seated on the raised quay, clear of the
navigation opening. Contact/cast masks remain below structures; beacons join
the ordinary ground-depth sort and never fade like buildings.
