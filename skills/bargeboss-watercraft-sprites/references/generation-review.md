# Generation and review workflow

Use repository tools rather than writing another ad hoc crop/registration tool.
Inspect their current arguments before invoking them; paths below are relative
to a Barge Boss checkout.

## Known evidence

The v1 text-only 16-pose sheet and requested intermediate edit repeated too many
poses. The v2 text-only 32-pose sheet filled all cells but had wrong/uneven
headings and moved fittings. The v3 generation used a deterministic 32-pose
geometry guide as its edit target and v2 only as a material reference. It followed
the guide substantially more closely. This is one observed improvement, not a
guarantee for other vessels. Current measurements and caveats are maintained in
`docs/art-studies/sprite-sheet-learnings.md`.

The v3 blockout itself incorrectly hid a stern winch because its renderer sorted
entire faces by depth. The image generator copied the missing equipment. v4's
guide uses per-pixel depth and restores the winch's visibility. Validate guide
occlusion and equipment before treating a high silhouette score as success.

An example prompt should identify: strict geometry guide; style-only reference;
one rigid hull; camera/elevation; row-major angle convention; waterline datum;
invariant fittings; separate effects; transparent background; unchanged canvas
and gutters. Don't repeat a wrong reference's poses while requesting correct
angles in text. Give geometry priority over dramatic scenery/lighting.

## Reusable tools

Generate the current tug guide (adapt model geometry for another craft):

```sh
mkdir -p build/tug-concept
javac -Xlint:-options --release 8 -d build/tug-concept tools/TugPoseGuide.java tools/VesselRegistration.java
java -Djava.awt.headless=true -cp build/tug-concept com.bargeboss.game.TugPoseGuide
```

`VesselRegistration guide.png candidate.png output.csv` reads two **8×4** atlases,
fits one uniform scale for the whole sheet, then small per-frame translations.
It exports proposed source pivots and silhouette intersection-over-union (IoU).
It never edits the input PNG. Use only when guide and candidate share vessel
geometry, grid and pose order. Fitting an unrelated shape can yield misleading
anchors. Its current search uses a 128×128 mask per cell, alpha threshold 128,
and offsets within ±24 mask pixels; a boundary optimum signals a possible bad
match rather than a validated anchor.

`./preview-tug-concept.sh` exports the current study. The Java review tool can
also accept `source.png output-directory registration.csv guide.png`; use `-`
for registration to inspect the raw prompt anchor. The HTML has raw/registered/
guide overlay modes, light/dark/water backgrounds, scale choices and frame
stepping. The guide overlay is diagnostic, not production art. Keep original
candidate and registration data so corrections are reversible.

`tools/AtlasBounds.java` gives read-only alpha bounds and source crops for other
grid shapes. `./preview.sh --rotation` checks the actual shared gameplay renderer
after integration. These serve different purposes; an isolated preview does not
prove Android/iOS native visual parity.

## Interpreting results

Alpha occupancy and clean crop edges only establish packing. High silhouette IoU
does not verify a winch, stack opening, window, bow identity or exact yaw. A
registered guide match can be excellent while the generated equipment changes.
Check every source pose and the loop seam manually. Preserve honest status:
style candidate, registered candidate, or integrated/validated asset.

View transparent edges on real composites before calling colored preview noise
an alpha defect. Check the measured PNG dimensions before promising resolution.
Do not use width/height stretching, flat bitmap rotation or interpolated duplicate
frames to disguise missing 3D views. Blending belongs after accurate source poses
and waterline anchors have been established.
