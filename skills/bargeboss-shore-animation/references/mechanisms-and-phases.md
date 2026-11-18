# Shore mechanisms and gameplay phases

## Unloading equipment

Use the current game's authoritative states rather than inventing a second
animation state machine in a platform adapter. Typical visual phases are idle,
approach/ready, reach or lower, attach/grab, lift/transfer, release, return and idle.
The current unload duration is three seconds **after** mooring and shore-parallel
alignment; verify the code's exact start/end conditions. Fit the visual subphases
inside that duration, not three seconds per subphase.

Define the crane/forklift/crew base ground anchor, boom hinge, cable head, hook,
payload attachment and destination contact. The payload should stay at the hook
until release. Keep the idle→work and return→idle endpoints compatible. Define
how a cancelled, interrupted or reset job selects a safe visual state from shared
game state; don't let a renderer's previous frame determine it.

Draw separate layers when needed: ground/base, moving arm/vehicle, cable/hook,
payload and foreground guards. A single sheet may suffice for a short closed
sequence, but do not bake background buildings or barge hulls into it. Grounded
equipment stays on shore while floating cargo follows the water until pickup.

## Bridge and gates

Define closed, warning, opening, open/waiting and closing endpoints using the
actual game rules. Rotate bridge leaves about a fixed hinge or translate a lift
span along a fixed shaft; do not slide the whole structure between cells. Towers,
pier foundations and approaches belong to static scenery layers when possible.
Art opening must correspond to the shared traversability state; never let a
native animation decide whether a vessel can pass.

### Live 3-D runtime motion

Prepare part-local geometry and reusable transform buffers during loading. Keep
all faces needed as the part turns; update rigid matrices and face normals from
the current phase, without rebuilding geometry or holding sampled poses. A
five-second bridge with 33 poses only moves visually about 6.4 times per second,
even when gameplay runs smoothly. Sprite-sheet frame counts do not apply here.

For the current `ArticulatedModel.Motion`, independently animated geometry nodes
must stay separated along an authored root-local axis throughout their travel.
Validate this assumption instead of using object-center sorting; use another
correct depth strategy for overlapping mechanisms. Maintain world-depth slices
against craft and ropes, plus part-parented light/smoke/shadow anchors.

Review every active tick and arbitrary fractional phases, including reversal,
rigidity and newly visible faces. Compare with exact geometry and profile an
actual moving scene. Follow the checkout's `docs/rendering/live-3d-scenery.md`;
record evidence and device limits in `optimization.md`.

## Conveyors, lights, steam and crew

Continuous belt motion should loop without translating its frame. Use separate
load or belt surface motion if it saves repeated static geometry. Lamps and
furnace glow can pulse without changing fixture locations. Smoke/steam emission
points belong to source equipment, not the top edge of each crop.

Workers need consistent scale, foot contacts and routes across approach, task
and return. Do not generate crowds that obscure lasso targets or cargo readiness.
Small, readable poses generally matter more than tiny facial detail. A forklift
has wheel contact and mast/carriage pivots, not a floating-vessel waterline pivot.

## Review evidence

Check the geometry guide itself for correct depth/occlusion. In the watercraft
trial, sorting whole faces hid equipment behind a broad deck even though it was
above the deck; the generated art reproduced that mistake. Mechanisms crossing
platforms, booms and facades need per-pixel depth or otherwise correct occlusion,
not a simplistic object-center draw order.

Retain the pose/phase specification, generated source sheets, actual dimensions,
alpha/gutter checks, per-part anchors and a full cycle preview with phase labels.
Test the first/last frame of each transition. Check the full moving silhouette
for dynamic occlusion, not only the idle machine's bounds. Record which conditions
were checked on shared renderer versus a real device.
