# Collision thuds v2 — 2026-11-21

The reported delayed static was reproducible in the event path: the impact emitted
its contact cue on the next fixed update, but the collision spray also emitted a
synthesized water-splash cue when its first particle returned to water. Depending
on impact strength, this happened roughly 0.35–1.1 seconds later, followed by up
to 0.92 seconds of noise-based splash audio. This was a separate event, not queued
playback of the original impact. Removed this landing audio; spray motion and
rendering remain unchanged. Teacher collision warnings still intentionally react
after 30 ticks and remain separate radio transmissions.

Use the CC0 [Kenney Impact Sounds](https://kenney.nl/assets/impact-sounds) soft
medium/heavy samples already retained in `sources/kenney`. The official source
page was checked again for CC0 licensing. An attempt to download additional
variants returned HTTP403; no unknown or unlicensed substitute was used.

`tools/prepare-collision-thuds.py` creates three onset-trimmed PCM variants:

| Cue | Source | Duration | Peak | Low-pass |
| --- | --- | --- | --- | --- |
| Bump | impactSoft_medium_000.ogg | 0.115 s | 0.20 | 650 Hz, two passes |
| Thud / tug fender | impactSoft_heavy_000.ogg | 0.250 s | 0.27 | 550 Hz, two passes |
| Hard contact | impactSoft_heavy_000.ogg | 0.320 s | 0.34 | 450 Hz, two passes |

35 Hz high-pass, DC removal, 4 ms attack and 40 ms tail fades; no added noise,
metal rattle or water layers. Mono44.1kHz16-bit, same shared asset manifest on all
hosts. Onsets exceed 5% of peak within1.3–1.5ms; edges are zero, no clipping.
Original source hashes and license notices remain in `licensed-cues-v1.md`.

Impacts expire after six simulation ticks (100ms) rather than30ticks so a late
host cannot replay half-second-old contact events. This is a simulation freshness
limit, not a measurement or guarantee of wall-clock/device audio latency.
Regression exercises contact→effect update→entire spray lifetime, requiring one
immediate thud and no landing cue. Radio priority and contact cooldown remain.

Sound selection/processing is based on source descriptions and signal analysis;
subjective speaker audition remains for user review. Do not claim measured
native sound latency from these checks.

Mix revision: user reported thuds masked by diesel. Peaks are now0.40/0.54/0.68
(+6dB), paired with a short engine dip; see engine-feedback-v1.md. Timing and
spectral processing above remain unchanged.

## Seawall follow-up

A real tutorial-seawall probe emitted BUMP at30 speed and THUD at60; gentle impacts
could drop below the previous8 speed threshold. Lowered to3, matching the existing
splash/contact threshold; resting/jitter below3 stays silent. Audio captures peak
contact before visual spray cooldown, so a stronger subsequent contact cannot be
hidden by a prior weaker spray. Added a real Game.step + training seawall test
at5 speed and a same-pair severity escalation test.

Phone audibility: restored one low-pass at1600/1400/1200Hz (light/medium/heavy),
80Hz high-pass, peaks0.60/0.70/0.80. The previous two low-pass passes at450–650Hz
made the softened body especially bass-heavy. Still the same short sampled thuds
with faded edges, no synthetic water/noise layer. Engine ducking retained.

Opt-in developer scene `seawall-audio` coasts a tug into the real training seawall.
`--ez bb_audio_trace true` on debug builds logs dispatched cue, simulation tick,
asset and SoundPool stream ID. A positive stream proves native dispatch, not
speaker audibility or measured acoustic latency. No logging in normal play.

Native validation: installed debug build, ran `seawall-audio` live with tracing.
Android logged `cue=3 stream=3 tick=12 asset=audio/fender-medium-kenney-v2.wav`:
the real wall collision reached SoundPool and obtained a valid playback stream.
No sound-delay inference from this log; no microphone/speaker recording made.
Returned to ordinary title launch after the diagnostic.
