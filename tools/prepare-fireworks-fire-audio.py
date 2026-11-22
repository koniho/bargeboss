#!/usr/bin/env python3
"""Reproducible recorded fireworks/fire/steam edits; provenance in docs/audio/fireworks-fire-v2.md."""
import array, hashlib, math, pathlib, subprocess, wave
ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'assets/audio'; RATE=44100
O=ROOT/'docs/audio/sources/orangefreesounds'; F=ROOT/'docs/audio/sources/freesound'
def edit(source,name,start,duration,filters,peak,loop=False):
    raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(source),'-af',f'atrim=start={start}:duration={duration},asetpts=PTS-STARTPTS,{filters}','-ar',str(RATE),'-ac','1','-f','s16le','-'])
    samples=array.array('h',raw)
    if loop:
        # Rotate a crossfade onto the seam: beginning follows the previous tail.
        n=int(.35*RATE);samples=array.array('h',[round(samples[-n+i]*(1-i/n)+samples[i]*(i/n)) for i in range(n)])+samples[n:-n]
    else:
        fade_in=int(.003*RATE);fade_out=int(.12*RATE)
        for i in range(fade_in):samples[i]=round(samples[i]*i/fade_in)
        for i in range(fade_out):samples[-1-i]=round(samples[-1-i]*i/fade_out)
    gain=32767*peak/max(1,max(abs(x) for x in samples));samples=array.array('h',[round(x*gain) for x in samples])
    path=OUT/name
    with wave.open(str(path),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(RATE);w.writeframes(samples.tobytes())
    print(name,round(len(samples)/RATE,3),'sec',hashlib.sha256(path.read_bytes()).hexdigest())
edit(O/'firework-launch-explode.mp3','firework-launch-recorded-v2.wav',.145,.63,'highpass=f=55,lowpass=f=9500',.42)
edit(O/'firework-launch-explode.mp3','firework-burst-a-v1.wav',1.30,1.18,'highpass=f=38,lowpass=f=11000',.64)
edit(O/'firework-burst-2015.mp3','firework-burst-b-v1.wav',.30,1.18,'highpass=f=40,lowpass=f=10000',.64)
edit(O/'firework-launch-explode.mp3','firework-burst-c-v1.wav',1.30,1.10,'asetrate=41895,aresample=44100,highpass=f=38,lowpass=f=8500',.61)
edit(F/'fire-burning-28314.mp3','timber-fire-roar-v1.wav',5,4.35,'highpass=f=45,lowpass=f=6500,acompressor=threshold=0.12:ratio=3:attack=12:release=180',.52,True)
edit(F/'water-sizzle-463370.mp3','fire-water-sizzle-v1.wav',8,2.35,'highpass=f=280,lowpass=f=8500,acompressor=threshold=0.08:ratio=3:attack=8:release=100',.48,True)
