# Specialized barge abilities

Body.ability identifies equipment independently of cargo destination and tow
slot. The tutorial's original first hull stays ordinary; both barges in the
train lesson carry fireworks. A separate Memphis-inspired fire barge waits in
the southern basin. The collection basin is now 700 units between bridges (y400 to -300).
The later train channel and exit basin move upstream by 500 units.

Short press (<12 simulation ticks without drag/push) on an equipped barge
selects it. The chart and zoom slider crossfade to equipment controls over ten
fixed ticks (about 0.17 seconds). Tap the same barge again to deselect; there is
no Back to Map button and no selection timeout. A lost/inactive barge also returns
the chart. Reselecting during a fade reverses from the current opacity.

The equipment panel occupies the chart's HUD space, opposite the configured
throttle side. Fireworks use one Launch button, with a three-second sequence and
four-second retrigger cooldown. The fire monitor uses a circular aiming pad:
hold for 12 ticks to pump, drag in the desired on-screen direction, and drag
farther from center for more reach (40–210 world units). Direction is unprojected
through the shared camera; camera zoom does not change the control's reach.
The center initially pumps forward. Release stops water. A second finger can
operate the hose while the throttle remains held.

HUD input never falls through to vessels or chart zoom while equipment occupies
the slot, including the short fade-out. Hull gestures outside that slot retain
assisted pushing and lasso priority. Equipment actions never alter tow topology;
tap the towline itself to disconnect. Pause/background, reset and switching or
deselecting equipment cancel a held nozzle. Two rotating monitor barrels emit
ballistic water stamps; real water impacts extinguish the tutorial's two piles.

Particles are fixed-step, deterministic and bounded to 320 slots. Rockets rise
before spawning bursts. Airborne effects merge with collision spray using
waterplane depth and shore occlusion. Native hosts own neither selection nor
ability state. Upcoming tutorial assets explicitly prewarm both equipment
families and their shadow/shading derivatives. Desktop screenshots are generated
by `BargeAbilitiesReview`; touch/lifecycle tests are `BargeAbilitiesTest`.

An unused short tap in the play area also deselects equipment; active HUD, radio,
vessel and rope targets retain their input priority. A fully offscreen selected
barge automatically deselects and stops its nozzle. Partial hull/equipment
visibility keeps selection. Both use the same animated return to the chart.

Water contacts convert existing stream particles into short directional splash
stamps on timber (including charred wood), quay ground and floating docks.
Impact stamps do not deal repeated extinguishing credit. Contact steam remains
separate for burning timber, and both effects use shared depth/shore clipping.
