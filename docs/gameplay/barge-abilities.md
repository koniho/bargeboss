# Specialized barge abilities

Body.ability identifies equipment independently of cargo destination and tow
slot. The tutorial's original first hull stays ordinary; the persistent pair
beyond the first called bridge carries fireworks and Memphis-inspired fire
pumps. The collection basin is now 700 units between bridges (y400 to -300).
The later train channel and exit basin move upstream by 500 units.

Short press (<12 simulation ticks without drag/push) on an equipped barge
reveals a pilot-glass equipment icon. A separate tap on the starburst launches
a three-second sequence, with a four-second retrigger cooldown. The action
does not change tow topology. A connected equipped barge also reveals a
broken-line icon: tap that to release its incoming line. Ordinary barges retain
tap-to-release. Hold/drag on hull still means assisted push or lasso; holding an
equipment icon owns a separate pointer and cannot become a hull gesture.

Hold the fire nozzle icon for 12 ticks and drag to aim in world coordinates.
Two continuously rotating monitor barrels emit detached ballistic water stamps.
Release, cancel, pause/background, reset or scene change stops pumping. Pump
exhaust originates at its own local XYZ stack, never the tug's sprite anchor.
This is usable spray equipment; fire targets and extinguishing missions have
not been added in this round.

Particles are fixed-step, deterministic and bounded to 192 slots. Rockets rise
before spawning bursts. Airborne effects merge with collision spray using
waterplane depth and shore occlusion. Native hosts own neither selection nor
ability state. Upcoming tutorial assets explicitly prewarm both equipment
families and their shadow/shading derivatives. Desktop screenshots are generated
by `BargeAbilitiesReview`; touch/lifecycle tests are `BargeAbilitiesTest`.
