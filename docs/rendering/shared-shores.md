# Permanent shores and surface materials

All Calumet missions and the tutorial use the same TrainingChannel water union,
shore silhouette, bridge, lock and industrial placements. CalumetMap exposes that
geography to collision, rope visibility, wake clipping and mission water queries.
Mission state selects objectives and machinery state, not a different coastline.
The eastern single-file branch has its center at x599 and opens into the southern
basin at y570. Assisted pickup routes through that basin rather than crossing quay.
Career bridges remain raised; the tutorial operates the bridge. Lock gates remain
open outside the lock mission.

Structural shores, dock decks, gangways, lakefront terraces, bridge leaves and lock
gates use shared geometry. Bridge/lock moving parts retain their mechanical pivots
and local UVs through projection and BSP splitting. SurfaceMaterial selects rust,
concrete, chipped yellow paint, steel, wood or corrugated metal. Metal and Android
AGSL execute assets/shaders/surface.inc; desktop and Core Graphics use solid-color
fallbacks. Existing water, wake, smoke and light shader primitives remain separate.
StaticTerrain keeps material polygons live, with only non-material markings cached.
Material-only chunks allocate no blank raster cache entries.

Montrose has a continuous promenade, joined dock networks, sloped access ramps and
24 gates. Twenty named gates follow the user-supplied dock map: A–O and P/R/S/T/U.
Four service/inlet gates remain unlettered. Upright signs above the gates face the
shared camera for readability. See ../art-studies/montrose/dock-lettering.md.
The green entrance has packed angular stone revetment and broad concrete access
steps; the promenade moves inland locally to leave a clear walking surface. A
rigid boat-hoist landmark west of Dock O has separate boom/lifting anchors for
future use. The lake-facing peninsula uses five broad concrete terraces. Exposed union edges
replace internal dock end faces, and terrain receiver masks include steps/ramps.

Industrial factory, loading-equipment, truck and bollard PNGs remain source sprites
with shared pivots, placement, selection and depth order. Source shore tile artwork
is retained as reference; structural ground uses geometry. New scenery is declared
in LevelAssets and included in Android's bounded asynchronous frame preparation.
The native iOS sprite cache still prepares demand-loaded scenery; cold first-visit
uploads remain a profiling concern, separate from warmed frame measurements.
