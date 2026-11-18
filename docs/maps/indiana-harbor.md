# Indiana Harbor geographic layout

Indiana now has a connected lake approach, steelworks peninsulas, outer harbor,
ship canal and both canal branches. North projects upper right. The south-mill
steel tow follows the south branch to the berth near 41.6372 N, 87.47139 W.
One metre is two world units; the origin is 41.64 N, 87.46 W.

## Provenance and offline export

`indiana-survey-v1.json` records source coordinates, land polygons, transport ways,
bridge crossings and attribution. References reviewed on 2026-11-18:

- [OSM Indiana Harbor water relation](https://www.openstreetmap.org/relation/15761774)
  and [Lake Michigan relation](https://www.openstreetmap.org/relation/1205149).
- [USGS imagery](https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryOnly/MapServer)
  for harbor mouth, canal forks and industrial land arrangement. The service uses
  older NAIP imagery; the review date does not date the photographed structures.

Derived geographic data is © OpenStreetMap contributors,
[ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/).
[Attribution details](https://www.openstreetmap.org/copyright). Imagery is a visual
reference, not shipped game art.

Run `python3 tools/build-indiana-survey.py` to regenerate `IndianaSurvey.java`
from the checked-in JSON without network access. `--refresh-osm` rebuilds that JSON
from explicitly downloaded XML in `out/indiana-reference`; review the survey diff
before accepting it. The exported material grid is baked offline, not recomputed
from GIS polygons during gameplay.

## Bounds and review limits

The reviewed box is -87.500 to -87.400 longitude and 41.632 to 41.705 latitude.
Including the full projecting east peninsula is important: cutting through it
previously created disconnected water components masquerading as one ring and
broke triangulation. The south limit stops before the Grand Calumet junction.
Remote lake edges are open visual extent boundaries, not fabricated seawalls.

Shoreline simplification is approximately 1.5 metres. Concrete, grass, industrial,
aggregate, rail and building-zone tiles establish the site layout. Hick Tower and
the breakwater light have geographic anchors; canal crossings have initial rigid
bridge silhouettes. The current bridge silhouettes are generic raised bascules;
individual bridge types, footprints, mechanisms, signals and collision need a
reference pass. The new large industrial surfaces are not yet populated with the
final factory artwork. This is a layout increment, not a finished harbor replica.

`SurveyedLocationTest` exercises route hull clearance, wet marker anchors and real
screen-space lasso/unload at the destination. `IndianaShoreTilesTest` verifies the
material inventory and that production ground receivers stay out of water.
`ShoreOcclusionPixelTest indiana` checks all 32 headings against geographic shores.

Five broad industrial surface regions were traced from the georeferenced NAIP
export where the OSM extracts lacked land-use coverage (the two mill peninsulas,
western refinery, southern tank yard and southern mill). They are marked
`reviewBase` and carry a source note in the JSON. Explicit mapped polygons render
over these base regions. They are material zoning, not precise building outlines.
The offline exporter and OSM refresh preserve these annotations. Review their
registration again whenever the source shoreline or imagery extent changes.

## Retained landmark references

The earlier rigid models retain their original study sources:
[USACE harbor project](https://www.lrd.usace.army.mil/Missions/Projects/Article/3638327/indiana-harbor/),
[USCG East Breakwater lighthouse history](https://www.history.uscg.mil/Browse-by-Topic/Assets/Land/Lighthouses-Light-Stations/Article/1944971/indiana-harbor-east-breakwater-lighthouse/),
[CREATE Hick Tower project](https://www.createprogram.org/projects/hick/) and
[shore reference/material kit notes](indiana-harbor-shore-reference.md).
The old six-rectangle map, fixed module placements and 396-face module budget are
superseded by this geographic polygon and visible-cell inventory. Detailed mill
models remain available for the subsequent geographically placed scenery pass.
