# Barge trains

## Player controls

Press a free barge, drag toward a highlighted tug or barge, and release over that
vessel to throw a connecting line. The throw starts on release. A selected moving
vessel remains the target during flight; a miss retracts. Established pushing
never switches into a linking gesture without first lifting the finger.

A barge can have one incoming line and multiple outgoing lines. This supports a
tug pulling several barges directly, a tug pulling a barge-to-barge train, or a
floating train assembled before its head is attached to the tug. Self-links,
cycles and a second incoming line are rejected. There are at most nine links,
matching the stage cargo capacity. Normal cast range is 170 world units to the
tug and 220 to another barge; closed bridges, banks and piers block the cast.

Tap a linked barge to release its incoming line. Its downstream barges stay
linked together, keep their momentum, and stop contributing to the tug's load
until reconnected. A long hold, cancelled touch or drag is not a release tap.
Mooring/expiry removes all links incident to that hull. Excessive stretch or an
obstructed rope releases the affected link, leaving other links intact.

## Physics and rendering

The shared `Towline` owns a bounded collection of tension-only deck-fitting
springs. Slot zero retains the original single-line fields for existing clients;
use `link`, `incoming`, `primary`, `count` and `connected` for graph queries. Do not
use the slot-zero `cargo` field to decide whether an arbitrary train is connected.
Each link stores its leader, follower, local end fittings, rest length, tension
and independent catch/release animation. Stable slot order makes force updates
and rendering deterministic. Both hulls receive linear impulses and torque;
slack cables cannot push.

The tug uses only the mass reachable through its connected links, including
indirect loads. Commanded top speed scales by `1 / (1 + 0.075 * connectedMass)`;
thrust acceleration scales by `1 / (1 + 0.05 * connectedMass)`. With each loaded
barge having mass 5, speed-command factors for one/two/three barges are about
73%/57%/47%. Actual speed also depends on cable tension, hull orientation, water
drag, turns and acceleration time. These are arcade tuning values, not nautical
speed units. Engine effects respond to tension from all tug-connected root lines.

All ropes contribute 24 sagging spans each to one stable depth sort shared with
vessels and scenery. Attachment offsets rotate with their own hull; each end has
its own heave. Solid shore clipping applies to every span. Near/far rope behavior
must not be implemented with a blanket overlay. The camera includes connected
loads; the bridge stays open while any link spans its passage.

## Tutorial and validation

The continuous tutorial has 14 lessons. The towing sequence follows the first
push, tie-up and unloading job:

1. Connect the waiting barge to the tug and physically tow it into the marked
   berth approach at `(245, 310)`, radius 58. Keep its line attached for reuse.
2. Attach the second waiting barge directly to the tug as a separate branch.
3. Wait for the articulated bridge to open, then pull both barges underneath.
   Each barge's complete collision hull must cross north of `Bridge.Y - 35`
   while both have direct tug connections. The tug must also be clear.
4. Tap either barge to release its tug line, then drag that free barge to the
   other barge. A tug → barge → barge chain in either order is accepted. The
   tutorial checks that the player actually released a line during this lesson;
   changing the graph programmatically cannot complete it.
5. Pull the same two barges through the narrow channel. The final hull, not just
   the tug or a barge center, must cross the exit gate before this step completes.
6. Tap both barges to uncouple the train.

All boats exist from the start. Normal progression never replaces a boat,
teleports it, unloads the reused towing loads, or rebuilds scenery. The first
unloaded barge remains alongside its original berth. The permanent connected
water extends from the original harbor through the bridge into a broad rigging
basin to world y=-200. The single-file reach spans x=270..330 and y=-520..-200;
its 60-unit width accommodates one 35-unit-wide barge, but not two abreast. The
exit basin extends to y=-1000, leaving room for the entire train to clear.
`TrainingChannel.WATER_RECTANGLES` and `WATER_BOUNDARY` describe the same shape
for collision, wakes, shore masking, static ground, scenery setbacks and roads.

Broken or released links return to the relevant rigging instruction in place.
Valid full-hull crossings already earned remain credited. Each obstacle must
still finish with the correct topology and all vessels clear. The bridge waits
for vessels and spanning towlines before closing. Transit camera framing includes
the connected train; close rigging includes the spare. Drag and joystick driving
use the same physical progression conditions.

`TowTrainTest` covers actual cast/release gestures, direct and chained loads,
prebuilt trains, cycle prevention, momentum preservation, cleanup, pause/resize,
all-heading rope ordering and measured speed changes with zero to three loads.
`TutorialTowCourseTest` exercises the entire new route with native screen-pointer
gestures on three landscape widths, plus joystick navigation. It checks actual
berth arrival, complete-hull gates, both configurations, deliberate unlinking,
recovery, and close-packed barge picking during reconfiguration. The continuous
tutorial tests retain vessel and scene continuity from initial driving through
final uncoupling. `TrainReview` exports double towing through the bridge,
reconfiguration, narrow-channel pulling and uncoupling using the shared renderer.
