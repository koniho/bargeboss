#!/usr/bin/env python3
"""Correlate retained Android frame records with the measured app draw window."""
import csv,json,sys
from pathlib import Path
folder=Path(sys.argv[1])
rows=[r for r in csv.DictReader((folder/'frames.csv').open()) if r['phase']=='sample']
start=float(rows[0]['draw_start_ns']);end=float(rows[-1]['draw_end_ns'])
header=None;frames=[]
for line in (folder/'gfxinfo.txt').open():
    if line.startswith('Flags,'):header=line.strip().strip(',').split(',');continue
    if header and line[:1].isdigit():
        fields=line.strip().strip(',').split(',')
        if len(fields)!=len(header):continue
        frame=dict(zip(header,map(int,fields)))
        # Android's DrawStart begins the traversal shortly before our onDraw.
        if not frame['Flags'] and start-1000000<=frame['DrawStart']<=end:frames.append(frame)
result={'matched_recent_frames':len(frames),'app_measured_frames':len(rows)}
for label,a,b in [('ui_record_ms','DrawStart','SyncQueued'),('render_queue_ms','SyncQueued','SyncStart'),('render_sync_ms','SyncStart','IssueDrawCommandsStart'),('issue_to_swap_ms','IssueDrawCommandsStart','SwapBuffers'),('intended_to_complete_ms','IntendedVsync','FrameCompleted')]:
    values=sorted((r[b]-r[a])/1e6 for r in frames if r[b]>=r[a])
    result[label]={'p50':round(values[len(values)//2],3),'p95':round(values[int(len(values)*.95)],3),'max':round(values[-1],3)} if values else None
# Dequeue is inside issue-to-swap on this backend, so report it separately.
# The remainder still includes driver work/other waits; it is not pure CPU time.
def summarize(values):
    values=sorted(values)
    return {'p50':round(values[len(values)//2],3),'p95':round(values[int(len(values)*.95)],3),'max':round(values[-1],3)} if values else None
for label,key in [('dequeue_buffer_ms','DequeueBufferDuration'),('queue_buffer_ms','QueueBufferDuration')]:
    result[label]=summarize([r[key]/1e6 for r in frames if 0<=r.get(key,-1)<(1<<63)-1])
result['issue_excluding_dequeue_ms']=summarize([(r['SwapBuffers']-r['IssueDrawCommandsStart']-r['DequeueBufferDuration'])/1e6 for r in frames if 'DequeueBufferDuration' in r and 0<=r['DequeueBufferDuration']<=r['SwapBuffers']-r['IssueDrawCommandsStart']])
for label,a,b in [('submit_to_gpu_complete_ms','CommandSubmissionCompleted','GpuCompleted'),('present_latency_ms','IntendedVsync','DisplayPresentTime')]:
    result[label]=summarize([(r[b]-r[a])/1e6 for r in frames if 0<r.get(a,0)<=r.get(b,0)<(1<<63)-1])
print(json.dumps(result,indent=2))
