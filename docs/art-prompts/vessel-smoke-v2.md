# Independent vessel exhaust

Boat exhaust is simulated in `VesselSmoke` at the shared 60 Hz tick. Each puff
captures the registered stack position, hull bob, engine loading and a small
initial contribution from vessel velocity. After emission it has no reference to
the source body: turns, stops, despawns and camera movement cannot relocate it.
World wind and buoyancy move it until its eight-second lifetime expires.

Emission ranges from three puffs per second at idle to twelve under full load.
Charcoal lobes expand and gradually fade; they do not use the pale highlight of
factory steam. Only the tiny throat puff stays attached to the painted stack.
The isometric stack offsets are inverted to world coordinates when emitted, so
all 32 orientations remain registered through camera pans and zooms.

The pool is capped at 384 puffs, shared by the player, delivery and cross-traffic
boats. Oldest slots are replaced if saturated. Rendering culls offscreen puffs
and draws one cached translucent cloud per puff. Airborne exhaust is composed
after scenery and before game controls; it does not trigger building fading.
The title has a separate pool and shares its boat pose with the renderer.
Pause freezes particles; restarting clears them. Drawing never advances them.

`VesselSmokeTest` checks stack registration, detached movement, lifetime, pool
bounds, pause/reset behavior, render purity and host-frame-rate determinism.
`EffectsPreview` exports the 32-heading sheet and a loaded-turn-idle sequence to
`out/animation-frames/smoke/` for checking that the old trail keeps drifting.
