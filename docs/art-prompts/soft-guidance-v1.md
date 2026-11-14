# Quiet gameplay guidance and shoreline depth

See [the full drawing-order contract](../rendering/drawing-order.md) for the
authoritative surface, height, orientation, clipping and fading rules.

Lower shoreline caps are solid foreground geometry: they occlude watercraft
and never fade. Short sections join the same far-to-near world ordering as
vessels, road vehicles and factories, so a cap can cover the hull at the bank
without being painted over a nearer factory roof or chimney. The underlying
ground and bank walls remain cached. `ShoreOcclusion` clips complete foreground
land coverage from vessels and ropes; cap sorting alone cannot hide a hull
that projects over the bank. Only eligible buildings whose painted
silhouettes actually obscure watercraft fade; shorelines are not buildings.

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

Connected towlines and release recoil are split into 24 spans, sorted into the
world pass by their attachment-dependent ground depth. The stern fitting follows
the tug orientation and the chosen barge end follows its heading. Deck height,
bob and vertical sag affect projection; near spans appear in front of a hull and
far spans behind. Guidance stays separate from this physical rope pass.

Building overlap computes a target opacity at the fixed 60 Hz step. `BuildingFades`
retains opacity by world placement, fading out over 18 steps (0.3 seconds) and
back in over 27 (0.45 seconds). Rendering only reads this state; repeated draws
cannot change it and pause freezes it. Only the exact opaque vessel/cargo and
building masks trigger fading. Wind, rope, lights and shoreline do not.
