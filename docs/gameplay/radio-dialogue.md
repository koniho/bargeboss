# Radio dialogue

`Game.dialogue` is the shared modal conversation controller. Call
`show(game, speaker, prompt, detail, choices...)` with one to four nonempty choices.
Keep prompts and replies short enough for the instruction panel; choice text is
fitted into the available chevron width. Each answer has a stable zero-based
index in `result()`; -1 means unanswered. The caller owns branching and calls
`close(game)` when it has consumed the answer. Opening a new prompt clears the
previous result and cancels drive, lasso, pinch and map touches.

The existing instruction-panel renderer supplies CRT startup, wipes and shimmer.
During dialogue, Pilot Glass chevrons replace map/throttle controls and the
Reset/Exit buttons. Only release inside the originally pressed choice commits;
dragging away, pointer cancellation, resizing or backgrounding cancels a pending
reply. Other fingers cannot select it. Back/Escape still opens pause. Dialogue
logic advances at the shared fixed step, never from drawing or native timers.

The tutorial's `TutorialArrival` starts the original player hull alongside the
south basin seawall. The teacher approaches over four seconds, asks the driving
question, acknowledges one of three answers, then hands over the same scene.
All replies retain the complete driving curriculum. The teacher moves to an
observer station outside the two buoy circles, holds there and turns toward the
student. It does not trace their maneuvers. When separation exceeds 520 world
units it relocates, stopping with roughly 300–320 units of clearance. Route
breadcrumbs are bounded to 1,024 points and used only for safe relocation through
canal corners, with whole-hull water-clearance checks and bridge waits. Leaving
the lesson area prompts a return radio call, at most once per fifteen seconds.
It never teleports across
a lesson boundary. It uses the existing vessel depth,
shore clipping, collision, wake, smoke, navigation and surface lighting paths.
Restarting practice repeats the radio opening; isolated lesson fixtures may call
`Tutorial.setup` directly. `ReplayInput.tutorial` answers through public pointers.

The green teacher livery is a cached `SpriteGrade` palette variant of the approved
32-pose tug sheet. It shares geometry, pivots, shadows and effect anchors; no
rotated bitmap or duplicate sprite sheet is introduced. Only the tutorial asset
manifest prefetches those extra 32 graded frames. Green paint never recolors
runtime red/green/white navigation lights.

Player contact queues a half-second reaction delay before a nonmodal warning in the instruction panel for three
seconds. A seven-second cooldown prevents repeated bump chatter; driving stays
available. Both teacher and player transmissions draw soft expanding arcs at
the registered mast anchor, within the normal vessel/shore depth scope.

`RadioAudio` emits receive/reply events through the shared `SoundEffects` bus.
Collision events select bump/thud/crunch at closing speeds 8 / 30 / 65 world
units per second; an 18-tick cooldown suppresses chatter and radio takes priority.
Collision sounds do not animate the radio mast. Android SoundPool,
iOS AVAudioPlayer and desktop Java Sound play the same short PCM clips. These
original squelch/static cues are reproducible with `tools/generate-radio-audio.py`.
No speech recording or external samples are used. Hosts suppress stale events,
stop on pause/background/audio interruptions, and never advance dialogue.

Validation: `TutorialEntranceTest` covers approach clearance, reply branches,
modal pointer ownership, cancellations, pause, 1–4 choices and handoff.
`TutorialEscortTest` drives the complete course with the observer, checks
physical contact warnings/cooldown and speaker-specific animation/lifecycle.
`DialogueReview` exports approach, question, acknowledgement, handoff and compact
four-choice frames from the actual shared renderer. Native smoke tests answer
the opening before attempting the driving course; full iOS gesture validation
remains manual under the development CI cadence.

## Continuous navigation curriculum

1. Leave the seawall: gentle then strong forward throttle with actual movement, left/right turns, then
   reverse. Highlight the throttle, temporarily showing it if disabled in settings.
2. Reach the rendezvous marker, then trace opposite windings around both buoys in
   forward gear. Repeat the figure eight with astern thrust. Radius tolerance is
   38–185 units, with a half-second grace outside the band; wrong gear, unpowered
   drift and position jumps cannot earn winding progress.
3. Approach the recessed western slip, point the bow away, and reverse inside.
   Contact at 25 units/second or above invalidates the attempt. The whole hull
   must leave the mouth by 20 units before another attempt. A clean attempt succeeds anywhere inside the slip once the tug center passes
   the mouth after powered astern travel and the player releases the drive control.
   Holding at neutral does not count as release. No precise depth or final heading is required.
4. The overview and zoom slider stay hidden until this map lesson. Exit to open water. The overview starts fully zoomed in; a real slider interaction must
   reduce it below 1.7x. Find and reach the amber chart target. No distant world
   arrow gives away this map-reading exercise; a local arrival ring appears nearby.
5. Navigate the canal, then push, berth, tow, and reconfigure the same barges.

The physical slip uses the same authoritative water rectangles/boundary for
collision, ground clipping, shore rendering and chart outlines. Progress never
replaces the hulls or teleports between lessons. `NavigationPracticeTest` covers
real wall-contact failure, exit/retry, required slider interaction and arrival.
