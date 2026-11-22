# Arcade finale review

Actual shared renderer,1333x600, driven by public input via `FireLessonReview`.

- [Fireworks](fireworks.png): gold/cyan/red bursts, ballistic comet trails,
  feathered cores, white-hot heads and brief expanding flash rings.
- [Mishap](mishap.png): the stray ember ignites a localized shore fire.
- [Firefighting](firefighting.png): curved red/gold/cream flame tongues, rising
  embers and drifting smoke, with the camera framing the fire away from the map.
- [Complete](complete.png): actual water impacts extinguish the fire, steam
  dissipates, and the instructor congratulates the captain.

The pool remains320 slots including burst flashes. Trails use analytic ballistic
history, three segments per particle, instead of per-frame history allocations.
All timings are shared fixed-step simulation time. These desktop images do not
substitute for native performance or pixel checks; see optimization.md.

[Native Android fireworks capture](android-fireworks.png) verifies the trails,
bursts and shore fire in the actual Vulkan scene. The separate firefighting
screenshot was interrupted by an app switch and excluded. Short performance
samples and limits are recorded in optimization.md.
