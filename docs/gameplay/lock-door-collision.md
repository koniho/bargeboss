# Lock doors as watercraft collision targets

`LockDoors` supplies the hinge positions, leaf length and opening angles used by
both `LockView` and collision. Each panel has a six-unit padded segment footprint.
The collision target follows the visible panel through its swing and remains
present when the door is folded beside the bank. A central gap becomes physically
clear as the panels move away; there is no invisible full-width wall across a
visibly open gap. Green signals and objective timing still wait for the normal
fully-open lock phase.

`Lock.collide(Watercraft, CollisionSplashes)` handles three-disc vessel hulls and
circular buoy/channel-marker bodies. Fully sealed doors retain a continuous-face
hull solve so a rotated hull straddling the seam stays wholly on its center's
side. Swinging and folded panels use their individual footprints. Iterative
contact correction also respects river banks. Collisions resolve position,
rebound inward velocity and emit the shared directional splash stamps at the
actual panel surface. Buoys also receive contact rocking.

The main simulation checks gates after integration and again after hull and buoy
contact resolution. This prevents a barge from pushing another craft through a
door after its initial gate check. Active feeder craft/loads and traffic use the
same target; floating marks check the doors during their own motion/contact
steps. Current entrance markers are placed outside the door sweep. Future buoy
mooring anchors must also leave room for both the float footprint and its tether
excursion; do not place a fixed mooring inside moving machinery.

Towline casts use the same leaf geometry, including panels folded along the
banks. Failure freezes the door pose without deleting its collision target.
Rendering never updates collision state, and all geometry and collision rules
remain shared Java across platforms.

`LockDoorCollisionTest` checks both gates and both leaves at five opening poses,
all 32 tug/barge headings, every buoy size, agreement with rendered rigid-part
endpoints, bank clearance, actual contact splashes, open-center passage, towline
obstruction, frozen failures and the post-contact gate check. Existing lock
cycle/current and goal-completion replays remain required.

`LockDoorReview` exports six actual tug/barge contact snapshots to
`out/lock-door-collision-review.png`: both gates closed, mid-swing and fully folded.
