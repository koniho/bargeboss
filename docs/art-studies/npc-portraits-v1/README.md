# Harbor radio crew
Six new NPCs, generated with built-in imagegen using the instructor as the
style/layout reference. Exact prompts in prompts.json. Transparent original
outputs are preserved under source/. Runtime sheets live under assets/sprites/
as portrait-<id>-v1.png:576x384,3x2 grid,192px cells,center pivot.
All source faces point right. Shared UI mirrors them to face inward.

| ID | Role / palette | Runtime use |
|---|---|---|
| bridge-operator-steel | Amber helmet / navy, moustached operator | Tutorial canal and exit bridge |
| bridge-operator-night | Navy cap / teal, silver-haired operator | Tutorial tow basin entry bridge |
| harbor-dispatcher | Coral cap / mustard, braided dispatcher | Art ready; no new conversation added |
| lock-keeper | Green cap / orange, bearded keeper | Art ready; no new conversation added |
| dock-foreman | Cream helmet / burgundy | Art ready; no new conversation added |
| fire-rescue-captain | Red helmet / navy and gold | Art ready; no new conversation added |

Frames:0 neutral closed mouth;1 small open;2 wide open;3 medium open;
4 closed smile;5 blink. The existing shared61-tick timing sequence is compatible.
Generated animation has slight drawn expression variation; registration is
reviewed visually, not claimed geometrically exact. Keep frame zero between
transmissions. Keep speaker identity through replies and wait states.
Only the two used bridge sheets join tutorial preloading; unused characters are
not eagerly decoded. Six192px RGBA frames cost about864KiB per prepared portrait
before native overhead; no claim of measured device residency.

Rebuild runtime sheets:
magick docs/art-studies/npc-portraits-v1/source/<id>.png -resize 576x384 assets/sprites/portrait-<id>-v1.png

tools/NpcPortraitReview.java exports crew-review.png and bridge-0/1.png, checks
all36 frames for actual alpha/nonempty bodies, and checks bridge assignment and
reply persistence. Palette/silhouette and small UI-size readability were reviewed.
