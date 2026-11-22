# Continuous practice harbor

Practice uses one connected world for seventeen lessons. The tug starts beside
its seawall dock at (-1600,810). A green instructor approaches and radios a
question, then moves aside to observe. See [radio and navigation rules](gameplay/radio-dialogue.md).
The opening teaches gentle and strong forward throttle, left/right turns and reverse, then sends
the player to (-1510,545). Forward and reverse figure eights use buoys at
(-1570,650) and (-1450,440), with opposite winding directions for each half.

Next is a recessed slip at x=-2000..-1856, y=250..350. The player points the bow
out and backs past the mouth anywhere in the slip, then releases the control; a significant collision requires pulling the whole hull clear
before retrying. After leaving the slip, the player reaches open water at
(-1740,650). The chart and slider first appear in this lesson. The chart starts zoomed in; a real zoom-out gesture reveals the
amber target at (-1320,260). Reaching it unlocks canal navigation.

The basin connects directly to the canal at x=-1260, y=442..558. Seven
rectilinear gates lead to the work harbor through x=170, y=442..558. The last
gate is at (80,500), before the mouth; the next instruction guides the player
into the harbor for barge handling and towing.

All three teaching barges exist from the start, with one in the first basin at
(292,700) and two behind the entry bridge at (245,310) and (365,310). Push the
first load to (240,600), then assist it into the far berth at (230,700) and unload.
Reconnect that same empty hull and tow it to the entrance-side target (245,550),
radius 58. Tap to release there; it remains in the first basin. Releasing outside
the target returns to the connection lesson rather than unlocking the next area.

Call the actual bridge house at the y=400 span and confirm its radio prompt to
enter the next basin. Both waiting barges must connect directly to the tug before
the second span at y=200 becomes callable. Call, wait for the full continuous
opening, and tow both complete hulls clear. Then reconfigure the same pair into
a train for the narrow channel. No approach automatically opens a towing bridge.
The canal-mouth span uses its existing separate call; authorization is per bridge.

Lesson progression changes instructions and completion gates. It does not
rebuild the world, replace vessels, move a hull to its next exercise, clear
the water, or snap the camera. The explicit Reset control may restart the
current exercise. Camera focus and zoom ease between the close 2.4x helm/slip
views, wider 1.4x figure-eight view, 1.65x canal and 2.15x work / 2.5x docking.
Working hulls and raised bollards remain in view. Lasso aiming/flight freezes
projection as in a shift. Neither the main camera nor physics changes when the
overview chart slider is used.

`TrainingChannel.WATER_RECTANGLES` and `WATER_BOUNDARY` define the complete
connected waterway. Collision, surface effects, scenery clearance and the
ground drawing use that geometry throughout practice. The canal mouth has
no hidden end wall. Ordered gates span the waterway's full width and retain
credit for genuine crossings without bypassing the earlier navigation lessons.

The `deliveryRecorded` flag records each delivery once. Delivered loads remain visible and solid; the first empty hull is reused for towing,
then parked near the entrance. Its unloading crew returns to idle. Cargo slots switch
which existing barge the tutorial teaches without changing vessel identity.

The replay suite exercises real gestures through every lesson on phone and
tablet layouts, checks hull and camera continuity at every simulation tick,
and verifies both basin/canal and canal/harbor mouths in both directions at sixteen hull headings.
Separate checks cover slip retry, chart slider interaction, full-width gates, tow retry, pause/reset,
single scoring, water clipping and the retained empty barge.

Factory placements use a 64-unit planning grid with whole-cell footprints.
Footprint edges, rather than arbitrary bitmap centers, align to the grid.
One-cell road lanes divide a sparse set of 83 compact pump houses, substations,
compressor houses and gatehouses. No large factory or steelworks sprite appears
in this tutorial yard. Placements and depth order are generated once; art/effect
bounds cull offscreen work. Small building heights vary while retaining dry
setbacks. Road haulers run near shore and unloaded shore cranes move slowly using
the existing continuous rigid model. Source
perspective must match the shared 45-degree yaw / 35.264-degree elevation:
each visible ground axis has a 30-degree screen slope. Moving a sprite's
pivot cannot correct an incorrect angle baked into its artwork.

The yard stands 18 shared display units above the water, behind corrugated
steel retaining walls. Wall faces and caps follow the exact water boundary;
the open canal mouth remains open. Yard texture, factory bases, lamps, dock
equipment and bollards share that elevation. `Game.bollardX/Y` exposes the
projected fitting location to both rope drawing and input, rather than relying
on a large touch radius to conceal an offset.

Lasso aiming, flight and recoil use `LassoPath`. The origin is the intersection
of the throw direction with the barge's rotated rounded hull envelope, lifted
to the deck. Release retains that local attachment and fixes the flight curve.
The flying loop and paid-out rope follow the previewed arc; the tether root
follows a moving/turning barge, and a miss returns to the current fitting.
Successful mooring draws a taut edge-to-bollard line.

Actual hull damage starts two brief pulses on the player tug. The shared timer
pauses with gameplay and does not trigger for ordinary fender contact, NPCs or
damage-free practice. No native painter requires a separate effect rule.


## Reverse driving before the canal

`REVERSE` follows `COAST` and precedes `CHANNEL`. On entry it latches a target
normally 60 world units behind the tug's current bow. Hull-contour samples check
the path against the connected water boundary and floating fixtures; nearby
reverse-sector angles and slightly shorter targets handle constrained approaches.
If there is no safe astern route, the card asks the player to make room in clear
water and retries at a bounded half-second cadence. It never moves a boat or
resets momentum to create the exercise.

Both joystick positions and a tug drag with the joystick disabled use the same
lesson gate. The enabled-pad card points to its amber reverse sectors and also
mentions dragging the tug back; the disabled-pad card explicitly teaches a
backward tug drag while keeping the bow ahead. Completion requires active reverse thrust, at least 40 units of
credited backward displacement, at least 40 net units toward the latched target,
and the actual tug inside its 22-unit ring. Waiting, backward momentum without
thrust, and turning around to drive bow-first cannot pass. Progress is limited
by net movement as well as credited astern travel. Soft animated rings/arrows
are world cues; directions and percentage remain in the coach UI.

Canal gates crossed before the player follows the coast instruction retain their
credit through reverse practice. Once reverse completes, an already-finished
canal can advance into pushing without a reset. The bounded full public-input
replay covers this late-coast case and normal scene continuity.
`ReverseTutorialTest` separately checks tug drag, left/right joystick, momentum
and forward-only rejection. Native lesson indices shifted: CHANNEL is 3 and
DONE is 15; native automation must follow the updated sequence.

Equipment update: the gated pair now waits in an open 700-unit collection
basin, y400 to -300. One carries fireworks; one carries Memphis-inspired fire
pumps. Short tapping an equipped hull opens its ability and (when linked) an
explicit release-line icon. The narrow train channel now runs y-700 to -1020,
with its exit basin ending at -1500. See [barge abilities](gameplay/barge-abilities.md).
