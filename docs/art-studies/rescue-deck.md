# Rescue deck registration

The accepted painted hull remains unchanged. Equipment is a continuous rigid
model with one identity root and supports all headings, including 64 sampled
headings. No cargo mirroring is used.

Local deck dimensions are 34 by 21, with rise 8 plus hull heave. Benches are at
Y +/-8, rail posts at X +/-16, Y +/-10. The starboard aft ladder sits at X -12
and reaches Y 22. Boarding begins at (-12, hull radius +5) and climbs inward.
Passengers retain local seating coordinates and follow heading and heave.
Ring racks are at (+/-13,0); launch origin is (0,0), rise 10. Rings have continuous
yaw/pitch/roll, four reflective bands and a fixed-step ballistic arc.

Navigation lights and fenders inherit accepted hull anchors. The passive barge
has an empty smoke-stack set. Equipment receives shared world-space lighting,
including the tug spotlight. The registered hull shadow supplies the water
shadow; interior equipment lies within its footprint. There is no separate
equipment-to-deck contact-shadow map. Future painted derivatives must retain
matching shadow and material registration.

Lifecycle tests cover tow prerequisite, public fling input, misses, swimming to
the ring, return, climb, twenty aboard, and delayed injury/disappearance.
StormRescueReview exports actual renderer checkpoints. iOS device validation
was not performed in this round.
