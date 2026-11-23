#!/usr/bin/env python3
"""Bake the reviewed original/-3/-6 semitone collision variants; no runtime DSP."""
from pathlib import Path
import array
import hashlib
import math
import subprocess
import wave

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/audio/sources/reitanna/323725__reitanna__thunk.wav'
RATE = 44100

def main():
    with wave.open(str(SOURCE), 'rb') as source:
        assert (source.getnchannels(), source.getsampwidth(), source.getframerate()) == (1, 2, RATE)
        samples = array.array('h', source.readframes(source.getnframes()))
    peak = max(map(abs, samples))
    onset = next(i for i, value in enumerate(samples) if abs(value) > peak * .01)
    start = max(0, onset / RATE - .002)
    for label, semitones, gain in [('light', 0, .65), ('medium', -3, .85), ('heavy', -6, 1.0)]:
        target = ROOT / f'assets/audio/collision-thunk-{label}-v1.wav'
        ratio = 2 ** (semitones / 12)
        filters = (f'atrim=start={start},asetpts=PTS-STARTPTS,asetrate={RATE * ratio},'
                   f'aresample={RATE},volume={gain},afade=t=in:d=0.001,'
                   'areverse,afade=t=in:d=0.015,areverse')
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(SOURCE), '-af', filters,
                        '-ac', '1', '-c:a', 'pcm_s16le', str(target)], check=True)
        with wave.open(str(target), 'rb') as clip:
            pcm = array.array('h', clip.readframes(clip.getnframes()))
        assert max(map(abs, pcm)) < 32767 and pcm[0] == pcm[-1] == 0
        onset_ms = next(i for i, value in enumerate(pcm) if abs(value) > max(map(abs, pcm)) * .01) / RATE * 1000
        assert onset_ms < 5 and len(pcm) / RATE < .51
        print(f'{target.name}: {len(pcm)/RATE:.3f}s, onset {onset_ms:.1f}ms, peak {20*math.log10(max(map(abs,pcm))/32768):.2f}dBFS')
    print('Source SHA256:', hashlib.sha256(SOURCE.read_bytes()).hexdigest())

if __name__ == '__main__':
    main()
