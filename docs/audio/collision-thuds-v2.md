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
