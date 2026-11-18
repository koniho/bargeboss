# Gameplay highlights — restored quiet style

The user rejected the broad neon redesign on 2026-11-16 and requested a full
visual rollback. The earlier `e48588a` highlight appearance is the current baseline.
Do not reintroduce thick saturated bands, filled arrows or segmented rings based
on the superseded guidelines in Git history.

Use thin translucent strokes, open arrowheads, continuous rings, restrained
feathering and the original gentle pulse. Restore the original hull-glow assets,
berth corner treatment and interaction highlighting. Preserve readable hulls and
keep docks dark. Any further visual redesign needs a small reviewed example first.

The independent moored-boat lighting correction and world-arrow depth ordering
remain. Arrows use the old open strokes within the retained world depth pass;
lighting, splash stamps, gameplay and physics are outside this visual rollback.

Valve's [Connecting Visuals to Gameplay](https://cdn.fastly.steamstatic.com/apps/valve/2008/MIGS08_ConnectingVisualsToGameplay.pdf)
remains useful background for readability and visual hierarchy. Our rejected
neon implementation was a project interpretation, not a Valve requirement.

See [the original guidance notes](../art-prompts/soft-guidance-v1.md) and
[drawing order](../rendering/drawing-order.md).

## Overhead overview map

The gameplay overview sits opposite the throttle. Fit the location’s gameplay bounds into the available overview space with equal
axis scale. The thin border is the projection of those world bounds, not the UI
panel edge; rectangular stages retain their actual aspect ratio. Fully enclosed
water areas (the practice canal/basins) add a uniform world margin of 6% of the
longer span, showing land between the shoreline and frame. Open-lake locations
keep their frame aligned with the playable limits. Use thin translucent
shoreline/dock contours. Navigable water has a softer translucent fill (about
17% effective opacity versus 53% for outlines); dry land and dock interiors stay
clear. Do not add a filled panel, buildings or isometric projection. World +X (upper-right on the game plane) is up on the chart; world +Y
(lower-right) is right. Keep this orientation independent of the camera. A yellow
player dot pulses gently; cyan cargo markers and amber target outlines remain
secondary. The chart is informational and does not intercept steering gestures.
Cache the immutable border, water fill and contours separately from live markers.
Apply chart opacity to the cached outline as a whole, not to individual line
segments: overlapping translucent endpoints create unwanted bright joint dots.
