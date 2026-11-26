#!/usr/bin/env python3
"""Original deterministic lasso Foley: a rope whoosh for the throw and a whip-crack catch.

No sampled recordings: filtered seeded noise, damped modes and a plucked-string twang.
Usage: python3 tools/generate_lasso_audio.py
Writes assets/audio/lasso-whoosh-v1.wav and assets/audio/lasso-hit-v1.wav.
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


def bandpass(noise, centre, quality):
    """Chamberlin state-variable band-pass with a per-sample centre frequency."""
    low = band = 0.0
    out = np.empty_like(noise)
    damp = 1 / quality
    for i, value in enumerate(noise):
        tune = 2 * math.sin(math.pi * min(centre[i], RATE / 6) / RATE)
        high = value - low - damp * band
        band += tune * high
        low += tune * band
        out[i] = band
    return out


def whoosh():
    """Rope swung hard: a rising, then falling, airy band with a fast whirr."""
    rng = np.random.default_rng(4107)
    count = int(RATE * 0.34)
    times = np.arange(count) / RATE
    progress = times / times[-1]
    noise = rng.uniform(-1, 1, count)
    # Pitch rises as the loop accelerates past the ear, then drops away (a doppler swing).
    centre = 380 + 2100 * np.sin(np.pi * np.clip(progress * 1.15, 0, 1)) ** 1.6
    air = bandpass(noise, centre, 2.2)
    edge = bandpass(noise, centre * 2.1, 5.0) * 0.35
    whirr = 0.75 + 0.25 * np.sin(2 * np.pi * (22 * times + 30 * times * times))
    envelope = np.sin(np.pi * np.clip(progress, 0, 1)) ** 1.3 * (0.6 + 0.4 * progress)
    save("lasso-whoosh-v1", (air + edge) * whirr * envelope, 0.62)


def hit():
    """Whip crack, a meaty rope thwack, a plucked line twang and a short cinch creak."""
    rng = np.random.default_rng(5233)
    count = int(RATE * 0.52)
    times = np.arange(count) / RATE
    noise = rng.uniform(-1, 1, count)
    # Crack: a 4 ms broadband snap, high-passed so it reads as a whip, not a thud.
    snap = np.diff(np.concatenate([[0], noise])) * np.exp(-times / 0.0018)
    snap += 0.5 * bandpass(noise, np.full(count, 4200.0), 3) * np.exp(-times / 0.006)
    # Thwack: the loop slapping the target.
    body = sum(
        amp * np.sin(2 * np.pi * hz * times + phase) * np.exp(-times * decay)
        for hz, amp, decay, phase in [
            (96, 0.55, 20, 0),
            (151, 0.3, 28, 0.4),
            (233, 0.12, 40, 1.1),
        ]
    )
    thump = bandpass(noise, np.full(count, 260.0), 1.2) * np.exp(-times / 0.035) * 0.6
    # Twang: the line snapping taut, a plucked string bending slightly flat as it settles.
    pitch = 182 * (1 - 0.06 * (1 - np.exp(-times * 9)))
    phase = 2 * np.pi * np.cumsum(pitch) / RATE
    twang = sum(
        np.sin(k * phase) / k**1.4 * np.exp(-times * (7 + 3 * k)) for k in range(1, 6)
    )
    twang *= np.clip((times - 0.012) / 0.006, 0, 1) * 0.45
    # Cinch: a brief fibrous creak as the loop tightens.
    creak = bandpass(noise, 1500 + 600 * np.sin(2 * np.pi * 17 * times), 7)
    creak *= np.exp(-(((times - 0.07) / 0.03) ** 2)) * 0.35
    attack = np.clip(times / 0.0007, 0, 1)
    save("lasso-hit-v1", (snap * 0.9 + body + thump + twang + creak) * attack, 0.78)


if __name__ == "__main__":
    whoosh()
    hit()
