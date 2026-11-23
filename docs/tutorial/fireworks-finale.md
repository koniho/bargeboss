# Victory fireworks and the unplanned final lesson

The convoy/reconfiguration exercises use two fireworks barges. A separate fire
barge waits at(-350,-1050) in a separate basin south of the celebration area
and is not part of that convoy. The basin spans(-500,-1190) to(-150,-850).
A160-unit-wide dogleg canal leaves the celebration basin at(170,-1350),
turns at(-50,-1350), then at(-50,-1050) into the rescue basin. The peninsula
prevents towing directly across land. This remains one continuous scene;
water rectangles, collision outline, retaining walls, ground cutouts and chart agree.

After the player clears the narrow channel and uncouples both fireworks barges:

1. The instructor congratulates the captain and requests a tiny victory display.
   Tap the fireworks barge, then Launch in the chart replacement panel. Merely arriving or
   waiting cannot complete this stage; earlier equipment use does not count.
2. A large gold/cyan/red display launches. A stray ember follows an arc to the
   dock at(448,-1160), igniting it140 simulation ticks after activation.
3. “THAT WAS NOT THE TINY BUTTON.” The instructor asks the player to collect the
   separate fire barge through the winding south canal. Connect it to the tug and tow it into the target near
   (330,-1230), returning through both bends. Guidance follows the canal and the
   camera stays local during retrieval; existing boats are not teleported at a lesson boundary.
4. Hold the fire barge's HUD aim pad, drag toward the flames and adjust reach with
   drag distance. The nozzle range remains210
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

The ignition point now has a permanent large timber stack on pallet sleepers.
Its fixed shared XYZ geometry is prepared at tutorial setup, and its contact
shadow, dusk-colored faces and the existing fire share ground-depth ordering.
The entire footprint stays above the quay edge; no new water collision obstacle.
FirewoodPile owns the fixed stack anchor at (463,-1160), grounded at LAND_RISE.
No lamps or exhaust outlets belong to this prop. Ignition/glow and fire smoke
remain separate animated effects anchored at FireworksLesson.X/Y, never baked
into wood. The pile remains visible before ignition and after suppression. Ignition selects
a separate prepared charred model with blackened beams and ash on their ends
and split grain; this state persists after steam expires and resets with tutorial
setup. Both variants retain the same footprint, pivot and stack height.

Tow reconfiguration explicitly introduces a narrow passage: two abreast will not
fit. Disconnect one tug-to-barge line, connect that free barge to the other barge,
then tow single file and clear the exit with both hulls. Recovery instructions
retain this reason when one or both towlines are missing.

Equipment instruction prompts begin at SHOW, after both train barges reach the
celebration basin and are cast off. Selecting equipment earlier must not replace
bridge, towing or train-reconfiguration coaching. The tutorial's two tow bridges
remain physical/radio-operated; there are no legacy lock doors between them.
