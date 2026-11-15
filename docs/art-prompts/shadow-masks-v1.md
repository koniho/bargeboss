# Independent sprite shadow masks

`GenerateShadowMasks` derives shadow alpha from the production tug, barge,
factory, chimney, yard-prop, warehouse, legacy bridge and lantern-buoy sprites. It does not
change their artwork. Eight PNG atlases contain 188 masks: one near-contact
and one softer cast variant for each of 94 source frames, including all 32
headings of both hull types. Static scenery retains its exact source frame.

Every cell is 128px: the source crop is resampled to 96px with 16px transparent
padding. Source alpha below64 is excluded; Gaussian sigma1.0 and3.2 produce
the two feather widths. The normalized pivot and draw size compensate for that
padding exactly. The generator rejects nonzero cell-edge alpha, preventing
bleeding from neighboring cells. These are procedural effect assets, with no
image-generation prompt or change to the accepted sprite designs.

Compile `tools/GenerateShadowMasks.java` with the shared core and run
`com.bargeboss.game.GenerateShadowMasks` to regenerate. `preview.sh` runs its
`--check` mode to compare every decoded pixel against current source artwork.
The masks add about11.5MiB of decoded source images, loaded through the existing
host source caches. No per-frame blur, bitmap regeneration or effect-sheet copy
is required.

`ShadowView` draws contact and cast layers independently of source shading.
Scenery shadows are placed on the receiving plane before vessels and buildings,
so they cannot darken a boat or paint across a roof. Watercraft use their actual
heading blend, stable waterline pivots and water-plane position; heave changes
their contact strength rather than moving the shadow up with the hull.

A smooth blend of nearby quay lamps changes cast direction and opacity; distant
scenery uses the shared ambient light direction. These are stylized silhouette
projections, not reconstructed3-D heights or shadow-map occlusion. They do not
account for one building blocking another lamp. The articulated3-D bridge keeps
its existing mesh shading; the legacy fixed bridge sprites have separate masks.

`Painter.spriteAffine` takes a normalized cell basis and applies its pivot.
Shared Java owns all projection/lighting decisions. Android Canvas, CoreGraphics
and Java2D execute the same affine image draw with bilinear sampling and group
opacity. Native painters do not infer light direction. `SpriteAffinePixelTest`
checks reflection, shear, crop, pivot and alpha behavior separately from the
shared JVM/ART/J2ObjC command traces.

[Independent direction study](../art-studies/shadow-v1/BargeBoss-independent-shadow-study-v1.png)
shows one unchanged factory sprite over three separately projected soft masks.
`ShadowActivityPreview` also exports an eight-second actual-render canal animation
with live factory/yard activity under `out/ambient-review/`; copies are in Downloads.

The buoy shadow atlas adds three variants × contact/cast = six cells. Atlas33
uses source atlas32; `ShadowArt.atlases()` explicitly skips foam atlas31. Buoy
color artwork is unchanged; each mask retains its measured waterline pivot.

The buoy atlas adds red, green and yellow near-contact cells 0–2 and soft-cast
cells 3–5 in `shadow-buoy-v1.png` (768×128). `BuoyArt` rolls these alpha masks
about each source waterline pivot before flattening them onto water. Both layers
are clipped to navigable water and drawn before hulls. Atlas 33 is separate from
foam 31 and color buoy 32; tools must use `ShadowArt.atlases()` rather than a
contiguous numeric range when enumerating all masks.
