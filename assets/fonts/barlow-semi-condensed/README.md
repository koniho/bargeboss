# Readable body face

Barlow Semi Condensed Medium by Jeremy Tribby, SIL OFL. Original TTF and OFL.txt
from google/fonts/ofl/barlowsemicondensed, downloaded2026-11-21. Upright medium
weight, proportional width and open counters complement the arcade headings.
Uppercase/lowercase, digits and supported UI punctuation are exported unchanged.
Current copy retains its authored case; mixed case is available for future copy.

```
java -Djava.awt.headless=true -cp build/font-tool GenerateUiFont assets/fonts/barlow-semi-condensed/BarlowSemiCondensed-Medium.ttf core/com/bargeboss/game/BodyGlyphs.java BodyGlyphs 'Barlow Semi Condensed Medium (SIL OFL)' 0 1 true
```

Compile exporter first: `javac -d build/font-tool tools/GenerateUiFont.java`.
All glyph parsing/triangulation happens offline, never during gameplay.
