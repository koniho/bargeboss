# Developer profiling hooks

Build an explicitly instrumented APK with the existing debug key:

```sh
BARGEBOSS_PROFILE=1 ANDROID_JAR=/path/to/android.jar ./build-android.sh
```

Install `build/bargeboss-debug.apk` through the usual authorized rish installation
path. `BARGEBOSS_PROFILE=1` adds `android:debuggable` to a generated manifest and
compiles `developer/shared`, `developer/android` and the existing test replay
input driver. The checked-in manifest stays non-debuggable. A normal build omits
these classes. Both generated class and dex directories are cleared on rebuild.
The runtime adapter also checks `FLAG_DEBUGGABLE` before accepting extras.

## Run and collect

```sh
./tools/profile-android.sh barge-arrival replay
./tools/profile-android.sh barge-push live
./tools/profile-android.sh harbor-close freeze
BARGEBOSS_PROFILE_TRACE=true BARGEBOSS_PROFILE_FRAMES=180 \
  ./tools/profile-android.sh harbor-close live
```

The script force-stops/relaunches the game, waits for the completed capture, and
saves frame CSV, `gfxinfo`/frame stats, process memory, app-only logs and a device
screenshot under `out/profiles/<timestamp>-<scene>-<mode>/`. Optional ART method
sampling produces `methods.trace` and a hotspot summary. Transfers stage through
`/storage/emulated/0/Download/BargeBoss-profiles` because this device's rish can
interleave large stdout/stderr streams and corrupt streamed files. The newest
staging files are overwritten; permanent run artifacts remain under `out/`.

Defaults are 120 warmup draws and 360 measured draws. Override with
`BARGEBOSS_PROFILE_WARMUP` and `BARGEBOSS_PROFILE_FRAMES`. The app bounds these to
0–1800 and 30–3600 respectively. Runs temporarily suspend the display timeout and restore it on exit, including
errors/signals, unless an outer development round already owns that awake scope.
They time out after 180 seconds; keep the phone unlocked. Keyguard security is
unchanged. For a whole development round use `./tools/phone-awake.sh on`, then
`./tools/phone-awake.sh off` afterward. The prior setting is saved under `out/`
so it can also be restored after an interrupted agent session.

Fixtures: `basin-entry`, `canal-entry`, `canal-middle`, `barge-arrival`,
`barge-push`, `harbor-close`, `barge-tow`, `barge-bridge`. These are reached by the
existing real tutorial gesture replay, rather than teleporting actors or changing
lesson rules. Scenario construction happens before measured rendering. The shared
fixture runner contains no Android APIs and can also drive JVM captures.

| Mode | Behavior | Use |
| --- | --- | --- |
| `freeze` | Repeatedly draw the same world/animation tick | Stable-pose rendering and cache diagnosis |
| `live` | Release input and use the normal real-time `Game.advance` path | Idle animation, momentum, normal fixed-step cost |
| `replay` | Apply real gestures and advance one `Game.step` per callback | Repeatable movement, effects, camera and lesson transitions |

Replay mode deliberately measures one simulation tick per callback, not real-time
catch-up behavior. Compare the same mode, dimensions, warmup and sample count.
A live scene can advance tutorial/camera state; the CSV includes actual lesson,
visual tick and zoom so this is visible.

For a direct launch, use a new Activity (extras are read on creation):

```sh
RISH_APPLICATION_ID=com.termux rish -c 'am start -S -W -n com.bargeboss.game/com.bargeboss.game.MainActivity --es bb_scene barge-push --es bb_mode live --ei bb_warmup 120 --ei bb_frames 360 --ez bb_trace false'
```

The profiling session uses an isolated unsaved Game, disables haptics and ignores
finger input. It never restores or writes captain progress. Back exits the
session. After the sample completes, animation and repeated draws stop, keeping
the last image available for capture. Relaunch without extras to play normally:

```sh
RISH_APPLICATION_ID=com.termux rish -c 'am start -S -W -n com.bargeboss.game/com.bargeboss.game.MainActivity'
```

## Measurements and limits

- `advance_ms`: simulation/input work accumulated since the previous draw.
- `draw_ms`: the actual View draw scope, including shared renderer, Canvas command
  recording and any synchronous decode/layer construction. It excludes GPU work.
- `interval_ms`: elapsed time between draw starts; scheduling and render-thread
  backpressure can make it much larger than `draw_ms`.
- Layer build/decode time and hit/build/fallback/eviction counters identify cold
  work. Cold primitive counts include drawing into newly constructed layers.
- Sprite/blend/affine, circle, polygon, line and clip counters cover native
  primitive submissions. Blend counts exclude endpoint shortcuts to one sprite.
- `draw_start_ns` / `draw_end_ns` correlate measured draws with Android frame stats.
  `gfxinfo` retains only a bounded recent frame history; it may cover fewer frames
  than the CSV. Its whole-run histogram also includes launch/warmup frames.

Frame rows use preallocated numeric storage. Formatting and atomic CSV publication
happen on a worker after measurement. Normal play does not enable counters or
read profiling clocks. Sampling traces add overhead: use separate untraced runs
for frame timings. The parser reports approximate exclusive and inclusive
main-thread CPU attribution; nested inclusive categories must not be added.
It validates monotonic clocks to reject corrupt transfers.

```sh
./tools/check-profile.sh
python3 tools/summarize-profile.py out/profiles/<run>/frames.csv
python3 tools/summarize-method-trace.py out/profiles/<run>/methods.trace
```

`check-profile.sh` verifies all eight fixtures are repeatable, validates exactly
one shared tick per replay callback, and rejects invalid names. The regular
shared suite remains `./check.sh`. Native telemetry is Android-specific; the
fixtures and gameplay remain shared. These results do not establish iOS device
performance or release-build FPS. CPU frequency, thermals, debug/JIT behavior and
background activity affect timings; repeat controlled runs before claiming a gain.
