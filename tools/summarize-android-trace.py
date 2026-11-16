#!/usr/bin/env python3
"""App-only analysis of a short atrace text capture with BB draw markers.
Inclusive slices overlap. Scheduler states describe time, not GPU execution.
Raw systemwide traces stay in ignored output; this report excludes other apps.
"""
import collections
import json
from pathlib import Path
import re
import sys

path=Path(sys.argv[1])
pid=None;entries=None
for line in path.open(errors='replace'):
    m=re.search(r'entries-in-buffer/entries-written:\s*(\d+)/(\d+)',line)
    if m:entries=list(map(int,m.groups()))
    m=re.search(r'B\|(\d+)\|BB draw \d+',line)
    if m:pid=int(m[1]);break
if pid is None:raise SystemExit('No BB draw markers; enable bb_system_trace and -a com.bargeboss.game')
pattern=re.compile(r'^\s*(.*?)-(\d+)\s+\(\s*([^)]*)\)\s+\[(\d+)\]\s+\S+\s+(\d+\.\d+):\s+(\w+):\s*(.*)$')
switch=re.compile(r'prev_comm=(.*?) prev_pid=(\d+) .*?prev_state=(\S+) ==> next_comm=(.*?) next_pid=(\d+)')
threads={pid:'main'};stacks=collections.defaultdict(list);slices=[];switches=[]
for line in path.open(errors='replace'):
    m=pattern.match(line)
    if not m:continue
    comm,tid,tgid,cpu,t,event,value=m.groups();tid=int(tid);t=float(t)
    end=t
    if event=='sched_switch':
        sm=switch.search(value)
        if sm:switches.append((t,int(sm[2]),sm[3],int(sm[5])))
    elif event=='tracing_mark_write':
        if value.startswith('B|'+str(pid)+'|'):
            threads[tid]=comm.strip();stacks[tid].append((t,value.split('|',2)[2]))
        elif value=='E|'+str(pid) or (value=='E' and tgid.strip()==str(pid)):
            if stacks[tid]:
                start,name=stacks[tid].pop();slices.append((tid,start,t,name))

states=collections.defaultdict(list);last={}
for t,prev,state,nxt in switches:
    for tid,status in [(prev,'runnable' if state.startswith('R') else 'blocked'),(nxt,'running')]:
        if tid not in threads:continue
        if tid in last:
            start,old=last[tid]
            if t>=start:states[tid].append((start,t,old))
        last[tid]=(t,status)
for tid,(start,status) in last.items():states[tid].append((start,end,status))

def time_in(items,start,finish):
    result=collections.defaultdict(float)
    for a,b,name in items:
        if b>start and a<finish:result[name]+=max(0,min(b,finish)-max(a,start))*1000
    return {k:round(v,3) for k,v in result.items()}

def stats(values):
    a=sorted(values)
    return {'count':len(a),'total_ms':round(sum(a),3),'p50_ms':round(a[len(a)//2],3),'p95_ms':round(a[int(len(a)*.95)],3),'max_ms':round(a[-1],3)}

render=[tid for tid,name in threads.items() if name=='RenderThread']
frames=sorted([(a,b,int(n.split()[-1])) for tid,a,b,n in slices if tid==pid and n.startswith('BB draw ')])
if not frames:raise SystemExit('No completed game draw slices')
lo,hi=frames[0][0],frames[-1][1]
aggregates=collections.defaultdict(list)
for tid,a,b,name in slices:
    if tid not in render or a<lo or b>hi:continue
    name=re.sub(r'DrawFrames \d+','DrawFrames',name)
    aggregates[name].append((b-a)*1000)
worst=[]
for i in range(len(frames)-1):
    start,finish,frame=frames[i];next_start=frames[i+1][0]
    if frames[i+1][2]!=frame+1:continue
    wait=sum(max(0,min(b,next_start)-max(a,start))*1000 for tid,a,b,n in slices if tid==pid and n=='postAndWait' and b>start and a<next_start)
    worst.append({'preceding_frame':frame,'following_interval_ms':round((next_start-start)*1000,3),'java_draw_ms':round((finish-start)*1000,3),'ui_post_wait_ms':round(wait,3),'render_thread_states':{str(tid):time_in(states[tid],start,next_start) for tid in render}})
result={'app_pid':pid,'buffer_entries_written':entries,'buffer_overwritten':entries[0]!=entries[1] if entries else None,'complete_draws':len(frames),'first_frame':frames[0][2],'last_frame':frames[-1][2],'window_ms':round((hi-lo)*1000,3),'render_thread_states':{str(tid):time_in(states[tid],lo,hi) for tid in render},'render_thread_slices_inclusive':{k:stats(v) for k,v in sorted(aggregates.items(),key=lambda kv:sum(kv[1]),reverse=True)[:18]},'longest_draw_intervals':sorted(worst,key=lambda r:r['following_interval_ms'],reverse=True)[:8]}
print(json.dumps(result,indent=2))
