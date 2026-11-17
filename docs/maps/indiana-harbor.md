# Indiana Harbor — first playable reconstruction

Indiana Harbor is appended as level 6 (stable ID 5). Existing Calumet and Montrose
IDs and maps remain unchanged. The initial job starts with a steel-coil barge in
the outer basin, follows the ship canal past the Hick rail corridor, and delivers
to the south branch mill. The Lake George branch remains navigable to the west.

This is a compressed game layout, not a survey or navigation chart. Distances,
basin edges, bridge count and industrial footprints are deliberately simplified.
The first pass has two permanently raised rail leaves; they are rigid XYZ parts
about mechanical pivots, ready for later operating bridge logic. They are not
painted rotating sprites. There are no new active bridge/counterweight hazards.

## Landmark references

- USACE: protected harbor entrance, breakwaters and canal branches:
  https://www.lrd.usace.army.mil/Missions/Projects/Article/3638327/indiana-harbor/
- NOAA Coast Pilot 6: outer basin, ship canal, Forks, Lake George and Grand Calumet
  branches, square East Breakwater light:
  https://www.nauticalcharts.noaa.gov/publications/coast-pilot/files/cp6/CPB6_WEB.pdf
- US Coast Guard lighthouse history:
  https://www.history.uscg.mil/Browse-by-Topic/Assets/Land/Lighthouses-Light-Stations/Article/1944971/indiana-harbor-east-breakwater-lighthouse/
- CREATE Hick Tower project location (East Chicago, canal / IHB railroad):
  https://www.createprogram.org/projects/hick/
- Conrail Historical Society photographic reference, November 1992: pale
  two-storey Hick Tower, upper control windows and external stair beside bridges:
  https://conrailphotos.thecrhs.org/photos/hick-tower-east-chicago-in-11-1992
- USGS describes sheet-pile canal banks and industrial fill:
  https://www.usgs.gov/centers/ohio-kentucky-indiana-water-science-center/science/indiana-harbor-canal-east-chicago

## Rendering and palette

Shared 3D model boxes/beams supply quays, lighthouse, mill halls/stacks, rail
approaches, Hick Tower and raised bridge leaves. Existing concrete, corrugated,
rust and steel material shaders supply surface detail on Android and Metal.
The reference Java2D renderer retains flat shaded faces. Static face ordering is
prepared during level setup, not built in the first active frame. Landmarks join
the same depth order as watercraft and ropes. Shore cutouts use the same water
footprint as collision/effect clipping.

Indiana has slightly redder dark water (0xff151b25) and warm, soot/rust-biased
scenery base colors under the established near-night grade. Navigation colors,
vessels, goals and HUD retain their normal meanings. Smoke anchors come from the
mill model's XYZ pose. The red cast is art direction, not a statement about actual
water color. Entrance markers use the shared red-right-returning convention.

## Follow-up detail

Refine real shoreline bends and the full rail bridge cluster from mapping/photo
references; add differentiated blast-furnace/ore-unloader equipment and more
quay-side services. This first pass establishes playable geography and landmarks.

## Landmark detail pass

Hick Tower now has upper windows on all four sides, dark mullions and sills,
lower doors, a stair landing with handrails, and roof equipment. The East
Breakwater light has recessed openings and a gallery railing. Mill halls have
structural ribs, roof-monitor windows, loading doors, awnings and stack collars.
Two mills add compact furnace-like rooftop masses, service platforms and large
exhaust ducts to vary the skyline. These industrial details are stylized scenery,
not claims of exact plant equipment or present-day building condition.
All detail stays within existing landmark footprints and uses preprepared rigid
geometry, with no new texture atlases or changes to channel collision.
