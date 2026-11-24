#!/usr/bin/env python3
"""Bake Tutmozis Metal Crack at -6/-9/-12 semitones, first impact only; no runtime DSP."""
from argparse import ArgumentParser
from pathlib import Path
import array
import hashlib
import math
import subprocess
import wave

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / '.private/audio'
RATE = 44100
# Retain the first strike; the renewed impact swell starts around 0.26 s.
# Fade its cut tail before pitching so each variant has the same source envelope.
FIRST_IMPACT_END = .250
TAIL_FADE = .040

def main():
    parser = ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path, help='Privately held source MP3')
    source = parser.parse_args().source
    OUTPUT.mkdir(parents=True, exist_ok=True)
    raw = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(source),
                                   '-af', 'pan=mono|c0=0.5*c0+0.5*c1', '-ar', str(RATE), '-f', 's16le', '-'])
    samples = array.array('h', raw)
    peak = max(map(abs, samples))
    onset = next(i for i, value in enumerate(samples) if abs(value) > peak * .01)
    start = max(0, onset / RATE - .002)
    for label, semitones, gain in [('light', -6, .65), ('medium', -9, .85), ('heavy', -12, 1.0)]:
        target = OUTPUT / f'collision-metal-crack-{label}-v1.wav'
        ratio = 2 ** (semitones / 12)
        filters = (f'pan=mono|c0=0.5*c0+0.5*c1,atrim=start={start}:end={FIRST_IMPACT_END},asetpts=PTS-STARTPTS,'
                   f'afade=t=out:st={FIRST_IMPACT_END-start-TAIL_FADE}:d={TAIL_FADE},asetrate={RATE * ratio},'
                   f'aresample={RATE},volume={gain},afade=t=in:d=0.001,'
                   'areverse,afade=t=in:d=0.015,areverse')
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(source), '-af', filters,
                        '-ac', '1', '-c:a', 'pcm_s16le', str(target)], check=True)
        with wave.open(str(target), 'rb') as clip:
            assert clip.getframerate() == RATE and clip.getnchannels() == 1 and clip.getsampwidth() == 2
            pcm = array.array('h', clip.readframes(clip.getnframes()))
        assert max(map(abs, pcm)) < 32767 and pcm[0] == pcm[-1] == 0
        onset_ms = next(i for i, value in enumerate(pcm) if abs(value) > max(map(abs, pcm)) * .01) / RATE * 1000
        assert onset_ms < 5
        assert abs(len(pcm) / RATE - (FIRST_IMPACT_END - start) / ratio) < .001
        assert sum(v*v for v in pcm) > 0
        print(f'{target.name}: {len(pcm)/RATE:.3f}s, onset {onset_ms:.1f}ms, peak {20*math.log10(max(map(abs,pcm))/32768):.2f}dBFS')
    print(f'Source cut: {start:.6f}–{FIRST_IMPACT_END:.6f} s; tail fade {TAIL_FADE:.3f} s')
    print('Source SHA256:', hashlib.sha256(source.read_bytes()).hexdigest())

if __name__ == '__main__':
    main()
