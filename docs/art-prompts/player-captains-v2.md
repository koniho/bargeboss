# Player captains (Whitney, Wesley, Dot)

Replaces the earlier procedural Java2D busts (`tools/PaintPlayerPortraits.java`, rejected by the
user as "really not good") and their prompt notes. Whitney and Wesley took their current names on
2026-11-26 (history in `optimization.md`); atlas ids 92-94 and saved captain indices 0/1/2 are
unchanged.

Selectable player characters for the captain intro, the title shortcut and the
always-visible top-left HUD portrait.

## Source art

All three captains come from finished character sheets the user supplied on 2026-11-26, one
per captain, in an ink-and-cel style (heavy warm-black outline, cel-shaded warm skin, painted
hair, three-quarter upward gaze). Each 1536x1024 sheet holds a hero bust, labelled rows
TALK A (4), TALK B (3), BLINK (2), WINCE (3) and ANGRY (4), and a 16-thumbnail strip. The raw
sheets are not committed. The repository keeps the processed alpha sources
`docs/art-studies/captains/{whitney-source-v1,wesley-source-v1,dot-source-v2}.png` (3x2, 512 px
cells) and the runtime sheets `assets/sprites/portrait-captain-{whitney-v1,wesley-v1,dot-v2}.png`
(576x384, 192 px cells).

- **Whitney:** dark brown top-knot bun with loose strands, big dark square sunglasses, wide
  grin, olive hooded utility jacket over a black tee, orange-tan crossbody strap. Her blink
  frame sits behind the lenses and is almost identical to neutral; that is intended.
- **Wesley:** short greying brown hair, blue eyes, big confident grin, navy hooded nautical
  jacket with an orange/red life-vest panel and an anchor patch, sunglasses hung at the collar.
- **Dot:** retired school lunch lady, deep warm brown skin, floral headwrap over grey curls,
  wire-rim glasses, pearl drop earrings, maroon scarf over a teal cardigan.

## Frame mapping

Runtime frames come from the large labelled rows only, never from the thumbnail strip:

| Runtime frame | Sheet frame |
|---|---|
| 0 neutral | TALK A 1 (closed) |
| 1 small speech | TALK A 3 (mid) |
| 2 open speech | TALK B 2 (wide open) |
| 3 blink | BLINK 2 |
| 4 mad | ANGRY 2 |
| 5 wince | WINCE 2 |

The remaining sheet frames (TALK A 2/4, TALK B 1/3, BLINK 1, WINCE 1/3, ANGRY 1/3/4) are
unused for now; `PlayerPortrait` keeps its six-frame contract shared with the HUD and picker.

## Processing

`tools/extract_sheet_portraits.py` keys out the near-white paper by flood-filling it from the
sheet border (the ink outline stops the fill), derives a soft edge alpha and removes the paper
blended into edge pixels, and selects heads by connected component, which drops the caption
pills, banners and effect marks. Rows are drawn at slightly different sizes and poses, so
each frame gets one uniform scale (within 6% of its head-height ratio), a small rotation (up to
10 degrees) and an offset that best match frame zero's softened ink lines by normalized
cross-correlation. No frame is stretched non-uniformly. Every frame then takes frame zero's
bust base (the lowest 44 px, blended over 16 px, plus frame zero's bust under any gap a
rotation exposes) and is cropped to frame zero's footprint, so `tools/PackGavinPortrait.java`
registers all six at base (95.5, 182) and `PortraitRegistration` needs no per-frame offsets.

Rebuild (the sheet paths are the user's local copies):

```sh
python3 tools/extract_sheet_portraits.py SHEET --list      # component boxes on the sheet
python3 tools/extract_sheet_portraits.py dot-sheet.png docs/art-studies/captains/dot-source-v2.png \
  --pick 460,200 820,200 650,470 1420,200 380,750 1240,470
python3 tools/extract_sheet_portraits.py wesley-sheet.png docs/art-studies/captains/wesley-source-v1.png \
  --scale 2.3 --pick 460,240 830,240 660,510 1420,240 370,790 1230,510
python3 tools/extract_sheet_portraits.py whitney-sheet.png docs/art-studies/captains/whitney-source-v1.png --pockets 0.5 \
  --pick 470,230 830,230 660,500 1420,230 360,770 1230,500
for n in whitney-v1 wesley-v1 dot-v2; do
  java -cp build/check com.bargeboss.game.PackGavinPortrait \
    docs/art-studies/captains/${n%-*}-source-${n##*-}.png assets/sprites/portrait-captain-$n.png
done
```

Each `--pick` point lies inside one head, in runtime frame order.

## Review

`tools/PlayerPortraitReview.java` renders the real shared renderer: `out/captain-picker-*.png`,
`out/captain-title-*.png`, `out/captain-tutorial-*.png` and every HUD state
(`out/captain-hud-{idle,blink,talk,wince,mad}-<captain>-<width>.png`) at 960x600 and 1243x600.
`PortraitRegistrationReview` checks every frame's painted base against `PortraitRegistration`.
