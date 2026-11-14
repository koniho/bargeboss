# Continuous practice harbor

Practice uses one connected world for all ten lessons. The tug starts at
(-1180, 500), travels through seven rectilinear canal gates, and enters the
harbor through the opening at x=170, y=442..558. The harbor occupies
x=170..430, y=215..817. The last gate is at (80, 500), before the mouth,
so the arrival instruction guides the player into the basin.

Both teaching barges exist at the beginning: the pushing load at (292, 700)
and the towing load at (350, 440). After pushing to (240, 600), assisted
steering continues with the same barge to the berth at (230, 700). The
unloaded barge remains empty and tied alongside; the next instruction directs
the player to the second barge. A failed or released tow retries in place.

Lesson progression changes instructions and completion gates. It does not
rebuild the world, replace vessels, move a hull to its next exercise, clear
the water, or snap the camera. The explicit Reset control may restart the
current exercise. Camera follow eases into a basin view by position rather
than lesson number, and lasso aiming/flight freezes projection as in a shift.
Local work targets 1.5× zoom, rising toward 1.75× for docking while fitting the
working hulls and raised bollard. The canal remains at 1×; zoom blends on entering
the basin. The view follows the tug and nearby teaching barge rather than holding
the entire harbor in frame.

`TrainingChannel.WATER_RECTANGLES` and `WATER_BOUNDARY` define the complete
connected waterway. Collision, surface effects, scenery clearance and the
ground drawing use that geometry throughout practice. The canal mouth has
no hidden end wall. Ordered gates span the waterway's full width and retain
credit even if the player keeps driving before following the coast prompt.

The `deliveryRecorded` flag records each delivery once. Normal shifts still
retire a delivered load; practice retains its body as a visible, solid,
moored empty barge. Its unloading crew returns to idle. Cargo slots switch
which existing barge the tutorial teaches without changing vessel identity.

The replay suite exercises real gestures through every lesson on phone and
tablet layouts, checks hull and camera continuity at every simulation tick,
and verifies the canal mouth in both directions at sixteen hull headings.
Separate checks cover late coasting, full-width gates, tow retry, pause/reset,
single scoring, water clipping and the retained empty barge.

Factory placements use a 64-unit planning grid with whole-cell footprints.
Footprint edges, rather than arbitrary bitmap centers, align to the grid.
Four-by-four-cell industrial blocks are packed with the sixteen approved modules;
one-cell road lanes divide the rows. Every eligible non-road cell outside the
shore apron is occupied, with 664 modules in the bounded tutorial yard. Placement
and depth order are generated once; actual art/effect bounds cull offscreen work.
Building height varies independently of footprint, and large/tall structures
stand farther inland to leave the quays and working apron readable. Source
perspective must match the shared 45-degree yaw / 35.264-degree elevation:
each visible ground axis has a 30-degree screen slope. Moving a sprite's
pivot cannot correct an incorrect angle baked into its artwork.

The yard stands 18 shared display units above the water, behind corrugated
steel retaining walls. Wall faces and caps follow the exact water boundary;
the open canal mouth remains open. Yard texture, factory bases, lamps, dock
equipment and bollards share that elevation. `Game.bollardX/Y` exposes the
projected fitting location to both rope drawing and input, rather than relying
on a large touch radius to conceal an offset.

Lasso aiming, flight and recoil use `LassoPath`. The origin is the intersection
of the throw direction with the barge's rotated rounded hull envelope, lifted
to the deck. Release retains that local attachment and fixes the flight curve.
The flying loop and paid-out rope follow the previewed arc; the tether root
follows a moving/turning barge, and a miss returns to the current fitting.
Successful mooring draws a taut edge-to-bollard line.

Actual hull damage starts two brief pulses on the player tug. The shared timer
pauses with gameplay and does not trigger for ordinary fender contact, NPCs or
damage-free practice. No native painter requires a separate effect rule.
