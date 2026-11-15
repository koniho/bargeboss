# Goal-based career levels

Career levels end when their objectives are complete, rather than when a shift
clock runs out. The HUD shows goal progress. Only the lock's entry and exit
obstacles have deadlines. All rules, input, timers and scene composition remain
in shared Java and advance at the fixed 60 Hz tick.

The continuous tutorial is separate: its bridge, canal, towing lessons and
untimed practice progression remain available without career rewards.

## Two Banks — level 1

Deliver exactly two barges and finish unloading both. One goes to berth A on
the left bank (`berth=2`, world Y=290); the other goes to berth B on the right
bank (`berth=1`, world Y=488). They can be delivered in either order. The first
two feeder lanes bring one assigned load each; there is no endless cargo queue.

The normal pushing, lasso, parallel-to-shore mooring and three-second unloading
mechanics apply. A load counts only after unloading completes, not on entering
the berth or catching its bollard. The level ends on the second delivery.

## Shared Slip — level 2

Unload three barges through one narrow unloading slip. All cargo assignments use
its same berth at world `(199,480)`. The slip is 58 units wide, with a solid quay
on its eastern side and a single opening to the south at Y=570. Aligned vessels
fit one at a time; a barge occupying the berth physically blocks the next load.
There is no letter-sequence eligibility rule. The order comes from moving real
hulls through the constrained space.

After each unload, drag the empty barge to the tug and release to cast a towline.
The shore line releases only when the towline successfully catches; a missed
throw leaves the barge safely secured. Pull the empty south through the mouth,
then around the quay into the open water east of it. The translucent **EMPTY
HOLDING** outline suggests a place to leave empties; it has no automatic snapping,
mooring or collision behavior. Make room before bringing the next load inside.
The third completed unload ends the level; clearing that final empty is optional.

## Lock Run — level 3

Move two assigned barges through a lock on the long river reach. There is no
delivery berth in this level. Pushing or towing the loads uses the same physics;
the lock tracks their stable body IDs and requires complete oriented hull
clearance, not just their centers crossing a line.

The lower entry gate is at world Y=330 and the upper exit at Y=0, spanning the
river from `Physics.LEFT` to `Physics.RIGHT`. The upstream reach extends to
Y=-520. Navigation continues toward decreasing world Y.

| Phase | Rule |
| --- | --- |
| READY | Preparation is untimed. With both feeder loads available, approaching within 140 world units downstream of the entry starts the entry window. |
| ENTRY | 45 seconds total, including 5 seconds opening the gate. Bring the tug and both complete barge hulls inside with clearance from both gates. |
| CLOSING | Once all are safely inside, the entry seals over 5 seconds. |
| FILLING | Both gates remain closed for an 8-second water-level cycle. |
| OPENING | The exit opens over 5 seconds. This motion is outside the exit deadline. |
| EXIT | 35 seconds begin when the exit is fully open. Both complete barge hulls must clear the upper gate. |
| COMPLETE | Both target barges are beyond the exit: the level ends immediately. The tug need not satisfy an additional exit quota. |
| FAILED | A missed entry/exit window or missing target cargo ends the attempt with a reason and a replay option. |

Entry completion requires the tug inside as well as the two barges. Hull checks
use all three oriented collision discs with radius and a three-unit clearance
beyond each gate's six-unit half-thickness. Closed gates physically block those
discs. A failed deadline stops the attempt instead of forcing a gate through
unfinished cargo. Pause/background freezes the phase and remaining time.

The central HUD shows only the active entry/exit deadline. Other phases show
goal clearance progress, with the current lock state in the status line. Moving
gate leaves are rigid parts of `ArticulatedModel`, with stable hinge pivots and
attached signal lamps. Five-second travel eases in and out; red remains lit until
full opening. Red/green lights, brighter rising wet-height marks, staff gauges on
both walls and a persistent HUD water percentage/bar communicate the cycle.
The HUD shows LOW, RISING, or HIGH / MATCHED even when a wall is offscreen.

The open entry and exit reaches push downstream against upstream travel. During
filling, shifting sluice currents move hulls sideways within the sealed chamber;
steer to hold the tug and convoy away from the banks. The filling force ramps up
and settles to zero at the matched level. Its influence ends at the chamber walls,
and through currents stop when gates seal. Translucent drifting streaks sample the
same `Lock.currentX/Y` field as physics, so their direction describes actual drift.

`Physics.integrate(body,waterX,waterY)` applies the existing keel/broadside drag to
velocity relative to water, then restores world velocity for movement. Towed and
free barges both respond; loads still waiting in a feeder are secured. All samples
use fixed simulation ticks, including during the game's two motion substeps;
pause freezes current-driven motion and the water cycle. These are tuned gameplay
currents, not a hydraulic simulation. The entry window remains 45 seconds including
opening, and the 35-second exit window begins only at full opening. Pull the tug
far enough beyond the exit for the whole trailing convoy to clear against the flow.

The water-level display is a visual
abstraction: vessel movement and collision retain one fixed navigation plane;
the game does not independently raise hull sprites or rebuild wake coordinates.

## Deep First — level 4

Three berths occupy one dead-end, single-file channel. Berth A is deepest at
`(199,220)`, B is at `(199,350)` and C is closest to the mouth at `(199,480)`.
Their internal IDs remain `{2,0,1}`. Solid quay geometry leaves the same 58-unit
passage used by the slip. An outer barge blocks access to the inner berths,
including after its cargo has been unloaded. Work deepest first, or rearrange
barges to make a clear route. Wrong-order arrivals are physically recoverable by
towing them back out; the game never rejects a tie simply because its letter is
later in the suggested order. `nextRequiredBerth()` supplies a planning cue;
`correctOrder()` does not impose a sequence gate.

`PhysicalBerths` supplies the same quay rectangles to collision, navigation,
water clipping, shoreline cutouts and scenery drawing. Berth outlines use the
stage's actual half-width. The first two feeder lanes bring A and B; C waits for
a free lane. The third completed unload ends the stage.

## Shared completion, cargo and rewards

`Game.goalComplete()` and `goalFailed()` replace the old global shift deadline.
The feeder queue is finite: two, three, two and three loads respectively. Cargo no
longer expires; a barge can wait while the player works another load. Existing
`SHIFT_TICKS`, `shiftTicks()` and `contractSeconds()` values are retained only
where legacy tooling needs a bounded replay budget or age scale; they are not
player deadlines. They must not be restored as finish/expiry conditions in new
gameplay or HUD code.

Unloading records delivery once without deactivating or deleting the hull. The
empty keeps its position, collision, mooring and visible empty hold. It cannot
score again or resume unloading. Secured loaded barges remain unavailable for
towing; completed empty barges accept the normal drag-to-tug or drag-to-barge
tow gesture. A successful catch transfers the attachment from the shore line to
the towline. Tapping a linked empty releases its tow as with any other barge.
This applies to all unloading stages and tutorial deliveries.

Completing a level with a working tug earns bronze and unlocks the next level.
No lost loads and at least 80% hull each add one star. Unique career stars award
the existing whimsical captain ranks and stripes; replay improvements replace
the stored best result without duplicating career stars. Hull depletion remains
a failure condition. Practice neither awards stars nor changes the suspended
career attempt.

The append-only `Challenges` catalog defines titles, quotas, kinds and menu copy.
IDs 0–2 remain stable and the deep channel is appended at ID 3. The challenge
board paginates three cards at a time; counts, page totals, save slots and the
maximum star total derive from the catalog. The `BB1` save reader accepts old
three-entry careers and initializes the new fourth entry to zero, preserving
records and selection. Existing rank thresholds are unchanged.

`LockCurrentTest` checks slow gate travel, local current forces, broadside response,
frame-rate equivalence, pause behavior and changing level feedback. `LockTest` validates the timed phase cycle, hull clearance, failure and gate
collision behavior. Goal and bounded replay tests cover finite dispatch,
no-expiry behavior, persistent empties, physical occupancy and immediate completion. `GoalLevelReview`
exports actual shared title, selection, delivery progress and lock-phase views
under `out/goal-*.png`. `BerthSwapReview` exports locked/unlocked second-page
menus, occupied shared slips and all deep-channel berths at 960 and 1333 widths.
`preview.sh` includes both tools.

## Adding another level

Append a `Definition` to `Challenges.LEVELS`; never reorder or reuse a published
ID. A new level can reuse an existing kind, while a new puzzle geometry needs
its own shared rules and footprint source. Keep berth positions, collision,
route planning, water masks, shore cutouts and scenery on that same source.
Catalog entries supply quotas and replay budgets; the latter are never timers.
Menu pages and save migration grow with the catalog automatically.

For every new entry, add a bounded replay using actual player gestures, verify
completion and recovery from the intended obstacle, and capture the actual
renderer at both reference and wide landscape sizes. Extend geometry-specific
collision and occlusion checks when the footprint changes. Run the shared JVM
and Android ART checks; native iOS parity remains part of the macOS workflow.
