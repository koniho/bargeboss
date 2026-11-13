# Provenance

The project adopts the user's existing DDDumpling cross-platform design:
pure Java game/renderer, a small Painter boundary, Android Canvas, and J2ObjC
translation into a UIKit/Core Graphics host. The iOS `bootstrap.sh` is copied
from that sibling project; build conventions and native adapter patterns are
adapted to the new, smaller game. DDDumpling was only read, never modified.

Game rules, stage layout, rendered hull art, scenery and 5x7 glyphs are authored
here. The Calumet reference is thematic; no real navigation map is used.

The user supplied the graphical concept retained at
`docs/reference/barge-boss-concept.png`, copied from their latest Pictures image
on 2026-11-13. It guides the red/cream tug, shaded rust barges, teal water, yellow
machinery, sunset title and arcade presentation. The shipped game draws its own
geometry and animations through the shared `Painter`; the reference sheet is
not bundled into native builds. This note makes no claim about the source
image's original authorship or license.

J2ObjC 3.1 is an external Apache-2.0 toolchain. Its checksum-verified source
archive and generated runtime remain ignored. iOS builds gather upstream
LICENSE/NOTICE text into a bundled ThirdPartyNotices.txt. Review runtime and
dependency notices before any store release. No distribution license is granted
for this private game's source by this notice.
