# Locations and contiguous jobs

The selector offers Calumet River, Montrose Harbor and Indiana Harbor. A location
owns one physical world. A job completion records its reward and activates the
next local assignment without resetting the tug, retained boats, tow graph,
physics clock, camera, wakes, smoke or scenery state.

Calumet runs opposite-bank deliveries, a shared-berth exchange, a two-empty-barge
bridge convoy and terminal deliveries near Turning Basin No. 5. The convoy reuses
two empty exchange barges. Eight cargo hulls coexist; unloaded barges are not
removed to make room. There is no O'Brien lock inside this river reach.

`CalumetRiver.JOB_SITES` owns six permanent berth positions and water-to-shore
normals. The job order uses sites 0/1, then 2, then a bridge gate, then 3/4/5.
`Body.destination` is the permanent berth index. `Body.voyageJob` identifies the
assignment, while `Body.berth` continues to identify cargo family. Never infer an
old body's berth coordinates from the current job: use the body overloads of
`Game.dockX`, `dockY`, `bollardX` and `bollardY`. Mooring aligns along the shore
tangent. `CalumetRiver.CONVOY_GATE` is a point and normalized forward vector;
the tug and both connected full hulls must clear its plane.

Montrose runs yacht delivery, rafted cruiser recovery and swimmer rescue in that
order. The yacht remains in its delivered slip; cruiser slots are reserved from
location entry. Starting later jobs initializes their actors and counters, without
moving the tug or restarting the harbor. Both delivered cruisers remain present
through the swimmer assignment. Indiana currently has one steel-delivery job.

The legacy BB1 career reward slots remain unchanged: Calumet jobs use 0–3,
Montrose uses 4/6/7, and Indiana uses 5. Old job selections enter the corresponding
whole location. Stars still improve per job without reward farming. The save
format still stores career results/settings, not a resumable in-progress voyage;
re-entering a location starts its first job. New persistence of complete world
state would require a separately versioned format.

Tutorial geometry and lesson progression remain a separate practice scene.
`LocationVoyageTest` checks job boundaries, stable destinations, retained hulls,
bridge full-hull/connected requirements, menu grouping and legacy reward slots.
Long route traversal and surveyed shoreline validity are separate level tests.

Mechanics tests and tooling can use package-private `Game.resetPracticeYard()`
for the historical small workyard. Its ID is -1, never a menu or persisted career
entry. This keeps isolated drag, rope, feeder, momentum and lighting fixtures
independent of real-world coordinates. Production-location tests always use
`reset()`. Lock and old narrow-slip geometry remain component-tested for future
locations, but there is no active lock inside the new Calumet reach.
