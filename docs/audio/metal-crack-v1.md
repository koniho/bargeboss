# Archived Tutmozis Metal Crack collisions

User-selected replacement for the Reitanna thunk, 2026-11-23.
Source: Metal Crack by Tutmozis, Pixabay sound 606770.
https://pixabay.com/sound-effects/film-special-effects-metal-crack-606770/
License: Pixabay Content License (verified on the source page on 2026-11-23).
https://pixabay.com/service/license-summary/
Not CC0; not covered by the code license. Incorporated into the game, not offered
as a standalone sound library.

The original and generated variants are not in the current public tree. Public
builds use the existing Reitanna CC0 collision thunks. With permission to use a
privately held source, run `python3 tools/prepare-metal-crack.py /path/to/original.mp3`
with ffmpeg to regenerate the archived variants under ignored `.private/audio/`.
Arithmetic stereo averaging avoids downmix gain/clipping. Trim the pre-impact
lead-in and keep only source seconds 0.058975–0.250000, before the renewed
impact swell around 0.26 s. Fade the final 40 ms of that source interval before
pitching; apply 1 ms attack / 15 ms release fades after resampling. The original
source remains intact. All variants are mono 44.1 kHz PCM16, unclipped, with zero-valued edges.

The strength tiers remain −6 / −9 / −12 semitones (each six semitones below its previous pitch), with gain
0.65 / 0.85 / 1.00. Closing-speed thresholds are unchanged: 3–<30, 30–<65, ≥65.
These apply to tug-to-tug, other vessels and scenery contacts. Slot 5 also maps
to the medium variant for compatibility. Radio priority, cooldown, Sound
mute/level and engine ducking are unchanged; ambient music is unaffected.
These notes describe the prior Metal Crack configuration, not the active asset selection.

Source SHA256: 57fdb5d116be9474a3519f7f769e502e871765c2483815a11ecc4d5f3b581129

Generated light/medium/heavy durations are 0.270/0.321/0.382 s, with peaks
−5.43/−3.12/−1.70 dBFS and 1%-peak onset at 2.8/2.6/3.1 ms. Each decoded sample
is below 34 KiB, within the existing startup-predecoded one-shot path. Pitching
uses offline rate conversion, so the lower variants have longer tails. No live
pitch processing, extra voices or renderer changes.

2026-11-23 first-impact revision: inspected the source's 10 ms RMS envelope and
cut before the second swell. Generator validates format, duration against the
cut/pitch ratio, nonzero energy, unclipped peaks and zero-valued edges. This is
waveform validation, not a listening audition.

Historical validation for this revision: full shared and asset checks, Android build and
native encoding checks passed (replay 310d2a08:2:1350). Updated APK installed on
the development phone and launched. Device listening and iOS playback have not
been auditioned for this revision.
