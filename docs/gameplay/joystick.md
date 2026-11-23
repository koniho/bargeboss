# Optional thumb joystick

The shared Java HUD offers a fixed throttle pad in the bottom left by default.
Title-screen Settings selects Left or Right. Throttle is the only manual tug
propulsion control; touching/dragging the hull never drives it. Cargo hold/drag
push assistance remains available. Hull dragging in the practice slip arms the
shore lasso instead. Tap its connected bollard to release the tug tether.

A pad press owns its pointer immediately; thrust waits for 14 logical points of
travel from the actual touchdown. After direction selection, the seven-point
neutral zone allows fine throttle/coasting control. An off-center press alone
never drives. Thumb travel up to 63 logical points scales requested speed.
Screen direction is
inverse-projected to the water plane, then uses the existing navigation, astern,
acceleration, tow-load, collision and coasting rules. Release cuts engine demand;
it does not zero velocity. More linked cargo still reduces towing performance.

The pad belongs to the safe logical canvas, above footer controls. Draw it last
in the live HUD, outside world depth/shore clipping; it never moves with camera
pan/zoom. It has priority over world hits only within its visible circle. Once
owned, dragging outside the circle remains joystick input and cannot turn into a
cargo lasso, link release or menu tap. Extra pointers cannot steal ownership.
Pause, cancellation, resize, reset and backgrounding clear its state.

Settings pauses simulation and Back restores the previous screen. Practice shares
player settings with its parent, but keeps its own career progress. Native hosts
continue storing the opaque Game.saveProgress() string on progressVersion changes.
The BBG2 wrapper contains the existing BB1 career snapshot, joystick side and
independent music/sound levels and mute flags. BBG1 saves preserve joystick side
and default audio to 100% unmuted; legacy BB1 saves also load with a left joystick.
Saved Off (0) migrates to Left;
there is no Off button because that would leave no manual driving control. Invalid wrappers are ignored atomically.
No native gameplay or input rule is added.

Validation: JoystickTest checks native pointer ownership, cancellation, responsive
placement, dead zone/clamping, projected motion, actual driving/coasting tutorial
completion and preference migration. JoystickReview exports the actual shared
renderer at 960 and 1333 logical widths. Native rasterization and touch latency
still require device testing on each host.

## Forward and reverse selection

Green segments show the forward selection region; amber segments show reverse.
Both are projected from the same world-space thrust threshold (the rear 120-degree
cone selects reverse) and rotate with the tug heading. They ease out while a finger
is held and back in after release/cancel, on fixed simulation ticks. The label
beneath the pad shows the selected FORWARD or REVERSE while a drive gesture owns it.

The first deliberate input chooses direction for the whole gesture: beyond the
throttle's 14-point intent threshold measured from touchdown. This selection occurs on pointer input, so a
second move before the next simulation tick cannot replace the first intent.
Heading changes, crossing the colored boundary and returning to neutral do not
change it. Neutral still cuts thrust; steering and proportional speed stay live.
Release and press again to change direction. Cancel, pause, resize, settings,
background and reset all clear the choice. Cargo pushing/tow-lasso classification
is unaffected; this latch is exclusively manual tug driving.

`DriveDirectionTest` covers both throttle sides, initial
neutral, between-tick input changes, continuing steering, neutral re-entry,
release/reselection, momentum preservation and lifecycle cancellation. The full
continuous public-input tutorial remains a regression check for practical route
completion under locked thrust direction.

Manual thrust follows the tug's actual keel: forward accelerates toward the bow,
reverse toward the stern. Drag direction steers the heading, rather than pulling
velocity sideways toward the finger. The existing steering-error speed reduction,
acceleration limits, tow load and coasting remain active. Automated cargo pickup
assistance retains its separate target-following behavior. Tight towing turns may
need less throttle so the following load can turn without overstretching its rope.

Joystick steering and neutral remain relative to the original touchdown for the
whole gesture, so landing on one side of the pad cannot invert an intended drag.
The fixed pad displays this relative command. Small opposite-direction jitter
before the threshold remains neutral and does not latch a gear.

## Practice slip tether

`SlipMooring` owns a separate tug-to-shore constraint; it does not run the cargo
winch/unloading flow. Drag from a tug inside the practice slip to the raised
bollard and release to throw. The existing arc, miss/retraction and impact effects
are reused. A catch retains momentum and stores the initial horizontal rope length
and body-local fitting choice. Each motion substep constrains only outward
fitting velocity, transferring angular impulse through the hull inertia. Inward
motion is free and creates visible slack; steering and the throttle remain live.
A positional length limit prevents sustained thrust from stretching the line.

Tap the attached bollard to cast off. Tug taps do not release, and cancelled or
dragged-off bollard touches leave the tether intact. Pause/resume preserves the
line, while reset creates a new untied hull. Existing barge tow release still uses
a tap on the connected barge. A tug tether can coexist with tow links. Tug tying
is optional; the slip lesson still requires backing in and lifting the control.
The map lesson waits for cast-off before leaving the slip phase.
