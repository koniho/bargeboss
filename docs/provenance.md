# Provenance

The project adopts the user's existing DDDumpling cross-platform design:
pure Java game/renderer, a small Painter boundary, Android Canvas, and J2ObjC
translation into a UIKit/Core Graphics host. The iOS `bootstrap.sh` is copied
from that sibling project; build conventions and native adapter patterns are
adapted to the new, smaller game. DDDumpling was only read, never modified.

Game rules, stage layout, hull art, scenery and 5x7 glyphs are authored here.
The Calumet reference is thematic; no map or third-party visual assets are used.

J2ObjC 3.1 is an external Apache-2.0 toolchain. Its checksum-verified source
archive and generated runtime remain ignored. iOS builds gather upstream
LICENSE/NOTICE text into a bundled ThirdPartyNotices.txt. Review runtime and
dependency notices before any store release. No distribution license is granted
for this private game's source by this notice.
