# Calumet and Indiana landmark bridge rigs

Reviewed 2026-11-18. Runtime source: `LocationBridges`, `LandmarkBridge`,
`BridgePassage`, `BridgeLayers`, and the shared `ArticulatedModel`.
These are original, simplified XYZ models informed by photographs, not textured
photogrammetry or engineering replicas. Historical photographs establish the
mechanism/silhouette; they do not establish current paint or operating status.
Reference photographs are kept in untracked `out/bridge-references`, not shipped
with the open-source assets. The models retain the game's dusk grading.

## Calumet inventory and visual references

| Crossing | Representation and reference |
| --- | --- |
| CN / former EJ&E 710 | Rust polygonal through truss, tapered open lift towers, head machinery and descending counterweights. [Photographic survey](https://historicbridges.org/bridges/browser/?bridgebrowser=illinois/calumet2/) |
| 92nd / Ewing | Two hinged pony-truss leaves, compact brick-toned houses. [Bridgehouse and deck photos](https://chicagoloopbridges.com/bridges12/CAL16/Ewing.html) |
| 95th | Low roadway with truss below deck and tall narrow control towers, rather than the generic above-deck truss. [1958 and bridgehouse photographs](https://chicagoloopbridges.com/bridges12/CAL16/95th.html) |
| Rail cluster north/south | Separate tower-guided lift spans and elevated central cabins. Multiple parallel track spans are grouped into two readable rigs. [Actual paired lifts](https://historicbridges.org/bridges/browser/?bridgebrowser=illinois/calumet/) |
| Chicago Skyway | Fixed high deck, cantilever chord peaks, diagonal steel bents and three approach piers per bank. [River-level photographs](https://historicbridges.org/bridges/browser/?bridgebrowser=illinois/skyway/) and [HAER record](https://www.loc.gov/item/il0824/) |
| 100th | Medium pony trusses and substantial bank houses. [Photographs](https://chicagoloopbridges.com/bridges12/CAL16/100th.html) |
| 106th | Deep greenish pony trusses with tall diagonal webs. [Photographs](https://chicagoloopbridges.com/bridges12/CAL16/106th.html), [HAER aerial](https://www.loc.gov/pictures/item/il0830.photos.318215p/resource/) |
| C&WI / Torrence | Adjacent vertical lifts before Basin 5, restored to the inventory. Railroad/road widths differ. [Paired bridges photograph](https://historicbridges.org/illinois/torrence2/little_p1270474.jpg) |

Calumet headings use surveyed transport alignments in `docs/levels/calumet/survey.json`:
CN -0.428 rad, Ewing -0.947, rail cluster -0.80 and Skyway -0.785. The previous
positive headings ran the inlet bridges along the water (825-world-unit spans).
The corrected spans are about 105–144 units. Skyway's crossing position follows
the actual mapped viaduct line; its old first-pass marker was south of that line.
Torrence/C&WI placement is an approximate bank intersection based on the paired
crossing location, rather than a cadastral engineering survey. Roots need not be
midspan: both banks are ray-marched independently through the navigable polygon.

## Indiana follow-through

The former eleven OSM-way models included three pieces of Dickey Road as three
independent bridges. A curated crossing inventory replaces that interpretation:
Cliffs, EJ&E main channel, CSX, NS/Amtrak, Dickey, Cline, Whiting/631,
Indianapolis Boulevard, Columbus Drive and the east works crossing.

- [EJ&E 728 photographic survey](https://historicbridges.org/bridges/browser/?bridgebrowser=indiana/indianaharboreje/)
  and [NS/Amtrak photographs](https://historicbridges.org/bridges/browser/?bridgebrowser=indiana/indianaharborns/)
  establish single-leaf through trusses, rolling heels and large rear counterweights.
  The main-channel EJ&E crossing is **not** Whiting Branch bridge 631.
- [Whiting Branch bridge 631](https://bridgestunnels.com/location/indiana-harbor-canal-bridge-no-631/)
  is the separate southwest branch crossing.
- [Dickey Road history and photos](https://bridgestunnels.com/location/dickey-road-bridge/)
  distinguish the modern plate-girder double bascule from its historical truss predecessor.
- [USACE/Argonne operations overview](https://indianaharbor.evs.anl.gov/dredging/traffic-bridge/)
  documents the navigational road crossings. Indianapolis/Columbus and the east
  works currently use restrained girder-family approximations: their individual
  control-building details still need better ground-level photographic coverage.
- [Cline Avenue owner photograph](https://clineave.com/background/) establishes
  the high concrete deck and Y-shaped piers. It remains fixed, like the real bridge;
  the "all except Skyway movable" rule applies to the Calumet inventory. Its
  center/heading follow the source OSM viaduct segment, not a long-way midpoint.

Rail rolling motion preserves the leaf as a rigid body while translating its
heel along a rack. This is a simplified Scherzer mechanism; it is not a full gear
and tooth contact simulation. Main-channel neighboring leaves share a family;
exact steel-member counts, span-by-span track inventories and modern repainting
are deliberately simplified for readability.

## Motion, anchors and shadows

`BridgePassage` belongs to each game/reset, never to the static model cache.
Approach requests wait 90 fixed ticks, open over 300 ticks, hold at least 360 ticks
and close over 300 ticks only with clear water. A craft in the crossing or a tow
line crossing the gate keeps it requested. Reapproach reverses a closing span.
Green means fully passable; closed/part-open passages retain a conservative
full-width collision gate. Skyway and Cline are always passable.

Moving parts now retain complete local BSP topology and one reusable live pose.
Every fixed simulation tick evaluates the exact hinge angle/lift height, including
reversal. There is no animation-frame table: the previous 33 poses made five-second
opening visibly step at about 6.4 Hz. Fixed geometry remains prepared once. All
coordinates, color, lights and decisions are shared Java; no host selects a
separate animation. See [continuous scenery rules](../../rendering/live-3d-scenery.md).
The 95th below-deck trusses taper toward the leaf tips; the two bascule leaves
have an expansion joint so their rigid geometry does not intersect during travel.

Lights have unique named IDs, parent node, local XYZ, outward normal, radius and
signal/warm role. Houses, channel signals, moving leaf tips, lift cabins, tower
beacons and approach lamps are independent glow emitters. Rigid geometry supplies
registration, so raster-cell coordinates are not applicable. Anchor review is
model-projected, not pixel-picked from the reference photos. Smoke and steam
outlet lists are explicitly empty: these electric bridge rigs have no exhaust.

Separate deck cast shadows are projected from the same posed corners onto the
water plane using (+0.35 Z, +0.25 Z), with four bounded opacity/softness bands.
They draw before land and craft. These are geometric deck approximations, not
full truss shadow maps; color faces retain material shading. Foundation contact
is represented by the opaque footings. No image-derived shadow mask is used.

## Review and known limits

Run `tools/LandmarkBridgeReview.java` with core and desktop classes to export
closed/half/open panels and production scene renders. Source tooling is tracked;
exports live in `out/landmark-bridges` and user copies in Downloads. The test suite
checks rigid lengths/pivots, translating lift alignment, anchor identity, continuous per-tick
motion and exact transform agreement, approach/hold/closure, collision, reset and pause.

`BridgeLayers` partitions rigid faces by the same ground-depth event stream as
boats, ropes, lights and other landmarks. Neither an entire high bridge nor its
near and far supports may be painted at a single midpoint. See the rendering
contract for the billboard-depth approximation and native validation limits.

`tools/ContinuousBridgeReview.java` compares 18 moving rigs at 11 exact phases
against rebuilt world-space BSPs and exports a 301-frame opening sequence.
Single-pixel split-edge raster seams are tolerated; substantial ordering errors
fail the comparison. It is a desktop oracle, not native GPU pixel parity.
