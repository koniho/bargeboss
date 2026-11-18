# Calumet River: geographic layout pass

The playable location runs from the Lake Michigan approach to Turning Basin No. 5
at the Lake Calumet junction. North is world +X (upper right in the isometric
camera); east is world +Y. The origin is 41.659 N, 87.58 W. Scale is 1.6 world
units/metre. The source is geographic geometry, not a stretched screenshot.

## Sources and dates

Reviewed on 2026-11-18:

- [OpenStreetMap Calumet River relation 5765586](https://www.openstreetmap.org/relation/5765586):
  shoreline, slips, centerline and nearby land-use/transport map extracts.
- [USGS imagery service](https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryOnly/MapServer):
  lake entrance, bridge cluster, industrial reaches, southwest bend and Basin 5.
  The service describes NAIP 2017–2021 imagery, refreshed June 2024. Review date
  is not photography date. Export response extents were used for registration.
- [USACE Illinois Waterway chart 120](https://www.mvr.usace.army.mil/Portals/48/docs/Nav/NavigationCharts/ILW/CHART_120.pdf)
  and [chart 121](https://www.mvr.usace.army.mil/Portals/48/docs/Nav/NavigationCharts/ILW/CHART_121.pdf):
  basin naming and river reach reference. These older charts do not establish
  present-day businesses or bridge schedules.
- [NOAA Coast Pilot 6](https://www.nauticalcharts.noaa.gov/publications/coast-pilot/files/cp6/CPB6_WEB.pdf):
  bridge types and named reach. The former EJ&E bridge is a vertical lift, not
  the historical swing span. Basin 5 is at the Lake Calumet junction, not Basin 3
  farther northeast. O'Brien Lock lies beyond this stage.

`survey.json` is the reviewed source. `tools/build-calumet-survey.py` regenerates
`CalumetSurveyData.java` offline. No downloaded imagery is shipped as artwork.
OSM-derived survey/geometry is © OpenStreetMap contributors, under
[ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/); see
[OSM attribution](https://www.openstreetmap.org/copyright). This data attribution
is separate from the game code's license.

## Represented in this pass

- The inlet, major river bends, branching industrial slips, widening basins and
  Lake Calumet junction, with the same polygon driving water, hull collision,
  tow clearance, raised-shore masking, effects and the navigation chart.
- Seven bridge groups: former EJ&E lift, Ewing/92nd, 95th, rail lifts, Skyway,
  100th and 106th. Models are rigid 3-D parts projected by the shared renderer.
- Grass, worksite/concrete, aggregate, rail-yard, woodland, wetland and neighborhood
  surface classes; mapped road and rail guide strips clipped to dry ground.
- Six permanent berth sites; four contiguous jobs preserve vessels and effects.
  See [continuous locations](../../gameplay/continuous-locations.md).

## Deliberate layout simplifications and next increments

Shoreline simplification is about 2.5 metres; land polygons use about 7.5 metres.
The offshore approach/bounding edges are gameplay extents, not real shore walls.
A small railway-pier island is omitted pending explicit island collision support.
Land classes are broad map-derived materials, not claims about individual current
operators. The distant lakefront beyond the cropped river reach is incomplete.

Bridge groups are fixed in passable poses for this layout review. Their exact
trusses, piers, approach decks, navigation signals, opening cycles and machinery
collision still need individual reference/model passes. Nearby railway bridges
are grouped, not a complete inventory of individual tracks/spans. Surface tiles
mark future factories, storage piles, conveyors and work areas; detailed buildings
have not been populated on this new geography yet.

The exchange job has a real occupied shared berth: an unloaded hull must be pulled
away before another can use it. The terminal job currently uses three open-bank
berths near Basin 5; it does not yet reproduce the former single-file deepest-first
puzzle. A future constrained slip must fit the real mapped shoreline rather than
silently adding a fictional quay across the river. Cargo begins at local waiting
positions; geographic NPC feeder approaches remain a subsequent increment.

## Editing and validation

1. Edit longitude/latitude in `survey.json`; preserve the connected water ring.
2. Run `python3 tools/build-calumet-survey.py` and review the generated diff.
3. Run `./check.sh`. `SurveyedLocationTest` samples complete route hulls and uses
   projected pointer throws to unload at every berth; `LocationVoyageTest` tests
   handoffs and retained hull identity. These are not a full autonomous voyage.
4. Compile/run `tools/LocationLayoutReview.java` with the desktop renderer for
   overview/detail exports. Review native pixels after installing Android.
5. Run `ShoreOcclusionPixelTest calumet` for all-heading foreground shore masks.

Avoid deriving a retained hull's destination from the active job. Use body-based
berth/bollard helpers. Keep source generation offline, local queries indexed, and
visible surface work bounded; record measurement evidence in `optimization.md`.
