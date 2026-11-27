# Boost energy pickup v1

The November 27 correction calls for unmistakable arcade powerups instead of
ordinary painted navigation buoys, including a replacement for the basic ring.

Shared procedural artwork in `BoostPickupArt`:
- Hovering gold lightning prism, cream bevel, burnt-orange extrusion.
- Open cyan energy brackets, gentle vertical bob and four rising sparks.
- Three tapered comet trails on the projected water plane; shared-endpoint
  ribbon geometry avoids bright translucent joint beads. Painted ripple texture
  and a restrained cyan reflection connect the effect to the water.
- Four breathing inward gold chevrons keep the pickup center open.
- Collection contracts the core over 12 ticks and sends eight gold sparklets
  across a fading painted wavefront over 40 ticks.
- Collected pickups leave a quiet 24-mark circuit displaying the actual re-arm
  fraction; the core returns only when the gameplay cooldown expires.

All phases use shared simulation ticks. Upright artwork uses the existing ground
sort and shore cutouts; water ribbons remain under boats. No generic buoy sprite,
solid target disc, new texture, emitter pool, or physics/timing change.

Review through `tools/BoostPickupReview.java` (ready/catch/spent/recharging at
0.9 and 2.1 zoom). Native fixture: `montrose-powerup-review` for the ready state,
`montrose-boost-review` for collection. These are runtime artwork, not concept
images promoted as vessel assets.
