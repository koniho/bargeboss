# Montrose rescue missions

Both missions reuse the surveyed Montrose shoreline, docks, mooring field, late-dusk materials and navigation entrance. They are independently selectable on challenge page three; the original yacht mission and saved challenge IDs are unchanged.

## Rafted cruisers

Two unpowered cabin cruisers are anchored side by side outside the entrance. Approach within 125 world units and tap the two rafting lines to release them. Throw a towline with the existing boat-to-tug drag gesture, then tow each cruiser into the harbor. Their lights remain off; their existing 32-pose cabin-cruiser artwork receives local directional illumination.

Dock C has two reserved bays, with their usual NPC boats omitted. The reference arrows are interpreted as C-38 on the northeast side and C-22 on the southwest side. The simplified survey uses Dock C row 8, longitudinal stations 345/405 and sides +1/-1 respectively. Those are existing bays, not new dock geometry. Since this game map has evenly spaced fingers rather than all real-world slip numbers, the labels identify the intended source-map bays rather than a claim of cadastral precision.

Release a slow cruiser on the water approach to its marked bay. Once an actual tug tow has occurred, dock crew can warp the released hull the last 115 units at up to 10 units/second along a clear route. This uses normal physical integration and dock collisions, not a position snap. A fully aligned, slow hull must settle for one second before it scores. Both must be returned.

## Swimmers

Twenty adults tread water beyond the southern seawall. Tap a swimmer, then release over them to throw a buoy. The tug must be within 135 units, have a clear water path, and move below 24 units/second. One buoy flies at a time for 36 fixed steps; a 12-step recovery follows. A secured swimmer holds an orange ring and remains a collision hazard. Any hull contact with a swimmer ends the mission without graphic injury; all twenty secured buoys complete it. Each rescue emits the existing semantic success haptic.

The camera eases from approach zoom 2.4 toward 3.6 within 240 units of the remaining group, retaining that close view until beyond 330 units. Aiming and buoy flight freeze projection. Swimmer touch targets are independent of their much smaller artwork.

## Implementation and verification

All state, gestures, placement, camera rules, atlas registration and drawing order live in shared Java. The twelve-cell swimmer atlas is warmed only for this mission; no unused yacht rotations are warmed for either rescue. Twenty swimmers and one flying buoy are the fixed entity limits. The buoy and swimmers join the common ground-depth merge and shore-occlusion path.

`MontroseRescueTest` checks public raft/tow gestures, reserved bays, physical settling at both target slips, all twenty throw transitions, contact failure, cancellation and camera hysteresis. It also completes a continuous swimmer rescue with public pointer gestures from the actual initial spawn in 1,892 fixed steps. The translated parity trace includes that entire rescue and raft-release renders. The two-cruiser voyage has phase/berth tests, not yet a complete entrance-to-both-slips autonomous replay; that remains a navigation/play-balancing validation task.

`MontroseRescuePreview` exports actual shared-renderer states for the raft, Dock C, approach, swimmers, buoy flight and secured swimmer. Developer scenes `montrose-raft` and `montrose-swimmers` allow native review/profiling without alternate gameplay rules.
