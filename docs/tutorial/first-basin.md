# First basin lesson sequence

After navigating the canal, the player finds one loaded barge on the far side
of the connected basin, in the narrow arm at (599,300), aligned along its length.
This follows the circled location in Screenshot_20261122-113310~2.jpg. The other two barges remain behind the
first tow bridge.

1. Follow the water around (320,700) and (599,700) into the far arm. Approach the first barge and drag from it to the tug to connect a towline.
2. Tow out of the narrow arm toward the berth. There is no intermediate parking
   circle or required tow position. Tap the towline when ready to switch to pushing.
   Waiting does not advance; an automatic line loss returns to connection practice.
3. Press the barge and keep holding: the tug automatically gets behind it. Without
   lifting, drag toward the berth to steer the push. This lesson requires actual
   contact and movement, not placing the barge on a second practice marker.
4. Lift, then press and hold again without dragging: assist steers toward the real
   berth. Drag to override the direction; lift to stop pushing (the boats can coast).
5. Continue holding to enter the berth, lasso the bollard and unload. The empty hull
   remains moored in the continuous scene.
6. Call the first bridge, collect both fireworks barges and continue the two-tow /
   train lessons.

Legacy lesson ID7 (PULL) is retired and redirects to CASTOFF on explicit reset;
normal play skips it. IDs after it remain stable for native tooling. Tests may
choose a roomy cast-off point as player input, but gameplay has no such position gate.

Normal transitions preserve bodies, position and momentum. Explicit skip/reset
may prepare a lesson fixture. Tests cover real gestures, full-scene continuity,
failed/released lines, unloading and bridge authorization on multiple widths.

Shared-renderer reviews: [far-side connection](../art-studies/first-basin-v2/connect.png)
and [cast-off instruction](../art-studies/first-basin-v2/cast-off.png).
Regenerate with `tools/TutorialTowReview.java` after compiling shared/desktop/test
sources plus `tools/DialogueReview.java`. It traverses the real input replay and
both subsequent radio gates, allowing panels to finish their wipes before capture.

Equipment-control follow-up: cast-off is now a tap on the towline itself, with an
open release cue at its midpoint. The two-barge convoy now contains two fireworks
barges; the separate fire-barge collection belongs to the
[celebration finale](fireworks-finale.md).

Tow gestures remain available during pushing/docking lessons for recovery; tutorial
stage IDs must not disable the base interaction. A fresh drag from a free barge
arms tow aim early, with forgiving initial direction. Release over the destination
craft throws the line. Press and hold first to commit push assist; an established
push must never switch into a lasso as the tug crosses beneath the finger.
Hull touches take priority over floating equipment icons and crossing towline
hit zones. Equipment icons seek nearby clear positions; rope release remains a
short tap on an exposed span. Reattaching during push practice shows an explicit
release-before-pushing instruction rather than silently blocking the gesture.
