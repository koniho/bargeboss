# Android rotation preparation — 2026-11-15

Production now prefetches every heading/effect frame of the **upcoming level's
watercraft**, using shared `LevelAssets.Plan` manifests and a 160 MiB cropped-cell
CPU cache. Required sets are pinned; old-level-only cells become ordinary LRU
entries when the plan changes. Source sheets and terrain/UI rasters remain separate.

This removes measured rotation cropping/eviction during play. Overall frame pacing
is similar; it does not solve native path/queue costs or guarantee GPU residency.

## Evidence groups

- `baseline-1`, `prefetch-1`, `prefetch-2`, `baseline-2`: initial all-catalog prototype
  before the user's scoped-level follow-up. 1,024 prepared cells, ~86 MiB. All four
  runs share the 1243.333×600 logical viewport. `comparison.json` verifies matching
  replay/camera data. These are historical results, not the final startup policy.
- `baseline-3`, `scoped-1`, `baseline-4`: final matched comparison at 1320.354×600.
  The tutorial prepares 704 cells (~53 MiB); foreground frame crops are 201 → 6 →
  201 and evictions 29 → 0 → 29. `scoped-comparison.json` verifies identical replay
  sequences and camera values. The remaining six crops are nonrotation artwork.
- `scoped-2`: supplementary final-policy capture at 1243.333×600. Five nonrotation
  crops, zero evictions, all 704 frames ready. Rejected from the three-run comparison
  because the viewport changed. Do not pool it with that matched timing result.
- `rotation-review.txt`: initial all-catalog native review. `rotation-review-final.txt`
  repeats it with the final implementation: 1,024 frames, 9,437,184 compared pixels,
  zero differences, 27 forced scenery evictions, zero rotation rebuilds, cancellation
  passed. `rotation-levels.txt` checks missing/unneeded cargo, level transitions,
  rapid replacement, old-only frame unpinning and current-frame retention.

All replay captures use 120 warmup and 360 measured draws, no method/native trace,
full device resolution, individual paths, cropped textures and asynchronous terrain.
No concurrent builds or tests ran during timed captures. Screenshots remain in ignored
`out/profiles/`. Raw native frame and memory snapshots accompany each CSV. `texture_mib`
is referenced bitmap storage, not GPU residency; process PSS includes other allocations.
`rotation_prepare_ms` is CPU preparation elapsed time. Replay advances per recorded
draw; throughput above 60 is not a claim of displayed FPS.

For a fresh comparison, install a `BARGEBOSS_PROFILE=1` APK and run:

```sh
BARGEBOSS_PROFILE_FRAME_CACHE_MIB=24 BARGEBOSS_PROFILE_ROTATION_PREFETCH=false ./tools/profile-android.sh barge-arrival replay
./tools/profile-android.sh barge-arrival replay
```

The final host uses scoped manifests. Full-catalog preparation remains available
via `CanvasPainter.prepareRotations()` for tooling; normal gameplay passes
`LevelAssets.upcoming(game).rotationAtlases()`. Shared replay signature is unchanged:
`ad11a073:2:1350`. See `optimization.md` and the developer profiling guide for
implementation details, measured distributions, memory tradeoffs and platform limits.
