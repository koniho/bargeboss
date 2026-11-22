# Engine mix and tactile feedback — 2026-11-21

User could not hear the quiet thuds over the engine. Raised all three collision
sample peaks by6dB (0.40/0.54/0.68) without changing filters, duration or clean
fades. On an accepted impact, shared SoundEffects holds engine gain at28% for
100ms, then smoothsteps back to100% over300ms. The envelope has its own age so a
later radio cue cannot abruptly restore the engines. Idle/work balance and pitch
are unchanged. All hosts consume EngineAudio.gain; no native-specific mixing rule.

EngineHaptics provides a separate, bounded pending pulse, avoiding interference
with existing semantic event IDs/priority. Idle cadence48ticks (1.25Hz), full-load
cadence18ticks (3.33Hz); low/high strength selected from engine load. No catch-up
queue. Disabled during conversations, nonplay and haptics-off. Input cancellation
resets the pending pulse; other semantic feedback suppresses rumble for24ticks.
Android maps low/high to system CLOCK_TICK/VIRTUAL_KEY, respecting system haptic
preferences. iOS maps to light impacts with0.15/0.35 intensity where available.
Desktop has no tactile device adapter. Hardware feel differs by device despite
shared cadence and load selection. No continuous vibration or per-frame allocation.

EngineAudioTest covers duck headroom/recovery and unchanged pitch, idle/load pulse
counts, one-shot consumption, impact priority, haptics-off and pause cancellation.
Subjective mix and tactile strength require phone review; automated checks do not
prove how loud or strong feedback feels.

## Continuous rumble revision

User rejected individual pulses. EngineHaptics now exposes a sustained, smoothly
ramped strength (0.10 idle→0.28 loaded), never a tap queue. Android prebuilds five
low-amplitude continuous waveforms with small variations and no motor-off slots;
only strength-band changes replace the running waveform. VIBRATE permission is
normal/install-time. Game and system haptics settings are both respected. Stop
before semantic haptics, on background, settings-off, conversation and nonplay.
Devices without amplitude control omit engine rumble rather than emit hard taps.
iOS uses a low-sharpness continuous Core Haptics event, looping and updating its
intensity. CoreHaptics.framework is included. iOS implementation is unbuilt here.

References: Android VibrationEffect.createWaveform timing/amplitude API and Apple
CHHapticAdvancedPatternPlayer looping/dynamic parameter API. Native hardware has
its own tactile response; shared strength does not imply identical physical feel.

https://developer.android.com/reference/android/os/VibrationEffect
https://developer.apple.com/documentation/corehaptics/chhapticadvancedpatternplayer

## Frequency-tuning investigation

Phone reports FREQUENCY_CONTROL and CAP_COMPOSE_PWLE_EFFECTS_V2; resonant
frequency134.58331Hz, reported frequency-profile bounds50.0–170.75Hz. Current
Android amplitude-only waveform leaves carrier frequency to the hardware. The
192ms modulation pattern (~5.2Hz) varies intensity, not that carrier frequency.
Android API36+ WaveformEnvelopeBuilder can specify amplitude/frequency/time
points; implementation must query areEnvelopeEffectsSupported and supported
frequency/intensity limits. Lower-frequency output can be weaker. Suggested
next audition70–90Hz, low intensity, smooth attack/release. Investigation only;
no frequency-control implementation in the industrial-clunk round. Device dump
playback history was discarded; retain only capability facts above.

## Lowest-frequency idle implementation

User requested lower actual frequency, then the lowest supported frequency at
idle. Android now queries the public API36 envelope capability/frequency profile.
Idle and fade-in use exactly the reported minimum (50Hz on development phone).
Five shared load bands rise toward90Hz, clamped to hardware range. The smoothed
shared amplitude selects bands; idle .10 stays in band1 (not band2 as a linear
0–.28 scale would do). All effects are prepared once.120ms attack then constant
low-frequency envelope segments; no5.2Hz amplitude modulation. The finite envelope
is repeated by the public createRepeatingEffect API; loop renewal includes the
short attack. Strength band transitions replace the native effect.

Public API reflection is isolated in EngineRumble because the retained build SDK
predates36. No private/hidden API access. Probe hardware min/max frequency, envelope
point count and point duration constraints. Unsupported/error cases retain a
quieter steady amplitude-only fallback; it cannot promise lower carrier frequency.
iOS sharpness is set to its lowest0; Core Haptics does not expose equivalent Hz.

Debug profile extra bb_haptic_review=true explicitly enables haptics for native
verification; ordinary performance profiles remain haptics-free. bb_audio_trace
also logs frequency capability and selected rumble band/Hz. Stop/priority/settings
behavior remains unchanged.
