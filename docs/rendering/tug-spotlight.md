# Tug spotlight receivers

Shared state stores on/off and tug-relative heading. The 480-unit beam has a
smooth 0.10-radian penumbra within a 0.28-radian half angle and squared distance
attenuation. Turning the tug rotates the light without changing relative aim.

- Water uses a bounded 4 by 10 feathered mesh, stopped at dry terrain.
- Painted watercraft, cargo and buoys remove directional surface darkness.
- Moored boats and swimmers use existing normal lighting and its four strongest
  source budget.
- Machinery, bridges and workers receive world-space normal lighting through
  SceneryLighting. Rings and rescue equipment use Rigid.drawSolid; Rigid.draw
  retains boat water-plane clipping and receiver-local shading semantics.
- Dock tops and shore walls use dry-only geometry and shoreline visibility tests.
- Painted factories, props, trees and land markers use registered albedo receiver
  frames at their original depth and opacity. Trees retain their exact sway.
  Frames are at most 256 pixels per edge, prepared before gameplay; moving the
  beam never invalidates terrain caches. SpotlightReceivers extracts them from
  approved source art rather than generating new scenery.

Painted scenery samples beam energy at its ground anchor. It has no per-pixel
geometry or full 3D shadow map. Geometry receives directional Lambert shading.
Discovery and shore lights retain terrain visibility checks; surface illumination
does not ray trace the whole scene. Buildings do not cast accurate beam shadows.

Keep future receivers on these paths. UI, portraits, guidance and emissive lamps
retain semantic colors. Never switch global lighting profiles per receiver,
prepare images in active draws, or bake moving light into static terrain caches.
