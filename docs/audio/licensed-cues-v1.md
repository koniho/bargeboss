# Restrained radio, contact and tutorial cues

Selected 2026-11-20. All five source clips are CC0, free for commercial use,
editing and redistribution; attribution is retained voluntarily.

| Role | Author/source | Original |
| --- | --- | --- |
| Radio receive | [Joseph SARDIN / BigSoundBank](https://bigsoundbank.com/walkie-talkie-conversation-s0591.html) | 0591.mp3, Midland G7 radio background burst |
| Radio reply | [Joseph SARDIN / BigSoundBank](https://bigsoundbank.com/talkie-walkie-bips-touches-s0590.html) | 0590.mp3, Midland G7 keypad tone |
| Light contact | [Kenney Impact Sounds](https://kenney.nl/assets/impact-sounds) | impactSoft_medium_000.ogg |
| Heavy contact / tug fender | Kenney Impact Sounds | impactSoft_heavy_000.ogg |
| Tutorial milestone | [Kenney Interface Sounds](https://kenney.nl/assets/interface-sounds) | confirmation_001.ogg |

Kenney license files are retained in `licenses/`. BigSoundBank's source pages
identify both sounds as CC0 and permit commercial use, modification and redistribution.
License: https://creativecommons.org/publicdomain/zero/1.0/
The original public audio files were downloaded from
https://bigsoundbank.com/UPLOAD/mp3/0590.mp3?v=d and
https://bigsoundbank.com/UPLOAD/mp3/0591.mp3?v=d.
These MP3 sources are compressed previews, not lossless original recordings.
Kenney originals came from the official site's downloadable ZIP packages.

Run `PATH=/opt/homebrew/bin:$PATH python3 tools/prepare-licensed-audio.py`
with ffmpeg installed to regenerate the five runtime WAVs. Exact excerpts,
filters and peak levels are in that script. Sources stay under `docs/audio/sources`
and are not bundled in the app. Runtime files are mono 44.1 kHz 16-bit PCM.
Radio is band-limited; impacts are low-passed; the heavy impact and confirmation
are pitched down 10 percent. Short fades and conservative peaks soften transients.
The intended palette is practical harbor equipment and muffled rubber-fender
contact, with a restrained confirmation. No actual boat-impact recording is claimed.

Tutorial milestones coalesce into one queued confirmation; radio has priority.
The pending tone expires after two seconds during a long radio exchange so an
old reward does not sound out of context.
Skipping a lesson does not earn a completion sound. Native adapters consume the
same shared cue manifest. Existing water splashes remain unchanged.

Source selection uses descriptions and signal inspection. Subjective audition
and the final engine/radio mix on device speakers remain unvalidated; no device
installation was performed because the user reserved the phone for another agent.

## Original SHA-256

- `063564703b6094d70718a3e787a55cc9141611e4ecd6b6637f8828f79b4a8c3a` — `docs/audio/sources/kenney/confirmation_001.ogg`
- `49e7ca88743fca974bb8676ea138b751cfd8f9033b5e7af8736c2a215d6edbc1` — `docs/audio/sources/kenney/impactSoft_heavy_000.ogg`
- `7d3ba0bb5e60a11b5d3e558c141303dcf494256675fbf753c0d252d2cf0481e3` — `docs/audio/sources/kenney/impactSoft_medium_000.ogg`
- `581dad45781ff5f5cea09a945caa2082d45a56043baf204b0b0e63c686b17c1e` — `docs/audio/sources/bigsoundbank/0590.mp3`
- `c8ba39e51c0daea8bb359773eb7ea8aded2af989a2f1ee39b41624eeacf2ac9d` — `docs/audio/sources/bigsoundbank/0591.mp3`
