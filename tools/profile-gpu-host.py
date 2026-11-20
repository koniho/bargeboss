#!/usr/bin/env python3
"""ADB-only renderer comparison. Build with BARGEBOSS_PROFILE=1 BARGEBOSS_VULKAN=1.

UI CSV timing is NOT presentation timing for the asynchronous Vulkan host.
Retain SurfaceFlinger timestamps and native timing logs alongside every capture.
"""
import argparse, csv, json, os, pathlib, re, shlex, shutil, statistics, subprocess, time
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('name');p.add_argument('--renderer',choices=['canvas','vulkan'],default='vulkan')
p.add_argument('--scene',default='barge-push');p.add_argument('--mode',choices=['freeze','replay'],default='freeze')
p.add_argument('--zoom',type=float,default=.65);p.add_argument('--warmup',type=int,default=240);p.add_argument('--frames',type=int,default=600)
p.add_argument('--reuse-commands',action='store_true');p.add_argument('--minimal',action='store_true');p.add_argument('--exclude',type=int,default=0)
a=p.parse_args()
if pathlib.Path(a.name).name!=a.name or a.name in ['.','..']: p.error('name must be a filename component')
if not re.fullmatch('[a-z0-9-]+',a.scene): p.error('scene must be a scenario identifier')
adb=os.environ.get('ADB') or shutil.which('adb')
if not adb: p.error('Set ADB or add platform-tools to PATH')
root=pathlib.Path(__file__).resolve().parent.parent
out=root/'out/profiles'/a.name;out.mkdir(parents=True,exist_ok=True)
def run(*args):
 if args[0]=='shell' and len(args)>2: args=('shell',shlex.join(args[1:]))
 return subprocess.check_output([adb,*args],text=True)
# Reuse an existing round's awake scope; restore only the scope opened here.
awake=os.environ.get('BARGEBOSS_AWAKE_MANAGED')!='1' and not (root/'out/phone-screen-timeout-before.txt').exists()
env=dict(os.environ,PATH=str(pathlib.Path(adb).parent)+os.pathsep+os.environ.get('PATH',''))
try:
 if awake: subprocess.run([str(root/'tools/phone-awake.sh'),'on'],env=env,check=True)
 remote='/sdcard/Android/data/com.bargeboss.game/files/profiles/'+a.scene+'-'+a.mode+'.csv'
 run('shell','am','force-stop','com.bargeboss.game');time.sleep(1)
 run('shell','rm','-f',remote)
 launch=run('shell','am','start','-W','-n','com.bargeboss.game/.MainActivity','--es','bb_renderer',a.renderer,'--es','bb_scene',a.scene,'--es','bb_mode',a.mode,'--ef','bb_camera_zoom',str(a.zoom),'--ei','bb_warmup',str(a.warmup),'--ei','bb_frames',str(a.frames),'--ez','bb_vk_reuse_commands',str(a.reuse_commands).lower(),'--ez','bb_vk_minimal',str(a.minimal).lower(),'--ei','bb_exclude',str(a.exclude))
 (out/'launch.txt').write_text(launch)
 if 'Warning: Activity not started' in launch: raise RuntimeError('Activity launch race; retry capture')
 pid=run('shell','pidof','com.bargeboss.game').strip()
 # Poll compositor history so fast scenes do not lose all but the last 63 frames.
 layer=None;presentation={};refresh='0'
 def collect_presentation():
  global layer,refresh
  if layer is None:
   layers=run('shell','dumpsys','SurfaceFlinger','--list');(out/'layers.txt').write_text(layers)
   candidates=[line.split('{',1)[-1].split(' parentId=')[0] for line in layers.splitlines() if 'bargeboss' in line and ('(BLAST)' in line if a.renderer=='vulkan' else 'VRI-' in line)]
   if not candidates:return
   layer=max(candidates,key=lambda text:int(text.rsplit('#',1)[-1]))
  raw=run('shell','dumpsys SurfaceFlinger --latency '+shlex.quote(layer)).splitlines()
  if raw:refresh=raw[0]
  for line in raw[1:]:
   values=line.split()
   if len(values)==3 and 0<int(values[1])<2**63-1:presentation[int(values[1])]=line
 for _ in range(360):
  collect_presentation()
  if subprocess.run([adb,'shell','test','-s',remote],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL).returncode==0: break
  time.sleep(.5)
 else: raise RuntimeError('Profile timed out')
 run('pull',remote,str(out/'frames.csv'))
 rows=list(csv.DictReader((out/'frames.csv').open()))
 if len(rows)!=a.warmup+a.frames: raise RuntimeError('Incomplete capture')
 sample=rows[a.warmup:]
 if a.renderer=='vulkan' and any(float(r['vulkan'])!=1 for r in sample): raise RuntimeError('Canvas fallback in measured Vulkan frames')
 (out/'native.txt').write_text(run('logcat','-d','--pid='+pid,'-s','BargeBossVulkan:I','BargeBoss:E'))
 summary={'renderer':a.renderer,'samples':len(sample),'reuse_commands':a.reuse_commands,'minimal':a.minimal,'exclude':a.exclude}
 for key in ['interval_ms','draw_ms']:
  values=sorted(float(r[key]) for r in sample);summary[key]={'median':statistics.median(values),'p95':values[int(.95*len(values))]}
 (out/'presentation.txt').write_text(refresh+'\n'+'\n'.join(presentation[k] for k in sorted(presentation))+'\n')
 start=float(sample[0]['draw_start_ns']);end=float(sample[-1]['draw_end_ns'])
 times=[stamp for stamp in sorted(presentation) if start<=stamp<=end]
 gaps=sorted((b-a)/1e6 for a,b in zip(times,times[1:]))
 if gaps: summary['presentation_ms']={'count':len(gaps),'median':statistics.median(gaps),'p95':gaps[int(.95*len(gaps))],'mean':statistics.mean(gaps)}
 (out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
finally:
 if awake: subprocess.run([str(root/'tools/phone-awake.sh'),'off'],env=env,check=True)
