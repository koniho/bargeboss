#!/usr/bin/env python3
"""Compare completed native captures; reject different replay ticks/lessons/dimensions."""
import csv
import json
from pathlib import Path
import re
import statistics
import subprocess
import sys

if len(sys.argv) < 3:
    raise SystemExit('Usage: compare-profile-strategies.py RUN_DIRECTORY RUN_DIRECTORY [...]')
reference = None
reference_size = None
results = []
for argument in sys.argv[1:]:
    folder = Path(argument)
    all_rows = list(csv.DictReader((folder / 'frames.csv').open()))
    rows = [r for r in all_rows if r['phase'] == 'sample']
    if not rows or any(None in r or None in r.values() for r in all_rows):
        raise SystemExit('Incomplete/corrupt capture: ' + argument)
    log = (folder / 'logcat.txt').read_text()
    size = re.search(r'READY scene=(\S+) mode=(\S+) width=(\S+) height=(\S+)', log)
    if not size or 'SAVED complete ' not in log:
        raise SystemExit('Missing completed-session metadata: ' + argument)
    size = size.groups()
    sequence = [(r['lesson'], r['tick']) for r in rows]
    if reference is None:
        reference = rows
        reference_size = size
    if size != reference_size or sequence != [(r['lesson'], r['tick']) for r in reference]:
        raise SystemExit('Different dimensions, scene, mode, sample count or replay sequence: ' + argument)
    zoom_delta = max(abs(float(a['zoom']) - float(b['zoom'])) for a, b in zip(reference, rows))
    # Small native/JIT floating point differences are reported, never hidden.
    # A camera difference above 0.5% invalidates this matched comparison.
    relative_zoom_delta = max(abs(float(a['zoom']) - float(b['zoom'])) / float(a['zoom'])
                              for a, b in zip(reference, rows))
    if relative_zoom_delta > .005:
        raise SystemExit('Camera differs by more than 0.5%: ' + argument)
    report = json.loads(subprocess.check_output(
        [sys.executable, 'tools/summarize-profile.py', str(folder / 'frames.csv')]))
    report['strategies'] = dict(line.split('=', 1) for line in (folder / 'strategies.txt').read_text().splitlines())
    report['throughput_fps'] = round(1000 / statistics.mean(float(r['interval_ms']) for r in rows), 2)
    report['max_zoom_delta'] = zoom_delta
    report['max_zoom_relative_delta'] = relative_zoom_delta
    report['gfx'] = json.loads((folder / 'gfx-summary.json').read_text())
    count = re.search(r'Number Slow bitmap uploads: (\d+)', (folder / 'gfxinfo.txt').read_text())
    report['slow_sync_frames_including_warmup'] = int(count[1]) if count else None
    results.append(report)
print(json.dumps(results, indent=2))
