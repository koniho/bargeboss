#!/usr/bin/env python3
"""Offline CC0 loop preparation; requires numpy, scipy, soundfile and ffmpeg."""
from pathlib import Path
import numpy as np
import soundfile as sf
from scipy.signal import resample_poly
root=Path(__file__).resolve().parent.parent
out=root/'assets/audio';out.mkdir(exist_ok=True)
def loop(source,start,duration,name):
    x,rate=sf.read(root/source,always_2d=True)
    x=x[int(start*rate):int((start+duration)*rate)].mean(axis=1)
    x=resample_poly(x,147,320) if rate==48000 else resample_poly(x,22050,rate)
    # Join tail to head with complementary fades; the output wraps at a contiguous
    # source boundary. No silence or duplicated engine attack at the loop seam.
    n=int(.16*22050);w=np.linspace(0,1,n,endpoint=False)
    cross=x[-n:]*(1-w)+x[:n]*w
    x=np.concatenate([cross,x[n:-n]])
    x-=np.mean(x)
    x*=min(1.,.8/max(np.max(np.abs(x)),1e-9))
    sf.write(out/name,x,22050,subtype='PCM_16')
    print(name,len(x)/22050,'seconds',len(x)*2,'PCM bytes')
loop('docs/audio/diesel-v1/diesel-focused-v1.flac',8,4.5,'diesel-idle-v1.wav')
loop('docs/audio/truck-revs-v1/dodge-rev-overlay-v1.flac',2.0,2.0,'diesel-working-v1.wav')
