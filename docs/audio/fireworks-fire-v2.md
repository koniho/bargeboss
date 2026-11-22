# Fireworks and firefighting audio v2

Recorded launch, aerial bursts, timber fire and water-on-heat replace the silent
burst/fire response. No synthesized explosion or white-noise hiss is used.

Sources (downloaded public high-quality MP3 previews; not the original WAVs):

- Alexander / Orange Free Sounds, [Launching And Exploding Firework Sound Effect](https://orangefreesounds.com/launching-and-exploding-firework-sound-effect-free-wav/), November 9, 2026. CC BY 4.0. Existing retained source `sources/orangefreesounds/firework-launch-explode.mp3`.
- Alexander / Orange Free Sounds, [Firework Sound Effect](https://orangefreesounds.com/firework-sound-effect/), May 6, 2015. CC BY 4.0. `sources/orangefreesounds/firework-burst-2015.mp3`.
- pcaeldries, [FireBurning.wav](https://freesound.org/people/pcaeldries/sounds/28314/), January 7, 2007. CC BY 4.0. Real burning logs recorded with Nagra ARES-M/NM-MICS-II. `sources/freesound/fire-burning-28314.mp3`.
- d.n.audio.uk, [Water Sizzle](https://freesound.org/people/d.n.audio.uk/sounds/463370/), March 13, 2019. CC BY 4.0. Water poured/dropped onto a hotplate, recorded with Zoom H2n. `sources/freesound/water-sizzle-463370.mp3`.

License: https://creativecommons.org/licenses/by/4.0/
Attribution ships in `assets/audio/LICENSES.txt`; no endorsement is implied.
Reproduce all six PCM files with `python3 tools/prepare-fireworks-fire-audio.py`.
The script specifies excerpts, filtering, modest pitch variation for burst C,
compression for loops, peak normalization, fades and 350ms seam crossfades.

All runtime files are mono 44.1kHz 16-bit PCM. Launch lasts 630ms; three burst
variants last 1.16–1.18s; fire and sizzle loops last 4s and 2s. These are bounded,
predecoded playback assets; no runtime decoding or random audio synthesis.

Launch and burst are separate simulation events. Burst variants cycle
predictably; their tails overlap the next rocket. Radio/contact events stop the
firework voices, and late one-shots are dropped after six simulation ticks.
Hosts follow shared interruption rules and loop gains. Fire fades with heat and
distance; sizzle requires actual simulated water hits and fades after the last
hit. Environmental loops duck during radio messages. Pause/background silences
all voices. Native device listening remains necessary to judge speaker balance;
source descriptions and signal checks alone do not establish subjective quality.

Source SHA-256:
```
41a074e014e0fe30d9dd217943abed4bc089f10941061d793a878fe760101ef2  fire-burning-28314.mp3
01eda8bd203f9f7dc6f0cc82019606accda9b45867b603a7ad925c2b8f4b9f44  water-sizzle-463370.mp3
6ebe4682dea4998528400c5b4996fbb43795f66cc04743eeddbdc603d3156d15  firework-burst-2015.mp3
```
Generated file hashes, durations, peak/RMS and sizes are in
[the audio manifest](fireworks-fire-v2-manifest.json).
