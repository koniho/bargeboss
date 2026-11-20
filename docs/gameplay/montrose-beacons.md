# Montrose beacon and planting reference pass

The three existing game lights now represent two different real structures. The
north beach horn uses a tapered red steel lattice with a low equipment enclosure,
anti-climb apron and open upper platform. The marina inlet pair use slender white
tapered round towers, two colored bands, small open top rails and square concrete
plinths. There is no enclosed lantern house or decorative conical roof.

## Observed references

Google Maps was operated through native Chrome automation on 2026-11-20. These
photographs were inspected directly, not inferred from their place labels:

- [Jason Mlady, Montrose Harbor Beacon](https://goo.gl/maps/7UqJjtFuFKn), uploaded
  October 2018, image dated August 2017: red open lattice, broad lower enclosure,
  narrow shaft, square top guard rail and small green lens. The location matches
  the northern beach horn. Another contributed photo shows an unrelated tall
  `WB1` tower, so the gallery name alone was not used to identify the structure.
- [Angel Pineda, inlet pair](https://goo.gl/maps/DuG9Eb2UbRRZWGL46), September 2021:
  both white cylindrical towers visible together, two red bands on the east tower
  and two green bands on the west tower, brown concrete plinths, open top railings.
- [USCG 2025 Light List, volume VII, page 115 / PDF page 117](https://navcen.uscg.gov/sites/default/files/pdf/lightLists/LightList_V7_2025.pdf):
  LLNR 20130 Wilson Avenue Breakwater Light at 41°58′07″ N, 87°37′53″ W is green,
  flashing every three seconds. This is the beach-horn location; it is distinct
  from LLNR 20105 Montrose Breakwater Light farther south. The inlet lights are
  20110 east/red and 20115 west/green, also three-second flashes. The list still
  calls the inlet structures skeleton towers; the directly inspected photos
  establish their replacement tubular forms.
- [Chicago Park District, Montrose Beach Dunes](https://www.chicagoparkdistrict.com/parks-facilities/lincoln-park-montrose-beach-dunes-natural-area):
  preserve open early-successional sand, not a dense carpet of vegetation.
  [Park District floristic assessment](https://plantsofconcern.org/assets/cpd-rfqa/report.lincoln-park-montrose-beach-dunes.pdf)
  identifies marram grass and eastern cottonwood among the dune plants.

The game depicts nominal light colors and recurrence, not current operational
status. The brief pulse envelope is an authored game treatment; the light list
specifies the recurrence, not that envelope. No sweeping beam or fog signal was
invented. No reference pixels are copied into production assets. Geometry and
relative scale are representative interpretations rather than an as-built survey.

## Shared implementation

`MontroseBeaconArt` owns three fixed `ArticulatedModel` instances and registered
lens anchors. Matte structure colors pass through the existing scenery dusk grade;
only the small lens and nearby halo emit during a flash. The horn now uses the
correct green three-second flash, with a water-clipped reflection. Foundation
contact shadows replace silhouettes from the old lighthouse sprite. Tutorial
markers retain their existing art. Entrance positions, solid shore, depth merge,
corrugated seawalls, collision and wave exposure are unchanged.

Geometry is prepared once. Inlet models each have 182 source faces and draw 183
BSP fragments; the horn has 202 source faces and draws 329. Redundant fine ring
beams were removed after an initial horn mesh drew 666 fragments. All three models
are culled before drawing; no new raster assets or caches are introduced.

Eastern dune grasses and low park-edge scrub use at most 80 candidate clusters,
with deterministic position/size variation and tile-bound rejection. Bare beach,
public paths and building aprons remain clear. Detail is painted only while
preparing existing bounded terrain layers at each distance tier; there is no
per-frame foliage update, wind animation or expanded texture working set.

`MontroseBeachReview` exports overview, beach, horn, pavilion and inlet frames,
including horn/inlet dark-phase frames. `MontroseBeaconTest` verifies actual light
identity, anchored elevations, bounded geometry/drawing, offscreen culling and
unchanged solid structure between flashes. Native timing and material appearance
remain separate validation; Java2D's plain material fallback does not show the
native corrugated surface detail.

Validation for this pass: full `./check.sh`, 11,589 beacon assertions,
2,434,414 all-heading shoreline pixels, 3,966 terrain preparation/budget checks,
and JVM/J2ObjC parity `f6b5cf85:2:1350`. Actual shared-renderer on/off and wide
frames were inspected. Terrain requests remain 4,376,820 bytes for the expanded
overview, 8,523,975 for the prior marina camera, 6,981,546 for the beach and
1,715,121 for the horn. These are requested raster bytes, not measured GPU memory.
Models are warmed during shared stage reset before active drawing. Android was
not operated in this refinement; native iOS deployment/review is coordinated by
the parent agent, which is separately investigating the existing wide-view cost.

Open refinement: lawn surfaces show a fine repeating diamond grid in Java2D and
native overview. The source sampling already crops 7% inside the grass diamond;
the inspected source contains no internal black grid. Separate 64-world-unit
clip/base/sprite-alpha coverage is a likely cause, not yet a verified diagnosis.
A controlled compositing fix should compare the whole lawn and tile boundaries;
blind overlap could replace dark seams with double-alpha bright seams. This pass
does not change that pipeline or increase its raster footprint.
