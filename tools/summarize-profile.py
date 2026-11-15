#!/usr/bin/env python3
"""Summarize raw Android counters; timings are measurements, never pass/fail assertions."""
import csv, json, sys
for path in sys.argv[1:]:
    with open(path) as source:
        all_rows=list(csv.DictReader(source))
    rows=[r for r in all_rows if r['phase']=='sample']
    if not rows:raise SystemExit('No measured frames: capture incomplete or interrupted')
    if any(None in r or None in r.values() for r in all_rows):raise SystemExit('Corrupt/truncated profile CSV')
    def percentile(key,p):
        values=sorted(float(r[key]) for r in rows)
        return round(values[min(len(values)-1,int(len(values)*p))],3) if values else None
    result={'file':path,'samples':len(rows),'lessons':sorted({int(float(r['lesson'])) for r in rows})}
    for key in ('advance_ms','draw_ms','interval_ms','layer_build_ms','decode_ms','cache_mib','sprites','blends','affines','circles','polys','clips'):
        result[key]={'p50':percentile(key,.5),'p95':percentile(key,.95)}
    for key in ('texture_mib','source_mib','frame_mib','frame_build_ms'):
        if key in rows[0]:result[key]={'p50':percentile(key,.5),'p95':percentile(key,.95)}
    for key in ('cache_builds','cache_fallbacks','cache_evictions'):
        result[key]=sum(int(float(r[key])) for r in rows)
    for key in ('frame_builds','frame_evictions'):
        if key in rows[0]:result[key]=sum(int(float(r[key])) for r in rows)
    result['cold_decode_ms']=round(sum(float(r['decode_ms']) for r in all_rows if r['phase']=='warmup'),3)
    print(json.dumps(result,indent=2))
