#!/usr/bin/env python3
"""Damped wooden-body chunks, with no metallic layer or added noise."""
from pathlib import Path
import subprocess,struct,wave,math
ROOT=Path(__file__).resolve().parents[1];RATE=44100
for label,duration,pitch,peak in [('light',.16,.90,.60),('medium',.22,.80,.73),('heavy',.28,.72,.80)]:
 raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(ROOT/'docs/audio/sources/kenney/impactWood_heavy_000.ogg'),'-af',f'aresample=44100,asetrate={int(RATE*pitch)},aresample=44100,highpass=f=75,lowpass=f=1300','-ac','1','-ar',str(RATE),'-f','f32le','-'])
 v=list(struct.unpack('<'+'f'*(len(raw)//4),raw));threshold=max(map(abs,v))*.02
 start=next(i for i,x in enumerate(v) if abs(x)>threshold);v=v[max(0,start-44):][:int(RATE*duration)]
 v=[x*math.exp(-i/RATE*9) for i,x in enumerate(v)];mean=sum(v)/len(v)
 v=[(x-mean)*min(1,i/(RATE*.003),(len(v)-1-i)/(RATE*.04)) for i,x in enumerate(v)]
 gain=peak/max(map(abs,v));pcm=[round(x*gain*32767) for x in v]
 assert pcm[0]==pcm[-1]==0 and max(map(abs,pcm))<32767
 onset=next(i for i,x in enumerate(pcm) if abs(x)>32767*peak*.05)/RATE
 assert onset<.015
 name='collision-chunk-'+label+'-v1'
 with wave.open(str(ROOT/'assets/audio'/(name+'.wav')),'wb') as f:
  f.setparams((1,2,RATE,0,'NONE','not compressed'));f.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
 print(f'{name}: {len(pcm)/RATE:.3f}s, onset {onset*1000:.1f}ms, peak {peak}')
