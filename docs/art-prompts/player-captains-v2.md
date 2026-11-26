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

- **Whitney:** dark brown top-knot bun with loose strands, big dark square sunglasses with gold
  trim, wide grin, olive hooded utility jacket over a black tee, orange-tan crossbody strap. Her
  current sheet (supplied 2026-11-26, replacing the first one) draws her facing left; it is
  mirrored on extraction so she faces right like Wesley and Dot. Her eyes show faintly through
  the lenses, so blink, mad and wince change only the lens interior and the skin above.
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
pills, banners and effect marks. `--mirror` flips the sheet left-right first (pick points stay in
sheet coordinates). Each frame then gets one uniform scale, a small rotation and an offset that
best match frame zero's softened ink lines (coarse registration).

The rows of a sheet are drawn at different head sizes and angles (mad and wince up to 5% larger
and a degree or two of tilt), so coarse registration alone left the head swimming by 1-4 px at
192 px whenever the HUD swapped frames. With `--captain NAME` the tool therefore rebuilds every
expression frame on top of the neutral frame (`tools/portrait_registration.py`):

1. The frame is registered to neutral by masked affine ECC over a pyramid, on the static head
   only (hair, headwear, ears, glasses, jacket; eyes, brows, mouth and the copied base band
   masked out).
2. Each face feature (an eyes/brows ellipse and a mouth/jaw ellipse, in fractions of the neutral
   bounding box, per captain in `CAPTAINS`) is registered on its own anchors: the neutral
   glasses (an exhaustive rotation/scale search with masked edge template matching, then ECC,
   keeping whichever matches the rims better) or nearby static areas such as the nose, cheek
   and ear. The feature is warped onto the neutral face and blended in with a feathered mask.
   Eye features stay inside the neutral silhouette; only the mouth may reshape the jaw line.
3. The neutral glasses are laid back on top: Dot's whole wire frame (her expressions' own rims
   are inpainted away first, since mad and wince draw them at another tilt), and the outer band
   and lens glints of Whitney's sunglasses, so the eyes behind her lenses still change.

Everything outside the feature ellipses is the neutral frame's pixels, so the outline, hair,
glasses, ears, earrings and shoulders are pixel-identical in every frame. The sheet is cut at the
neutral bust floor and laid out on the union footprint, `tools/PackGavinPortrait.java` registers
all six cells identically at base (96, 182) (Wesley 95.5, 182), and `PortraitRegistration`
carries no per-frame offsets for the captains. `PlayerPortrait` keeps its breathing rise running
through every state so an idle-to-talk swap does not drop the bust; the deliberate mad/wince
shake is unchanged.

Rebuild everything with one command (the sheet paths are the user's local copies, never
committed; Java must be on `PATH`):

```sh
tools/rebuild-captain-portraits.sh WHITNEY_SHEET WESLEY_SHEET DOT_SHEET
```

It writes `docs/art-studies/captains/*-source-*.png`, packs `assets/sprites/portrait-captain-*.png`
and prints each sheet's static-region residuals (offset, rotation, scale, RMS displacement and
mean static pixel difference of every frame against neutral; all zero after compositing). The
presets carry the picks (Whitney `--mirror --scale 2.3 --pockets 0.5`, Wesley `--scale 2.3`,
Dot `--scale 2.6`). For a new sheet:

```sh
python3 tools/extract_sheet_portraits.py SHEET --list [--mirror]   # component boxes on the sheet
python3 tools/portrait_registration.py assets/sprites/portrait-captain-dot-v2.png dot  # residuals
python3 tools/captain_portrait_review.py out/captain-registration after  # flip/diff/GIF renders
```

Each `--pick` point lies inside one head, in runtime frame order. A new captain needs a
`CAPTAINS` entry: picks, scale, feature ellipses and anchors, and a glasses box if any.

## Review

`tools/PlayerPortraitReview.java` renders the real shared renderer: `out/captain-picker-*.png`,
`out/captain-title-*.png`, `out/captain-tutorial-*.png` and every HUD state
(`out/captain-hud-{idle,blink,talk,wince,mad}-<captain>-<width>.png`) at 960x600 and 1243x600.
`PortraitRegistrationReview` checks every frame's painted base against `PortraitRegistration`.

## Dark-background edge cleanup (2026-11-26)

The captain-picker screenshot exposed gray/white paper residue around the hair,
jaw and shoulders. `CleanPortraitEdges.java` now runs after packing, before the
registration report. It flood-fills only neutral gray outer pixels connected to
transparent space (a three-pixel band at runtime size), recovers their coverage
against nearby opaque dark ink, and replaces contaminated color with that ink.
Very faint alpha dust and isolated paper fragments are removed. Interior skin,
teeth, glasses highlights and headwear are protected by the ink contour.

All six frames retain their existing 192 px cells, placement and dimensions;
there is no crop, scale, silhouette blur or new pose generation. The committed
source cutouts remain unchanged, so the cleanup is reproducible and reversible.
The tool runs offline; all platforms draw the same cleaned PNGs without additional
runtime work. Review on navy as well as light backgrounds and inspect every
expression, including Dot's pearls/glasses and Whitney's hair strands.
