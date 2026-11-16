# Green-marker entrance revetment

The [user-supplied shoreline reference](green-marker-shore-reference.png) shows packed pale angular limestone, dark wet waterline stones, and concrete steps joining the western green-marker pier.

`MontroseEntranceShore` creates a fixed, deterministic three-row rock field along the western entrance bank. Blocks have staggered positions, rotated and clipped outlines, varied dimensions, flat fractured caps, and bevelled edges. Concrete materials supply stone grain and wet darkening. The old scattered boulder sprites and the flat grass cap are omitted from this strip. The west entrance pier uses concrete, with five wider tapered steps descending from its gate toward the existing pier.

The promenade remains one connected surface; it moves smoothly 24 world units inland only beside this rock strip. Its existing gate-root search follows the shifted path. All other docks, lakefront terraces, mission geometry and navigation remain unchanged.

Rock vertices remain wholly inside the original dry footprint and at or below its 18-unit receiver height. Steps remain within dry land or the existing green-pier footprint and below the existing sloped access receiver. Consequently existing shore masks remain conservative: no hull pixels can leak through the new ground. This deliberately retains the established collision geometry and shore index rather than introducing a new waterline.

Geometry, stable UVs, colors and projected face bases are prepared once. Screen bounds cull each block, and the draw uses borrowed fixed-size vertex buffers. No image atlas or runtime texture is added. The whole-strip grass cut uses fixed quad triangulation without per-frame polygon clipping. Native frame-time and material validation remains part of final integration profiling.
