# Rigid 64-pose barge fleet

Generated with `tools/bake-barge-fleet.sh`, from `BargePoseGuide` hull and load
geometry. This replaces the painted 32-pose hull, steel, container and coal
sets. Each heading is a fresh model projection at 5.625 degrees, not a duplicated
frame or a rotated bitmap. Fixed orthographic camera elevation 35.264 degrees,
256px cells, eight columns/eight rows, waterline pivot (0.5, 0.72), scale 1.
The 62×24 hold, floor 3.05 and rim 11 are shared by all loads. Hull depth clips
the load behind its foreground coaming before export. Empty hulls stay empty
and retained after unloading; equipment uses the same datum.

Base colors preserve the authored palette with restrained local-space wear and
neutral form shading for the current painter. These are geometric renders with
simpler surfaces than the earlier generated paintings. Their perspective,
registration and fitting identities remain fixed through the turn. Normal/height
and material sheets are separate, identically covered geometry exports, reserved
for the future material shader; they are not loaded in the current game.

`assets/sprites/frames/<sheet-stem>/<frame>.png` is the production texture format.
Canonical color/derivative sheets remain available to offline tooling. Pack
validation compares every pixel to those inputs. Both equipment families are
packed the same way. Complete upcoming-level manifests include later cargo and
all shadow/shading/glow variants, without loading other locations' fleet.

Effect anchors retain the hull guide's stable IDs and local coordinates. See
`anchors.json`; the 64-heading CSV supplies projected mooring, tire/contact and
cargo-origin points. Service and navigation lamps remain shared code fixtures,
not painted glows. Cargo has explicit empty light, stack and tire inventories.
No hull smokestack exists; the separate fire equipment owns its exhaust.

Shadows are regenerated alpha silhouettes: 128px cells, inner96/pad16,
sigma1 contact and sigma3.2 cast. Hull masks receive on water; cargo masks are
hold-floor deliveries and are not a second water shadow. Source pivot/scale
conversion is unchanged. Surface shading, glow and opaque-interior masks are
regenerated from these sources. Alpha shadows are not physical 3D shadow maps.

Review exports: `out/cargo-style-current.png`, `out/cargo-style-all-poses.png`
and `out/cargo-style-unloading.png`. The review fixture deliberately suppresses
shore clipping; its old fixed location became dry land after geographic level
changes. The separate shore pixel test exercises real banks and clipping.
Native iOS raster/material validation remains unrun on this development host.
