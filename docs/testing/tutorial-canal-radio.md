# Tutorial canal radio and escort

The canal mouth has a physical bascule bridge. Approaching it does not open it.
Tap the highlighted bridge house to call on channel 08, then choose passage or
standby in the radio dialogue. Standby keeps the bridge closed. Passage starts
the existing timed, continuous rigid lift; the green signal marks clearance.

Instruction and dialogue panels identify the transmitter. The star pulses while
waiting for an operator or player reply, and stops when the reply is selected.
After clearance, the green instructor tug enters first and radios “Follow me.”
It follows the rectilinear canal route and waits 44 world units beyond each of
the eight lines, advancing only when the player reaches that milestone. Leaving
the canal resets the trailing instructor's breadcrumbs to avoid backtracking.

`CanalRadioTest` covers actual shared pointer calls, cancellation, standby,
authorization, pause, continuous bridge travel and all eight escort waits at
three canvas widths. The native UI walkthrough now includes the house call and
reply. `TutorialRadioReview` exports production renderer frames for radio,
operator, opening, following and milestone waits at 960 and 1333 pixels.
Developer simulator scenes have corresponding `canal-*` names.

Validation for this increment: full shared/asset/pixel checks, Android APK build,
and native/JVM parity (`1bb85bd0:2:1350`); iPhone 17 simulator build and native
operator/milestone visual inspection. The full native gesture walkthrough was
updated but not rerun. No physical phone installation or validation: the user
reserved it for another agent.
