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
for label,a,b in [('ui_record_ms','DrawStart','SyncQueued'),('render_sync_ms','SyncStart','IssueDrawCommandsStart'),('issue_to_swap_ms','IssueDrawCommandsStart','SwapBuffers'),('intended_to_complete_ms','IntendedVsync','FrameCompleted')]:
    values=sorted((r[b]-r[a])/1e6 for r in frames if r[b]>=r[a])
    result[label]={'p50':round(values[len(values)//2],3),'p95':round(values[int(len(values)*.95)],3)} if values else None
print(json.dumps(result,indent=2))
