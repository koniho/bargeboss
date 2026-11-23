# Tutmozis Metal Crack collisions

User-selected replacement for the Reitanna thunk, 2026-11-23.
Source: Metal Crack by Tutmozis, Pixabay sound 606770.
https://pixabay.com/sound-effects/film-special-effects-metal-crack-606770/
License: Pixabay Content License (verified on the source page on 2026-11-23).
https://pixabay.com/service/license-summary/
Not CC0; not covered by the code license. Incorporated into the game, not offered
as a standalone sound library.

Original: `sources/tutmozis/metal-crack-606770.mp3`.
Run `python3 tools/prepare-metal-crack.py` with ffmpeg to regenerate the derivatives.
Arithmetic stereo averaging avoids downmix gain/clipping. Trim the pre-impact
lead-in, retain the full metallic decay, and apply 1 ms attack / 15 ms release
fades. All variants are mono 44.1 kHz PCM16, unclipped, with zero-valued edges.

The strength tiers remain original pitch / −3 / −6 semitones, with gain
0.65 / 0.85 / 1.00. Closing-speed thresholds are unchanged: 3–<30, 30–<65, ≥65.
These apply to tug-to-tug, other vessels and scenery contacts. Slot 5 also maps
to the medium variant for compatibility. Radio priority, cooldown, Sound
mute/level and engine ducking are unchanged; ambient music is unaffected.
The previous thunk sources/derivatives remain archived but are no longer selected.

Source SHA256: 57fdb5d116be9474a3519f7f769e502e871765c2483815a11ecc4d5f3b581129

Generated light/medium/heavy durations are 1.012/1.204/1.431 s, with peaks
−5.46/−3.13/−1.69 dBFS and 1%-peak onset at 2.0/2.4/2.8 ms. Each decoded sample
is below 127 KiB, within the existing startup-predecoded one-shot path. No live
pitch processing, extra voices or renderer changes.

Validation: asset generation assertions, full shared checks, Android packaging
and native encoding checks, and iOS simulator build passed. JVM/J2ObjC replay and
drawing parity remains 310d2a08:2:1350. No Android device was connected, so the
updated APK could not be installed and the new clip was not auditioned on a phone.
