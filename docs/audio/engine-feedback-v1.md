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
