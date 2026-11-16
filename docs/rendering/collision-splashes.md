# Directional collision stamps

`Watercraft` is the shared contact-effect participant implemented by `Body`
(tugs, barges, feeder loads/boats and traffic) and `Buoys.Buoy` (yellow practice
buoys and red/green floating channel marks). Buoys keep their mooring physics;
they do not need a vessel-shaped hull to use the same collision effects. Fixed
shore beacons retain their dry-land foundations. Contact against the water-facing
bank beside them uses the shoreline collision path rather than making a land
beacon float.

`CollisionSplashes` receives actual contact locations and point velocities from
the collision solvers, before impulses change those velocities. Hull point
velocity includes angular motion. Bank, quay, tutorial shoreline, bridge and
lock gate collisions report through the same path, independently of damage or
player ownership. Floating marks also report when their footprint hits a bank;
reaching a mooring-chain limit alone does not count as a water collision.

Lock door targets follow the visible rigid leaves in every pose, including their
folded bank-side positions. See [lock door collision](../gameplay/lock-door-collision.md)
for geometry registration, all-watercraft coverage and post-contact resolution.

## Direction and lifetime

The faster incoming participant is the striker. Its velocity relative to the
other participant determines spray direction; equal-speed ties use world
position to remain independent of solver argument order. Reversing therefore
sprays in the direction of impact, not the vessel's sprite heading. Normal
closing speed controls strength; its ratio to relative speed controls fan
width, so glancing contacts produce a narrower burst. All six stamps launch
inside that forward-facing fan.

Contacts below three world units/second are suppressed. The same unordered
participant pair has a fourteen-tick effect cooldown, preventing the three-disc
solver and two motion substeps from emitting duplicate bursts. A renewed impact
after the cooldown can splash again. Static surfaces use a null second
participant, sharing one cooldown per vessel to avoid corner-contact spam.

Effects advance once per real 60 Hz tick, outside the doubled vessel-motion
steps. Detached stamps keep their own position, velocity and ballistic height;
moving or turning the source cannot move an existing splash. Horizontal travel
stops/damps when it reaches dry ground while spray can still rise at the edge.
Pause freezes the pool, finish screens let existing bursts retire, and stage or
tutorial reset discards all contacts. Rendering and camera visibility never
advance or emit effects.

## Artwork and composition

Use the shared procedural foam material on Metal/AGSL and the existing
`assets/sprites/vessel-foam-v1.png` fallback elsewhere. No extra atlas is needed.
The arcade treatment adds a short radial flash and heavy-hit hot streaks.

- Surface foam uses streak frames 0/1, expands along the impact direction and
  fades over 72 ticks. Draw it with water before all vessels and scenery, clipped
  against all dry ground.
- Airborne spray uses rolling foam modes 2/3 (the corresponding texture frames
  on fallback painters). Stretch
  each irregular stamp along its projected velocity and lift it by its ballistic
  height. Vary sizes deterministically within the burst.
- Merge airborne stamps into the existing far-to-near vessel, navigation-mark,
  rope, shore, gate and factory order using each stamp's current **water-plane
  position**, not its raised screen Y or its source vessel's depth. Apply the
  same foreground solid-shore clip as upright objects. Spray never triggers
  building fading or becomes a late HUD overlay.

The fixed pool contains at most 24 bursts, six airborne stamps per burst and one
surface stamp per burst. Offscreen stamps are culled. A ten-tick impact flash
uses one disk and seven short rays per burst. At closing speeds of at least 45.5
world units/second, each airborne stamp also carries a two-stroke orange/yellow
spark that cools within 24 ticks. These use the same depth and shore scope as
the spray, never a late overlay. No new particle pool or texture cache is added.
Physics impulses and trajectories are unchanged.

## Validation and review

`CollisionSplashTest` checks all 32 headings, solver-order independence, reverse
motion, glancing contact, angular point velocity, weak/separating contacts,
duplicate suppression, all floating marker variants, feeder and fixed-geometry
contacts, retirement, pause/reset, 30/144 Hz agreement, depth, balanced clips, heavy-hit spark threshold and cooling. `FoamPixelTest` also checks collision surface stamps against raised
tutorial land on each bank.

`CollisionSplashReview` captures four actual collision cases through the shared
renderer: vessel/vessel, yellow buoy, red channel marker and quay. Output lives
in `out/collision-splash-review.png` and `out/collision-splash-motion/`.
