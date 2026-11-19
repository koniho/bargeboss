# Pilot Glass UI font

Rajdhani SemiBold by Indian Type Foundry, distributed under the SIL Open Font
License in OFL.txt. Source: https://github.com/google/fonts/tree/main/ofl/rajdhani
Downloaded 2026-11-19. The original font is included unchanged for reproducibility.

Runtime UI uses uppercase shared Java glyph geometry, not platform text APIs or
native font installation. Size means cap height. Proportional advances include
0.055 cap-height tracking. Existing text caches retain rendered strings; opacity
and position do not cause new glyph rasters. The original title logo remains in
LogoLettering.

Regenerate from the repository root:

```sh
mkdir -p build/font-tool
javac -d build/font-tool tools/GenerateUiFont.java
java -Djava.awt.headless=true -cp build/font-tool GenerateUiFont assets/fonts/rajdhani/Rajdhani-SemiBold.ttf core/com/bargeboss/game/TechnicalGlyphs.java
```

The exporter flattens outlines and partitions them into disjoint trapezoids,
preserving counters and punctuation with the existing cross-platform Painter.
No font parsing, outline extraction or host-dependent shaping occurs at runtime.
Use Lettering.width for all fitting and alignment; do not assume a fixed character
width. Add new supported characters to the exporter if copy requires them.
