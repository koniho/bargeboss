# Cargo authoring and integration contract

Apply this contract to every new or revised cargo family, together with the
`bargeboss-watercraft-sprites` skill (`~/.codex/skills/bargeboss-watercraft-sprites/SKILL.md`).
The skill supplies the generation, reference, anchor and shadow workflow; this
repository contract supplies the current barge dimensions and runtime behavior.
Use the [industrial concept](../reference/industrial-isometric-concept.png) for
material/style direction and the accepted production barge for the actual fit.
The [cargo style review](cargo-style-review.md) records the problems this prevents.

## Shared physical and visual frame

- Author inside the same 62 × 24 world-unit hold, with floor Z=3.05 and rim Z=11.
  Leave room for the rim, cleats, tires, tow attachments and gameplay feedback.
  Keep broad readable forms and material contrast consistent with the hull;
  realistic detail density is not a substitute for matching the accepted style.
- Use the fixed orthographic 3/4 camera at 35.264 degrees elevation. Rotate one
  rigid model/load through 32 actual poses, 11.25 degrees apart, in row-major 8×4
  order. Use the same heading convention as the hull and `RiverCamera`.
- Cargo uses the hull's waterline origin and world scale, not its visible-pixel
  bounding-box center. Encode the cargo-floor elevation in the authored pose.
  Keep one uniform sheet scale; permit measured per-frame translation only.
  Never repair changing geometry with per-frame stretching or flat image rotation.
- Deliver genuine transparent color PNGs without any hull, registration frame,
  floor/background, detached shadow, smoke, wake, labels or baked glow. Surface
  shading belongs in the art; animated illumination belongs in separate effects.
- Cut cargo behind the foreground coaming using the shared guide's depth rule.
  Cargo may rise above the rim, but its base must not paint across the near rim.
  Verify this on the actual accepted hull; a geometry guide alone cannot certify
  agreement with all painted source details.

## Runtime representation

All cargo types use one whole-load layer registered to the hull. Runtime draws
that layer with the same hull heading, world origin, scale and heave. It must not
repeat independently sized legacy sprites across arbitrary offsets to build the
load. `CargoArt` owns the predicate, source registration and uniform scale;
`SpriteAtlas` exposes the exact same source selection and transforms to every host.

The three-second unloading operation now transfers the whole registered load on
one rigid steel lifting tray. It remains opaque and hull-registered until pickup
at tick 24; then its unchanged artwork, scale and heading follow the crane's
world-space cargo origin. Keep the foreground coaming cutoff until the load clears
the rim. Lower onto a verified dry receiving pad, release at tick 132, and return
the crane to idle by tick 180. Delivered barges stay empty and towable; the latest
batch remains ashore until the next operation replaces it. This is an arcade
whole-batch lift, not a simulation of individual coal grabs or container picks.

`CargoTransfer` owns the choreography and `CargoView.lifted` preserves source
registration, surface lighting and all 32 headings. A separate model-based tray
supports loose material; it is not baked into the production sheets. Suspended
and landed cargo cannot trigger watercraft-only building fades. See
[offloading](../rendering/cargo-offloading.md). A future discrete-unit unloading
model still requires authored subloads with the same fit/depth contract, rather
than arbitrary repeated/scaled sprites.

## Required deliverables and metadata

Each family must provide the color sheet, reviewed 32-row registration CSV,
source/generation prompt and provenance, and effect metadata with explicit
`lights`, `smokestacks` and `tires` collections. These cargo collections are empty;
missing collections are not equivalent to empty. Cargo must not invent vessel
navigation, fender or exhaust fixtures. Preserve hull-owned anchor identities.

Deliver independent near-contact and soft cast shadow masks at all 32 headings.
Record their source, receiving plane, pivot/padding transform and alpha-derived
limitations. Follow [shadow generation](../art-prompts/shadow-masks-v1.md), and
regenerate the shared surface-lighting data when changing source pixels or
registration. Cargo shadow support must not silently double-darken the water under
the hull or project through its rim; choose the receiving plane explicitly.

Keep runtime textures cropped per frame, asset manifests complete for the
upcoming level and cache working sets bounded. Do not generate or blur textures
inside active draws. Follow the [performance rules](../performance/development-rules.md).

## Acceptance checks

Compile `tools/CargoAssetCheck.java` alongside shared core and run
`com.bargeboss.game.CargoAssetCheck`. It checks alpha packing, all 32 nonempty
source crops, exact duplicate content, single-sheet mapping, finite pivots, one
uniform scale and agreement with reviewed CSV metadata. It also checks explicit effect metadata, delivered contact/cast masks and
surface-lighting derivative presence. Cargo masks are delivered separately under
`docs/art-studies/` while runtime keeps the hull-only water shadow. Run `GenerateCargoShadows --check` and `GenerateSurfaceLighting --check` for derivative
freshness; `GenerateShadowMasks --check` covers the runtime hull/scenery masks.

Source color sheets may retain generated edge residue no greater than 2/255
alpha; the checker reports its pixel count and maximum instead of claiming exact
transparency. This narrowly bounded tolerance preserves generated originals and
still rejects visible/opaque neighboring-frame spill. Derived contact/cast shadow
gutters must have exactly zero alpha. Current accepted source boundary counts:
steel 6,869 (max2), containers76 (max1), coal v3:70 (max1); inspect actual composites
and record new counts when changing source sheets.

Pixel uniqueness is not proof of 32 geometrically correct angles. Alpha gutters
and high registration IoU do not prove perspective, texture coherence, anchor
identity or foreground-rim occlusion. Record these as separate manual findings.

Review raw source frames, anchor overlays, and actual-renderer loaded/empty/partial
unload composites on the production hull at all 32 headings, plus the 31→0 seam.
Use normal gameplay size and magnification on dark, light and water backgrounds.
Review glow, contact/cast shadows and shore clipping without letting effects mask
bad cargo fit. Check the same fixed anchor through heave and heading blending.
Reject or explicitly document remaining geometry drift; do not certify it solely
from having 32 populated cells.

Track accepted runtime PNGs and prompts; keep rejected candidates outside runtime
assets. Copy versioned review outputs to Downloads. Record measured dimensions,
validation limits and any remaining mismatch in the art notes and `optimization.md`.
Run the shared checks and the standing Android build/install/launch workflow.
Report actual native iOS/device validation limits rather than inferring them from
shared source behavior.
