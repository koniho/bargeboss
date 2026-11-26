# Player captains (Brittney, Weslie, Dot), v1

Selectable player characters for the captain intro, the title shortcut and the always-visible
top-left HUD portrait.

## Production path

No image-generation tool was available in the authoring session, so the three sources are painted
procedurally in Java2D by `tools/PaintPlayerPortraits.java` (deterministic shapes, no tracing, no
photo pixels). They follow the radio-portrait contract used by Gavin, Chas and Alex:

- right-facing three-quarter head-and-shoulders bust, transparent backdrop, no props or text;
- bold dark ink contours, warm cream/gold key light on the front planes, cool form shadow with fine
  diagonal hatching on the back planes, warm orange rim on back edges;
- six frames with an identical head/shoulder silhouette: 0 neutral, 1 small speech, 2 open speech,
  3 blink, 4 mad (angled brows, gritted teeth, flush, anger mark), 5 wince (squeezed eyes, clenched
  grimace, sweat drop). Brittney's lenses hide her eyes, so her blink frame is a travelling glint.

Canonical alpha sources (1536x1024, 3x2, 512 px cells): `docs/art-studies/captains/<name>-source-v1.png`.
They are packed by the existing `tools/PackGavinPortrait.java` (one uniform scale for all six
frames, shoulder-base registration at y 183) into the 576x384 runtime sheets
`assets/sprites/portrait-captain-<name>-v1.png`. `PortraitRegistrationReview` measures all 18
frames at base (95.5, 182); `PortraitRegistration` records them.

Rebuild:

```sh
java -cp build/check com.bargeboss.game.PaintPlayerPortraits docs/art-studies/captains
for n in brittney weslie dot; do
  java -cp build/check com.bargeboss.game.PackGavinPortrait \
    docs/art-studies/captains/$n-source-v1.png assets/sprites/portrait-captain-$n-v1.png
done
```

## Likeness notes

User-supplied reference photos for Brittney and Weslie were viewed locally only; they are not in the
repository and were not sent anywhere. The portraits are stylized, not traced.

- **Brittney:** dark hair pulled back into a high top-knot bun, big square dark sunglasses with a
  gold top rim, warm wide toothy smile, olive-green utility jacket (stand collar, zip, chest pocket)
  open over a black crew tee, tan crossbody strap.
- **Weslie:** short swept light-brown hair with grey highlights, tanned skin, broad confident grin,
  crow's feet, white open-collar shirt under a charcoal notched-lapel blazer, gold pocket square.
  The frontal reference was extrapolated to the shared three-quarter view.
- **Dot (original):** retired lunch lady in her seventies, deep brown skin, white curls under a red
  rib-knit beanie with pompom, gold hoop earring, stocky sloping shoulders, bright yellow rain
  slicker with snaps, teal knit scarf. Distinct in age, build, hair and palette from the other two.

## Review

`tools/PlayerPortraitReview.java` renders the real shared renderer: `out/captain-picker-*.png`,
`out/captain-title-*.png`, `out/captain-tutorial-*.png` and every HUD state
(`out/captain-hud-{idle,blink,talk,wince,mad}-<captain>-<width>.png`) at 960x600 and 1243x600.
