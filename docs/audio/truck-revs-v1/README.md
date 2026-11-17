# Diesel truck rev overlay auditions

Downloaded 2026-11-17. Both Freesound source pages identify **CC0 1.0**.
These are public HQ previews, not login-protected original downloads. Originals
of the downloaded previews are preserved in `../sources/`.

| Source | Recording | Public preview |
| --- | --- | --- |
| [canucklovingbrit, 478125](https://freesound.org/people/canucklovingbrit/sounds/478125/) | Truck-Diesel 07Dodge Rev, about 7.6 seconds | https://cdn.freesound.org/previews/478/478125_1903175-hq.mp3 |
| [DAVESTALKER, 443319](https://freesound.org/people/DAVESTALKER/sounds/443319/) | Modified Ford F350 start, idle, revs, about 26.6 seconds. Creator reports background talking/directing. | https://cdn.freesound.org/previews/443/443319_2417207-hq.mp3 |

License: https://creativecommons.org/publicdomain/zero/1.0/

Both truck derivatives reduce source gain to .5 before filtering to prevent
filter/decoder overshoot clipping.

`dodge-rev-overlay-v1.flac` uses a 65 Hz high-pass and 3200 Hz low-pass, 40 ms
fade-in and 250 ms fade-out starting at 7.35 s, resampled to 48 kHz. This is a
short throttle transient candidate, not a seamless sustained-load loop.

`truck-revs-overlay-candidate-v1.flac` is the longer Ford candidate, filtered
55–2800 Hz with 50 ms fade-in and 200 ms fade-out at 26.4 s. Filtering does not
remove background speech; do not ship the full clip without selecting clean takes.

`marine-diesel-plus-dodge-rev-demo-v1.mp3` is a 12-second audition: marine
engine stem seconds 5–17 at gain .70, with the Dodge recording starting at second
2 at gain .28. Mixed without automatic normalization; a .95 limiter with no
makeup gain guards peaks. This is a proposed balance, not listening-approved.

No runtime audio or engine-load control has been added yet. Playback integrity
and signal peaks are checked; perceptual quality, voice-free segments and
in-game load transitions still need listening review. Phone was disconnected.
