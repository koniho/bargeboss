# Designing Barge Boss levels

Start with a top-down map. The isometric camera is presentation; the level's
water, collisions, routes and touch targets are defined on the flat world plane.
You can sketch on paper, annotate an image, or edit the
[blank SVG template](level-design/sketch-template.svg) in a drawing application.
Exact coordinates and polished scenery are not needed for a first design.

## What to mark

| Mark | Meaning | Useful notes |
| --- | --- | --- |
| Blue area | Navigable water | Canal shape, narrow reaches, turning basins |
| Solid grey | Land, piers, islands | Which boundaries are solid |
| T + arrow | Player tug start | Starting heading |
| F1, F2 | Barge feeder slots | Entry direction and space for the NPC to leave |
| A, B, C | Delivery berths | Cargo type, approach direction, shore edge |
| Small circle | Bollard | Place inland beside its berth |
| Bridge line | Crossing bridge | Open/closed timing and waiting space |
| Numbered arrows | Intended route | Optional checkpoints and alternative routes |
| Dashed boxes | Scenery | Buildings and equipment; separate from collisions |

Give each level one main idea: a first delivery, tight maneuvering, waiting for
a bridge, recovering a stranded barge with a towline, or juggling incoming loads.
Mark the desired solution and any deliberate shortcuts or traps. Draw the
hardest turn with a barge attached; a tug alone is not a sufficient clearance
check. Leave room around feeder slots, berth approaches and bridge waiting areas.

## Copy-and-fill brief

```
Name:
Main skill/challenge:
Goal (deliveries, route, or both):
Tug start + heading:
Feeder slots + entry/exit directions:
Berths + cargo types + bollards:
Water shape + narrowest points:
Bridge/traffic timing:
Time limit + incoming-cargo pace:
Expected route:
Scenery mood/landmarks:
What should make a good run satisfying?
```

A marked-up sketch plus this brief is enough to implement and playtest a first
pass. Prefer gameplay placement first; lighting, smoke, cranes and skyline can
then reinforce the route without hiding it.

## Current authoring gap

There is no level editor or loadable level-data format yet. `Game.java` chooses
starts, feeders and berth positions; `HarborGeometry.java` defines solid piers;
`TrainingChannel.java` defines the practice canal and ordered gates. Rendering,
automated players and native UI tests also use this geometry. A sketch or JSON
file dropped into the repository will not currently become a playable stage.

The next authoring milestone should separate these definitions into one shared
level model and provide a lightweight top-down editor. It should support placing
water rectangles/polygons, feeder slots, berths, bollards, starts, bridges and
scenery; show tug/barge clearance; validate disconnected water and blocked
spawns; and offer an isometric preview plus “Play this level.” The exported data
must drive the same Java simulation on Android, iOS and desktop. A tutorial
sequence should reference objectives in that model instead of embedding another
copy of the map.

For now the sketch template is a planning aid, not an implemented editor. The
current practice canal is a useful first authoring fixture: it already shares
rectilinear boundaries between drawing and collisions and has a complete
seven-gate route test.
