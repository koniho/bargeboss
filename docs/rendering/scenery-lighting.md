# 3-D scenery receivers

`SceneryLighting` collects a world-space light snapshot at the start of each
shared gameplay draw. Its fixed spatial hash holds up to 2,048 sources. Source
radii cannot exceed the 256-unit grid cell, so each receiver queries nine cells.
No native host selects emitters, infers animation state, or rebuilds an atlas.

`ArticulatedModel` applies ambient/sun grading, then colored Lambert point-light
response to every non-emissive face. Flat and procedural materials both receive
light. Physical model XYZ, transformed normals and distance falloff determine
response. A depth slice retains the original polygon's lighting sample, avoiding
color changes when boats, ropes or particles split a face into drawing intervals.
Large planar faces currently use one centroid sample; this is face lighting, not
per-pixel shadow mapping or line-of-sight light occlusion.

`SurfaceMaterial.LIT` marks a procedural face whose final color was calculated
in shared Java. `LightingProfile.surface` leaves that color alone. Native hosts
remove the flag when selecting the original wood/steel/concrete/etc. shader.
UVs, wetness, transparency and clip order retain their previous meaning. Never
apply the scenery/night grade again to these colors. Emissive lenses stay on
their existing path and are not brightened by incoming lights.

Bridge and crane lamps use the same continuous rigid pose as their visible
fixtures. Lock lights, yard vehicle lamps, marina pedestals, beacons, building
windows, vessels and fires contribute their authored positions and simulation
brightness. Camera visibility only rejects bridge assemblies too distant to
contribute; it does not change a local receiver's source direction.

Separate small geometry renderers use `SceneryLighting.face` or `polygon`:
workers/spectators, yard vehicles and work benches, pedestal bodies and inlet
limestone. Worker normals come from the fully transformed limb/lean geometry.
Projected-height geometry is converted to physical model Z before lighting.
Painted scenery sprites retain their existing asset lighting paths.

Validation: `SceneryReceiverTest`, `SurfaceMaterialTest`, and the prelit cases in
`VulkanEncodingTest`; JVM/J2ObjC parity; shared and native captures of
`land-bridge-lit` and `land-workers-lit`. The developer-only
`RenderExclusions.SCENERY_POINT_LIGHTS` switch (128) disables this source pass
for same-fixture timing comparisons while retaining the new ambient path.
