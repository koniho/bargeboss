# Guide-based tug refinement

Latest: **v4 registered candidate**, not yet integrated into gameplay.

- [Transparent 32-frame sheet](tug-isometric-v4-32.png)
- [Corrected geometry guide](tug-isometric-v4-pose-guide.png)
- [Registration estimates](tug-isometric-v4-registration.csv)
- [Exact prompt](../art-prompts/isometric-tug-v4-guide.txt)
- [Experiment learnings](sprite-sheet-learnings.md)

Built-in `image_gen` used the corrected guide as its edit target and v3 as a
material reference. Measured output is **1774×887**, 8×4 cells; the requested
3072×1536 was not delivered. Source pixels have not been programmatically edited.

The guide establishes 32 intended 11.25° headings under one 35.264° camera. The
generated turn follows this sequence much more closely than v2. Bow knees stay
at the bow, the cabin stays forward, and the aft winch no longer disappears in
the side/quarter views where the earlier guide had hidden it behind the deck.
The guide now uses per-pixel depth and checks known unobstructed winch tops.

Read-only registration uses one uniform scale (0.9671) and small per-frame
translations. Mean silhouette intersection-over-union is 0.878 raw and 0.929
registered; minimum registered overlap is 0.909. These measure mask agreement,
not physical accuracy or exact camera yaw. An identity check against the same
guide returned overlap 1, scale 1 and unchanged pivots for all 32 frames.

Tires, roof fittings and winch details still vary slightly between painted views.
Guide-derived pivots are proposed metadata; per-frame smoke/tow/contact anchors
are not yet authored. This is a better review candidate, not a certified rigid
3D model or a completed camera conversion. The live game remains unchanged.

Run `./preview-tug-concept.sh` for the latest self-contained review at
`out/isometric-tug/review.html`, registered contact sheet, GIF turntable and
alpha/crop measurements. The review supports raw/registered/guide overlay modes,
light/dark/water backgrounds, three display scales and individual frame stepping.
No blending or flat image rotation conceals pose changes.

The geometry guide and registration are reproducible:

```sh
mkdir -p build/tug-concept
javac -Xlint:-options --release 8 -d build/tug-concept tools/TugPoseGuide.java tools/VesselRegistration.java
java -Djava.awt.headless=true -cp build/tug-concept com.bargeboss.game.TugPoseGuide
java -Djava.awt.headless=true -cp build/tug-concept com.bargeboss.game.VesselRegistration docs/art-studies/tug-isometric-v4-pose-guide.png docs/art-studies/tug-isometric-v4-32.png docs/art-studies/tug-isometric-v4-registration.csv
```

Earlier v2/v3 candidates, prompts, guides and v3 registration remain as evidence.
No native antialiasing or device behavior is certified by this isolated tool.
