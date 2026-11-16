# Montrose Harbor layout and entrance audit

Audit date: 2026-11-16. Sources below inform an intentionally enlarged game harbor, not a navigation chart. The 80-foot tow and enlarged N-dock bay are game adaptations; the harbor's public listing describes 25–50-foot accommodations.

## References and what they establish

- [Chicago Harbors: Montrose](https://www.chicagoharbors.info/harbors/montrose/) confirms 711 slips, mooring cans and star docks, a yacht club, water/power at slips, pump-out, small-boat dry storage and public crane service. The page's listed entrance is 41°57.578′N, 87°38.401′W.
- [Official dock map](https://www.chicagoharbors.info/wp-content/uploads/2013/04/montrose-zoom4.jpg), reviewed locally as `out/montrose-dock-map.jpg`, supplies the curved basin/peninsula outline, dock row positions, mooring field, star docks, walks and short entrance breakwaters. Coordinates below are image traces, not surveyed coordinates.
- [USCG 2025 Light List, Volume VII](https://navcen.uscg.gov/sites/default/files/pdf/lightLists/LightList_V7_2025.pdf), Montrose entries, identifies the east entrance light LLNR 20110 at 41°57′33.000″N 087°38′25.000″W, flashing red every three seconds, and west LLNR 20115 at 41°57′33.400″N 087°38′28.000″W, flashing green every three seconds. Both are skeleton towers. These published characteristics support fixed breakwater fixtures; this audit does not establish their current operational status. The separate Montrose Breakwater Light LLNR 20105 is farther northeast and should not be confused with this entrance pair.

## Recognizable features to retain

| Feature | Game mapping / integration contract |
| --- | --- |
| Round western basin and hooked southern peninsula | Keep the traced coast; do not replace it with a rectangular marina. |
| Lakeward approach | Tug starts at map (453,1340), yacht (453,1417), entirely outside the entrance line. |
| West breakwater / port entrance | Root (232,1247), tip (369,1258); green fixed beacon at the tip. Rocks continue lakeward below this pier along the western bank. |
| East breakwater / starboard entrance | Root (585,1138), tip (548,1290); red fixed beacon at the tip. |
| Concrete shoreline walk and seawall | Continuous around the basin and peninsula; use perspective-consistent shoreline art, with solid shore collision/cutouts. |
| Wooden finger docks | Fifteen unequal, angled spines: O through J on north bank, A through I around south/east bank. |
| Dock utility lights | `MontroseHarbor.PEDESTALS` gives 127 fixed world-space positions/directions: between finger stations on all fifteen rows, plus five star hubs. These placements are gameplay embellishment, not a utility survey. |
| Star docks | Five western clusters, matching P/R/S/T/U group placement. |
| Mooring cans | Western basin field, visually distinct from navigation lights. The reduced field is a readable game approximation. |
| Northern shore services | Building/apron on the map; official amenity locations are not resolved precisely. Pump-out/crane/dry-storage cues may be placed here as creative interpretation. |
| Peninsula park | Grass, tree groups and looped pedestrian path. Outer shore rocks and port-side entrance rocks should both be represented. |
| Secondary north-shore fixtures | Map shows a linear service float near (415,300)–(625,270), plus short pier near (770,265). Optional detail; avoid closing the central fairway. |

The source image transforms to world space with `x = sourceX * 1.5 - 600`, `y = sourceY * 1.5 - 1300`. Use those helpers for any additions so collisions, markers and artwork remain registered. The entrance's returning vector is perpendicular to the line between the two slightly offset tips; red remains right returning independently of camera projection.

## Playable route and evidence

The deterministic driver uses only public screen-space pointer down/move/up: attach yacht, cross the entrance, tow through the western basin, turn east across the central fairway, then approach the enlarged N bay. It does not teleport hulls, inject velocity, or alter the physics. Its waypoints are map (440,1160), (330,950), (330,650), (860,570), (1015,550), (1003,345).

`MontroseTest.run()` passed 291 assertions after the revised start and fixture placement. This includes full three-disc hulls lakeward of the mouth, marker tip registration, pedestal support on dock geometry, retained momentum, solid dock collision, no completion without inlet towing, and a bounded public-input voyage. Delivery ends with hull 100, one 1200-point reward, yacht retained in the slip. These are JVM shared-logic checks; actual renderer/Android install and native parity are coordinated by the parent integration round.

### Integrated service detail and geometry regression

The optional north service float now runs from map (415,300) to (625,270), with
a short connecting gangway (525,263) to (528,284). The short north pier runs
(764,225) to (772,267). Their connection endpoints were checked against the
traced shoreline. Five additional utility lights bring `PEDESTALS` to 132;
exact service assignments (pump-out/crane) remain unspecified. The two entrance
breakwaters remain the final two geometry entries for fixed-beacon receiver
registration. Revised shared geometry checks also cover raised seawall/dock
front-versus-rear clipping and disjoint native clip holes; see optimization.md
for construction costs and native validation limits.
