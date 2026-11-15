# Dynamic sprite surface lighting and shading

Water receivers opt in through `SurfaceLightArt`: tugs (including feeders), empty
and loaded barge hulls, steel, containers, coal, and all three floating buoy types.
Land beacons and scenery are not enabled as receivers. Existing water reflections,
lamps, navigation lights, hull glows and ground/water shadows remain separate passes.

`SurfaceLighting` samples world-space lamp positions and combines four directional
shading responses. Away from lights, a bounded navy layer darkens the sprite.
Nearby light removes shading on its facing surface, revealing the original
painted colors. This replaces v1 additive surface brightening. Initial emitters are quay lamps, other vessels' warm work/service lamps,
lock machinery lamps, and nearby tutorial factory furnace/window anchors. Factory
anchors use a lazy 192-unit spatial index; each receiver inspects nine cells rather
than scanning the entire yard. Furnace activity modulates the same emitters used
by factory animation. Offscreen lamps can illuminate visible receivers. Camera
position, zoom, and screen size never change the light energy. Title vessels use
the title's warm key rather than invisible gameplay lamps.

Falloff is continuous and overlapping sources share a bounded energy budget.
The four directional shade masks partition each source silhouette rather than
stacking four full-strength dark coats. Maximum per-face shading is 68%; the
combined interior darkening is typically about 50%. Title shading is reduced
to 45% of gameplay strength for the sunset composition.
There is no nearest-lamp switch. Source directions are projected into the upright
sprite's axes; buoy roll rotates the response back into local axes. Only visual
state is read. The field does not alter hull physics, lock currents or timing.

## Assets and composition

`tools/GenerateSurfaceLighting.java` produces six response PNGs: 652 cells total,
including four responses for every 32-heading vessel/cargo frame. Each cell is
96 by 96 with eight columns. Decoded response data totals about 23 MiB; PNGs are
loaded/cached normally by the native painters. Runtime adds at most four sprite
submissions per receiver, using the existing cross-platform sprite/blend/affine
primitives. It does not process image pixels or allocate lighting textures per frame.

These first masks estimate broad rounded surfaces from the artwork's occupied
bounds. They are **not authored geometry normal maps** and do not reconstruct
cabin faces, cargo self-shadowing, or cast-light occlusion. Baked highlights stay
in the original art. Response pixels use a dark navy tint (`#081220`). Light reduces their opacity
instead of adding brightness over baked highlights. Other existing warm rim
glows and fixture halos remain separate. Red/green navigation lenses do not
recolor nearby surfaces. Reviewed tug lantern anchors, buoy lens anchors and
bright warm tug-window cores are excluded from shading. Lamp/nav effects are
drawn afterward and retain their normal brightness.

Masks are conservatively restricted to source pixels with alpha at least 245,
including a one-source-pixel guard around each response texel. Most production
sprites have interior alpha around 252–253 rather than 255; ordinary alpha-over
may raise that interior alpha slightly. Transparent pixels remain transparent.
No exterior halo is generated. During heading transitions the response follows
the same two frames and blend curve as the art; its support stays inside their
combined silhouette. Source PNGs are never modified.

Draw each response immediately after its base sprite. Hull response comes before
cargo; every individual cargo layer receives its response before the next nearer
load is drawn. Keep the entire pass inside the vessel's existing opaque-shore clip.
Buoys additionally keep the response inside the same moving waterline exclusion.
Lamp halos, contact foam, gameplay highlights and smoke retain their existing order.

## Selective scenery rollout

1. Choose a few important receivers first, such as a waterside warehouse wall,
   crane cabin or lock machinery. Keep other scenery excluded by default.
2. Author face/material masks or true normal maps registered to the source cells.
   Export the same directional response format; replace approximate masks without
   changing native painters. Use already graded scenery colors when baking material
   response, so the night palette is not accidentally undone.
3. Add the source to the explicit receiver catalog and invoke the response after
   the base sprite, within the same clip, depth and building-fade opacity scope.
   Moving rigid parts need their own masks and transformed local light directions.
4. Keep changing illumination outside retained static terrain layers. Cache only
   immutable masks. Add receiver budgets before enabling dense factory rows.
5. Extend pixel, orientation, pause, blend and fade checks, then review close and
   wide views. Add colored emitters and occlusion only with corresponding assets
   and tests; do not infer full lighting geometry from a painted sprite.

`SurfaceLightingTest` checks registration across all headings, blend alignment,
continuous falloff, opt-in boundaries, camera independence and render purity.
`SurfaceLightingReview` checks all 652 responses against actual native sprite
pixels and exports source/unlit/left/right comparisons plus the harbor view. Pixel checks
require unlit materials to be visibly darker and near lamps to reveal them.
The shading comparisons are `out/surface-shading-materials-v2.png` and
`out/surface-shading-harbor-v2.png`.
`preview.sh` verifies reproducible masks and runs those pixel checks, alongside
existing shore, buoy-waterline, cargo-depth and rendering-budget regressions.
