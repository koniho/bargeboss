# Optional thumb joystick

The shared Java HUD offers a fixed thumb pad in the bottom left by default.
Settings (title screen or live HUD) selects Left, Right or Off. Both pad and tug
press-and-drag remain available. A pad press owns its pointer immediately; thrust
starts beyond its seven-point dead zone, without the tug gesture's 12-tick hold.
Thumb travel up to 42 logical points scales requested speed. Screen direction is
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
The BBG1 wrapper contains the existing BB1 career snapshot plus joystick side;
legacy BB1 saves load with a left joystick. Invalid wrappers are ignored atomically.
No native gameplay or input rule is added.

Validation: JoystickTest checks native pointer ownership, cancellation, responsive
placement, dead zone/clamping, projected motion, actual driving/coasting tutorial
completion and preference migration. JoystickReview exports the actual shared
renderer at 960 and 1333 logical widths. Native rasterization and touch latency
still require device testing on each host.

## Forward and reverse selection

Cyan segments show the forward selection region; amber segments show reverse.
Both are projected from the same world-space thrust threshold (the rear 120-degree
cone selects reverse) and rotate with the tug heading. They ease out while a finger
is held and back in after release/cancel, on fixed simulation ticks. The label
beneath the pad shows the selected FORWARD or REVERSE while a drive gesture owns it.

The first deliberate input chooses direction for the whole gesture: beyond the
joystick's seven-point dead zone, or when direct tug drag enters its existing
42-world-unit steering threshold. This selection occurs on pointer input, so a
second move before the next simulation tick cannot replace the first intent.
Heading changes, crossing the colored boundary and returning to neutral do not
change it. Neutral still cuts thrust; steering and proportional speed stay live.
Release and press again to change direction. Cancel, pause, resize, settings,
background and reset all clear the choice. Cargo pushing/tow-lasso classification
is unaffected; this latch is exclusively manual tug driving.

`DriveDirectionTest` covers both joystick sides and direct tug dragging, initial
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
