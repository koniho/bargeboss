# Initial engine audio integration

`EngineAudio` in shared Java selects two loop paths, gains, playback rates and
play-state muting. Android SoundPool and iOS AVAudioEngine/Varispeed execute those
parameters. There are no native throttle, gear, tow or load heuristics. Desktop
engine playback is not implemented in this initial mobile integration. The later
[radio dialogue system](../gameplay/radio-dialogue.md) adds shared one-shot severity-selected hull impacts and CB
cues on Android, iOS and desktop, alongside these mobile engine loops.

- Idle: marine diesel loop at gain .42 and rate .72.
- Load rises: marine rate approaches .92; its gain reduces toward .28.
- Working growl: Dodge loop smoothly fades from zero above .12 load to gain .48
  at full load; playback rate varies from .68 to .96.
- Input is the existing smoothed `tug.engineLoad`, which includes thrust demand,
  tow tension and pushing contact. It does not depend on ground speed: a loaded,
  slow-moving tug can roar. Reverse uses the same engine character as forward.
- Both loops stay running during play to avoid retriggering on every throttle
  change. Menus, pause, help, finish and background stop them. Practice follows
  the active game. Android observes audio focus; iOS observes interruptions and
  uses Ambient audio so the silent switch is respected. System media volume
  controls loudness. Title audio and an in-game sound toggle are not added here.

`tools/build-engine-loops.py` creates assets offline using the previous study
FLACs: marine seconds 8–12.5, Dodge seconds 3.55–4.35 (sustained rev plateau only, envelope leveled), with a .16-second complementary
wrap crossfade. The marine loop additionally has a sixth-order, zero-phase
500 Hz low-pass after the first phone audition identified intrusive splashing.
Its 50–250 Hz throb is preserved; upper engine character comes from the truck.
This suppresses water noise rather than claiming perfect source isolation.
The working loop has an additional 1000 Hz low-pass; lower playback rates
also lower the audible cutoff. Both are mono 22050 Hz 16-bit PCM WAV, 4.34 and .64 seconds,
219618 total decoded bytes, below SoundPool's per-sample limit. No runtime
separation, resampling preparation, asset loading or decoding occurs in drawing.
iOS buffers decode at host initialization; Android asynchronously loads at startup.

This is an initial sound design pass, not clean semantic water isolation or a
recording of the same engine at multiple RPMs. The original 1.84-second truck excerpt repeated a rev swell that the user heard
as a splash every two seconds. The shorter sustained excerpt removes the attack
and decay; its residual envelope is leveled offline. Blend/seam quality still
needs device listening. Source
licenses ship in assets/audio/LICENSES.txt. Full originals/previews are not shipped.
