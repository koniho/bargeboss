# Articulated bridge validation

The preview uses the production shared `BridgeView`, `BridgeModel` and
`ArticulatedModel`; no separate rendering or animation implementation is used.
The inspection background isolates the bridge, while the stage PNG uses the
complete production `Renderer`. Desktop antialiasing matches the desktop host.

## Geometry and behavior checks

`BridgeModelTest` passed 277,287 assertions after the mechanism towers were moved
inland. It checks stable node IDs and parent order, fixed hinges, constant span
length, pairwise rigid geometry distances, expected tip rise and horizontal reach,
every lamp's position/normal transform, deterministic output, gameplay purity,
projected obstruction fading and offscreen culling.

Actual mesh XYZ bounds of each moving leaf—including beams and light fixtures—
remain more than one world unit clear of the fixed tower's inward X plane at
every one of the 121 integer bridge opening ticks, on both banks. The towers are
separate fixed nodes, so the check covers their complete source geometry rather
than an assumed decorative envelope. The earlier towers intersected the raised
truss sweep; the current setback fixes that defect.

The complete 121-pose sweep passed without non-finite or degenerate projected
polygons. The model has 852 source faces, at most 1,705 BSP splits (tick 3), and at
most 2,131 painted fragments. The regression budget remains below 3,000 splits.
An experimental axis-plane-first insertion order increased the closed pose to
5,838 splits and was discarded; the tested source order is retained.

## Timing scope

One warmed Termux JVM sweep measured pose construction, BSP and projection at
1.213 ms mean, 1.058 ms median, 1.768 ms p95 and 11.610 ms maximum. Those numbers
exclude native painting/rasterization and are not a device frame-rate guarantee.
The model retains only one derived pose/BSP; camera bounds, opacity, light phases
and signals are evaluated from the current game. Tests confirm repeated renders
are deterministic and camera/obstruction changes are not frozen into that cache.

## Visual review

Inspected closed, waiting, quarter/half/three-quarter opening, fully open,
closing and vessel-occlusion views. Trusses, decks, tip markers and lamps rotate
as rigid parts; the tower beacon and warm foot lamps remain on fixed structures.
Signals change independently of the moving leaf. Lamps on hidden faces do not
shine through geometry. The fade exposes the tug underneath the bridge's screen
projection. A pixel probe across the closed deck confirmed continuous gray deck
coverage; the dark material can resemble water in a small contact-sheet view.

Minor subpixel antialias seams can appear where BSP splits a large coplanar deck
face, most visibly on the fully raised right leaf. They do not represent missing
mesh faces. Native antialiasing and device performance still require the normal
Android installation and iOS parity/device workflow.

Review artifacts:

- `BargeBoss-articulated-bridge-v1-contact.png`: nine phase/occlusion captures.
- `BargeBoss-articulated-bridge-v1-stage.png`: full game composition at half lift.
- `BargeBoss-articulated-bridge-v1-cycle.gif`: 48 frames at 12 fps, opening and
  closing over four seconds with live lamp animation.

All three were copied to the phone's Downloads folder with the same names.
The PNG cycle frames remain under ignored `out/bridge-rig-frames/`. Regenerate
with `tools/BridgeRigPreview.java`; measure the whole pose sweep with
`tools/BridgeRigBenchmark.java`. The parent workflow handles final full-suite,
native parity, build and installation validation.
