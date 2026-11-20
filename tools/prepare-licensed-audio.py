#!/usr/bin/env python3
"""Prepare selected CC0 originals for the restrained Barge Boss sound palette."""
from pathlib import Path
import subprocess, shutil, wave, struct
ROOT=Path(__file__).resolve().parents[1]
FFMPEG=shutil.which('ffmpeg')
if not FFMPEG:raise SystemExit('ffmpeg is required to decode the checked-in originals')
AUDIO=ROOT/'assets/audio';SRC=ROOT/'docs/audio/sources'
# Original source, excerpt start/length, filters, output peak. Native hosts use PCM.
PICKS=[
 ('radio-receive-recorded-v1','bigsoundbank/0591.mp3',.268,.49,'highpass=f=300,lowpass=f=2800',.30),
 ('radio-reply-recorded-v1','bigsoundbank/0590.mp3',.165,.23,'highpass=f=350,lowpass=f=2400',.24),
 ('fender-light-kenney-v1','kenney/impactSoft_medium_000.ogg',0,.118,'lowpass=f=1500',.32),
 ('fender-heavy-kenney-v1','kenney/impactSoft_heavy_000.ogg',0,.562,'aresample=44100,asetrate=39690,aresample=44100,lowpass=f=1200',.48),
 ('step-clear-kenney-v1','kenney/confirmation_001.ogg',0,.323,'aresample=44100,asetrate=39690,aresample=44100,lowpass=f=2400',.25),
]
for name,src,start,duration,filters,peak in PICKS:
 raw=subprocess.check_output([FFMPEG,'-v','error','-i',str(SRC/src),'-ss',str(start),'-t',str(duration),'-af',filters,'-ac','1','-ar','44100','-f','f32le','-'])
 values=struct.unpack('<'+'f'*(len(raw)//4),raw);mean=sum(values)/len(values)
 values=[(v-mean)*min(1,i/220,(len(values)-1-i)/661) for i,v in enumerate(values)]
 gain=peak/max(abs(v) for v in values);samples=[round(v*gain*32767) for v in values]
 with wave.open(str(AUDIO/(name+'.wav')),'wb') as f:
  f.setparams((1,2,44100,0,'NONE','not compressed'));f.writeframes(struct.pack('<'+'h'*len(samples),*samples))
 print(name,round(len(samples)/44100,3),'seconds; peak',peak)
