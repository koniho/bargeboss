# Continuous live 3-D scenery

Bridges, cranes, gates and future live machinery evaluate rigid transforms from
the exact shared animation phase. The game advances at fixed 60 Hz; renderers
draw the current simulation state. The watercraft's 32 directional sprites are
an artwork convention, never a limit on live 3-D motion. Do not round phases,
crossfade sampled poses or interpolate vertex positions between baked frames:
those approaches either stutter, ghost or deform a rigid mechanism.

## Prepare topology, transform motion

`ArticulatedModel.Motion` prepares a complete BSP in each geometry node's local
coordinates, including faces that are initially hidden. Each phase change
updates reusable vertex/normal buffers with the exact parent/hinge/translation
matrices. BSP traversal uses the current camera direction in local coordinates.
It does not construct/split a new BSP on every animation tick. Fixed scenery
continues to use a prepared immutable `Pose`.

This optimization has an explicit constraint: all geometry nodes must descend
from the supplied root, and independently drawn parts must remain disjoint along
the supplied root-local separation axis for their entire travel. The bridge kit
uses its span X axis. Transformed bounds validate separation on every update;
overlap throws rather than silently sorting parts by their centers. Partition
another mechanism appropriately or implement a correct different depth strategy
if it cannot meet this constraint. Do not force arbitrary intersecting machinery
through this API. Faces within each part still use full BSP ordering.

The returned live `Pose` is an ephemeral, thread-confined view: another
`motion.pose(value)` updates that same object. Do not retain it as a historical
snapshot or share it across render threads. The cache stores only derived render
geometry; gameplay state belongs to each `Game`. Rendering never advances it.
The older immutable `model.pose(value)` remains an exact reference path; it is
not a replacement for prepared motion in a new heavy live mechanism.

## One transform for every visible attachment

Parent lamps, smoke/steam outlets, cargo contacts and cables to stable named
rigid nodes with local XYZ and outward normals. Query the same exact pose as the
mesh. Project cast shadows from those transformed points. Do not independently
snap effect anchors to cached frames. Lamps remain subject to face occlusion;
moving machinery stays opaque. Preserve world-depth clipping against vessels,
rope segments and foreground shore; a complete bridge bitmap cannot represent
these interleaved depths. See [drawing order](drawing-order.md).

## Acceptance checks

- Verify every opening/closing simulation tick, endpoints, pause/resume,
  interruptions and reversal. A moving tip must advance every active tick;
  constant-speed phases must not hold then jump. Check arbitrary fractions too.
- Check rigid lengths, fixed hinges, lift alignment, normals, effect anchors and
  shadows against exact transforms. Preparation/storage must stay bounded over
  repeated cycles, with no per-phase topology accumulation.
- Compare projected moving geometry with the exact world-space BSP reference at
  representative phases and headings, including newly exposed faces. Review
  full-speed motion, not only closed/half/open stills.
- Measure an actual moving scene on Android and available iOS hardware. Separate
  low animation cadence from long gameplay frames; continuous geometry cannot
  conceal a renderer missing its frame budget. Record device limits and evidence
  in `optimization.md` without claiming native parity from JVM tests.

`LocationBridgeTest`, `ContinuousBridgeReview` and `LocationBridgeBenchmark`
provide the bridge examples. Keep the reusable rule in `AGENTS.md`, performance
rules and the shore-animation skill as the machinery catalog grows.
