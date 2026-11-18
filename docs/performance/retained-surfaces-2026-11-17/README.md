# Retained static shore commands

Android now records immutable 256-unit shore surface chunks into RenderNodes.
Each chunk keeps its original place in the draw order. Camera translation and
scale apply at playback; markings, caps, dynamic lighting, vessels and foreground
occlusion remain separate. Other painters and unsupported wrappers retain the
original direct path. There are no new bitmap textures or compositing layers.

The cache retains at most 96 nodes and 8 MiB of reported command allocation.
Android's `computeApproximateMemoryUsage` excludes referenced bitmaps and child
nodes, so this is not a total GPU-memory guarantee. These chunks have neither
bitmap nor child-node commands. See [RenderNode documentation](https://developer.android.com/reference/android/graphics/RenderNode).
Eviction drops ownership without discarding a node that an already-recorded
parent might still reference. Native invalidation triggers rerecording. Painter
close releases retained lists. Material/geometry identity is supplied by core.

## Measurement

Pixel 11 Pro, frozen Shared Slip, requested zoom .65, actual scale .7834479.
60 warmup frames then four 60-frame direct/retained/retained/direct blocks.
Discard eight transition frames per block. `summary.json` records medians,
p95 and means. Raw data and native screenshots: local `out/surface-shader`.

- Draw medians: direct39.146, retained24.714, retained24.504, direct37.315ms.
- Frame intervals:70.469,70.810,70.592,70.928ms. No FPS improvement claimed.
- Retained command storage:.453MiB. Bitmap caches remained106.614MiB frames
  and28.813MiB terrain. Existing160/48MiB budgets unchanged.
- A smaller dedicated surface shader was tested first and rejected: direct draw
  40.232/40.835ms versus candidate42.039/41.450ms. No consistent benefit.
- Native screenshot comparison:2534/2602800 pixels changed, max channel error5,
  RGB mean absolute error.000655/255. Different floating-point transform paths
  can slightly affect antialiasing; this is not pixel-identical.
- Shared Slip replay exercised camera scale.8467..2.5911; retained commands stayed
  .453MiB and frame-texture evictions stayed zero. This checks camera reuse, not
  navigation through every part of the map.

Reproduce with a profile APK and `--es bb_scene shared-slip-wide --es bb_mode freeze
--ef bb_camera_zoom 0.65 --ei bb_warmup 60 --ei bb_frames 240 --ez bb_surface_sweep true`.
Mask128 in `bb_exclude` disables retained commands. `bb_surface_review` runs native
bounds, eviction, reload, invalidation and close checks separately from timing.
The CSV field `surface_command_mib` reports retained command allocation.
