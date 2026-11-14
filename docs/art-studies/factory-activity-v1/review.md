# Factory activity, 2026-11-14

This update adds visible work cycles to existing factory artwork. No PNG was regenerated, no new outlet or lamp point was invented, and no building or equipment mesh moves.

- Blast furnaces, the coking plant, the foundry and the rolling mill cycle a charge flare between reviewed hot openings. A brief orange-to-white bloom and three short glowing sparks suggest hot material moving inside. The rolling mill uses its existing loading-opening anchor.
- Eleven modules with reviewed window anchors get staggered glow surges across their window banks. They retain their baseline lighting; the work cycle expands its spill rather than switching whole buildings off.
- Boiler houses, power stations, pipework, filter banks, pump houses and compressor houses intermittently vent three pale puffs from their existing primary exhaust. Continuous gray smoke remains in place underneath the brief steam release.

All fixture IDs and image-space locations come from [the approved factory inventory](../factory-v1/factory-anchors.json). The new `FactoryActivity` masks classify that inventory; they do not establish new local XYZ geometry or make the older inferred normals more precise. Steam uses only source outlet index 0 on the six listed modules.

Each module has a 7–11 second cycle with a location-dependent phase. Different hot openings take successive charges. The visual tick drives every envelope and particle position, so repeated paused renders are identical and drawing never changes gameplay state. Existing source pivots, uniform scale, land rise and whole-building fade apply to the added effects.

## Cost and validation

Two immutable 28×28 kernels hold the flare and steam shapes, using cache keys `0x42000011` and `0x42000010`. Their pulse, position, expansion and fade remain live parameters. The maximum retained raster cost across both 2×/3× density variants is about 80 KiB. Window activity reuses existing glow submissions. A hot-work event adds at most four visible submissions, and a steam event at most three; the two asset groups are disjoint.

`FactoryActivityTest` passes 45,734 assertions covering valid existing anchors, cycle rest/peak states, one active hot opening, phase independence, finite coordinates, invisible-building behavior, exact flare attachment, deterministic repeat output and a bounded effect budget. The observed per-building maximum is four additional calls.

`FactoryActivityCost` sampled a minute of visual time in all twelve responsive tutorial fixtures. Added visible activity averaged 0–13 submissions/frame; the largest sampled frame added 26, or 0.318% of the total target submissions. Raw results are in [factory-activity-cost.csv](../../performance/factory-activity-cost.csv). This measures command cost, not native frame rate, and excludes the one-time raster construction of the two small kernels.

## Actual-render review

[Workshop study](BargeBoss-factory-activity-study-v1.gif) and [canal scene](BargeBoss-factory-activity-canal-v1.gif) show twelve seconds at 12 fps. The preview holds the camera and vessel pose fixed while advancing the shared visual clock by five ticks per frame. It is an effects review, not a recorded player run. Both use the actual shared factory renderer.

The [charge still](BargeBoss-furnace-charge-v1.png) and [steam still](BargeBoss-utility-steam-v1.png) show the effects attached to existing source fixtures. Source artwork remains unchanged. Animated reviews and selected stills were also copied to Downloads.

Android/iOS performance, antialiasing and lifecycle behavior require native validation; the shared tests do not prove those properties.
