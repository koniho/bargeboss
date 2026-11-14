# Scenery fades only when covering watercraft

The former fade envelope extended 24 world units beyond an oversized boat box,
then also protected ropes, bollards and canal gates. Factory smoke expanded that
envelope further. This made nearby buildings disappear even when they did not
cover a vessel.

The renderer now records the actual watercraft and cargo sprite submissions in
paint order. Foreground scenery checks its opaque silhouette against those
already drawn sprites. Registration includes frame pivots, scale, blend weights,
rotated headings, cargo positions and wave bob because it uses the same drawing
commands. Background scenery has no boat behind it to obscure and stays opaque.

The 187 shared alpha masks are generated from unmodified production PNGs. Each
64×64 mask retains only cells with a fully opaque source interior (alpha at least
160). Transparent padding, cast shadows and soft sprite edges do not trigger
fading. Four overlapping samples reach 28% opacity; the easing stays inside the
actual overlap. Very thin or soft-edge-only intersections intentionally remain
opaque. There is no bitmap readback, physics change or persistent fade state.

Articulated bridge nodes test projected visible faces. Dock machinery and raised
shore caps test their painted polygons and strokes individually, avoiding large
rectangles containing empty space. Smoke, wakes, ropes, gates and bollards never
register as watercraft. A building's smoke visibility bounds remain separate
from its solid fade shape.

Validation:

- 1,442 new silhouette, draw-order, transparent-padding and no-proximity checks.
- Full Java and Android ART suites pass with signature `2738db6c:11:8970`.
- All 187 masks regenerate identically from current PNGs and atlas crops.
- Desktop build, shared previews and existing sprite/culling pixel checks pass.
- Android APK built, installed through Shizuku, and launched successfully.
- All nine scene/width rendering budgets pass; maximum 14,367 submissions.
  At 1333×600: entry 10,163, middle 12,398, close harbor 9,819.
  Opaque neighboring factory glows can now submit a few previously invisible
  low-alpha circles; the existing budget thresholds were not relaxed.
- `tools/OcclusionPreview.java` exports the four visual cases to
  `out/BargeBoss-scenery-occlusion-v1.png`; a review copy is in Downloads.

This validates shared drawing behavior, not native raster equality or device
frame rate. iOS execution still requires the CI/device path.
