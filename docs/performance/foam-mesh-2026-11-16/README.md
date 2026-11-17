# Android expanding-foam batching

Same installed APK, `tow-tight-circle` replay, 600 warmup + 900 sampled
frames, 1243.3334 x 600 logical viewport. Compare `bb_mesh_foam=false`
and `true`, then repeat in reverse order. Full foam rendering stays enabled.

The optimized path uses Android API 34 Mesh for ordered kind-5 crest patches.
It keeps the shared `water-foam.inc` coverage kernel and every emitted patch.
Each submission holds at most 512 patches, using a reusable 72 KiB staging
buffer. Each flush creates a native Mesh; this is not an allocation-free path.
Older Android versions and failed Mesh shader initialization retain the original
RuntimeShader/primitive fallback. iOS Metal already batches its vertices; the
new shared scope is explicitly a no-op on native iOS painters.

The scope contains only crest foam. Clip changes flush queued patches, and
ending the scope flushes before restoring the shoreline clip. Coordinate and
opacity wrappers forward the scope while preserving original patch transforms.

Reproduce with a profiling APK:

```sh
adb shell am force-stop com.bargeboss.game
adb shell am start -W -n com.bargeboss.game/com.bargeboss.game.MainActivity \
  --es bb_scene tow-tight-circle --es bb_mode replay \
  --ei bb_warmup 600 --ei bb_frames 900 --ez bb_mesh_foam true
```

Pull the completed CSV from
`/sdcard/Android/data/com.bargeboss.game/files/profiles/tow-tight-circle-replay.csv`.
Use `false` for the original path. Keep the phone unlocked and awake throughout.
Raw CSVs/screenshots remain untracked under `out/foam-mesh`.

First pair: 24.18 -> 59.85 FPS (mean interval 41.351 -> 16.707 ms);
interval p95 58.314 -> 17.417 ms; UI draw p95 13.945 -> 8.978 ms.
All 1,500 rows match in tick, zoom, headings, speeds, tow count/tension and positions.
Native final-frame screenshots: 6,023 / 2,602,800 pixels differ, maximum channel
delta 6/255, RGB mean absolute difference 0.001144/255. Visual review retains
the wake arcs, density, positioning and overlap. Minor raster rounding changes
are expected from triangle interpolation. No sample cache builds/evictions.

Full checks, Android/iOS builds, JVM/J2ObjC parity, shared shoreline/foam pixel
regressions and iOS simulator replay passed. These are not proof of identical
pixels on every Android GPU or a physical iPhone. The replay reaches roughly
60 FPS; total GPU headroom has not been quantified.

Reverse-order confirmation: optimized 60.05 FPS (mean 16.653 ms, p95 17.488 ms),
original 23.89 FPS (mean 41.859 ms, p95 58.183 ms). UI draw p95
9.130 vs 13.827 ms. All four runs have exactly matching physics/camera fields
across 1,500 rows. Reverse-pair screenshot comparison has the same 6,023 changed
pixels, maximum channel delta 6 and MAE 0.001144. The optimized screenshots were
reviewed. This confirms the gain is not simply run ordering or a cooler device.
