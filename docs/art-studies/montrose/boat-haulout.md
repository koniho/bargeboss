# Montrose boat-haulout crane

The user-supplied [satellite reference](boat-haulout-location-reference.png) marks the launch/service apron immediately west of Dock O. The static shared model is centered at source-map `(780, 200)`, world `(570, -1000)`, with its footing wholly inland of the promenade and its jib pointing south over the launch water. The 22-unit concrete base, yellow steel pedestal, separate lattice jib, cable and hook are game-scale scenery, not a surveyed mechanical reconstruction from the satellite image.

`MontroseBoatHoist` owns the rigid model. `montrose-haulout-boom` pivots at the mast head; `montrose-haulout-lifting-eye` is its child with local XYZ metadata. `liftingAnchor()` returns the visible hook's transformed world XYZ. The current pose is static. No boat lifting, mission state, interaction or collision footprint has been introduced.

The renderer merges the crane with vessels, tow spans, lights and dock pedestals by unraised ground depth. World/model UVs feed the existing rust, steel and concrete materials. No raster atlas or runtime texture is added. JVM review frames use the material fallback; native shader pixels and frame intervals need the parent integration validation.
