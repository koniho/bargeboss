# Provenance

The production three-quarter industrial tug is documented in
[isometric-tug-v5-review.md](art-studies/isometric-tug-v5-review.md), with its supplied
reference, built-in image generation prompt, candidate atlas and validation
limits. The approved source sheet is bundled unchanged as
`assets/sprites/tug-isometric-v5-32.png`, with registered pivots and calibrated
smoke points in shared Java. Its design and turntable prompts are
[isometric-tug-v5-design.txt](art-prompts/isometric-tug-v5-design.txt) and
[isometric-tug-v5-32.txt](art-prompts/isometric-tug-v5-32.txt).

The [static scenery trial](art-studies/scenery-trial/review.md) is bundled unchanged
as `assets/sprites/static-harbor-v1.png`; only its warehouse cell is placed in the
live scene. The [original prompt](art-prompts/scenery-trial-v1.txt) and measured
window-light anchors remain tracked. Trial quay cells are not used as level
geometry.

The project adopts the user's existing DDDumpling cross-platform design:
pure Java game/renderer, a small Painter boundary, Android Canvas, and J2ObjC
translation into a UIKit/Core Graphics host. The iOS `bootstrap.sh` is copied
from that sibling project; build conventions and native adapter patterns are
adapted to the new, smaller game. DDDumpling was only read, never modified.

Game rules, stage layout, shared rendering/animation code, procedural scenery
and 5x7 glyphs are authored here. Boat and cargo sprite artwork is generated as
described below. The Calumet reference is thematic; no real navigation map is used.

The user supplied the [graphical concept](reference/barge-boss-concept.png),
copied from their latest Pictures image on 2026-11-13. It guides the red/cream tug,
shaded rust barges, teal water, yellow machinery, sunset title and arcade
presentation. The concept sheet is retained as a development reference and is
not bundled into native builds. This note makes no claim about the source
image's original authorship or license.

## Generated production artwork

The built-in `image_gen` tool created the directional boat and cargo artwork
using that concept as the visual reference. The production transparent PNG
atlases are tracked in [assets/sprites](../assets/sprites/), and their generation
instructions are retained in [docs/art-prompts](art-prompts/). The first boat
prompts are [tug-atlas.txt](art-prompts/tug-atlas.txt) and
[barge-atlas.txt](art-prompts/barge-atlas.txt). The tug has a red hull, cream
pilothouse and tire fenders; the empty barge provides a common deck for separately
animated steel, freight and bulk loads.

Before the isometric conversion, each rotating asset used a corrected base sheet (`tug-16.png`, `barge-16.png`,
`steel-16.png`, `container-16.png`, `bulk-16.png`) and a corresponding `-half.png`
sheet with 16 newly generated intermediate views. Both physical sheets use 4×4
grids. The game interleaves base and intermediate cells to produce 32 logical
headings, clockwise from east in 11.25° steps. Existing base drawings are retained
rather than regenerated. The corresponding
[tug](art-prompts/tug-16-atlas.txt), [barge](art-prompts/barge-16-atlas.txt),
[steel](art-prompts/steel-16-atlas.txt), [container](art-prompts/container-16-atlas.txt)
and [bulk](art-prompts/bulk-16-atlas.txt) generation prompts are retained, along
with correction/extraction prompts and the intermediate-view generation prompts
in the same directory. Earlier eight-heading prompts remain as
provenance; superseded PNGs are recoverable through Git history.

The game chooses headings, per-frame registration points and phase effects in
pure Java, then native hosts draw the same assets. Per-frame X/Y scale calibration
matches intermediate drawings to neighboring base footprints while registration
pivots compensate for unequal padding. Adjacent registered drawings
are blended over a brief 2.25° interval with weighted premultiplied colors and
alpha, keeping their shared
footprint anchor fixed and avoiding an opacity dip. This is interpolation
between painted views, not a skeletal rig or a live 3D rendering pipeline. Smoke, wakes, aiming guides, thrown/retracting ropes,
line-tightening effects, unloading equipment and score celebrations are shared
runtime animations rather than alternate platform-specific asset sequences.
The title screen also uses the production boat art within its animated harbor.

The landscape presentation keeps the whole river visible in a responsive logical
interface based on 960×600. The concept's other illustrated levels and progression menus remain
visual references; they do not imply that extra playable stages or upgrade
systems have been implemented.

J2ObjC 3.1 is an external Apache-2.0 toolchain. Its checksum-verified source
archive and generated runtime remain ignored. iOS builds gather upstream
LICENSE/NOTICE text into a bundled ThirdPartyNotices.txt. Review runtime and
dependency notices before any store release.

## Open-source license scope

The original project source, tooling, skills and documentation are distributed
under the root [MIT License](../LICENSE). Project-generated artwork in
`assets/sprites/` and `docs/art-studies/` is included under the same permission
to the extent the contributors hold rights in it. This does not assert that
AI-generated images have exclusive copyright protection.

User-supplied concept/reference images in `docs/reference/` are retained as
design references and are excluded from the project's MIT grant; their original
authorship/license has not been established here. Third-party dependencies,
upstream license texts and notices keep their own licenses. The project's MIT
license does not override those notices or grant rights in third-party material.
