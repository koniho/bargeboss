#!/usr/bin/env python3
"""Original deterministic fender thud and layered water Foley; no sampled recordings."""
import math, random, struct, wave
from pathlib import Path
RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / 'assets/audio'
def save(name, samples, peak):
    # DC removal, tiny edge fades and headroom prevent clicks or digital clipping.
    mean = sum(samples) / len(samples)
    samples = [(v-mean)*min(1, i/132, (len(samples)-1-i)/882) for i,v in enumerate(samples)]
    gain = peak / max(abs(v) for v in samples)
    pcm = [round(v*gain*32767) for v in samples]
    with wave.open(str(ROOT / (name+'.wav')), 'wb') as f:
        f.setparams((1,2,RATE,0,'NONE','not compressed'))
        f.writeframes(struct.pack('<'+'h'*len(pcm), *pcm))
    print(name, len(pcm)/RATE, 'seconds; peak', peak)

def thud():
    rng=random.Random(3801); samples=[]; low=slow=0
    for i in range(int(RATE*.34)):
        t=i/RATE; noise=rng.uniform(-1,1)
        low += .055*(noise-low); slow += .014*(low-slow)
        # Brief rubber/fender compression with damped, inharmonic hull modes.
        body=sum(amp*math.sin(2*math.pi*hz*t+phase)*math.exp(-t*decay)
                 for hz,amp,decay,phase in [(82,.38,27,0),(127,.17,34,.2),(193,.06,43,.8)])
        v=(body+.75*slow*math.exp(-t*35)+.28*(low-slow)*math.exp(-t*65))*min(1,t/.006)
        samples.append(v)
    save('tug-fender-thud-v1',samples,.55)

def splash(name,large,seed):
    rng=random.Random(seed); duration=.92 if large else .63
    samples=[0.0]*int(RATE*duration); low=mid=fast=0
    for i in range(len(samples)):
        t=i/RATE; white=rng.uniform(-1,1)
        low += .018*(white-low); mid += .14*(white-mid); fast += .58*(white-fast)
        # Broad water body, breaking sheet, then a quieter granular wash.
        body=(low*.9+(mid-low)*.48)*math.exp(-t*(7 if large else 11))
        sheet=(fast-mid)*.18*math.exp(-t*13)*(1+.35*math.sin(t*71)+.17*math.sin(t*113))
        wash=(mid-low)*.16*math.exp(-((t-.15)/.17)**2)
        samples[i]=(body+sheet+wash)*min(1,t/.009)
    # Nonuniform damped bubble modes and returning droplets, spread through the tail.
    for k in range(46 if large else 29):
        start=rng.uniform(.012,duration*.83); span=rng.uniform(.025,.095)
        hz=rng.uniform(360,2200); amp=rng.uniform(.015,.065)*math.exp(-start*2.8)
        phase=0
        for j in range(int(span*RATE)):
            at=int(start*RATE)+j
            if at>=len(samples):break
            u=j/RATE; phase+=2*math.pi*hz*(1+.22*u/span)/RATE
            env=min(1,u/.0015)*math.exp(-u/(span*.18))
            samples[at]+=amp*math.sin(phase)*env
    save(name,samples,.49 if large else .34)
if __name__=='__main__':
    ROOT.mkdir(parents=True,exist_ok=True)
    thud();splash('water-splash-small-v1',False,3802);splash('water-splash-large-v1',True,3803)
