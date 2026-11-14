# Night harbor and rendering budget

This update combines wide-view rendering optimizations with a darker industrial
ambience. The optimization-only measurements and comparisons are retained in
[wide-scenes.md](wide-scenes.md); those pixel-identity comparisons precede the
intentional night palette and lighting changes described here.

Water, roads, yard surfaces, retaining walls and the articulated bridge use a
cooler, darker palette. Factory and other scenery textures are graded once when
loaded, with bright warm windows preserved. All three hosts use the same
`SpriteGrade` math and cache the result. Source PNGs and alpha are unchanged.
Watercraft/cargo atlases are excluded, retaining their saturation, bright
paintwork and readable silhouettes against the dark water.

Existing authored scenery lights retain their animation and attachment points,
with stronger glow energy rather than more particle layers. Quay light spill
and broken water reflections are brighter. Factory smoke retains its complete
emission cadence in the wide view while reducing secondary lobes. The shared
viewport filter, terrain culling, cached road connections and redraw scheduling
remain enabled. Vessel wakes and physics are unchanged.

The runtime grade is a one-time decoding cost, not a per-frame postprocessing
pass. Android and Java2D use row buffers. iOS retains one graded premultiplied
RGBA buffer as image-provider backing, with per-source autorelease pools. The
shared grade tests cover alpha, unchanged vessel pixels, warm highlight retention
and premultiplied conversion; parity traces include representative grade values.
Native Core Graphics rasterization/provider lifetime still need the iOS CI/device
path, beyond shared Java/ART verification.

Tug mast and roof lamps use reviewed per-frame UV anchors, including visibility
when a fitting is hidden by the cabin. These follow all 32 headings, frame blends
and vessel bob, including NPC boats. Barges receive two new low service-light
fittings, drawn before cargo so loads can occlude them. Warm tug reflections are
clipped to navigable water. The fixtures add at most 24 submissions per tug and
12 per barge; their light does not tint the underlying hull texture.

The final combined [submission counts](night-scenes-counts.csv) at 1333×600 are
10,151 at canal entry (47.1% below the original 19,177), 12,398 mid-canal (40.6%
below 20,873), and 9,793 in the close harbor (21.6% below 12,483). All nine
scene/width combinations pass the existing rendering budgets, with a maximum of
14,364 submissions. Recorded CPU timings are diagnostic only: other validation
jobs ran concurrently, and these numbers do not measure Android FPS.

Validation: the full JVM and Android ART suites pass with replay signature
`be3a8e8d:11:8970`. The production Java2D previews, desktop package and pixel
checks pass, including 21,971 identical pixels with viewport rejection enabled.
Vessel-light tests cover anchor interpolation, hidden fixtures, cargo ordering,
bob, pause determinism and drawing cost. Source PNGs remain unchanged.
The 287 production-image anchor checks pass. Android built, installed through
the existing Shizuku path, and launched successfully on the development phone.
The final scene and 32-heading vessel reviews are in
[the vessel lighting study](../art-studies/vessel-lighting-v1/review.md), with
selected review images also copied to Downloads.

iOS remains a native validation limitation. The preceding revision's
[CI run](https://github.com/koniho/bargeboss/actions/runs/43599828081) passed
JVM/native parity, simulator compilation and three UI flows, but its interactive
tutorial test timed out on the first drag at lesson 0. That failure predates this
update; current iOS compilation, rendering and UI checks require the new CI run.
