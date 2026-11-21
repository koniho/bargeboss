#!/usr/bin/env python3
"""Short, quiet fender thuds from the checked-in CC0 Kenney soft impacts."""
from pathlib import Path
import subprocess,struct,wave
ROOT=Path(__file__).resolve().parents[1]
# No synthesized noise/water layers. Distinct onset-aligned, low-passed hits.
for name,source,duration,cutoff,peak in [
 ('fender-light-kenney-v2','impactSoft_medium_000.ogg',.14,650,.40),
 ('fender-medium-kenney-v2','impactSoft_heavy_000.ogg',.25,550,.54),
 ('fender-heavy-kenney-v2','impactSoft_heavy_000.ogg',.32,450,.68)]:
 raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(ROOT/'docs/audio/sources/kenney'/source),'-af',f'highpass=f=35,lowpass=f={cutoff}:p=2,lowpass=f={cutoff}:p=2','-ac','1','-ar','44100','-f','f32le','-'])
 values=list(struct.unpack('<'+'f'*(len(raw)//4),raw))
 threshold=max(map(abs,values))*.025
 start=next(i for i,v in enumerate(values) if abs(v)>threshold)
 values=values[max(0,start-88):][:int(duration*44100)]
 mean=sum(values)/len(values)
 values=[(v-mean)*min(1,i/176,(len(values)-1-i)/1764) for i,v in enumerate(values)]
 gain=peak/max(map(abs,values)); pcm=[round(v*gain*32767) for v in values]
 with wave.open(str(ROOT/'assets/audio'/(name+'.wav')),'wb') as f:
  f.setparams((1,2,44100,0,'NONE','not compressed'));f.writeframes(struct.pack('<'+'h'*len(pcm),*pcm))
 onset=next(i for i,v in enumerate(pcm) if abs(v)>32767*peak*.05)/44100
 assert onset<.02 and pcm[0]==pcm[-1]==0
 print(f'{name}: {len(pcm)/44100:.3f}s, onset {onset*1000:.1f}ms, peak {peak}')
