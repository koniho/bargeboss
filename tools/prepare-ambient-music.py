#!/usr/bin/env python3
"""Prepare the user-licensed soundtrack as portable PCM for all three hosts."""
from pathlib import Path
import hashlib
import subprocess
import wave

root = Path(__file__).resolve().parents[1]
source = root / 'docs/audio/sources/sergepavkinmusic/a-long-way-166385.mp3'
target = root / 'assets/audio/a-long-way-v1.wav'
subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(source), '-af',
                'afade=t=in:d=1,afade=t=out:st=271:d=2', '-ar', '44100', '-ac', '2',
                '-c:a', 'pcm_s16le', str(target)], check=True)
with wave.open(str(target), 'rb') as clip:
    assert (clip.getnchannels(), clip.getsampwidth(), clip.getframerate()) == (2, 2, 44100)
    assert 273 <= clip.getnframes() / 44100 < 274
    print(f'{target.name}: {clip.getnframes()/44100:.3f}s, stereo 44.1kHz PCM16')
print('Source SHA256:', hashlib.sha256(source.read_bytes()).hexdigest())
