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
uses its span X axis. Cargo cranes use vertical Z separation between their
fixed pedestal/counterweight and upward-luffing jib; cables and payloads have
independent depth events. Transformed bounds validate separation on every update;
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

## Repeated depth slices

A single bridge pose may be visited hundreds of times while the renderer merges
vessels, ropes and particles into the scene. Cache each transformed polygon's
exact min/max `(worldY - worldX)` at pose update, and the enclosing pose range.
Reject nonintersecting intervals before scanning faces. Do not recompute vertex
bounds for every particle, quantize moving poses, or reorder the geometry to
save work. Refresh bounds for every new rigid transform and every clipped face.
`tools/BridgeDepthReview.java` compares exact primitive/UV/color order against
a separately compiled previous implementation. The first comparison covered
4715 intervals and185215 commands with identical raw-float hashes.

## Marina fleet

`HarborBoatModel` prepares six single-part local BSPs at Montrose setup.
`ArticulatedModel.Rigid` traverses those BSPs with the inverse continuous viewing
vector and projects yaw/pitch/roll into reusable thread-local buffers. It does
not build a pose table or split topology during drawing. Normals and registered
cleat/fender anchors use the hull coordinate system (+X bow, +Z up). Water-plane
clipping removes geometry below the moving surface during rocking; foreground
shore clips remain active. Hull movement belongs to MooredFleet's fixed step.

MooredBoats blends to live geometry at 48–72 logical pixels of hull length,
within 180–210 world units of the player. Smaller/distant boats keep existing
sprites and their cache behavior. At full live weight, no color/canvas/normal
sprites are submitted. Both paths retain separate water ripples. The parked
fleet has no emissive lamps or smoke. Sailboats have no canopy in either path.

Live marina hulls use shared `BoatMaterial` vertex lighting:
wrapped diffuse, smooth hull normals, a broad low-strength highlight and soft
waterline shading. Cabin bevels catch light without a hard outline. Nearby dock
pedestals, harbor beacons and active vessel lights remain receivers' sources;
never replace this with an unlit stylistic tint. Positions and normals use the
same continuous rigid transform. Original-face vertices share cached colors
within each draw, including BSP fragments, and water clipping interpolates
premultiplied color. Cabins, fabric and small metal parts retain dynamic face lighting to bound cost.

`Painter.shadedTriangle` carries shared colors to native interpolation on Vulkan,
Canvas and Metal. Java2D uses a barycentric paint; Core Graphics uses the bounded
16-triangle shared approximation. This reference fallback is not pixel-identical
to hardware interpolation. No texture atlas or offscreen surface is created for
the material. BoatMaterialTest checks lamp reception and normal response;
VertexShadePixelTest checks color/alpha interpolation, and VulkanEncodingTest
checks premultiplied vertex transport. This is procedural shading, not a painted
texture replacement or proof of native iOS visual parity.

### Production rollback, 2026-11-23

The user rejected the live marina/active cruiser appearance. Production now
uses sprites at every zoom, with no HarborBoatModel preparation at stage start.
The live helpers and material studies are retained for offline comparison only.
Do not reactivate them without a new art review. Boat sprite depth and registered
canvas/normal layers are now baked with a supersampled per-pixel depth buffer;
never restore average-face sorting for overlapping roofs and hulls.
