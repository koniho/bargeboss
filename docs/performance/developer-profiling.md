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
| `replay` | Apply real gestures and advance one `Game.step` per recorded draw | Repeatable movement, effects, camera and lesson transitions |

Replay mode deliberately measures one simulation tick per recorded draw, not real-time
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
one shared tick per replay input, and rejects invalid names. The regular
shared suite remains `./check.sh`. Native telemetry is Android-specific; the
fixtures and gameplay remain shared. These results do not establish iOS device
performance or release-build FPS. CPU frequency, thermals, debug/JIT behavior and
background activity affect timings; repeat controlled runs before claiming a gain.

## Native texture and path comparisons

The debug session accepts independent host rendering switches. They do not
change shared geometry, artwork, sprite frames, pivots, clipping or simulation:

```sh
BARGEBOSS_PROFILE_FRAME_TEXTURES=false BARGEBOSS_PROFILE_COMPOUND_PATHS=true \
  ./tools/profile-android.sh barge-arrival replay
BARGEBOSS_PROFILE_FRAME_TEXTURES=true BARGEBOSS_PROFILE_COMPOUND_PATHS=false \
  ./tools/profile-android.sh barge-arrival replay
```

The default is cropped frames with separate native paths.
`bb_frame_textures` selects retained cropped frame bitmaps versus whole source
sheets. `bb_compound_paths` selects a compound native path versus expanding the
same water batch into individual paths. Android now skips shared grouping when
compound paths are disabled. Set `BARGEBOSS_PROFILE_WATER_GROUPING=true`
(`bb_water_grouping`) to force the previous grouping-then-expansion path for a
controlled comparison; the default is false. Compound paths always request
shared grouping. `strategies.txt` records these settings.
Run the four combinations, repeat in reverse order, and keep the phone free of
builds/tests/other activity while measuring. Compare lesson/tick/zoom sequences
as well as dimensions; exclude interrupted captures. Replay now advances in
`beforeDraw`, because Android may issue a callback without actually drawing.

Additional CSV columns:

- `texture_mib`: sum of unique bitmap allocation sizes referenced during the draw,
  including terrain/UI layers. This is a working-set estimate, **not GPU resident
  memory or measured transfer bytes**. A cold layer build can include its sources.
- `source_mib`: retained decoded source storage.
- `frame_mib`: additional retained cropped-cell storage, bounded to 24 MiB;
  full-image cells alias their existing source and do not count again.
- `frame_build_ms`, `frame_builds`, `frame_evictions`: cell creation and cache
  pressure, separate from PNG decode and terrain/UI raster construction.

For native software-raster pixel review, launch the instrumented APK with
`--ez bb_texture_review true` alongside the usual scene/mode extras. A worker
compares all production atlas frames with rotation, filtering, alpha and blends,
then four complete tutorial scenes and the title. It also forces cache eviction
and checks a reloaded tug frame. It writes `texture-pixels.txt`,
`texture-scenes.txt`, `texture-cache.txt` and comparison PNGs to the same external profiles directory.
This deliberately expensive review runs before the fixture; do **not** use its
session for performance numbers. Reports count exact differences and maximum
channel errors rather than assuming all bitmap storage strategies rasterize
identically. Hardware screenshots and device timing remain separate checks.

Compare complete runs with:

```sh
python3 tools/compare-profile-strategies.py out/profiles/<run-a> out/profiles/<run-b>
```

This rejects mismatched scene/mode/dimensions, interrupted sessions, different
lesson/tick sequences and camera scale differences above 0.5%. Smaller camera
scale differences are explicitly reported in the JSON. See the
[texture/path comparison](texture-paths.md) for measured results and limits.

## Terrain preparation

Use `BARGEBOSS_PROFILE_ASYNC_LAYERS=false` / `true` to compare synchronous raster
construction against the bounded worker and nearby-chunk hints. The default is
true. Both modes use the same terrain geometry and native raster density. See
[terrain preparation](terrain-preparation.md) for memory accounting, added counters,
validation and device evidence. `--ez bb_terrain_review true` runs the expensive
native queue/cache/pixel check before configuring a fixture; keep that review out
of timed comparisons.

## Water grouping review

`--ez bb_water_review true` compares grouped/expanded versus direct individual
wave fills in the native software rasterizer: all eight tutorial fixtures, three
widths and three animation phases. It writes `water-pixels.txt` before starting
the selected fixture. Keep this session out of timing comparisons. The desktop
`WaterBatchReview` also compares direct source order against compound batches.
The host preference only changes optional submission grouping; water geometry,
shore clipping, alpha and world animation remain shared.

## Render-thread queue and short system traces

The native summary now includes `render_queue_ms = SyncStart - SyncQueued`,
`dequeue_buffer_ms`, `queue_buffer_ms`, `issue_excluding_dequeue_ms`,
`submit_to_gpu_complete_ms` and `present_latency_ms`, plus stage maxima. Queue
wait is outside the Java `draw_ms` timer. `issue_excluding_dequeue_ms` subtracts
only the recorded buffer-acquisition duration, not every driver/scheduler wait.
Submission-to-GPU-complete includes queueing and is not GPU execution time alone.

For a separate diagnostic capture, use an instrumented APK and
`BARGEBOSS_PROFILE_SYSTEM_TRACE=true` to emit `BB draw N` sections. Run a longer
replay, wait for the **new** app PID's SAMPLING log, then record a short interval:

```sh
RISH_APPLICATION_ID=com.termux rish -c 'atrace -b 32768 -t 2 -a com.bargeboss.game -o /storage/emulated/0/Download/BargeBoss-profiles/short-trace.txt gfx view sched dalvik'
cp /storage/emulated/0/Download/BargeBoss-profiles/short-trace.txt out/short-trace.txt
python3 tools/summarize-android-trace.py out/short-trace.txt > out/short-trace-summary.json
```

Verify `buffer_overwritten` is false and `first_frame` is past warmup. This tool
reports only the game's slices and scheduler states; raw systemwide traces stay
in ignored output. A boundary-crossing slice may be absent, so use complete
interior draws. Inclusive slice durations overlap. Keep traces out of A/B timing
comparisons because detailed native path tracing adds substantial overhead.
See [render-queue findings](render-queue.md) for the measured result and limits.

### Complete Android rotation cache

Normal startup supplies `LevelAssets.upcoming(game)` to one background worker.
`CanvasPainter.prepareRotations(plan.rotationAtlases())` retains every heading/effect
variant of the listed watercraft within the 160 MiB cropped-cell cache;
other crops use the remaining LRU space. Source sheets and terrain/UI layers are
separate memory categories. This does not explicitly preupload or pin Android GPU
textures. New directional assets must join the shared preparation catalog with
all effect variants, and each level's full-set byte budget must be rechecked.
The no-argument `prepareRotations()` is the explicit full-catalog tooling path.

`LevelAssets.challenge(id)` derives cargo requirements from the same delivery-order
catalog as feeder spawning; `LevelAssets.tutorial()` includes all later lesson
loads. `LevelAssets.upcoming(root)` follows challenge selection and practice entry.
The host requests preparation only when that immutable plan changes. Rapid changes
replace pending work; generation checks reject stale publication/readiness. Old-only
frames become ordinary LRU entries and shared ones stay cached. Source-sheet storage
is still separately retained by the existing atlas cache; changing the plan does not
promise immediate release of previously decoded source sheets. This round scopes
watercraft rotations; scenery continues its existing demand/terrain preparation.

Compare the previous strategy using the same instrumented APK and fixture:

```sh
BARGEBOSS_PROFILE_FRAME_CACHE_MIB=24 BARGEBOSS_PROFILE_ROTATION_PREFETCH=false ./tools/profile-android.sh barge-arrival replay
./tools/profile-android.sh barge-arrival replay
```

The switches map to integer `bb_frame_cache_mib` (1–160) and boolean
`bb_rotation_prefetch`. They are configured before startup. Production defaults
are 160 and true. CSV columns `rotation_frames`, `rotation_ready` and
`rotation_prepare_ms` describe the current plan (reset on a plan change); `frame_build_ms` still
measures foreground cropping only. `decode_ms` includes waiting for a concurrently
loading atlas. Readiness does not imply GPU residency.

For the separate native review, launch a profiling session with
`--ez bb_rotation_review true --ez bb_rotation_prefetch false`. It checks every
prepared rotation against demand-loaded pixels, foreground/preparation races,
exact cache accounting, retention under unrelated eviction pressure, and
cancellation without late publication. It also checks tutorial/challenge manifests,
unused cargo remaining undecoded, rapid selection changes and old-level unpinning.
Results go to `profiles/rotation-review.txt` and `profiles/rotation-levels.txt`.
Run this separately from timed comparisons; review uses its own painters and a
96 MiB budget to force scenery eviction. The host prefetch switch is disabled in
this command solely to avoid a redundant preload during the review.

## iOS Metal / Core Graphics comparison

Use `CONFIGURATION=Release BARGEBOSS_IOS_PROFILE=1 ./ios/scripts/build.sh`, install
`ios/build/DerivedData/Build/Products/Release-iphonesimulator/BargeBoss.app`, then
run `python3 ios/scripts/profile.py <unique-name> --scene=barge-arrival --replay
--warmup=120 --frames=360`. `SIMULATOR_ID` chooses the device when multiple are
booted. Metal is the default; `--coregraphics` retains the reference backend.
Omit `--replay` for frozen scenes; omit `--scene` for the title. `--tutorial`
selects the opening tutorial without the optional replay fixture classes.

Results and native screenshots go under `out/ios-profile/<unique-name>/`.
See the iOS section in [optimization.md](../../optimization.md) for measured
headroom, memory budgets, accepted visual differences, and timing limitations.
The native host ignores saved progress during capture. Build normally without
`BARGEBOSS_IOS_PROFILE=1` to omit the shared test input/fixture classes.

## Paired contributor exclusions (Android)

A profile build accepts `--ez bb_contributors true` in `freeze` mode. Use
`shared-slip-wide`, `--ef bb_camera_zoom 0.65`, `--ei bb_warmup 120` and
`--ei bb_frames 1440`. It alternates 45-frame baseline/exclusion blocks, then
repeats the order in reverse. `tools/summarize-contributors.py capture.csv`
discards six transition frames and compares each exclusion to its preceding
baseline. The CSV records the exclusion mask; ordinary gameplay uses zero.

For a single exclusion use `--ei bb_exclude MASK`: 1 factory artwork, 2 factory
smoke/steam, 4 factory glows/furnace effects, 8 ambient water crests, 16 static
shore surfaces/caps, 32 native foam submission, 64 flat surface materials, or
7 all factory visuals. These are scoped passes, not all lighting or all water.
Foam submission retains shared preparation. Shore exclusion retains collision
and solid masks; flat materials retain polygon geometry. Omissions change
occlusion, overdraw and cache pressure: marginal deltas are not additive GPU
costs. Freeze isolates rendering but does not measure active tow/physics work.

`--ez bb_surface_sweep true` uses 60-frame direct/retained/retained/direct
blocks for static shore command reuse (freeze mode,60 warmup,240 samples).
Mask128 disables command reuse; `surface_command_mib` records retained native
command allocation. `--ez bb_surface_review true` runs native cache lifecycle
checks separately from timing. See [retained surfaces](retained-surfaces-2026-11-17/README.md).

Geographic bridge fixtures: `calumet-bridge` (106th bascule), `calumet-skyway`
(high fixed viaduct), and `indiana-bridges` (main-channel rail cluster). They place
an approaching tug before measurement, then use the real `BridgePassage` fixed
steps; use `live` to inspect opening/holding and `freeze` for a steady pose.
`tools/LocationBridgeBenchmark.java` separately reports cold geometry preparation
and approximate retained JVM heap; it is not a native frame-time measurement.


Cargo offloading: `calumet-offload` starts a correctly oriented barge at its
surveyed destination and uses real mooring updates before the 180-tick crane
cycle. Use 60 warmup / 240 replay samples (optionally a fixed camera zoom) to
include the lift, transfer, landing and return. The completed hull and shore
load remain present. Record timed samples with builds/tests stopped and the
phone unlocked; replay frame intervals are not equivalent to a live FPS claim.

`--ez bb_surface_mesh false` compares the retained-path reference against the
API 34+ static shore mesh default. Meshes retain each chunk's shader, geometry,
UVs and clipping order; `surface_command_mib` includes estimated vertex storage.
Use fixed zoom and a frozen fixture for A/B, then check moving-camera behavior.
See [wide tutorial measurements](tutorial-wide-2026-11-19/README.md).


### iOS fixed-camera comparison

Developer iOS builds accept `--ios-zoom=.65` with `--ios-profile` and
`--ios-scene=barge-push`. The shared developer fixture preserves the current
world anchor while changing camera zoom, matching Android's `bb_camera_zoom`.
Use frozen mode for fixed-camera comparisons; `--ios-replay` resumes normal
shared camera behavior after initial setup. The simulator helper accepts
`--zoom=.65`. Physical devices can launch with the same `--ios-*` arguments via
`xcrun devicectl device process launch`; retrieve `Documents/ios-profile.csv`
and `Documents/ios-profile-metadata.json` from the app data container afterward.
Different device aspect ratios change visible coverage, so compare zoom variants
on the same device rather than treating Android/iPad timings as like-for-like.

For interactive inspection, builds made with `BARGEBOSS_IOS_PROFILE=1` also
accept `--ios-live-scene=montrose-beacon` (or another shared fixture name).
Launch without `--ios-profile`: normal input, simulation and camera behavior
continue after the fixture loads. This mode neither restores nor writes career
progress. An optional `--ios-zoom=` sets the initial zoom only. Release builds
without the developer fixture flag ignore the live-scene argument.

### Weather experiment

Android profile builds accept `--ei bb_rain 0..3`, `--ei bb_fog 0..3`, `--ei bb_lightning 0..2`
and `--ez bb_weather_flash true`. These set shared session-only weather settings;
the flash flag starts the fixture at the peak of a test flash. Use frozen mode
for a repeatable heavy-rain/peak-flash comparison, and live mode to inspect motion.
Defaults are clear. The in-game WAVES + WEATHER panel provides the same rain, fog and
lightning controls, plus TEST FLASH and RESET ALL. Weather is currently Montrose-only.

For iOS water isolation, build with `BARGEBOSS_IOS_PROFILE=1` and use
`ios/scripts/profile.py NAME --scene=montrose-overview --water-detail=0`.
Mode 2 is normal rendering; mode 1 omits the fine ambient WaveView strokes;
mode 0 also omits water-surface crests/foam, vessel ripples/bow foam and light
reflections. The base water fill, scene, physics and camera stay intact. These
are unsaved render diagnostics, not quality settings. Mode 1 still includes
SeaWaves' larger wave crests. Compare equal warmup/frame counts and repeat in
reverse order; do not run builds or CPU sampling during timed captures.

Android profiling builds accept `--ei bb_water_detail 0|1|2` and
`--ez bb_water_quads false|true` with the normal scene/mode extras. The latter
selects the original polygon path or direct convex-quadrilateral path in the
same APK; default is true. Neither switch affects water physics or saved settings.

`barge-abilities` is a shared equipment stress/review fixture: enlarged collection
basin, fireworks in flight and both fire monitors spraying. Use frozen mode for
pixel/cache checks and live mode for sustained spray; fireworks end naturally.
The fixture uses the real ability pointer controls after lesson setup.

Finale effects: `tutorial-fireworks` prepares an actual victory launch90 ticks into
the display; `tutorial-firefight` prepares a connected fire barge with active aimed
spray. Use short samples (`BARGEBOSS_PROFILE_WARMUP=0`,120–180frames) to measure the
finite effect rather than averaging mostly idle frames after it finishes. Replay
mode uses public controls and can complete the lesson; live mode keeps the held
nozzle running. Both fixtures use the same level asset plan and core effects.
