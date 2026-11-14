# Articulated industrial bridge

The movable crossing is a shared Java XYZ model. Its rusty through-trusses,
charcoal lattice mechanism towers, concrete abutments, warm base lamps and red
tower beacons follow `docs/reference/industrial-isometric-concept.png`. The
two-leaf bascule motion preserves the existing bridge challenge and also matches
the raised leaves in `docs/reference/barge-boss-concept.png`. This is an adaptation
of the concept's industrial structure to the game's existing hinged crossing.

`ArticulatedModel` is reusable for other rigid machinery. Named nodes have local
translation, ground yaw and an upward-pitch joint; child geometry and attachment
normals inherit the complete parent transform. Boxes and rectangular beams retain
their true dimensions. Meshes are projected with the actual camera's 35.264-degree
elevation: physical Z contributes `0.81649658 * Z * camera.scale` vertical pixels.
They do not use flat sprite yaw or the display-height convention of upright scenery.

The west leaf's local +X points across the water; the east bank's parent rotates
180 degrees, so its same local +X points inward. Both hinge origins remain at
`(bankX, Bridge.Y, 12)`. Every deck panel, truss, counterweight, lamp housing and
light normal rotates with its leaf. The mechanism towers sit 45 world units
farther inland; tests compare their actual mesh bounds with the complete moving
leaf geometry throughout all 121 opening poses, including the raised lamp casings.

The existing `Bridge` class still owns all timing and collision. A request waits
360 ticks, opening lasts 120 ticks, and the normal open window lasts at least
720 ticks. `lift >= .95` permits passage. Closing reverses when vessels approach.
The renderer only reads `phase`, `ticks`, `lift()` and `passable()`; it adds no
animation clock and never changes physics.

Faces receive shared directional shading over rust, charcoal, concrete and
safety-yellow materials. Convex-face BSP
splits resolve truss/deck crossings instead of sorting entire rigid objects by
their center. The renderer keeps only the latest exact-lift pose and its derived
BSP tree. Camera transforms, node visibility and overlap alpha are recomputed for
each draw; no vessel state, camera state or clock is cached. The bridge's actual
moving bounds use the same scenery translucency policy as other obstacles.

The code-authored emitter inventory is
[`art-studies/bridge-rig-v1/anchors.json`](art-studies/bridge-rig-v1/anchors.json).
Every point records its stable ID, parent, local XYZ, outward normal, color and
radius. These are exact model locations, not coordinates inferred from a bitmap;
source-cell UV is therefore absent. Warm lamps have organic shared-clock glow.
Navigation signals turn green only when the gameplay gate is passable. Warning
beacons remain red. Moving markers follow the complete leaf transform. A face
ray test suppresses halos behind bridge steel, and glow opacity follows its
parent's vessel/rope overlap fade. Smoke and steam inventories are explicitly
empty: this bridge has no modeled exhaust outlet.

`BridgeModelTest` checks rigidity, hinge positions, lights/normals, full-cycle
clearance, projected fade bounds, culling, deterministic output and bounded face
fragmentation. The phase images and animation use the actual shared renderer.
JVM timing excludes native Canvas/Core Graphics raster costs; real-device testing
is still needed for those platform-specific costs and antialiasing.

Regenerate the exact anchor inventory after changing fixtures:

```
javac --release 8 -cp build/check -d build/check tools/BridgeRigInventory.java
java -cp build/check com.bargeboss.game.BridgeRigInventory > docs/art-studies/bridge-rig-v1/anchors.json
```
