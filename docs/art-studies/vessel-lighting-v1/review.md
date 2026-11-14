# Vessel lights, 2026-11-14

The tug keeps its original saturated v5 PNG. Runtime lighting enhances three existing painted fixtures: `mast-lantern`, `roof-worklight-a`, and `roof-worklight-b`. The same renderer serves the player, feeder boats and traffic. Source pixels, hull registration, smoke outlets and animation frames are unchanged.

## Reviewed source points

[The runtime anchor inventory](../tug-isometric-v5-runtime-lights.json) records all 96 frame/fixture entries. The 79 visible UV points were inspected against the original 1774×887 atlas using exact rounded cell boundaries. The 17 unresolved roof points are disabled. The mast is visible in every heading; the painted roof pair merges or disappears at some angles. A hidden point never receives an estimated glow. Visibility blends with the same frame weights as the hull; a single visible contributor remains attached while fading.

This validates image-space attachment, not a reconstructed vessel model. The source has inconsistent details across headings, so no measured local XYZ or surface normal is claimed for its painted lamps. The earlier guide inventory remains explicitly unverified; this work does not promote its other lights, tires or fittings.

The four `source-page-N.png` reviews use cyan for the mast, green for roof A and magenta for roof B. The four `runtime-headings-N.png` pages show all 32 actual shared-render headings. Canonical copies are in this directory; regeneration tools are `VesselLightAnchorReview` and `VesselLightingPreview`.

## Newly authored barge fittings

Two low coaming service lamps are added as shared geometry, with dark casings and warm lenses. They are new fittings, not claims about painted-source fixtures. Stable IDs are `stern-coaming-lantern` and `bow-coaming-lantern`. Body-local anchors are `[-(half + .65*radius), 0, 5]` and `[+(half + .65*radius), 0, 5]`; local normal is `[0,0,1]`. Body yaw rotates XY, the isometric camera projects Z with `.81649658`, and wave bob moves the entire casing/glow together.

The two lamp centers were checked against opaque hull pixels at every source heading. They remain on the hull ends rather than floating beside the sprite. Cargo draws after fittings, so containers, steel and coal occlude them. Loaded review exports cover all 32 headings for each cargo kind under `out/vessel-light-review/loaded-{0,1,2}-headings-{0,1,2,3}.png`.

## Effects and cost

Each visible lamp uses four warm halo shells and one small bright lens. Brightness varies slowly from the shared visual tick; rendering never advances simulation. Tug reflections are nine short, broken warm strokes drawn before the hull, anchored beneath the vessel, and clipped to the actual water/shore geometry. This is a restrained stylized shimmer, not physically traced illumination. Reflections stay on the water plane while vessel fixtures bob.

Maximum additional submissions are 24 per tug before shoreline partition splits and 12 per barge. No bitmap filtering, decoding or platform-specific gameplay was added. Boats and cargo retain original artwork colors while the separate scenery grade darkens the environment.

## Validation

- 15,797 shared assertions: exact source-frame blend weights, camera yaw, negative/wrapped headings, explicit occlusion, consistent bob, repeat/paused render purity, NPC behavior and bounded primitive cost.
- 287 production-image assertions: all 79 active tug points land on opaque warm-painted features; all 64 authored barge fitting positions lie on opaque source hull pixels.
- Existing 60 dense-scene submission-budget checks pass; largest fixture 14,364 commands after this lighting addition, within the 16,200 budget at 1920 width.
- All 32 empty-hull headings and marker pages visually reviewed. Loaded container, steel and coal exports checked for cargo hiding fixtures. The 1333-wide harbor composition was reviewed with dark environment, bright boats and factory windows.

These JVM checks do not prove matching native antialiasing, touch latency, safe areas or lifecycle behavior. Android build/install and native checks are recorded by the main development round.
