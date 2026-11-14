# Concept-faithful tug revision

The user rejected v4's squat, boxy design. This revision restores the concept's
rounded dark hull, prominent tires, red/cream lower deckhouse, raised white
pilothouse, orange roof, mast, stack and railings. It remains an isolated review
candidate; gameplay art is unchanged.

- [32-frame source](tug-isometric-v5-32.png)
- [Single-vessel design reference](tug-isometric-v5-design.png)
- [Geometry guide](tug-isometric-v5-pose-guide.png)
- [Registration](tug-isometric-v5-registration.csv)
- [Effect-point metadata](tug-isometric-v5-anchors.json)
- [Authored guide points](tug-isometric-v5-guide-anchors.csv)
- Prompts: [design](../art-prompts/isometric-tug-v5-design.txt),
  [32-frame sheet](../art-prompts/isometric-tug-v5-32.txt)

Both art images use built-in image generation. The original industrial concept
provided the design reference. For the sheet, the 32-pose guide was the geometry
target, the single-vessel rendering supplied materials, and the original concept
reinforced vessel identity. Source pixels have not been edited by scripts.
The standalone rendering contains glow and is a design reference, not an accepted
runtime cutout. The sheet measures 1774×887 (requested 3072×1536).

The guide has 32 exact intended yaw steps; generated views follow the ordered
turn but are not proven geometrically exact. One-scale registration yields mean
silhouette overlap 0.854, minimum 0.809. Some painted fittings and proportions
still vary. Do not confuse this geometric score with fidelity to the concept.

## Effects

Five authored lights, one stack outlet and ten tire fenders have stable IDs in
every frame, with source-cell coordinates, local XYZ, radii, normals and guide
visibility estimates. These are **guide-projected**, not hand-verified points.
The generated image adds small cabin/roof fixtures beyond the guide; inspect
and author those or revise the art before marking the inventory complete.
Do not use a high hull overlap score to certify emitter locations.

Run `./preview-tug-concept.sh` for the latest turntable and contact sheet. Its
HTML review has an **Effect anchors** selector for lights, exhaust outlets,
tires or all categories, plus raw/registered/guide modes and background controls.
Dashed circles indicate predicted occlusion, not visibility measured from the
generated image. The data schema retains that distinction.

To regenerate guide/metadata:

```sh
javac -Xlint:-options --release 8 -d build/tug-concept tools/TugPoseGuide.java tools/VesselRegistration.java
java -Djava.awt.headless=true -cp build/tug-concept com.bargeboss.game.TugPoseGuide --concept
java -Djava.awt.headless=true -cp build/tug-concept com.bargeboss.game.VesselRegistration docs/art-studies/tug-isometric-v5-pose-guide.png docs/art-studies/tug-isometric-v5-32.png docs/art-studies/tug-isometric-v5-registration.csv
python tools/project-vessel-anchors.py docs/art-studies/tug-isometric-v5-guide-anchors.csv docs/art-studies/tug-isometric-v5-registration.csv docs/art-studies/tug-isometric-v5-32.png docs/art-studies/tug-isometric-v5-anchors.json
```

Downloads copies use `BargeBoss-tug-v5-design.png`, `BargeBoss-tug-v5-32-frames.png`,
`BargeBoss-tug-v5-review.png`, `BargeBoss-tug-v5-turntable.gif` and
`BargeBoss-tug-v5-review.html`. Prompts and canonical source images remain here.

## First runtime integration

The user approved the v5 design in the interactive review. The unchanged sheet
is now copied to `assets/sprites/tug-isometric-v5-32.png`. Runtime addressing is
one 8×4 atlas, with the existing 32 guide-fitted waterline pivots and one uniform
0.8957603 scale. The guide's hull spans .78 of a cell; runtime hull size uses
that fraction and square draw dimensions, without per-heading deformation.

`tools/export-tug-runtime.py` exports registration and the separate
`tug-isometric-v5-stack-anchors.csv` to shared `TugArt.java`. The main stack outlet
was visually calibrated against the painted rim in the production image for 31
headings. Frame 10 does not expose a reliable stack rim: its outlet is interpolated
between neighboring registered views and explicitly marked `interpolated-occluded`.
This is an artwork limitation, not proof that the source model contains a
geometrically correct hidden stack. Full light and fender inventories remain
tracked in the guide JSON and unverified for runtime effects; do not relabel all
512 guide points as pixel-reviewed because the main exhaust was calibrated.

`tools/TugRuntimePreview.java` renders the actual desktop atlas and overlays
runtime stack points (mint = visually calibrated; pink = estimated) and the fixed
waterline anchor. Review output: `out/isometric-tug/runtime-stack-anchors.png`.
The smoke transform uses the same heading offset, blend weights, pivots and
uniform scale as the rendered hull; `TugAtlasTest` checks that contract through
negative angles, intermediate blends and turn wraparound. Source PNG pixels are
unchanged. This integration did not run image generation or retouch the sheet.
