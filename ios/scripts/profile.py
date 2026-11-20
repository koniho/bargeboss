#!/usr/bin/env python3
"""Capture a frozen native title, including deferred CG playback intervals.

Build/install Release first. Run: python3 ios/scripts/profile.py NAME [--lazy]
The app ignores saved progress for this opt-in capture and stops after 150 draws.
"""
import csv
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time


def sim(*args):
    return subprocess.check_output(["xcrun", "simctl", *args], text=True).strip()


name = sys.argv[1]
if Path(name).name != name or name in (".", ".."):
    raise SystemExit("NAME must be a single directory name")
devices = json.loads(sim("list", "devices", "booted", "-j"))
device = os.environ.get("SIMULATOR_ID") or next(
    d["udid"] for group in devices["devices"].values() for d in group
)
bundle = "com.bargeboss.game.ios"
container = Path(sim("get_app_container", device, bundle, "data"))
capture = container / "Documents/ios-profile.csv"
capture.unlink(missing_ok=True)
output = Path(__file__).resolve().parents[2] / "out/ios-profile" / name
output.mkdir(parents=True, exist_ok=False)
args = ["--ios-profile"]
if "--lazy" in sys.argv:
    args.append("--ios-lazy-sprites")
if "--coregraphics" in sys.argv:
    args.append("--ios-coregraphics")
if "--tutorial" in sys.argv:
    args.append("--ios-tutorial")
if "--water-textures" in sys.argv:
    args.append("--ios-water-textures")
if "--metal" in sys.argv:
    args.append("--ios-metal")
if "--replay" in sys.argv:
    args.append("--ios-replay")
for argument in sys.argv[2:]:
    if argument.startswith(("--scene=", "--frames=", "--warmup=", "--texture-mib=", "--zoom=", "--lighting=")):
        args.append(argument.replace("--", "--ios-", 1))
print(sim("launch", "--terminate-running-process", device, bundle, *args), flush=True)
deadline = time.monotonic() + 120
while not capture.exists():
    if time.monotonic() > deadline:
        raise SystemExit("Capture timed out; check for system alerts or backgrounding")
    time.sleep(0.25)
shutil.copy2(capture, output / "frames.csv")
metadata=container / "Documents/ios-profile-metadata.json"
if metadata.exists():
    shutil.copy2(metadata, output / "metadata.json")
sim("io", device, "screenshot", str(output / "native.png"))
with capture.open() as stream:
    rows = list(csv.DictReader(stream))
    values = [float(row["interval_ms"]) for row in rows]
expected = next((int(arg.split("=")[1]) for arg in args if arg.startswith("--ios-frames=")), 120)
assert len(values) == expected
ordered = sorted(values)
median=len(values)//2
p95=max(0,int(len(values)*.95)-1)
summary = dict(device=device, arguments=args, frames=len(values),
               mean_ms=sum(values)/len(values), median_ms=ordered[median],
               p95_ms=ordered[p95], max_ms=max(values),
               fps=1000*len(values)/sum(values))
if "draw_ms" in rows[0]:
    draw = sorted(float(row["draw_ms"]) for row in rows)
    summary.update(draw_median_ms=draw[median], draw_p95_ms=draw[p95], draw_max_ms=max(draw))
if "gpu_ms" in rows[0]:
    gpu=sorted(float(row["gpu_ms"]) for row in rows)
    summary.update(gpu_median_ms=gpu[median], gpu_p95_ms=gpu[p95])
(output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
print(json.dumps(summary, indent=2))
