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

The continuous tutorial now has 12 lessons. A third barge is present from the
start, parked clear of the earlier pushing route. After the single-barge tow,
the player links the spare to their tow, physically pulls the second barge at
least 45 world units, then taps each linked barge to uncouple. No body teleport
or scene reset occurs at normal lesson transitions. A broken link returns to
the relevant connecting lesson. The linking camera includes the spare barge.

`TowTrainTest` covers actual cast/release gestures, direct and chained loads,
prebuilt trains, cycle prevention, momentum preservation, cleanup, pause/resize,
all-heading rope ordering and measured speed changes with zero to three loads.
The continuous tutorial runs through real screen-pointer gestures on phone and
tablet layouts. Existing push-gesture, mooring, bridge and rendering tests remain
required. `TrainReview` exports the shared renderer's linking and pulling scenes.
