#!/usr/bin/env python3
"""Original deterministic CB squelch/static cues; no recordings or dependencies."""
import math, random, struct, wave
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'assets/audio'
for name, duration, seed in [('cb-receive-v1', .72, 801), ('cb-reply-v1', .34, 802)]:
    rng = random.Random(seed)
    rate = 22050
    low = high = 0.
    samples = []
    for i in range(int(rate * duration)):
        t = i / rate
        white = rng.uniform(-1, 1)
        low += .72 * (white - low)
        high += .10 * (low - high)
        noise = low - high
        edge = min(1, t/.012, (duration-t)/.025)
        burst = .25 + .65*math.exp(-t*20) + .45*math.exp(-(duration-t)*30)
        chop = .65 + .35*math.sin(t*43)**2
        tone = .065*math.sin(2*math.pi*(1050 if seed==802 else 760)*t)*math.exp(-t*22)
        value = max(-1, min(1, edge*(noise*burst*chop*.8+tone)))
        samples.append(int(value*32767))
    with wave.open(str(root / (name+'.wav')), 'wb') as f:
        f.setparams((1,2,rate,0,'NONE','not compressed'))
        f.writeframes(struct.pack('<'+'h'*len(samples), *samples))

# Hull/fender impacts: increasing low resonance, metal rattle and displaced water.
for level, name in enumerate(['bump', 'thud', 'crunch']):
    rng = random.Random(910+level)
    duration = [.22, .38, .60][level]
    samples = []
    low = 0.
    for i in range(int(rate*duration)):
        t = i/rate
        noise = rng.uniform(-1, 1)
        low += .16*(noise-low)
        attack = min(1, t/.003)
        body = math.sin(2*math.pi*(115-level*23)*t)*math.exp(-t*(22-level*4))
        rattle = math.sin(2*math.pi*439*t)*math.sin(2*math.pi*173*t)*math.exp(-t*13)
        splash = low*math.exp(-t*8)
        value = attack*(body*.35+rattle*(.04+level*.07)+splash*(.3+level*.25))
        value *= min(1, (duration-t)/.02)
        samples.append(int(max(-.9,min(.9,value))*32767))
    with wave.open(str(root / ('collision-'+name+'-v1.wav')), 'wb') as f:
        f.setparams((1,2,rate,0,'NONE','not compressed'))
        f.writeframes(struct.pack('<'+'h'*len(samples), *samples))
