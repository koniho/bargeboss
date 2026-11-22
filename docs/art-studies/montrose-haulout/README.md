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

The fixed mobile gantry is based on the twin-sling, wheeled frame of
[Marine Travelift's BFMII boat hoists](https://www.marinetravelift.com/products/boat-hoists-25-100-ton/).
Its shared rigid model includes four tires and hubs, braces, hoist blocks, cables,
two submerged slings, a hydraulic cabinet and operator step. A single prepared
BSP pose is split through the scene's existing depth stream so hulls and ropes
can pass between the legs. This round adds a crew handoff into the well, not a
lifting/yard-driving animation. It adds no textures or dynamic pose caches.

The opening assignment uses the accepted cabin-cruiser artwork, with its original
32 headings, lighting, waterline and physics. The tug starts beside the enlarged
yacht slip. The player acknowledges Dockmaster, attaches the tow, brings the boat
to the entrance and releases at low speed. The crew guides it into the well;
the aligned, settled hull is secured and retained for the rest of the voyage.
The subsequent yacht, rafted-cruiser and swimmer jobs each receive a fresh radio
briefing. Player-facing text uses only DOCKMASTER / MONTROSE.

Challenge ID 8 is appended; IDs 4,6,7 and their earned progress are preserved.
Montrose's full location route is now 8,4,6,7. All four use the same upcoming asset
manifest, including later yacht and swimmer assets.

`MontroseHauloutTest` exercises the public radio choice, tow, throttle and release
through a safe handoff, independent wheel-support/well checks, no credit without
a tow, retained hull identity, per-job reward and next dispatch. The translated
parity trace also includes that public voyage. Actual-renderer captures are
reproducible with `tools/MontroseHauloutReview.java`; `montrose-dispatch` and
`montrose-haulout` are native profiling fixtures.

Validation: native/JVM trace `e501393e:2:1350`; 1,756,523 all-heading shoreline
pixel checks at the new well and quay. Native Metal daylight and dusk screenshots
were inspected in iPhone17Pro/iOS26.5 simulator. Simulator timing was captured
while desktop validation ran and is not a device-performance comparison.
