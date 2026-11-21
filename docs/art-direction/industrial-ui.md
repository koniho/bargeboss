# Industrial nameplate UI

The user approved the smooth Barge Boss wordmark and requested the game UI use
its style on 2026-11-21. Use the shared `PilotGlass` primitives; the class name is
retained for existing call sites, but the visual system is now the title's navy,
cream, gold and red palette.

- Navy #101c30 is the panel/button surface. Keep enough opacity to read text over
  busy scenery, while leaving the central navigation area open.
- Cream #fff0ce is primary text. Muted warm gray #b9b5a8 is supporting text.
  Keep the approved Rajdhani UI font; custom title lettering is for the wordmark.
- Gold #ffc52f marks primary actions and selected choices. Gold-filled controls
  use navy labels/icons, never gold or cream labels that lose contrast.
- Brick red #a92d37 is restrained structural edging, not a flashing alarm.
  Use smooth chamfered corners, thin closed outlines and one subtle upper edge
  highlight. No pixel cells, texture noise, heavy chrome, extra decoration or
  new permanent HUD labels.
- Use the same treatment for title menus, location cards, settings, pause/result
  panels, mission heading, tutorial instructions, reply chevrons and ability UI.
  Pending/pressed radio replies use the existing state transitions and fades.
- Preserve fixed instruction-panel placement, CRT opening/closing, wipe path and
  sparkle timing; tint these effects warm gold/cream. Do not reflow the panel on
  each lesson change.
- Keep throttle geometry, sector fade and green-forward/amber-reverse semantics.
  Its idle body uses navy, restrained red edging and cream detail. Preserve the
  chart's navigable fill, faint viewport frame, brighter true boundary, clipping,
  player-following rules and zoom indicator. The slider uses gold and a compact
  navy plate, wholly inside its existing touch bounds.
- World-space goals, marine lights and collision feedback keep their semantic
  colors and the separate gameplay-highlight rules. This is a UI treatment,
  not a global scenery/vessel recolor.

Implement shared vector geometry, existing font atlas and cached effects. Do not
add per-frame rasterization, blur, texture generation or native-specific layouts.
Hit targets, safe-area positioning and available play space must remain stable.

`tools/UiStyleReview.java` exports actual shared-renderer examples at 960 and1333
pixels wide: title, locations, settings, driving on both control sides, pause,
tutorial instructions, four-choice radio and pressed reply. Run the existing
map pixel, dialogue, throttle, overlay and full shared checks after changes.
Native screenshot validation must confirm the game is foreground; do not retain
an unrelated device screenshot as review evidence.
