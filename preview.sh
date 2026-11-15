#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/preview
mkdir -p build/preview
find core desktop -name '*.java' | sort > build/preview/sources.txt
echo tools/GenerateSurfaceLighting.java >> build/preview/sources.txt
echo tools/SurfaceLightingReview.java >> build/preview/sources.txt
echo tools/GenerateShadowMasks.java >> build/preview/sources.txt
echo tools/SpriteAffinePixelTest.java >> build/preview/sources.txt
echo tools/GenerateVesselGlow.java >> build/preview/sources.txt
echo tools/VesselGlowReview.java >> build/preview/sources.txt
echo tools/BuildingFadeReview.java >> build/preview/sources.txt
echo tools/ShoreOcclusionPixelTest.java >> build/preview/sources.txt
echo tools/FoamPixelTest.java >> build/preview/sources.txt
echo tools/GoalLevelReview.java >> build/preview/sources.txt
echo tools/BerthSwapReview.java >> build/preview/sources.txt
echo tools/LandMarkerReview.java >> build/preview/sources.txt
echo tools/BuoySpriteReview.java >> build/preview/sources.txt
echo tools/BuoyReview.java >> build/preview/sources.txt
echo tools/NavigationReview.java >> build/preview/sources.txt
echo tools/VesselNavigationReview.java >> build/preview/sources.txt
echo tools/JoystickReview.java >> build/preview/sources.txt
echo tools/TrainReview.java >> build/preview/sources.txt
echo tools/WaterlineReview.java >> build/preview/sources.txt
echo tools/ShorelineReview.java >> build/preview/sources.txt
echo tools/SoftCueReview.java >> build/preview/sources.txt
echo tools/EffectsPreview.java >> build/preview/sources.txt
echo tools/Preview.java >> build/preview/sources.txt
echo tools/AnimationPreview.java >> build/preview/sources.txt
echo tools/ScreenSizesPreview.java >> build/preview/sources.txt
echo tools/RotationPreview.java >> build/preview/sources.txt
echo tools/StaticLayerPixelTest.java >> build/preview/sources.txt
echo tools/DesktopPainterTest.java >> build/preview/sources.txt
echo tools/GenerateOcclusionMasks.java >> build/preview/sources.txt
echo tools/TitleLayoutReview.java >> build/preview/sources.txt
echo tools/OcclusionPreview.java >> build/preview/sources.txt
echo tools/ViewportPixelTest.java >> build/preview/sources.txt
echo tools/VesselLightPixelTest.java >> build/preview/sources.txt
echo tests/com/bargeboss/game/ReplayInput.java >> build/preview/sources.txt
javac -Xlint:-options --release 8 -encoding UTF-8 -d build/preview @build/preview/sources.txt
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.GenerateOcclusionMasks --check
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.GenerateVesselGlow --check
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.GenerateShadowMasks --check
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.GenerateSurfaceLighting --check
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.SurfaceLightingReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.SpriteAffinePixelTest
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.TitleLayoutReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.OcclusionPreview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.StaticLayerPixelTest
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.DesktopPainterTest
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.ViewportPixelTest
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.VesselLightPixelTest
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
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.BuildingFadeReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.ShoreOcclusionPixelTest
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.FoamPixelTest
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.GoalLevelReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.BerthSwapReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.LandMarkerReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.BuoySpriteReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.BuoyReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.NavigationReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.VesselNavigationReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.JoystickReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.TrainReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.WaterlineReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.ShorelineReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.SoftCueReview
java -Djava.awt.headless=true -cp build/preview com.bargeboss.game.EffectsPreview

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
