# Water drawing and overlay text — 2026-11-15

Water retains its current appearance while doing less clipping and native drawing
work. Text has been removed from the world: vessel tags, berth/cargo names, buoy
numbers, dock machinery status, rope captions and floating control instructions.
Menus, HUD, tutorial coach panels and screen-edge navigation UI retain text.
Feeder captain calls use fixed HUD lanes and gate-clear notices use a fixed panel.

## Implementation

- The water polygon clipper skips non-crossing clip planes, rejects offscreen
  projected bounds before clipping, and reuses precomputed circle directions.
- Wave segment sine constants are precomputed. Invisible/dry cells are rejected
  before calculating their animated crest phase.
- `WaterBatchPainter` joins same-color screen polygons in one fill only when
  padded bounds are disjoint and no intervening overlap can change compositing.
  Buffer size is 256 polygons, with at most 48 groups searched per insertion.
  Any clip or non-polygon operation flushes pending geometry.
- `Painter.polygons` executes matching multi-contour paths in Android Canvas,
  desktop Java2D and iOS Core Graphics; trace painters expand individual shapes.
- Remaining UI lettering uses bounded immutable text rasters instead of drawing
  every glyph cell again each frame. Content, RGB and source size identify each
  raster; screen position and pulse opacity remain live. Native cache limits stay
  at 48 MiB with one new raster per frame. Source text cache is bounded to 128
  entries and evictions never alias older native keys.

These are shared rendering decisions. Collision, forces, wakes, buoy motion,
scenery fading, rope depth, light attachment and gameplay timing are unchanged.
See the [drawing-order contract](../rendering/drawing-order.md#water-batches-and-overlay-only-text).

## Water-only visual and submission checks

Across 960, 1333 and 1920 widths, four real tutorial fixtures and three animation
phases, **30,333,600 water pixels were identical** to the pre-change wave renderer
and clipper. Separately, the same number of pixels matched between native batched
fills and individual fills. No crests, reflections, source resolution or animation
frames were removed to obtain the reduction.

| Width / fixture | Previous polygon fills | Batched native fills | Reduction |
| --- | ---: | ---: | ---: |
| 960 close harbor | 1,482 | 1,307 | 11.8% |
| 1333 canal middle | 1,368 | 1,172 | 14.3% |
| 1333 close harbor | 1,797 | 1,576 | 12.3% |
| 1920 close harbor | 2,039 | 1,781 | 12.7% |

Twelve full-scene before/after images were also reviewed. Differences include the
requested world-text removal, UI text raster antialiasing and relocating the
progress/notification text into overlays; full-scene pixels are intentionally
not identical. UI remains readable, while animated arrows, rings and hull glows
continue to identify touch targets.

## Controlled Android comparison

Both runs used the `barge-arrival` replay, 90 warmup draws, 240 sample draws and a
1320.354×600 logical canvas on the same development phone. The rows have identical
lesson, visual-tick and zoom sequences. The baseline APK is from `5bc974a`; the
second APK contains the combined water/text changes. Counters include the entire
scene, so they do not isolate water from cached/removed text.

| Measurement | Before | After |
| --- | ---: | ---: |
| Median UI draw submission | 11.14 ms | 9.61 ms |
| p95 UI draw submission | 15.43 ms | 15.09 ms |
| Median native polygon fills | 3,818 | 2,366 |
| p95 native polygon fills | 4,371 | 2,717 |
| Median retained layer cache | 25.96 MiB | 28.08 MiB |
| Warm cache builds / fallbacks / evictions | 6 / 1 / 0 | 12 / 6 / 0 |
| Median draw interval | 39.05 ms | 41.81 ms |
| p95 draw interval | 46.91 ms | 49.85 ms |

This is about **14% less median UI recording time** and **38% fewer native polygon
fills**, but **it is not an overall FPS improvement claim**. Frame intervals were
worse in this pair despite lower UI work. The native issue-to-swap portion remained
expensive and both runs reported many slow bitmap uploads (230/225 whole-run
frames). CPU frequency, thermals, upload pressure and native render-thread work
remain material follow-up targets. Texture/layer cache growth is recorded rather
than assumed free. Cold asset decoding also remains about 0.58 seconds here.

A subsequent stationary repeat encountered the Activity's transient portrait
layout during rotation and is excluded from the comparison. The developer hook
now waits for landscape dimensions before preparing a profiling fixture.

Raw CSVs, summary JSON, water submission counts and pixel-check results are in
[water-ui-2026-11-15](water-ui-2026-11-15/summary.json). Full screenshots, logs and
comparison images remain under ignored `out/`. Use the [profiling hooks](developer-profiling.md)
for future matched captures; avoid comparing different scenes or background loads.

## Validation

- Full shared rules/replay suite and Android ART parity passed:
  `f9fd76a9:2:1350`. The drawing signature changes with text layers and batching;
  the bounded player still delivers two loads for 1350 points with full hull.
- 129,793 batch assertions cover alpha-overlap barriers, capacity flushes,
  primitive ordering and reduced real harbor submissions.
- UI cache/world-label checks cover identity, opacity, RGB/content invalidation,
  eviction key safety and preserving HUD/tutorial lettering.
- Full production preview suite passed, including shoreline masks, affine foam,
  native-style blending, vessel lighting and all heading/depth pixel checks.
- Android installation/profile capture was exercised. The iOS primitive is
  implemented, but local iOS native raster/device performance is unverified;
  cross-platform CI performs the translated rules and simulator checks.
