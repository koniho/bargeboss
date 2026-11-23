# Reitanna collision thunks

Source: `thunk.wav` by Reitanna, Freesound sound 323725, supplied by the user.
https://freesound.org/people/Reitanna/sounds/323725/
License: CC0 1.0 Universal, verified on the source page on 2026-11-23.
https://creativecommons.org/publicdomain/zero/1.0/
Source SHA256: 9074bd2daea0d437beb6021df997c318e98e41f53c18e5fa5ea6f2865ac1d036
Retained original: `sources/reitanna/323725__reitanna__thunk.wav`.

`python3 tools/prepare-reitanna-thunks.py` reproduces the auditioned variants:

| Contact strength (closing speed) | Pitch | Source gain | Duration |
| --- | --- | --- | --- |
| Light, 3 to below 30 | Original | 0.65 | 0.351 s |
| Medium, 30 to below 65 | −3 semitones | 0.85 | 0.417 s |
| Heavy, 65 and above | −6 semitones | 1.00 | 0.496 s |

The initial 44.1 ms lead-in is trimmed. One-millisecond attack and 15 ms end fades
avoid edge clicks. Lower pitch also lengthens the body naturally; no added noise,
ring, splash or aggressive filtering. All variants are mono 44.1 kHz 16-bit PCM,
zero at both ends, unclipped, and reach 1% of their peak within 3 ms.

Every collision, including tug-to-tug, uses the same strength tiers. Radio priority,
contact cooldown, engine ducking, stale-event rejection and Sound settings remain
in force. Slot 5 remains reserved for compatibility and points to the medium thunk;
new collisions select slots 2–4. Hosts predecode these short samples during startup,
with no live pitch shifting or per-collision decoding.

These are edited object Foley, not recordings of real boat collisions. Signal and
routing checks cannot establish the subjective mix on phone speakers.
