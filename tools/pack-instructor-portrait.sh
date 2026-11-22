#!/bin/sh
set -eu
# Preserve the supplied animation canvas/disposal and white portrait matte.
# Runtime uses a small 3x2 atlas; no GIF decoder or per-frame resizing in play.
tmp=build/instructor-portrait
mkdir -p "$tmp"
magick assets/sprites/source/instructor/captain-talking.gif -coalesce -resize 192x192 "$tmp/frame-%d.png"
magick montage "$tmp"/frame-*.png -tile 3x2 -geometry 192x192+0+0 assets/sprites/instructor-portrait-v1.png

# Reviewed background extraction; keep the built-in edit alpha when sizing for runtime.
magick assets/sprites/source/instructor/captain-transparent.png -resize 576x384 assets/sprites/instructor-portrait-v2.png
