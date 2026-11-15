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

## Right Order — level 2

Deliver three barges in the displayed order **A, B, C**. The internal berth IDs
are `{2,0,1}`; the player-facing letters always come from `Game.berthLetter`.
The feed order is deliberately `{1,2,0}`: C, A, then B. The first two feeder
lanes bring C and A, and B waits for a free feeder lane. Clearing a drop slot
and choosing which barge to work first are part of the challenge.

`Game.nextRequiredBerth()` and `correctOrder()` are the shared ordering authority.
Tie eligibility, catch validation, mooring advancement and delivery accounting
reject later berths until their turn. Being physically inside the right berth
does not override the required delivery order. A wrong-order attempt leaves the
load available for later and reports the next required letter.

The HUD shows the next letter and full A-B-C sequence. Berth cues show NEXT,
LATER or DONE; the destination arrow follows the required berth even when a
different load was fed first. The third valid completed unload ends the level.

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
| ENTRY | 45 seconds total, including 1.5 seconds opening the gate. Bring the tug and both complete barge hulls inside with clearance from both gates. |
| CLOSING | Once all are safely inside, the entry seals over 1.5 seconds. |
| FILLING | Both gates remain closed for an 8-second water-level cycle. |
| OPENING | The exit opens over 1.5 seconds. This motion is outside the exit deadline. |
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
attached signal lamps. Red/green lights, wet-height marks, a gauge and local
filling turbulence communicate the cycle. The water-level display is a visual
abstraction: vessel movement and collision retain one fixed navigation plane;
the game does not independently raise hull sprites or rebuild wake coordinates.

## Shared completion, cargo and rewards

`Game.goalComplete()` and `goalFailed()` replace the old global shift deadline.
The feeder queue is finite: two, three and two loads respectively. Cargo no
longer expires; a barge can wait while the player works another load. Existing
`SHIFT_TICKS`, `shiftTicks()` and `contractSeconds()` values are retained only
where legacy tooling needs a bounded replay budget or age scale; they are not
player deadlines. They must not be restored as finish/expiry conditions in new
gameplay or HUD code.

Completing a level with a working tug earns bronze and unlocks the next level.
No lost loads and at least 80% hull each add one star. Unique career stars award
the existing whimsical captain ranks and stripes; replay improvements replace
the stored best result without duplicating career stars. Hull depletion remains
a failure condition. Practice neither awards stars nor changes the suspended
career attempt.

`LockTest` validates the timed phase cycle, hull clearance, failure and gate
collision behavior. Goal and bounded replay tests cover finite dispatch,
no-expiry behavior, required order and immediate completion. `GoalLevelReview`
exports actual shared title, selection, delivery progress and lock-phase views
under `out/goal-*.png`; `preview.sh` includes it.
