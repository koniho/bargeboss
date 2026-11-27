# Scenery review sources

The canonical painted sources are `woodland-v1/source.png` and
`ground-v1/source.png`. Runtime frame packs live in `assets/sprites/frames/`;
rebuild them from the repository root with Java 17:

```
java -Djava.awt.headless=true tools/PackIslandWoodland.java
java -Djava.awt.headless=true tools/PackIslandGround.java
```

Prompts and registration rationale: `docs/art-prompts/islands-scenery-v1.md`.

`tools/SceneryAestheticsReview.java` exports the actual shared renderer at the
four matched Montrose/island study cameras. These cameras also exist in Android
profile builds. Output images and profile CSVs belong under ignored `out/`.

Acceptance checks: `IslandSceneryTest` covers manifest isolation and every lift
travel tick/reversal; `IslandShorePixelTest` compares production hull pixels with
independently rendered painted ground alpha, and enforces the 3 MiB source-pixel
budget. `./check.sh` runs both. Native performance evidence is recorded in
`optimization.md` after matched phone captures.
