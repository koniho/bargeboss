# Watercraft surface material contract

Applies to all new or revised watercraft: player, teacher and feeder tugs,
barges, yachts, rescued cruisers, and moored motorboats and sailboats. Livery
variants share geometry and surface data unless their physical fittings differ.
This is the migration contract for [issue 13](https://github.com/koniho/bargeboss/issues/13),
not a claim that the production fleet has already migrated.

## Artwork and geometry

Author one rigid model with stable local coordinates and real surface normals.
Use the established game camera: 64 genuine headings (5.625 degrees) for barges
and cargo, and 32 (11.25 degrees) for the current tug/yacht families.
Preserve the production vessel silhouette, proportions, recognizable fittings,
palette and worn industrial finish. Dusk illumination must come from the
lighting model; do not paint a strong key light or lamp glow into base color.
Do not derive supposedly accurate geometry normals from painted RGB pixels.

Deliver separately registered albedo, normal/height and material channels.
Every map must have identical coverage, cell size, pivot, heading order and
padding. Height is relative to the hull's water-plane origin, including submerged
surfaces. Geometry owns normals; material wear stays in object coordinates as
the model rotates. Tangent, view and world normal spaces are not interchangeable.
Document the transform explicitly and test it using known planar surfaces.

Prototype v1 uses 256-square cells, pivot (0.5, 0.72), and world normals with the
sprite-to-world 45-degree heading registration applied. Red and green encode
octahedral unit normals; blue encodes height from -8 through +88 model units.
The material map encodes roughness in red, emission strength in green and a
reserved zero blue channel. The base map is sRGB; all other channels are linear
data. Alpha is coverage, not roughness or light intensity. Backends must undo
premultiplication before decoding data and normalize decoded normals.

The prototype's palette-to-material conversion is a temporary guide importer.
Production exports require explicit authored materials and emission groups;
color matching must not decide whether a fixture emits light. Moored boats have
emission disabled. Rescued cruisers use shared anchor/navigation-light state.

## Anchors and registration

Export named local XYZ anchors for tow eyes, cleats, stack outlets, lamps,
navigation lights, tires and cargo hold bounds. Include parent rigid-part IDs,
local normals and empty collections where applicable. Project these anchors
using the same model and heading as the pixels. Preserve gameplay attachment
identities; a changed roof or mast cannot keep an old painted-art light offset.
Check every heading and the 31-to-0 blend, including title-screen projection.

Barges must retain the [cargo contract](cargo-authoring-contract.md): 62 by 24
hold, floor 3.05, rim 11, registered cargo and foreground coaming occlusion.
Regenerate shadow, clipping and lighting derivatives whenever silhouette,
registration or geometry changes. Normal lighting does not implement occlusion.
Keep submerged hull cuts, shoreline cuts, rope spans and cargo ordering under
shared scene control according to the [drawing-order contract](../rendering/drawing-order.md).

## Rendering boundary

Shared Java selects models, poses, blends, pivots, livery, fixture state and a
bounded set of nearby lights. Native painters execute the same material response;
they do not infer gameplay from pixels or select different artwork. Combine
albedo with ambient and directional diffuse light, restrained material-dependent
specular response, and separately controlled emission. A light behind a face
must not brighten that face. Light color must survive modulation. Normal maps
alone do not supply cast shadows or self-shadowing: document these limitations.

Define one shader equation and coordinate contract for Android and Metal.
Retain the desktop/reference path for comparisons, but do not count CPU image
shading as a desktop GPU implementation. Backend capability and fallback must be
explicit during migration; never silently switch to a dark unlit replacement.

## Runtime assets and budget

Production textures are individual frame packs, not whole rotation sheets.
Prototype packs stack three 256-square bands in one 256 by 768 RGBA texture.
A complete 32-heading set is 24 MiB decoded, excluding driver copies. Two
neighboring poses cost 1.5 MiB. This is a memory calculation, not a measured
working-set or performance result. Full guide sheets belong in offline review.
Account for all families, liveries, shadow data and concurrent source/frame cache
copies before selecting final resolution. Do not preload both old derivative
sets and replacement materials without an explicit bounded migration budget.

No texture generation, normal reconstruction from geometry, blur or per-pixel
CPU lighting during active native draws. Decode/prewarm outside gameplay and
use bounded caches. Follow [performance development rules](../performance/development-rules.md).

## Release gates

1. Run deterministic geometry and asset checks: opposite-face light rejection,
   signed height, all 32 distinct registered poses, identical coverage, transparent
   borders, correct frame packs and finite metadata. The prototype checker is
   `com.bargeboss.game.WatercraftMaterialCheck`.
2. Review actual rendered vessels against accepted artwork, including both light
   directions, colored navigation lights, unlit moored boats, all headings,
   heading transitions and different light distances. Pixel uniqueness cannot
   certify correct geometry or art quality.
3. Check anchor overlays, waterline bobbing, cargo alignment, foreground shore
   occlusion, rope attachments and shadows with the production renderer. Run the
   all-heading shoreline pixel regression and shared gameplay/replay checks.
4. Compare Android, iOS Metal and desktop/reference output. Record unsupported
   backends and device validation limits explicitly.
5. Measure frame intervals, submission cost, GPU time and cache memory on the
   development phone: close tug, turning tow, wide industrial scene and dense
   Montrose. Compare identical camera/replay states against the old pipeline.
   Do not infer 60 FPS headroom from fewer draws or an offline shader preview.
6. Promote families only after these gates pass. Keep issue 13 open while any
   required backend, fleet family or authoring deliverable remains incomplete.
