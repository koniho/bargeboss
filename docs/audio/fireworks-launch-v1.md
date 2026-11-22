# Fireworks launch audio

Historical v1 recipe; superseded by [fireworks and firefighting audio v2](fireworks-fire-v2.md).

Source: [Launching And Exploding Firework Sound Effect](https://orangefreesounds.com/launching-and-exploding-firework-sound-effect-free-wav/)
by Alexander / Orange Free Sounds, November 9, 2026.
[CC BY 4.0](https://creativecommons.org/licenses/by/4.0/); attribution and changes
ship in assets/audio/LICENSES.txt. The downloaded source is the public MP3 preview,
not the advertised high-resolution WAV. Launch occurs near 0.2s and explosion
near 1.3s. Only the launch is used, avoiding a prerecorded burst at the wrong time.

Runtime: 345ms, mono 44.1kHz, 16-bit PCM. Shared SoundEffects cue 9 fires on each
actual rocket launch; all native hosts load the shared manifest automatically.
Radio and very recent hull impacts take priority. Existing engine duck opens
space for the launch. No deferred queue: events older than 6 simulation ticks
are dropped. A clip fits inside the 400ms rocket interval without new audio voices.

Reproduce:
```sh
ffmpeg -v error -y -i docs/audio/sources/orangefreesounds/firework-launch-explode.mp3 -af 'atrim=start=0.145:end=0.49,asetpts=PTS-STARTPTS,highpass=f=70,lowpass=f=6500,volume=8dB,afade=t=in:d=0.006,afade=t=out:st=0.27:d=0.075,alimiter=limit=0.7:level=false:latency=true' -ar 44100 -ac 1 -c:a pcm_s16le assets/audio/firework-launch-recorded-v1.wav
```

Selected from source description and waveform timing/levels. Subjective listening
and final speaker balance require device audition; no claim of acoustic realism
beyond the source's recording description.
