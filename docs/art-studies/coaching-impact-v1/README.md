# Coaching and arcade impact
Shared renderer previews: praise.png, encouragement.png, effects.png.
Reproduce with tools/CoachingReview.java and tools/ArcadeEffectsReview.java.

Each earned lesson queues a four-second instructor message. Hard tug impact
(relative impact25+) or a slip retry selects encouragement; otherwise the message
praises the specific task. No time-based grade. Explicit lesson skips earn no
praise. Re-completing a fallback does not enqueue the same award twice.
The bounded queue continues on the final completion screen and pauses during
modal dialogue. Driving remains available; these messages add no answer button.
Instruction text retains its fixed position and existing CRT transitions.

Bright yellow success sparks and rings, cyan guidance and red retry/damage
replace muted cue colors. Core widths2.8–5.5 pixels and capped glow preserve
open centers and readable vessels; native shaders/materials remain unchanged.

Canal teacher advances about one reach sooner, follows the same rectilinear
route faster and can use a water-checked side-passing maneuver after being
overtaken. The passing direction remains fixed until the stern clears the
player, avoiding corner re-aiming traps. No teleporting or intentional player
position changes. CanalLeadTest checks recovery with actual hull collision
resolution and zero player displacement in the stationary-player regression.
