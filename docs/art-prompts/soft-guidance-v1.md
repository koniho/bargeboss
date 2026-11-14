# Quiet gameplay guidance and shoreline depth

All shoreline caps belong to the cached terrain pass, before vessels, road
vehicles and factories. Do not redraw the lower bank caps after a vessel: their
raised projection cuts across painted hulls close to shore. Buildings and boats
still share far-to-near ordering, while the complete shoreline remains beneath
both. Collision geometry continues to keep vessels inside the water.

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

`SceneCueTest` checks shoreline/vessel/factory ordering across tutorial phases and
viewport sizes, transparent open guidance and bounded stroke thickness.
`SoftCueReview` exports wide-phone driving, pushing and lasso captures through
the real shared renderer. The full render-budget checks remain enabled.

Connected towlines and their release recoil also belong below the vessel sprites.
`TowView.drawRope` runs in the world pass; `TowView.draw` draws only interaction
cues afterward. Keep this separation in both tutorial and regular stages.
