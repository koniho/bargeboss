#!/usr/bin/env python3
"""Short sampled metal/body collisions; no synthesized static or delayed splash."""
from pathlib import Path
import subprocess,struct,wave,math
ROOT=Path(__file__).resolve().parents[1];RATE=44100

def source(name,filters):
 raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(ROOT/'docs/audio/sources/kenney'/name),'-af',filters,'-ac','1','-ar',str(RATE),'-f','f32le','-'])
 v=list(struct.unpack('<'+'f'*(len(raw)//4),raw));threshold=max(map(abs,v))*.025
 start=next(i for i,x in enumerate(v) if abs(x)>threshold);v=v[max(0,start-44):]
 peak=max(map(abs,v));return [x/peak for x in v]

metal=source('impactMetal_heavy_000.ogg','aresample=44100,asetrate=37044,aresample=44100,highpass=f=100,lowpass=f=2300')
body=source('impactSoft_heavy_000.ogg','highpass=f=80,lowpass=f=1400')
for name,duration,metal_gain,body_gain,peak in [
 ('industrial-clunk-light-v1',.19,.40,.80,.60),
 ('industrial-clunk-medium-v1',.30,.85,.65,.73),
 ('industrial-clunk-heavy-v1',.40,1,.70,.80)]:
 values=[]
 for i in range(int(RATE*duration)):
  t=i/RATE;m=metal[i] if i<len(metal) else 0;b=body[i] if i<len(body) else 0
  values.append(m*metal_gain*math.exp(-t*5)+b*body_gain)
 mean=sum(values)/len(values)
 values=[(v-mean)*min(1,i/(RATE*.002),(len(values)-1-i)/(RATE*.045)) for i,v in enumerate(values)]
 gain=peak/max(map(abs,values));pcm=[round(v*gain*32767) for v in values]
 assert pcm[0]==pcm[-1]==0 and max(map(abs,pcm))<32767
 onset=next(i for i,v in enumerate(pcm) if abs(v)>32767*peak*.05)/RATE
 assert onset<.015
 with wave.open(str(ROOT/'assets/audio'/(name+'.wav')),'wb') as f:
  f.setparams((1,2,RATE,0,'NONE','not compressed'));f.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
 print(f'{name}: {duration:.2f}s; peak {peak}; onset {onset*1000:.1f}ms')
