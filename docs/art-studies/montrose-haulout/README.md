# Montrose haulout and dockmaster

The user's annotated harbor map places the haulout on the north shore between
Dock N and Dock M, immediately north of the enlarged yacht slip. This is the
placement reference, not a claim that the game reproduces a surveyed operating
boatyard. In the existing map coordinates the well is centered at (1060,240).

The coastline now includes two connected concrete wheel tracks around a 40-map-unit
wide water well. A 90 by 115 map-unit hardstand joins the original north-shore land
and public promenade behind it. The coastline itself drives physics, water masks,
raised shore occlusion and the overhead chart. The yard surface and safety marks
sit at the existing land elevation; the old service crane west of Dock O remains.

The mobile gantry is based on the twin-sling, wheeled frame of
[Marine Travelift's BFMII boat hoists](https://www.marinetravelift.com/products/boat-hoists-25-100-ton/).
Its shared rigid model includes four tires and hubs, braces, winches, hoist blocks,
cables, two broad webbing straps, a hydraulic cabinet, exhaust stack and operator
step. Fixed chassis partitions surround a continuously translated sling model.
The moving model prepares topology once and reuses vertex buffers at every exact
simulation phase; it adds no textures or pose tables. The hoist cables share the
block anchors. Foreground quay cutouts hide lowered webbing behind solid ground.

The tug starts beside the enlarged yacht slip. The player acknowledges Dockmaster,
taps the lift's radio marker and explicitly selects LOWER THE STRAPS. The operator
refuses if any tug, cargo or moored boat overlaps the well or its entrance. Clearance
is checked again on confirmation and after physics on every lowering tick. An
intrusion pauses at the exact phase; clearing the well resumes lowering.

Both eight-unit-wide straps descend over four seconds. Water contact emits small
expanding ripple patches at each webbing/water intersection into a fixed 32-event
pool; motor motion emits exhaust from the model's stack anchor into the existing
smoke pool. No fresh motion effects are emitted while blocked or after lowering;
existing particles continue to drift and fade. Lowered straps are required before
the disabled boat can be handed over.

The opening assignment uses the accepted cabin-cruiser artwork, with its original
32 headings, lighting, waterline and physics. After lowering, the player attaches
the tow, brings the boat to the entrance, releases, then holds the boat to push it
into the straps. The assisted push slows inside the well and aligns the hull; the
crew only takes over once the player has physically pushed it inside. The aligned,
settled hull is secured and retained for the rest of the voyage. The subsequent
yacht, rafted-cruiser and swimmer jobs each receive a fresh Dockmaster briefing.
The operator identifies as TRAVEL LIFT / MONTROSE; no personal name is displayed.

Challenge ID 8 is appended; IDs 4,6,7 and their earned progress are preserved.
Montrose's full location route is now 8,4,6,7. All four use the same upcoming asset
manifest, including later yacht and swimmer assets.

`TravelLiftTest` checks radio ownership/refusal, late and mid-motion obstructions,
retained/NPC hull clearance, every travel tick, exact anchors, bounded effects and
lifecycle pause. `MontroseHauloutTest` exercises public radio choices, tow, throttle,
release and physical pushing
through a safe handoff, independent wheel-support/well checks, no credit without
a tow, retained hull identity, per-job reward and next dispatch. The translated
parity trace also includes that public voyage. Actual-renderer captures are
reproducible with `tools/MontroseHauloutReview.java`; `montrose-dispatch` and
`montrose-haulout`, `montrose-lift-lowering` and `montrose-lift-water` are native
profiling fixtures.

Updated validation: native/JVM trace `dd774209:2:1350`; 1,756,523 all-heading shoreline
pixel checks at the new well and quay. Native Metal daylight and dusk screenshots
were inspected in iPhone17Pro/iOS26.5 simulator. Simulator timing was captured
while desktop validation ran and is not a device-performance comparison.
