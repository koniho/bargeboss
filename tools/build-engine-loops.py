#!/usr/bin/env python3
"""Offline CC0 loop preparation; requires numpy, scipy, soundfile and ffmpeg."""
from pathlib import Path
import numpy as np
import soundfile as sf
from scipy.signal import resample_poly, butter, sosfiltfilt
from scipy.ndimage import gaussian_filter1d
root=Path(__file__).resolve().parent.parent
out=root/'assets/audio';out.mkdir(exist_ok=True)
def loop(source,start,duration,name,lowpass=None,steady=False):
    x,rate=sf.read(root/source,always_2d=True)
    x=x[int(start*rate):int((start+duration)*rate)].mean(axis=1)
    x=resample_poly(x,147,320) if rate==48000 else resample_poly(x,22050,rate)
    if lowpass is not None:
        # Remove the marine recording's residual splashes without adding CPU work
        # to playback. Leave exhaust fundamentals; truck supplies upper growl.
        x=sosfiltfilt(butter(6,lowpass,fs=22050,output="sos"),x)
    if steady:
        # Use only the sustained top of the rev, and remove its residual volume
        # envelope. Shared engine load supplies the acceleration, not the sample.
        envelope=np.sqrt(gaussian_filter1d(x*x,22050*.06)+1e-12)
        x*=np.clip(np.median(envelope)/envelope,.5,2)
    # Join tail to head with complementary fades; the output wraps at a contiguous
    # source boundary. No silence or duplicated engine attack at the loop seam.
    n=int(.16*22050);w=np.linspace(0,1,n,endpoint=False)
    cross=x[-n:]*(1-w)+x[:n]*w
    x=np.concatenate([cross,x[n:-n]])
    x-=np.mean(x)
    x*=min(1.,.8/max(np.max(np.abs(x)),1e-9))
    sf.write(out/name,x,22050,subtype='PCM_16')
    print(name,len(x)/22050,'seconds',len(x)*2,'PCM bytes')
loop('docs/audio/diesel-v1/diesel-focused-v1.flac',8,4.5,'diesel-idle-v1.wav',lowpass=500)
loop('docs/audio/truck-revs-v1/dodge-rev-overlay-v1.flac',3.55,.8,'diesel-working-v1.wav',lowpass=1000,steady=True)
