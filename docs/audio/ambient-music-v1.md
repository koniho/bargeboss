# Archived soundtrack setup: A Long Way

“A Long Way” by SergePavkinMusic, Pixabay track 166385, supplied by the user.
Source: https://pixabay.com/music/build-up-scenes-a-long-way-166385/
General site license: https://pixabay.com/service/license-summary/

The source page also says “NOT for songs, apps, music libraries.” The user was
shown that restriction and explicitly replied “I have permission for game use”
on 2026-11-23. Integration relies on that separate permission; the track is not
CC0, is not covered by the repository code license, and no standalone
redistribution right is asserted. The original and full-length WAV are not included in the current public tree.
Public builds use a synthetic silent WAV and retain Music settings.

Record of the previous input: `a-long-way-166385.mp3` (not in this tree).
SHA256: 5bf68ead6d92a4d839525a87a67134273740926a6b4c4bdaf4860ba9db833f1e

With permission to use a privately held source, run
`python3 tools/prepare-ambient-music.py /path/to/original.mp3` with ffmpeg.
It writes the archived conversion to the ignored `.private/audio/` directory,
not to packaged assets. Preserve the full approximately 4:33 stereo track, with 1-second opening and
2-second closing fades. The same 44.1 kHz PCM16 WAV plays on every host, including
standard Java Sound without an MP3 plugin. This adds about 46 MiB on disk; it is
never decoded into a soundtrack-sized gameplay buffer. Android uses asynchronously
prepared MediaPlayer; iOS uses AVAudioPlayer; desktop uses a daemon, an 8 KiB
transfer buffer and a requested 16 KiB SourceDataLine buffer. No render-time file
reads, pitch DSP or soundtrack-sized Java allocations.

MusicAudio owns asset selection, base gain (22%), independent Music preference,
and radio ducking (25% of the music mix while an audible radio cue has priority).
Music plays across title, gameplay, menus and settings. Mute and background/focus
loss pause native playback while retaining its position. Changing location does
not restart the song. Sound mute leaves music running and suppresses radio ducking.
Looping is native; these transport clocks never feed gameplay or replay state.

Collision Foley is documented separately in metal-crack-v1.md.

Historical validation (2026-11-23): full shared checks and Android build passed; JVM/J2ObjC
replay/drawing signature is 310d2a08:2:1350. Final Android host recompilation and
Vulkan encoding checks passed after tightening the music-ready focus guard.
iOS simulator build passed. AVAudioPlayer logged successful playback at gain
0.110 (50% music setting), duration 273.084 seconds, then successful resume at
10.716 seconds after mute/unmute. Settings panel and live slider/mute states were
reviewed in the simulator. No Android phone was connected, so installation and
physical speaker/headphone mix validation remain outstanding. Desktop compiles;
physical Java Sound output and full-track loop boundary were not auditioned here.
