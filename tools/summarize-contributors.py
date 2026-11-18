#!/usr/bin/env python3
"""Paired marginal render costs from --ez bb_contributors true (freeze mode).
Each 45-frame block alternates baseline/exclusion. Drop six transition frames.
These deltas include changed overdraw/cache behavior and must not be added.
"""
import csv,json,statistics,sys
names={1:'Factory artwork',2:'Factory smoke and steam',4:'Factory glows and furnace effects',8:'Ambient water crests',16:'Static shore surfaces and caps',32:'Native foam submission',64:'Surface materials replaced by flat color',7:'All factory visuals'}
rows=list(csv.DictReader(open(sys.argv[1])))
warmup=sum(r['phase']=='warmup' for r in rows)
blocks={}
for r in rows:
    if r['phase']!='sample':continue
    sample=int(r['frame'])-warmup
    if sample%45<6:continue
    blocks.setdefault(sample//45,[]).append(r)
results=[]
for block,rs in sorted(blocks.items()):
    mask=int(float(rs[0]['exclusion']))
    if block%2==0 or not mask:continue
    base=blocks.get(block-1,[])
    if len(base)!=39 or len(rs)!=39:raise SystemExit('Incomplete paired block')
    if any(int(float(r['exclusion']))!=mask for r in rs) or any(float(r['exclusion'])!=0 for r in base):raise SystemExit('Mixed contributor block')
    def median(data,key):return statistics.median(float(r[key]) for r in data)
    item={'block':block,'mask':mask,'name':names[mask]}
    for key in ('draw_ms','interval_ms'):
        b=median(base,key);v=median(rs,key)
        item[key]={'baseline':round(b,3),'excluded':round(v,3),'saved':round(b-v,3),'percent_saved':round(100*(b-v)/b,1)}
    item['cache_fallbacks']=sum(int(float(r['cache_fallbacks'])) for r in rs)
    item['cache_evictions']=sum(int(float(r['cache_evictions'])) for r in rs)
    results.append(item)
print(json.dumps({'file':sys.argv[1],'transition_frames_dropped':6,'pairs':results},indent=2))
