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

## Layout and component redesign (2026-11-21, v2)

The user explicitly rejected a palette-only restyle. Components now have distinct
roles, shapes and information hierarchy:

- Title: independent training entry under the wordmark; one voyage setup card on
  the right containing harbor identity, Choose Harbor, time-of-day tabs, and a
  single prominent Set Sail action. Captain progress is a compact footer ribbon.
- Containers: quiet navy surfaces with a left structural rail and cut corner,
  rather than a red octagonal outline around everything.
- Actions: solid cut-corner primary buttons with a lower edge and directional
  cue; secondary actions use an underline and rail. State selectors use a tick
  and underline, not the same treatment as a departure action.
- Harbor cards: job type, harbor name, connected-job count, actual completed-job
  progress and an explicit selected state. Remove large unexplained empty stars
  and the crowded career summary from this selection screen.
- Settings: labeled rows, segmented driving-hand choices, a real haptics switch
  with the whole row tappable, and Done. Pause: one Resume action, with Restart
  and Back to Title grouped below. Results: captain recognition plus a distinct
  three-metric summary, rather than scattered numbers.
- Gameplay: a compact mission ribbon, radio-source icon/status badge, continuous
  lesson progress, inset reply chevrons with numbered badges, a labeled chart
  zoom fader with a grip, and a thumb grip on the throttle puck. Preserve the
  throttle's directional sectors, input semantics and semantic colors.

Layout changes are authorized for this redesign. Keep rendering and hit targets
in the same shared Layout/Dialogue/LightingChoices definitions. Tests cover safe
bounds, nonoverlapping title actions, choosing a harbor then sailing, settings
rows, and pause navigation. Instruction-panel position and CRT/wipe choreography
remain fixed; world-space highlight and marine-light rules remain separate.
