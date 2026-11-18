# Cargo offloading

The existing 180 fixed ticks begin only after the mooring line has drawn the
barge alongside and aligned it. `CargoTransfer` derives all animation from
`Body.unloadingTicks`; rendering never advances a second timer. Pause/background
therefore freezes the operation, and reset has no lingering crane action.

| Ticks | Visible operation |
| --- | --- |
| 0–24 | Lower the empty hook; cargo remains opaque in the hull. |
| 24–54 | Lift the complete registered load on a rigid steel tray. |
| 54–108 | Luff the jib upward to carry the load inward onto shore. |
| 108–132 | Lower onto the dry receiving pad. |
| 132–150 | Release the sling and raise the empty hook. |
| 150–180 | Return the jib and hook to their original idle pose. |

Smoothstep travel preserves continuous positions at phase boundaries. The jib
rotates about its actual pedestal pivot. Its local sheave point drives the cable
end; the load has a fixed sling offset while attached. Small vessel heave is
blended out during pickup. The counterweight stays with the fixed pedestal.
This is a readable arcade batch lift, including a steel tray for bulk material,
not individual container or grab-bucket simulation. No existing cargo artwork
was regenerated, stretched, shrunk, or split into arbitrary sprite pieces.

Calumet matches `Body.destination`, not cargo type, so sequential jobs still use
the correct physical berth. Indiana and practice match the berth. Only an active,
secured barge with an advancing unload timer drives the rig. A delivered load
stays at the receiving pad after its hull leaves; the next operation replaces the
pad's displayed batch. Montrose's scenery-only boat hoist remains idle because
its current yacht jobs do not offload cargo.

`HarborCrane.cargo` builds the shared cargo-duty geometry. `DockView` prepares its
part-local BSP once, with a bounded location cache. Fixed and moving geometry
have a validated Z separating plane across all travel. Prepared topology has no
per-phase growth. The rig never fades. Cables, the rigid tray and independent
load projection use the same shared camera on Android, iOS and desktop.

Named lights `cargo-mast-worklight` and `cargo-tip-worklight` include parent node,
local XYZ and normal in `HarborCrane.CARGO_LIGHTS`. They use exact current poses,
face occlusion and the existing soft organic glow. Smoke/steam lists are empty:
these are electric cargo cranes. Cargo's own emitter categories remain empty.

The tray casts a separate soft footprint onto the receiver below its current
position. This is a bounded planar approximation, not a full crane shadow map;
receiver selection uses the footprint center. The pedestal's opaque base provides
contact. No raster shadow derivatives changed because the source sheets did not.

`BridgeLayers` slices crane faces alongside bridges through the common depth
stream, including the tutorial. Hook and cargo use independent ground-depth
events, with a small tie-break after the hull at pickup. Cargo remains coaming-cut
until it clears the rim. Suspended and landed cargo are excluded from vessel-only
scenery fade masks. Surface-light sampling uses the load's actual world location.

Validation: `CargoTransferTest` checks every phase tick, rigid sheave registration,
fixed sling offset, pad and mast clearance, source-layer handoff, return and
retained landing. Existing cargo tests cover all source headings. Run
`tools/CargoOffloadReview.java` with desktop/core classes for 181 actual-scene
frames and a contact sheet. The `calumet-offload` developer fixture approaches
through real mooring/step logic; use 60 warmup and 240 replay samples to include
pickup through return. Device results must be reported separately from JVM tests.
