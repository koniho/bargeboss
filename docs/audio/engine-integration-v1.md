# Initial engine audio integration

`EngineAudio` in shared Java selects two loop paths, gains, playback rates and
play-state muting. Android SoundPool and iOS AVAudioEngine/Varispeed execute those
parameters. There are no native throttle, gear, tow or load heuristics. Desktop
playback is not implemented in this initial mobile integration.

- Idle: marine diesel loop at gain .42 and rate .86.
- Load rises: marine rate approaches 1.10; its gain reduces toward .28.
- Working growl: Dodge loop smoothly fades from zero above .12 load to gain .48
  at full load; playback rate varies from .78 to 1.12.
- Input is the existing smoothed `tug.engineLoad`, which includes thrust demand,
  tow tension and pushing contact. It does not depend on ground speed: a loaded,
  slow-moving tug can roar. Reverse uses the same engine character as forward.
- Both loops stay running during play to avoid retriggering on every throttle
  change. Menus, pause, help, finish and background stop them. Practice follows
  the active game. Android observes audio focus; iOS observes interruptions and
  uses Ambient audio so the silent switch is respected. System media volume
  controls loudness. Title audio and an in-game sound toggle are not added here.

`tools/build-engine-loops.py` creates assets offline using the previous study
FLACs: marine seconds 8–12.5, Dodge seconds 2–4, with a .16-second complementary
wrap crossfade. Both are mono 22050 Hz 16-bit PCM WAV, 4.34 and 1.84 seconds,
272538 total decoded bytes, below SoundPool's per-sample limit. No runtime
separation, resampling preparation, asset loading or decoding occurs in drawing.
iOS buffers decode at host initialization; Android asynchronously loads at startup.

This is an initial sound design pass, not clean semantic water isolation or a
recording of the same engine at multiple RPMs. The truck loop may retain audible
rev modulation; blending and loop seam quality need device listening. Source
licenses ship in assets/audio/LICENSES.txt. Full originals/previews are not shipped.
