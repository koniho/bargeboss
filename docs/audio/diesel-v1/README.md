# Fishing-boat diesel separation audition, v1

Source: [Diesel motor on Fishing Boat.MP3 by Bansemer](https://freesound.org/people/Bansemer/sounds/35499/).
The source page lists **CC0 1.0**, verified 2026-11-17:
https://creativecommons.org/publicdomain/zero/1.0/
The description identifies a large diesel recorded near the transom, with bilge
water pumping in the same recording.

The retained file in `../sources/` is the publicly available **HQ MP3 preview**,
not the login-protected original:
https://cdn.freesound.org/previews/35/35499_341440-hq.mp3
Decoded length: 40.416979 seconds, 48 kHz stereo. Re-encoding as FLAC does not
restore detail lost in the source MP3.

## Audition files

- `diesel-focused-v1.flac`: reduced broadband/transient energy, retained low
  exhaust throb and persistent harmonics.
- `water-noise-residual-v1.flac`: the complementary residual. Includes engine
  rattle and exhaust noise as well as water; it is NOT an isolated water recording.
- `comparison-original-engine-residual-v1.mp3`: 5–15 seconds of each, in that
  order, with one-second silent gaps and matched gain. Total 32 seconds.

This is a signal-processing draft, not a listening-approved separation. A
stereo-linked soft spectral mask estimates persistent harmonics and retains bass
below approximately 260 Hz. Both sources overlap, so low water noise remains in
the diesel and some machinery enters the residual. The source is preserved.
Do not describe the residual as clean water or ship these studies automatically.
There are no runtime audio changes in this round.

## Reproduce

Use an isolated Python environment with numpy, scipy and soundfile, and ffmpeg:

```sh
ffmpeg -i docs/audio/sources/bansemer-35499-diesel-fishing-boat-hq-preview.mp3 -c:a pcm_f32le /tmp/diesel-source.wav
python tools/separate-diesel-water.py /tmp/diesel-source.wav docs/audio/diesel-v1
ffmpeg -i docs/audio/diesel-v1/comparison-original-engine-residual-v1.wav -c:a libmp3lame -b:a 192k docs/audio/diesel-v1/comparison-original-engine-residual-v1.mp3
```

The FLAC stems share one gain and sum to the source at that gain (within 24-bit
quantization). `separation-report.json` records peaks and reconstruction error.
No stem clips. Spectral checks establish processing integrity, not perceptual
source separation quality. The disconnected development phone prevented a
Downloads copy and on-phone audition in this round.
