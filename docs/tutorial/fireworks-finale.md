# Victory fireworks and the unplanned final lesson

The convoy/reconfiguration exercises use two fireworks barges. A separate fire
barge waits at(365,-1400) in the final basin and is not part of that convoy.

After the player clears the narrow channel and uncouples both fireworks barges:

1. The instructor congratulates the captain and requests a tiny victory display.
   Tap the fireworks barge, then its single rocket button. Merely arriving or
   waiting cannot complete this stage; earlier equipment use does not count.
2. A large gold/cyan/red display launches. A stray ember follows an arc to the
   dock at(448,-1160), igniting it140 simulation ticks after activation.
3. “THAT WAS NOT THE TINY BUTTON.” The instructor asks the player to collect the
   separate fire barge. Connect it to the tug and tow it into the target near
   (330,-1230); existing boats are not teleported at a lesson boundary.
4. Hold the fire barge's nozzle button and drag toward the flames. Aiming at the
   visible flames resolves to the elevated dock target; nozzle range remains210
   world units. Simulated water particles remove fire heat on impact. Wrong aim,
   out-of-range spray, selection alone and idle time earn no extinguishing credit.
5. The fire shrinks, turns to steam, and the instructor declares that we have
   invented wet fireworks. Only then does normal tutorial completion occur.

Equipment UI has one primary action. Tap a towline itself to disconnect; its small
midline release cue appears during release lessons or when its barge is selected.
A drag, cancelled touch or long hold on a rope cannot release it. Plain cargo's
legacy short-tap release remains compatible; specialty barge taps select equipment.
No equipment button changes tow topology.

Fireworks use the existing glow material, water uses the splash stamp, and the
shore fire enters the shared ground-depth effect merge. Smoke uses the existing
bounded drifting pool. The ability pool is capped at320 particles (formerly192);
there are no new textures or live raster builds. State advances at60Hz; pause,
reset and cancellation follow normal shared rules.

Validation: FireworksLessonTest, full public-input convoy/continuous replays,
BargeAbilitiesTest, actual renderer frames from FireLessonReview. Native visual
and frame-time measurements are separate from shared trace validation.
