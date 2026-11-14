#!/bin/sh
# Render the isolated art candidate without changing gameplay's shared atlas.
set -eu
cd "$(dirname "$0")"
mkdir -p build/tug-concept
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/tug-concept tools/TugConceptPreview.java
java -Djava.awt.headless=true -cp build/tug-concept com.bargeboss.game.TugConceptPreview
if command -v ffmpeg >/dev/null; then
    ffmpeg -hide_banner -loglevel error -y -framerate 6 \
        -i out/isometric-tug/frames/%02d.png \
        -filter_complex '[0:v]split[a][b];[a]palettegen=max_colors=192[p];[b][p]paletteuse=dither=bayer' \
        -loop 0 out/isometric-tug/turntable.gif
fi
