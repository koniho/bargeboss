# Scenery grounding audit — November 14

The phone report `Screenshot_20261114-011816.png` shows two separate issues:
decorations are not consistently registered to a common ground grid, and several
painted foundation edges use a shallower view than the actual river camera.
The existing source images were inspected alongside the screenshot and the
static-scenery skill contract. No source pixels were altered for this audit.

## Camera and measured source edges

`RiverCamera` projects the two ground axes at exactly +30 and -30 degrees from
screen horizontal (`DEPTH = 1 / sqrt(3)`). Sprite draw calls use zero bitmap
rotation and uniform width/height, so they preserve the source-image angles.
Moving a pivot or snapping a placement to a tile can fix location, but cannot
correct a baked perspective mismatch.

These measurements use the existing recorded foundation corners. The corners
were inspected in earlier reviews; concealed rear corners remain inferred.
Angles below use only the two visible front edges, not the inferred corner.

| Asset | Left-to-front edge | Front-to-right edge | Assessment |
| --- | ---: | ---: | --- |
| Crate bundle | +23.12° | -27.36° | Shallow first axis; imperfect tile alignment |
| Pipe rack | +20.21° | -31.91° | Strong asymmetric perspective mismatch |
| Tank skid | +22.00° | -27.80° | Both axes shallower than the camera |
| Utility shed | +21.25° | -32.04° | Strong asymmetric perspective mismatch |
| Warehouse | +27.20° | -30.79° | Closest of the existing buildings, still approximate |

Corner sources are
[`tutorial-scenery/isometric-props-v2-anchors.json`](tutorial-scenery/isometric-props-v2-anchors.json)
and [`scenery-trial/warehouse-v1-anchors.json`](scenery-trial/warehouse-v1-anchors.json).
The two chimney foundations also look shallow, roughly 20–24 degrees; this is
a visual estimate, not a surveyed corner set. Upright chimney shafts do not
prove that their ground bases match the camera.

The pipe rack additionally had a registration error independent of its camera:
the recorded four-corner center is (305,451), while the runtime pivot was
(305,439). Its Y pivot is now 451/627 in both Java and the anchor inventory.
That is a 12-source-pixel correction, approximately 2.9 world draw units at its
152-unit image size. Other recorded prop and warehouse pivots lie within one
source pixel of their recorded foundation centers. The pipe correction does
not make the source edges isometric.

## First factory candidates

The first four new factory candidates have the same structural problem despite
their camera prompt. Approximate front-edge angles from source inspection are:
blast furnace +22/-23 degrees, boiler house +20/-21, coking plant +17/-27,
foundry +23.5/-30. The blast furnace touches the top image boundary; the boiler
house has almost no bottom gutter. These are diagnostic estimates, not accepted
calibration metadata. They should not be declared aligned production tiles.

## Geometry guide for regeneration

Use an explicit orthographic footprint and extrusion guide as the geometry
input. Keep the prior art as a material reference only. With local world axes
X/Y and upright Z, project guide points using:

```
pixelX = centerX + pixelsPerWorldUnit * 0.70710678 * (X + Y)
pixelY = centerY + pixelsPerWorldUnit * 0.40824829 * (Y - X)
         - pixelsPerWorldUnit * Z
```

For a 1254-square candidate, one workable study setup is center (627,940),
3.4 pixels per world unit, 64-world-unit tiles, and footprint half-extents of
32 times the tile count. A 2×2 foundation then has visible front corners near
(319,940), (627,1118), and (935,940), with its rear corner near (627,762).
A maximum box height of 175 world units leaves roughly 167 pixels of top
padding even at the rear corner. Smaller buildings can use authored 100- and
60-unit height envelopes, rather than filling every image to the same height.

The generator must preserve the guide's parallel edges, footprint proportions,
verticals and padding. Inspect and measure the returned foundation before
integration. Do not shear, stretch or bitmap-rotate a building to conceal a
camera mismatch. A common 64-unit placement grid, setbacks and fixed ground
pivots complement calibrated artwork; they do not substitute for it.

## Guided furnace result

The guided `factory-v1/blast-furnace-candidate-v2.png` substantially corrects the
first candidate. Actual dimensions are 1254 square. Its alpha≥128 bounds are
(241,158) through (1013,1195), inclusive, leaving left/top/right/bottom gutters
of 241/158/240/58 pixels. A least-squares fit to the bottom silhouette over
X=280..580 and X=680..970 gives **+29.701° and -29.835°**, with the two fitted
edges meeting near (626.3,1196.3). These ranges were visually checked to follow
the two unobstructed concrete foundation edges.

A ground pivot near (627,975.5) and a uniform full-image draw size near 293 world
units fit that returned foundation inside its reserved 128×128 logical footprint.
The visible top lies about 191 world draw units above the projected footprint
center. This is a conservative screen-height measurement, not recovered local Z:
a chimney's ground position within the footprint also affects its projected top.
Keep that measurement separate from the guide's authored 175-unit height.
The result needs no source warp. This validates this furnace's measured edges,
not all other assets generated from the same prompt.

The read-only `tools/SceneryFoundationAudit.java` reproduces the edge/gutter check:

```
javac -d out tools/SceneryFoundationAudit.java
java -cp out com.bargeboss.game.SceneryFoundationAudit \
  docs/art-studies/factory-v1/blast-furnace-candidate-v2.png 280 580 680 970
```

Select edge ranges separately for each source image. Roof overhangs, rounded
feet, shadows and disconnected pixels can invalidate an automatic silhouette
fit, so its printed angle is evidence only after a source inspection.

## Freestanding registration tolerance

The accepted review tolerance for these freestanding modules is at most 1.5
degrees of visible foundation-edge error on either ground axis, with residuals
recorded in [`factory-v1/grounding-audit.json`](factory-v1/grounding-audit.json).
That tolerance does not apply to adjoining quay seams: their shared rendered
boundaries remain exact geometry.

A generated foundation may occupy less than its reserved 1×2 or 2×1 tile pad.
Inverse-project its measured corner vectors through the actual camera, then
choose one uniform scale that fits all corners inside the reserved X/Y bounds.
Preserve its aspect ratio and keep its inferred ground center fixed. The unused
part of the common grid-aligned apron is intentional; do not claim an exact
rectangular fill or seamless building connection. The audit JSON records the
observed foundation separately from reserved tile occupancy.

## Final sixteen-module review

Every production source now has independently inspected foundation ranges in
[`factory-v1/foundation-review-ranges.csv`](factory-v1/foundation-review-ranges.csv).
The JSON records the final accepted source versions plus superseded candidates;
boiler house, four rectangular modules and gatehouse needed another guided render.
All sixteen final sources meet the freestanding 1.5-degree limit. Some retain
roughly one degree of residual, which is recorded rather than treated as a
seamless-tile guarantee.

`tools/FactoryArtPixelTest.java` reads production PNGs with ImageIO and fits their
actual source silhouettes again. It checks those axes, ground-center registration,
uniform inverse-projected footprint fit, opaque-content gutters, unchanged source
bytes and light/smoke anchors near source content. Warm light points also require
nearby warm luminous pixels. Smoke's semantic attachment to a chimney opening
still requires the labeled visual review; opaque pixels alone do not establish it.
Isolated image-border pixels at alpha 1/255 are retained as generation noise;
the test rejects any greater border alpha. No source pixels were masked or warped.

Run the pixel test separately from the pure-Java gameplay suite:

```
javac --release 8 -cp build/check -d build/check tools/FactoryArtPixelTest.java
java -cp build/check com.bargeboss.game.FactoryArtPixelTest
```
