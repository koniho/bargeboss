# Gameplay highlights — restored quiet style

The user rejected the broad neon redesign on 2026-11-16 and requested a full
visual rollback. The earlier `e48588a` highlight appearance is the current baseline.
Do not reintroduce thick saturated bands, filled arrows or segmented rings based
on the superseded guidelines in Git history.

Use thin translucent strokes, open arrowheads, continuous rings, restrained
feathering and the original gentle pulse. Restore the original hull-glow assets,
berth corner treatment and interaction highlighting. Preserve readable hulls and
keep docks dark. Any further visual redesign needs a small reviewed example first.

Do not draw a persistent circle around a watercraft just because it is being
towed. Towlines show the connection; brief catch feedback and active targeting
or tutorial instructions remain separate interaction cues.

The independent moored-boat lighting correction and world-arrow depth ordering
remain. Arrows use the old open strokes within the retained world depth pass;
lighting, splash stamps, gameplay and physics are outside this visual rollback.

Valve's [Connecting Visuals to Gameplay](https://cdn.fastly.steamstatic.com/apps/valve/2008/MIGS08_ConnectingVisualsToGameplay.pdf)
remains useful background for readability and visual hierarchy. Our rejected
neon implementation was a project interpretation, not a Valve requirement.

See [the original guidance notes](../art-prompts/soft-guidance-v1.md) and
[drawing order](../rendering/drawing-order.md).

## Overhead overview map

The gameplay overview sits opposite the throttle and follows the player while
zoomed in. Center independently on each axis, clamping the scroll offset so a
play-area edge cannot move inside the viewport when that axis is larger than the
window. Near a boundary, the map stops scrolling and the player dot moves away
from center. An axis that fits within the viewport stays top/left aligned; do
not center that short axis or add a surrounding margin.
Minimum/default zoom fits the full authoritative play bounds, with no surrounding
world margin, including in the tutorial. Do not allow zoom-out below that fit.
Use equal axis scale: rectangular stages retain their actual aspect ratio, with
any unused space on the right/bottom. The stronger border is the projection of
world bounds. Use thin translucent
shoreline/dock contours. Navigable water has a softer translucent fill (about
17% effective opacity versus 53% for outlines); dry land and dock interiors stay
clear. Do not add a filled panel, buildings or isometric projection. World +X (upper-right on the game plane) is up on the chart; world +Y
(lower-right) is right. Keep this orientation independent of the camera. A yellow
player dot pulses gently; cyan cargo markers and amber target outlines remain
secondary. Clip the scrolling map and live markers to its fixed square viewport.
Draw a faint inset viewport outline (0x26 alpha, 0.8px) beneath chart contents;
the actual play boundary retains its stronger chart opacity wherever visible.
The chart is informational and does not intercept steering gestures.
Its horizontal zoom slider sits in the bottom HUD row, centered eight pixels below the map;
Help/Practice are centered on the screen so the slider clears them on either side.
The slider captures its own pointer ID until release/cancel. A separate finger
can simultaneously steer with the joystick or drag the tug; either control can
start first and releasing/canceling one must preserve the other. Slider touches
never enter steering, lasso or camera pinch routing. Pause/background/reset cancel
both owners. Its permanent thumb and filled track show relative
zoom even while idle. Left is maximum zoom-out: exactly the full-chart fit scale. Right is
four times fit scale; use logarithmic interpolation for even zoom steps. The
default is fit scale. Only the overview changes; main camera zoom is independent.
The old in-play score panel and direction/status strip are removed; results
still show earned scores. Preserve tutorial instruction overlays and joystick cues.
Cache the immutable border, water fill and contours separately from live markers.
Translate/scale the existing chart texture; do not cache each slider position or
rebuild geography while dragging. The two existing raster-density buckets bound
texture growth. Keep marker sizes fixed while the chart scales.
Apply chart opacity to the cached outline as a whole, not to individual line
segments: overlapping translucent endpoints create unwanted bright joint dots.
