# Indiana Harbor shore reference and tile kit

Reviewed 2026-11-16 for the first Indiana shoreline detail pass. The level remains a
compressed game reconstruction. Satellite imagery guides shoreline character and
land use; it does not establish current equipment, exact elevations or every quay
material. Preserve the playable water footprint during this detail increment.

## Imagery inspected

The [USACE functional harbor map](https://lre-ops.usace.army.mil/OandM/functionalharbormaps/Indiana%20Harbor%20IN.pdf)
contains a satellite/aerial base with the whole harbor, canal and forks. Its image
acquisition date is not apparent on the sheet. The PDF was downloaded and visually
reviewed at 1800 pixels, including its north arrow and channel legend.

Visible observations from that sheet:

- Two large industrial land areas flank the outer harbor. Long parallel roof,
  rail and yard bands dominate, with broad pale storage/fill areas toward the lake.
- The entrance opens into a maneuvering basin, then narrows into a diagonal canal.
  The canal continues toward a fork, with a westward branch and a southward branch.
- Outer margins combine long engineered edges and irregular shore/fill outlines.
  Inner industrial yards are not a uniform square pavement grid.
- Tank farms become prominent farther inland; the outer basin is dominated by
  mill buildings and yards. The small marina southeast of the industrial complex
  is separate from the main shipping basin.

The colored polygons are **authorized dredged channels, not coastline**. Do not
trace their boundaries as the water/land boundary. The game deliberately rotates,
compresses and simplifies the underlying geography.

## Material evidence and landmark context

[USACE's October 2025 factsheet](https://lre-ops.usace.army.mil/OandM/factsheets/IndianaHarbor.pdf)
documents stone and concrete-caisson breakwaters, plus the main canal and two
branches. This supports a distinct hard outer-breakwater module instead of using
smooth industrial quay walls everywhere.

[USGS's canal station description](https://www.usgs.gov/centers/ohio-kentucky-indiana-water-science-center/science/indiana-harbor-canal-east-chicago)
explicitly describes sheet-pile banks supported by steel-slag and gravel fill at
the monitoring site. That is direct evidence for this canal reach, not proof that
every bank has the same construction.

[CREATE's Hick project](https://www.createprogram.org/projects/hick/) locates the
Hick rail corridor at the canal/IHB railroad. Keep its rail beds, bridge abutments
and tower as a coherent local group. The historical tower-photo reference is
already recorded in [the level document](indiana-harbor.md); do not treat that
1992 photograph as a present-day condition survey.

For a later coastline revision, [NOAA's IN1801-CS-N shoreline dataset](https://www.fisheries.noaa.gov/inport/item/63483)
provides imagery-derived vector shoreline. Its metadata identifies 2018-05-16
imagery and 2021 creation; a 2026 catalog update does not make the imagery current.
The vector data itself has not been imported or checked for this increment.

## Proposed modular kit

These are implementation recommendations inferred from the references, not
claims of surveyed dimensions or individual facility layouts.

| Module | Appearance and placement | Existing approach to reuse |
| --- | --- | --- |
| Working quay | Concrete cap over dark rust-stained vertical sheet piling; sparse bollards | Calumet concrete/steel/rust materials and raised-ground convention |
| Slag/gravel apron | Dark irregular aggregate patches behind the quay, with broad service clearances | Bounded surface polygons with deterministic variation |
| Service lane | Rough patched road parallel to the industrial edge; connect pads rather than crossing buildings | `IndustrialRoadView` patch/crack style, adapted to Indiana's dry footprint |
| Rail yard strip | Two rails on a restrained ballast strip, limited repeated ties, aligned with bridges or shed bands | Shared rigid beams and static prepared geometry |
| Outer breakwater | Low caisson/coping spine with rough stone-facing variation; preserve entrance markers | Existing concrete material plus bounded 3D rock/revetment approach |
| Mill pad | Large rectangular foundations, service apron and parallel infrastructure strips | Existing articulated mill footprints and near-night shader palette |
| Transition corner | Broken/stepped paving joining rock margin to quay without floating slivers | Geometry clipped to the same dry-land footprint as collision |

Do not call Calumet's placement code unchanged: `IndustrialRoadView` and parts of
`CalumetArt` query `TrainingChannel` and their own fixed road/building locations.
Reuse materials, primitives and footprint validation, then author Indiana's own
placements. Keep trucks wholly on connected lanes with building clearance.

## Rendering constraints

Use shared 3D shore geometry and surface materials, not large painted isometric
shore sprites. Reuse Calumet's worn concrete, steel and rust language while keeping
Indiana's slight red dusk bias. Sparse local lamps provide contrast; do not fill
industrial shore strips with bright evenly spaced marina lights.

Prepare fixed geometry outside active drawing; use bounded chunks and visibility
rejection. Keep moving water, wakes and lights separate. A drawn edge treatment
must not extend solid land into navigable water without corresponding collision
and shoreline-mask changes. Review native pixels and all-heading hull occlusion.
The satellite references do not justify a new raster atlas by themselves.
