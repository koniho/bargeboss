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
