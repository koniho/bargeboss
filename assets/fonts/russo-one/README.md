# Barge Display heading face

Title-inspired UI display geometry derived from Russo One (Jovanny Lemonad), SIL
OFL, downloaded unchanged from google/fonts/ofl/russoone on2026-11-21. Original
TTF and OFL.txt retained. The derived in-game face is named **Barge Display**;
it is an approximation of the painted title's heavy/slanted character, not a
claim that the generated logo used this font.

Export: cap-normalized outlines,22% forward shear,88% horizontal scale, uppercase
forms, proportional tracking. Shared Java geometry and existing text-layer cache
render it identically across hosts; no runtime font installation/host text API.

```
java -Djava.awt.headless=true -cp build/font-tool GenerateUiFont assets/fonts/russo-one/RussoOne-Regular.ttf core/com/bargeboss/game/ArcadeGlyphs.java ArcadeGlyphs 'Barge Display, derived from Russo One (SIL OFL)' .22 .88 false
```
