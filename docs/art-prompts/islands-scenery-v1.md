# Thousand Islands woodland and granite ground, v1

Built-in imagegen, 2026-11-27. Fixed camera: orthographic 45° ground yaw,
35.264° elevation. Neutral diffuse source art is graded by the shared lighting
profile. No native host selects frames or infers scenery placement.

## Woodland

Reference: `assets/sprites/marina-tiles-v1.png`, its accepted painted park style.
Canonical source: `docs/art-studies/islands/woodland-v1/source.png`.

Prompt: Production fixed-camera scenery sheet for Barge Boss. Exactly six
cutouts in a 3×2 grid with equal square cells, padding and visible root contacts.
Mature eastern white pine with irregular asymmetrical branch tiers and exposed
trunk; wind-shaped white pine with airy horizontal branches; dense irregular
cedar; paired medium white pines; low spreading juniper/cedar; older crooked
spreading pine. St Lawrence woodland, rich organic needle masses, muted cool
sage and deep greens, gray-brown bark and subtle lichen. Orthographic 45° yaw,
35.264° elevation. Soft neutral diffuse illumination, no shadows, lamps, ground,
water, buildings or labels. True alpha gutters; root contacts near 90% cell
height. Avoid geometric Christmas-tree triangles, outlines and neon colors.

Extraction pass: Preserve the six painted silhouettes, branch holes, bark,
layout, scale and intact roots. Remove the white background and all white
fringing with real transparent alpha. No added ground, shadows or checkerboard.

`tools/PackIslandWoodland.java` uniformly reduces each 512px source cell into a
248px image inside a 256px frame. `IslandWoodland` registers individual root
contacts. Six frames occupy 1.5 MiB of decoded source pixels.

## Ground

Canonical source: `docs/art-studies/islands/ground-v1/source.png`.

Prompt: Exactly four different bare Thousand Islands granite islets in a 2×2
square-cell grid. Same orthographic 45° yaw / 35.264° elevation and nearly circular
footprint. Low ground ellipse approximately 440×254px in each 512px cell, centered
at (256,280), with a small rocky lip. Hand-painted mossy turf, weathered pink-gray
Canadian Shield bedrock, cracks, ledges, lichens, dry needles, exposed rock shelves
flowing naturally into grass. Detailed organic texture, restrained neutral diffuse
illumination, broad clear centers for separately placed cottages. Four variations
of rock/moss arrangement and broken shoreline. At most a small ground fern; no
trees, shrubs, buildings, docks, water, cast shadows, geometric rings, smooth cap
strokes, labels or checkerboard. Every island isolated on real alpha transparency.

`tools/PackIslandGround.java` fits each alpha-bounded image uniformly to 360px
wide in a 384×256 transparent frame; no aspect stretching. The registered image
center is the island center, lifted five world units. The original collision
circles/routes remain unchanged. Four frames occupy 1.5 MiB of decoded source
pixels. The packer also exports conservative radial opaque contours used for
foreground hull/rope cutouts. The independent PNG-versus-hull pixel regression
covers tug, cruiser and M3 at all headings.

## Runtime scope

The two frame packs are requested only by Thousand Islands manifests. No new
textures are loaded by Montrose or the other levels. Tree/cottage ground contacts
are sorted with boats; geometry and registration live in shared Java. Cottages and
Montrose beach buildings use prepared rigid geometry and shared material lighting.
The footbridge span uses one continuously translated prepared model, with fixed
lift towers, preserving the existing gate timings and clearances.
