# Android direct Vulkan experiment — 2026-11-20

Status: opt-in development backend, **not the default renderer**. This experiment
has not established 60 FPS, visual parity across all headings, or broad device
compatibility. Canvas remains the normal launch path. iOS was not changed.

## Build and capture

Set the existing Android SDK/JDK environment, then:

```sh
BARGEBOSS_PROFILE=1 BARGEBOSS_VULKAN=1 ./build-android.sh
adb install --no-incremental -r build/bargeboss-debug.apk
python3 tools/profile-gpu-host.py vulkan-review --renderer vulkan
python3 tools/profile-gpu-host.py canvas-review --renderer canvas
```

Native compilation uses NDK 27.0.12077973 (`ANDROID_NDK_HOME` overrides it),
Vulkan 1.0 and arm64-v8a. A normal build needs no NDK. Launch the experiment with
`adb shell am start -S -n com.bargeboss.game/.MainActivity --es bb_renderer vulkan`.
An ordinary launch uses Canvas. Unsupported initialization or drawing failures
fall back to Canvas. A missing translucent-layer bitmap uses a Canvas preparation
frame rather than applying incorrect per-primitive opacity, then retries Vulkan.

## Architecture and bounds

Shared Java still owns simulation, input, fixed steps, geometry, art selection,
shader parameters, clipping and draw order. VulkanPainter encodes ordered triangles
and texture references. Native code owns textures, pipelines, stencil, submission
and presentation. Existing Canvas resource preparation supplies decoded cells and
bounded static raster layers; this is not a second game implementation.

- GPU texture budget: 160 MiB, independently of the existing CPU bitmap budget.
  References retained by this cache can prolong CPU bitmap lifetimes. This budget
  is not a measurement of physical GPU residency or driver allocations.
- Immutable surface command cache: at most 96 entries / 8 MiB. Cached positions
  remain local; camera transforms are applied at use. Chunks remain independently
  sortable. Mutable effects are never included.
- Encoded geometry: at most 32 MiB / frame and 65,536 batches. Java staging/direct
  buffers, two native packets and two GPU staging buffers each have their own
  storage; their maxima are not a single shared 32 MiB budget.
- Two native packet slots: one rendering and at most one pending. A newer packet
  replaces an obsolete pending one instead of growing latency. Gameplay remains
  on its existing thread; only drawing packets cross to the native render thread.
- Vulkan resource operations are serialized with drawing. Rare texture eviction
  waits for retirement and discards an obsolete pending packet. Upload transfer
  storage is drained in 32 MiB bursts, rather than queue-idling for every texture.
- Scoped stencil writes are scissored to the conservative bounds of enclosed color
  draws, padded by one native pixel. Push/pop use identical bounds, including
  nested scopes. No shore geometry or vessel occlusion is simplified.
- Single-sample rendering is currently selected. The 4x MSAA trial was too slow;
  polygon edge quality remains a rollout limitation. Shader circles have analytic
  edge coverage, and translucent line caps do not overlap their shafts.

## Exploratory measurements

Device reports PowerVR C-Series CXTP-48-1536 MC1 (`grizzly`). Fixture:
`barge-push`, frozen tick 8509, camera override .65 (actual camera scale .7834479),
120 warmup + 300 measured UI draws. Thermal service reported LIGHT during part
of this sequence. These are sequential exploratory runs, not a controlled ABBA
proof of each individual change. Raw CSVs and summary.json are retained here.

| Trial | Median draw-start interval | Median UI drawing elapsed |
|---|---:|---:|
| Fresh Canvas control | 27.24 ms | 13.96 ms |
| Initial 4x MSAA Vulkan | 141.45 ms | 139.86 ms |
| Retained surfaces, 4x MSAA | 114.41 ms | 113.33 ms |
| Shader family specialization, 4x MSAA | 84.67 ms | 83.72 ms |
| Early fragment tests, 4x MSAA | 87.55 ms | 86.43 ms |
| Every shader kind specialized, 4x MSAA | 109.95 ms | 108.63 ms |
| Same variants, single sample | 42.35 ms | 41.47 ms |
| Bounded stencil, single sample | 33.71 ms | 32.91 ms |

Bounded/unbounded single-sample native screenshots were identical across
2,602,800 pixels (zero changed pixels). Shared all-heading shoreline regressions
pass in check.sh; those are **not** a complete Vulkan-native all-heading review.

The first GPU timestamp attempt (`vk-gpu-timing`) hit Canvas fallback while a
translucent layer was unprepared. Its timings are excluded. An activity-launch
race also invalidated the first mailbox capture. Per-frame `vulkan` CSV markers
now distinguish Canvas draws from Vulkan packet submissions.

## What the timestamp investigation established

With scoped, transient S8 stencil, actual GPU execution was about 4.6 ms, while
synchronous `vkQueuePresentKHR` elapsed time was about 19 ms. This is not evidence
that the GPU itself needs 19 ms. Moving submission to a native thread reduced UI
drawing to about 11 ms, but SurfaceFlinger still showed ~28.74 ms mean presentation
interval in that FIFO run. More than 100 queued packets were replaced. Reporting
UI FPS alone would have falsely implied success.

A mailbox presentation trial measured about 12.2 ms of thread CPU *inside*
presentation, as well as waits. Restoring broad geometry batches and eliminating
redundant state bindings reduced that driver CPU figure to ~7 ms, with GPU work
~6.5 ms. Do not sum these pipelined averages or infer displayed FPS from them.
The final comparison below uses compositor timestamps as well as UI CSVs.

## Remaining validation before any default switch

Require repeated real presentation wins versus Canvas, including moving/towing,
Montrose and scenery-heavy stages, sustained thermal runs, native shoreline and
32-heading asset comparisons, cold/eviction behavior, suspend/resume and multiple
GPU vendors. Current captures cover one phone and a frozen tutorial. No broad
compatibility or 60-FPS/headroom claim is justified yet.

## Final displayed-frame comparison

Order: Vulkan A → Canvas A → Canvas B → Vulkan B. Each has 240 warmup +
600 sampled UI draws. All measured Vulkan rows confirm native submission;
no Canvas preparation frames occurred in those sample windows. Compositor
history contains only the final 62 intervals (roughly 1.4–1.7 seconds), not
all 600 UI draws. Longer presentation traces remain necessary.

| Run | UI drawing median | Displayed interval mean | Displayed interval p95 |
|---|---:|---:|---:|
| Vulkan A | 11.00 ms | 22.69 ms | 25.01 ms |
| Canvas A | 12.65 ms | 22.56 ms | 33.33 ms |
| Canvas B | 12.84 ms | 22.29 ms | 33.32 ms |
| Vulkan B | 19.42 ms | 27.93 ms | 33.34 ms |

Vulkan did **not** deliver a repeatable throughput win; the second pass was
worse. GPU timestamps stayed about 4.3–4.6 ms. Driver CPU inside presentation
was ~8.6–10.0 ms, plus ~2.7–3.3 ms native command encoding; presentation elapsed
was ~19–25 ms. The normal path remains Canvas. No cache increase is justified
by these results: both Vulkan sample windows had zero frame-image constructions,
and the queued preparation reservation stayed constant at 3.90 MiB.

The final encoder batches compatible shader families while sharing the plain
color/circle pipeline, avoids redundant descriptor/scissor bindings, and uses
vertex-only stencil pipelines. Command count fell from 4,827 to 1,223 including
non-drawing scope markers. Shader specialization per individual effect kind was
rejected: it fragmented batches and increased driver work. The asynchronous
renderer fixes UI blocking, but must not be advertised as an FPS improvement
without compositor evidence. Thermal service reported LIGHT; the cause of the
second Vulkan pass's degradation was not isolated.

A short moving replay smoke test (60 warmup + 120 samples) stayed on Vulkan,
with ~25.92 ms mean displayed interval. This checks motion/queue execution,
not sustained performance or full native pixel parity. Final full check.sh and
Android Vulkan/profile build passed; the checked APK was installed.

Normal-title smoke testing found repeated uploads of a shared sky bitmap under
separate atlas-frame keys, exhausting the GPU budget and correctly activating
Canvas fallback (`title-budget-fallback.txt`). GPU textures now deduplicate by
Bitmap identity as well as authored cache key. Each strip retains its source UVs;
this fixes duplicate storage without enlarging the budget. Full checks and the
Android build passed again after the fix.

After texture sharing, the normal title stayed on Vulkan with no budget fallback.
Native completion logs recorded roughly 120 frames per two seconds with negligible
packet replacement. This is a title smoke check, not evidence of 60 FPS in the
wide tutorial. Background/foreground recreated the native surface successfully
without fallback (`lifecycle-fixed.txt`). The phone was returned to normal Canvas
launch after validation.
