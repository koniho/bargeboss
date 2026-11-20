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
shoreline/dock contours. Navigable water has a faint blue-black fill (about 10% effective opacity).
Non-traversable land and dock interiors carry a cyan overlay (about 23% combined
opacity versus 53% for outlines). Cut water from that overlay using the location’s navigable geometry,
including dock exclusions. Keep this geometry in the cached chart, not the live
draw loop. Do not add an opaque panel, buildings or isometric projection. World +X (upper-right on the game plane) is up on the chart; world +Y
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

## Mission header and menu access

Keep one mission title centered at the top of the safe gameplay canvas. Put
relevant delivery counts, timed lock windows, and lock water level in a smaller
subtitle underneath. Show hull condition there when damaged. Tutorial progress
uses the same subtitle; never show the ambiguous FREE label or empty rating stars
in play. Stars belong to results and career displays.

Do not put Settings, Haptics, or Pause buttons back over the live scene, including
invisible hit targets. Settings belongs on the title screen; haptics is a setting
inside that menu. Android Back, desktop Escape, and the iOS left-edge back gesture
route to the shared Game.back action. Back cancels held inputs before pausing or
resuming, including nested tutorials, and closes Settings to its prior screen.
Keep the instruction overlay below the header and the driving/map controls clear.

## Pilot Glass UI

Use the approved Pilot Glass concept for screen-space panels and controls:
translucent blue-black surfaces, a single fine cyan perimeter with clipped
corners, short calibration marks, cool readable lettering, and restrained amber
for active/primary actions. Primary buttons use a thin edge accent and open
chevron, not solid yellow fills or moving attention arrows. Keep instructional
backing dark enough to read over detailed scenery. Progress segments show real
lesson progress; do not animate decorative progress unrelated to the task.

`PilotGlass` owns these UI primitives. Keep `VisualStyle` world materials, vessel
cues, map projection and throttle behavior independent. Preserve the Barge Boss
logo artwork/colors and animated title harbor while styling the title menu,
Settings, rank strip, pause/help/results, mission header and tutorial cards.
Retain existing control hit boxes and Back navigation. This is a UI treatment,
not a replacement for the separate tutorial-flow improvement ticket.

Pilot Glass typography uses Rajdhani SemiBold, with proportional uppercase
lettering and restrained tracking. Pale cyan is the primary text color; amber
marks selection, progress and secondary mission state. Use `Lettering.width`
for fitting, never fixed pixel-font character counts. The original Barge Boss
logo deliberately retains `LogoLettering`. Font provenance and regeneration
instructions are in [the font README](../../assets/fonts/rajdhani/README.md).

Tutorial instructions occupy one fixed upper-right screen-space slot (432x124,
18 units from the right edge, y=68). Arrival, driving, canal, pushing and towing
share its title/body baselines. Change copy/progress within the slot; never flip
sides based on lesson, vessel position or camera. Completion uses the separate
results screen. Keep the slot clear when framing future tutorial objectives.

Instruction-panel motion: show uses an 18-tick CRT beam expansion; hide uses an
8-tick collapse. Content changes retain the full frame and clear the old snapshot
left-to-right over 12 ticks. A small soft star follows the wipe at the title
top edge with a soft shimmery vertical line down to the content bottom. It
drops vertically at the right edge over 8 ticks, collapsing the line; returns
right-to-left along the bottom over 12 ticks with the line collapsed; then rises
vertically at the left edge over 8 ticks, extending the line downward. It then
leads the new snapshot reveal left-to-right over
12 ticks. Keep contents cleared during the drop/return/rise. A soft 24-tick shimmer
crosses the panel after opening or revealing text, then stops entirely. Numeric progress/countdown updates stay live without replaying the wipe.
Transition state advances only in simulation steps and freezes on pause. Clip
only content, never the frame or world; balance native clip scopes in all cases.

## Tug hull glow removal (2026-11-20)

The tug no longer uses the gold silhouette pulse while navigating/pushing, or
the faint idle hull bloom. Keep its actual work/navigation lamps and their fog
scattering. Cargo targeting highlights remain unchanged.
