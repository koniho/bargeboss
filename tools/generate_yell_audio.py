#!/usr/bin/env python3
"""Original deterministic cartoon yell for a struck swimmer: a panicked "WAAAH!" and a gulp.

No sampled recordings: a jittered glottal pulse train through "ah" formants, then a gurgle.
Usage: python3 tools/generate_yell_audio.py
Writes assets/audio/swimmer-yell-v1.wav.
"""

import math
import wave
from pathlib import Path

import numpy as np

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / "assets/audio"


def save(name, samples, peak):
    """DC removal, edge fades and headroom avoid clicks and clipping."""
    samples = np.asarray(samples, dtype=np.float64)
    samples -= samples.mean()
    count = len(samples)
    index = np.arange(count)
    samples *= np.minimum(1, np.minimum(index / 88, (count - 1 - index) / 441))
    samples *= peak / np.max(np.abs(samples))
    pcm = np.round(samples * 32767).astype("<i2")
    output = wave.Wave_write(str(ROOT / (name + ".wav")))
    try:
        output.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        output.writeframes(pcm.tobytes())
    finally:
        output.close()
    print(name, round(count / RATE, 3), "seconds; peak", peak)


def resonator(source, centre, bandwidth):
    """Two-pole formant resonator with a per-sample centre frequency."""
    out = np.empty_like(source)
    radius = math.exp(-math.pi * bandwidth / RATE)
    previous = before = 0.0
    for i, value in enumerate(source):
        cosine = math.cos(2 * math.pi * centre[i] / RATE)
        current = value + 2 * radius * cosine * previous - radius * radius * before
        out[i] = current
        before = previous
        previous = current
    return out


def gurgle(times, rng):
    """Gulp and gurgle: bubbly, upward-chirping blips as the head dips under."""
    out = np.zeros(len(times))
    for k in range(7):
        start = 0.62 + k * 0.045 + 0.01 * rng.uniform()
        age = times - start
        live = age > 0
        hz = 260 + 140 * rng.uniform()
        chirp = np.sin(2 * np.pi * hz * (1 + 2.5 * age[live]) * age[live])
        out[live] += chirp * np.exp(-age[live] / 0.03)
    return out


def yell():
    """Pitch leaps up in alarm, wobbles with panic, then breaks and drops into a gulp."""
    rng = np.random.default_rng(9031)
    count = int(RATE * 0.95)
    times = np.arange(count) / RATE
    progress = times / times[-1]
    # Alarm leap, a ragged hold, and the voice cracking down as the swimmer goes under.
    contour = (
        330
        + 300 * np.clip(times / 0.08, 0, 1)
        - 330 * np.clip((progress - 0.55) / 0.4, 0, 1) ** 1.5
    )
    wobble = (
        1
        + 0.06 * np.sin(2 * np.pi * 9 * times)
        + 0.02 * rng.standard_normal(count).cumsum() / 60
    )
    pitch = np.maximum(110, contour * wobble)
    phase = np.cumsum(pitch) / RATE
    # Glottal pulses: a sharpened sawtooth, slightly breathy.
    pulse = (phase % 1.0) ** 2.2 - 0.32
    source = pulse + 0.12 * rng.uniform(-1, 1, count)
    # "Waaah": the vowel opens from w (low F1/F2) into a wide ah, then closes as it drops.
    opening = np.clip(times / 0.07, 0, 1)
    first = 350 + 470 * opening - 180 * np.clip((progress - 0.7) / 0.3, 0, 1)
    second = 750 + 520 * opening
    voice = (
        resonator(source, first, 90)
        + 0.6 * resonator(source, second, 120)
        + 0.25 * resonator(source, np.full(count, 2650.0), 200)
    )
    loud = np.clip(times / 0.025, 0, 1) * (
        1 - 0.65 * np.clip((progress - 0.6) / 0.4, 0, 1)
    )
    voice *= loud
    voice /= np.max(np.abs(voice))
    save("swimmer-yell-v1", voice + 0.35 * gurgle(times, rng), 0.8)


if __name__ == "__main__":
    yell()
