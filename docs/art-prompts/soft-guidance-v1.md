# Quiet gameplay guidance and shoreline depth

The shoreline cap sections in the continuous tutorial participate in the same
far-to-near ordering as vessels, road vehicles and factories. The cached terrain
still paints the ground and all bank edges first. Short foreground cap sections
are then merged by world depth, so they can cover a hull at the bank without
being composited over a nearer factory roof or chimney. Do not restore a single
foreground terrain pass after all buildings.

`SoftCue` defines the shared gameplay guidance style: translucent feathered
strokes, unfilled arrowheads, diffuse dots and slow visual-clock breathing.
Stroke widths stay thin through docking zoom. Rings leave their centers empty;
colors and progress fractions still communicate target and hold state. Cached
hull bloom breathes at a restrained opacity rather than flashing opaque gold.

Reduce competing instructions as well as their opacity: only the current
training buoy has a ring, the idle tug has no permanent ring, steering replaces
the tutorial's demonstration arrow, and a live cargo drag replaces the animated
finger demonstration. Only the closest actionable barge shows a readiness
label. Physical ropes, machinery and UI instructions keep their readable form.

`SceneCueTest` checks shoreline/factory ordering across tutorial phases and
viewport sizes, transparent open guidance and bounded stroke thickness.
`SoftCueReview` exports wide-phone driving, pushing and lasso captures through
the real shared renderer. The full render-budget checks remain enabled.
