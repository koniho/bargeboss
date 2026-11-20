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
All replies retain the complete driving curriculum. The teacher first clears the student's departure line, then follows a bounded
breadcrumb trail at about 155 world units, with extra room for each towed load.
It skips redundant route points only after a whole-hull water-clearance check,
waits for bridges, and stops near the student or cargo. It never teleports across
a lesson boundary. It uses the existing vessel depth,
shore clipping, collision, wake, smoke, navigation and surface lighting paths.
Restarting practice repeats the radio opening; isolated lesson fixtures may call
`Tutorial.setup` directly. `ReplayInput.tutorial` answers through public pointers.

The green teacher livery is a cached `SpriteGrade` palette variant of the approved
32-pose tug sheet. It shares geometry, pivots, shadows and effect anchors; no
rotated bitmap or duplicate sprite sheet is introduced. Only the tutorial asset
manifest prefetches those extra 32 graded frames. Green paint never recolors
runtime red/green/white navigation lights.

Player contact triggers a nonmodal warning in the instruction panel for three
seconds. A seven-second cooldown prevents repeated bump chatter; driving stays
available. Both teacher and player transmissions draw soft expanding arcs at
the registered mast anchor, within the normal vessel/shore depth scope.

`RadioAudio` emits one-shot semantic receive/reply events. Android SoundPool,
iOS AVAudioPlayer and desktop Java Sound play the same short PCM clips. These
original squelch/static cues are reproducible with `tools/generate-radio-audio.py`.
No speech recording or external samples are used. Hosts suppress stale events,
stop on pause/background/audio interruptions, and never advance dialogue.

Validation: `TutorialEntranceTest` covers approach clearance, reply branches,
modal pointer ownership, cancellations, pause, 1–4 choices and handoff.
`TutorialEscortTest` drives the complete course with the follower, checks
physical contact warnings/cooldown and speaker-specific animation/lifecycle.
`DialogueReview` exports approach, question, acknowledgement, handoff and compact
four-choice frames from the actual shared renderer. Native smoke tests answer
the opening before attempting the driving course; full iOS gesture validation
remains manual under the development CI cadence.
