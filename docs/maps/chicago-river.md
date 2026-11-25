# Chicago River geographic layout: Navy Pier to Wolf Point

The Chicago River stage spans the lakefront at Navy Pier west along the main stem to
Wolf Point, where the North and South Branches meet. North projects to the upper
right of the isometric camera, matching every other surveyed stage. 3.2 world units
are one metre (raised from 2.0 so the harbor lock fits about three tug beams), and
the downtown bascules sit high enough closed for the tug to pass beneath without
opening; the origin is 41.888 N, 87.62 W. The play box is -87.6430 to -87.5940
longitude and 41.8815 to 41.8945 latitude, so both branches run off the north and
south edges just past Grand Avenue and Madison Street.

## Mission: Navy Pier fire

Sparks from the Centennial Wheel have set the timber pavilions on Navy Pier's south
edge alight, and the wheel's lower gondolas are smoking. The tug starts in the Navy
Pier basin, locks in through the Chicago Harbor Lock, throws a line to the fire
barge at the Chicago Fire Department Engine 2 fireboat pier (Chicago Marine Safety
Station, just inside the lock on the south bank), locks back out with the barge in
tow and sprays the three fires from the harbor south of the pier.

- The lock levels automatically when a tug approaches bow-first from either side,
  opens, waits for the tug and every towed hull to be enclosed clear of the gate
  swing, closes, levels (0.6 m, 9 s) and opens the far side. Hulls in a closing
  gate's swing hold it open. The same leaf poses drive collision, tow-line
  blocking, signal lamps and the rigid 3-D leaf models.
- Lock levelling stirs a deterministic sluice current inside the chamber.
- The wheel stands 72 m back from the south pier wall, beyond the monitors' reach,
  so it only smokes. The three burning pavilions stand 8 m back from the wall, low
  enough for the monitors' flat arc and within reach of the marked spray station
  20 m off the wall.
- Downtown bascules stay down: their closed decks clear the tug, so they never
  open, collide or block tow lines.

## Sources and dates

Reviewed 2026-11-24:

- OpenStreetMap Overpass extract of the play box (`out/wolfpoint-reference/span.xml`,
  untracked): river water relations including
  [Chicago River](https://www.openstreetmap.org/relation/2192735) and both branches,
  [Lake Michigan](https://www.openstreetmap.org/relation/1205149), Ogden Slip, the
  confluence basin, harbours, piers, breakwaters and quays,
  [Chicago Harbor Lock](https://www.openstreetmap.org/way/762092241) with both mapped
  sector gates, the [Centennial Wheel](https://www.openstreetmap.org/way/686996484),
  building footprints with `height` / `building:levels`, streets, parks and bridges.
- A user-supplied Google Maps satellite crop identified the Engine 2 station, its
  fireboat pier and the South Pier lock approach. Google Maps 3D review in the
  browser is still pending (the browser extension was not connected this session);
  no Google imagery or geometry is exported.

Derived data is (c) OpenStreetMap contributors, [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/).
See [OSM copyright](https://www.openstreetmap.org/copyright).

## Offline export

`python3 tools/build-chicago-survey.py` regenerates `ChicagoSurvey.java` from the
checked-in `docs/maps/chicago-river-survey-v1.json` with the standard library only.
`--refresh-osm` rebuilds that JSON from the downloaded XML; it needs `shapely` for
polygon union, slitting and constrained triangulation. Review the JSON diff first.

- Water is the union of the mapped water polygons, minus closed piers, breakwaters
  and quays, clipped to the play box. Tiny pilings and finger floats under 400 m2
  are omitted; larger detached breakwaters and piers stay solid via 0.3 m slits to
  the outer ring, so the coast is one simple ring for collision and masking.
  Simplification tolerance is 0.6 m. Water triangles are precomputed offline.
- Buildings: 1,070 footprints, using the OSM height, else levels x 3.6 m + 2 m,
  else 14 m (5-6 m for service/boathouse). Near-rectangular footprints become their
  oriented rectangle; others are simplified to <= 24 vertices with offline roof
  triangles. Footprints are clipped to land. Painter order is presorted.
- Streets (motorway to residential, plus pedestrian ways), excluding tunnels,
  negative layers and bridge decks. Parks and grass are clipped to land.

## Deliberate simplifications and known gaps

- DuSable and Monroe Harbors and the Ohio Street Beach corner are disconnected from
  the main ring by the box/breakwaters and render as land.
- 18 movable bridges use the shared bascule kit at their OSM midpoints and deck
  axes. They lift for the tug like other stages; real downtown bridges rarely open
  for small craft. Double decks, L tracks and bridge-house styles are generic.
- Building materials and colours are stylised dusk palettes with a few named
  landmarks tinted (Wrigley, Trump, 333 Wacker, Wolf Point towers, Marina City,
  Merchandise Mart). Building parts/setbacks are not modelled.
- A tower fades only while it stands in front of the tug or a towed hull and its
  projected solid covers the hull or a narrow margin around it. Walls and roofs take
  scene lights (vessel lamps, searchlight, lock, wheel and fire).

## Validation

`ChicagoRiverTest` (in `./check.sh`) checks geography (confluence, both branches,
Ogden Slip, Wolf Point and Navy Pier land), lock proportions, monitor range, both
lock passages, the public line throw, mission phases and fire completion.
`tools/ChicagoReview.java` renders review frames to `out/chicago/`.
