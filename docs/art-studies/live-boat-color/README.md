# Live boat color comparison — 2026-11-23

The user wants live 3-D boats to match the painted 2-D fleet, especially in color.
Compare [before](comparison-before-v1.png) and [after](comparison-after-v1.png).
`tools/BoatColorReview.java` draws the actual shared models beside accepted tug
and yacht sprites, with nominal hull length normalized for inspection, matching
headings and day/dusk profiles. No additional nearby lamp is supplied to the
models. These are Java2D reference renders, not Android screenshots. Their fine
internal triangle seams are reference antialiasing artifacts; inspect native
output before diagnosing them as hull details.

## Findings and this increment

- Live boats inherited scenery's dusk RGB gain (92,108,138), giving white paint
  a pronounced blue cast. Painted tug/yacht pixels do not use that scenery grade;
  their separate directional darkness masks retain more source color.
- Most model surfaces also used similar blue-gray albedos. Replaced these with
  ivory/cream painted cabins and varied pale hulls, warm tan decks/cockpits and
  navy glass. Canvas retains the existing eight-color livery palette.
- Added a vessel-only ambient grade (dusk 202,204,210), while retaining geometry
  normals, soft hull shading and colored point-light reception. Other scenery,
  buoys and swimmers retain their existing grade. This intentionally matches
  the readable painted fleet, not physically exposure-correct night photography.
- Small active rescue/haulout cruisers now share the live marina model at close
  range and its distant sprite LOD. Navigation/anchor-light state, tow gameplay,
  heave and shoreline clips remain in the existing shared path. No new bitmap
  or mesh family is loaded. The hull stays at the existing world-waterline datum.

## Remaining rendering/art gaps

Color matching is not full style parity. Flat cabin surfaces still lack authored
ambient occlusion around roof overhangs, textured paint/wood, window reflections
and small bevel highlights present in the painted sprites. Prioritize local
material/occlusion data and better fitted forms before any global blur filter.
Keep these details in object space so they do not slide while turning. Preserve
nearby-light reception; do not bake glowing lamps into an unlit color texture.
The hull already has smooth normals; shading every BSP vertex proved costly in
the preceding experiment, so retain bounded lighting work and native profiling.

The 80-foot yacht is a separate vessel from the small rescue/haulout cruiser.
Its accepted sprite remains. `tools/YachtPoseGuide.java` can supply starting
geometry, but is explicitly a blockout. A live yacht must preserve its two-level
cabin, cream paint, blue window band, teak areas, fittings and navigation/tow
anchors. Do not stretch a marina cruiser or promote the pose guide as final art.

## Reproduce

Compile core, tests and desktop sources plus `tools/BoatColorReview.java`, then:

```
java -Djava.awt.headless=true -cp build/check com.bargeboss.game.BoatColorReview boat-color-review
```

The output is `out/boat-color-review.png`. Run `./check.sh` and the Android
profile workflow for changes; native timing and limitations are in optimization.md.
