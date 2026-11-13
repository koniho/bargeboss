#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/preview
mkdir -p build/preview
find core desktop -name '*.java' | sort > build/preview/sources.txt
echo tools/Preview.java >> build/preview/sources.txt
echo tools/AnimationPreview.java >> build/preview/sources.txt
echo tools/ScreenSizesPreview.java >> build/preview/sources.txt
echo tools/RotationPreview.java >> build/preview/sources.txt
echo tools/DesktopPainterTest.java >> build/preview/sources.txt
echo tests/com/bargeboss/game/ReplayInput.java >> build/preview/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/preview @build/preview/sources.txt
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.DesktopPainterTest
if [ "${1:-}" = '--idle-water' ] || [ "${1:-}" = '--title' ]; then
    sequence=idle-water
    if [ "${1:-}" = '--title' ]; then sequence=title; fi
    java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.AnimationPreview "$sequence"
    if command -v ffmpeg >/dev/null; then
        ffmpeg -hide_banner -loglevel error -y -framerate 12 \
            -i "out/animation-frames/$sequence/%04d.png" \
            -filter_complex '[0:v]scale=640:400:flags=lanczos,split[a][b];[a]palettegen=max_colors=128[p];[b][p]paletteuse=dither=bayer' \
            -loop 0 "out/concept-$sequence.gif"
    fi
    exit 0
fi
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.Preview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.ScreenSizesPreview

if [ "${1:-}" = '--animations' ]; then
    java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.AnimationPreview
    if command -v ffmpeg >/dev/null; then
        for sequence in title mooring results feeder feeder-blocked water scenery-fade; do
            ffmpeg -hide_banner -loglevel error -y -framerate 12 \
                -i "out/animation-frames/$sequence/%04d.png" \
                -filter_complex '[0:v]scale=640:400:flags=lanczos,split[a][b];[a]palettegen=max_colors=128[p];[b][p]paletteuse=dither=bayer' \
                -loop 0 "out/concept-$sequence.gif"
        done
    fi
fi

if [ "${1:-}" = '--rotation' ] || [ "${1:-}" = '--animations' ]; then
    java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.RotationPreview
    if command -v ffmpeg >/dev/null; then
        ffmpeg -hide_banner -loglevel error -y -framerate 30 \
            -i 'out/animation-frames/rotation/%04d.png' \
            -filter_complex '[0:v]split[a][b];[a]palettegen=max_colors=192[p];[b][p]paletteuse=dither=bayer' \
            -loop 0 out/concept-rotation.gif
    fi
fi
