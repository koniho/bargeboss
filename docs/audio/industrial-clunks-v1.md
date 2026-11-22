# Industrial collision clunks — 2026-11-21

User requested a more industrial clunk after the softened fender thuds. The
shared BUMP/THUD/CRUNCH assets now combine a pitched-down sampled metal impact
with the existing soft-impact body. Light contacts favor the body; medium/heavy
hits favor short metal resonance. Tug-to-tug retains its softer fender cue.
No synthetic static, delayed splash or added playback delay. Existing engine
volume dip and corrected gentle-seawall triggering are unchanged.

Source: Kenney Impact Sounds, CC0, verified on the official pack page:
https://kenney.nl/assets/impact-sounds

New original: `sources/kenney/impactMetal_heavy_000.ogg`
SHA256: e07045693e4a2b3d165c424e3dab4c781d9ff8880a386880ac89a51315d7f831
Downloaded from public redistribution (official ZIP returned403 in earlier work):
https://raw.githubusercontent.com/euuuuuuan/voidclad-public/main/assets/sfx/kenney/impactMetal_heavy_000.ogg
Mirror's CC0 inventory and exact hash match:
https://github.com/euuuuuuan/voidclad-public/blob/main/CREDITS.md
Existing Kenney source license: `licenses/kenney-impact.txt`.
Soft body original/provenance remains in `licensed-cues-v1.md`.

Reproduce with `python tools/prepare-industrial-clunks.py`. Metal is pitched down
16%, filtered100–2300Hz and damped exponentially. Body is filtered80–1400Hz.
Both onsets trimmed/aligned; final DC removal,2ms attack and45ms fade-out.

| Variant | Duration | Peak | Metal/body weights |
| --- | --- | --- | --- |
| Light | 190ms | .60 | .40/.80 |
| Medium | 300ms | .73 | .85/.65 |
| Heavy | 400ms | .80 | 1.00/.70 |

Mono44.1kHz16-bit. All edges zero; no clipping; onset at5% peak within0.4ms.
These are edited metal Foley, not claimed recordings of actual barge collisions.
Source character and signal checks guided selection; phone-speaker audition is
still a subjective user-review step.
