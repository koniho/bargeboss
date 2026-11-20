# Vulkan command reuse and swapchain lifetime, 2026-11-20

Device: connected Android grizzly, PowerVR C-Series; landscape 2410x1080 screen,
2238x1080 game surface. Thermal status sampled as LIGHT (1); no override.
Branch: `perf/android-vulkan-command-reuse`. Canvas remains the production default.
The comparison captures precede the final rebase onto `c50f07e` (finite tutorial
scenery/camera). Do not attribute those scenery changes to this A/B comparison.

## Finding

The experimental renderer rebuilt its swapchain and pipelines on every
`VK_SUBOPTIMAL_KHR` presentation result. The surface reports transform=2
(ROTATE_90); the renderer intentionally uses IDENTITY for its existing geometry.
The mismatch persists after recreation. Instrumentation found resource generation
advancing every frame and cached commands cleared every frame, despite identical
vertex data. This made command reuse impossible and included repeated device-idle,
resource destruction, swapchain creation and pipeline construction in the metric
labelled `present`. Earlier interpretation as inherent presentation/driver cost
was therefore too broad. These findings concern our Vulkan host, not proof of the
same defect in Android Canvas/HWUI.

The fix retains a usable suboptimal swapchain when its observed dimensions and
surface transform are unchanged. OUT_OF_DATE still rebuilds; a changed extent or
transform also rebuilds. Surface destruction recreates the whole renderer.
[Khronos specifies SUBOPTIMAL still permits successful presentation](https://docs.vulkan.org/refpages/latest/refpages/source/VK_KHR_swapchain.html).
The fixed captures report one swapchain build per renderer, not one per frame.
Proper prerotation remains a separate possible improvement, not necessary for
this fix and not tested here.

## Measurements

All frozen captures: `barge-push`, zoom .65, 240 warmup + 600 measured UI draws.
The tool polls compositor history across the entire sample window and filters to
CSV monotonic timestamps. UI frame production is not displayed FPS. Captures run
sequentially; CPU scheduling/thermal variation is not fully controlled.

| Capture | Mean displayed interval | Display p95 | Interpretation |
| --- | ---: | ---: | --- |
| record-a | 23.01 ms | 25.06 ms | Original recreation on every frame |
| reuse-b | 23.27 ms | 25.02 ms | Original, zero command hits |
| minimal-c | 15.68 ms | 24.91 ms | Original, clear only, full Java composition |
| reuse-diagnostic | 23.49 ms | 25.04 ms | Revealed per-frame invalidation |
| fixed-record-a | 12.16 ms | 16.67 ms | No repeated recreation |
| fixed-reuse-b | 138.29 ms | 366.42 ms | Rejected mapped-memory comparison |
| cpu-reuse-a | 11.82 ms | 16.68 ms | Fixed CPU-side comparison, steady 120/120 hits |
| fixed-record-b | 16.50 ms | 24.98 ms | Repeat, slower CPU draw/encoding |
| cpu-reuse-b | 13.16 ms | 16.69 ms | Repeat, steady 120/120 hits |
| fixed-minimal | 19.85 ms | 25.00 ms | Java draw itself rose to 18.52 ms median |
| fixed-tow-replay | 12.57 ms | 16.68 ms | Moving tight-circle tow, reuse OFF |
| fixed-record-c | 18.91 ms | 24.98 ms | Late repeat; Java draw 18.07 ms median |

The original native GPU time was about 4.4 ms and measured presentation CPU about
9 ms. After the swapchain fix GPU time remains about 4.5–4.8 ms, while presentation
CPU is typically 0.2–0.6 ms. The faster full-scene runs complete every submitted
packet. The late minimal run is CPU-production-bound and cannot be interpreted as
clear-only GPU work being more expensive; it demonstrates uncontrolled CPU
variation and is retained rather than discarded. No locked-60/headroom claim.

Command reuse needs a CPU-owned snapshot: reading 5.1 MiB from mapped device memory
for exact equality caused the severe rejected regression above. Host coherence
does not guarantee inexpensive CPU reads. Snapshots are bounded by the existing
32 MiB vertex packet limit per each of two frame slots (64 MiB worst-case extra,
about 10.2 MiB for this fixture), only when reuse is explicitly enabled. Normal
animated gameplay changes vertices; reuse remains diagnostic, not a default.

## Validation and limits

- Full Android checks and Vulkan-enabled signed build passed; final native changes
  compiled and signed again, installed preserving the existing development key.
- Before/after swapchain fix and normal/reused command screenshots each matched
  exactly: 2,602,800 pixels, zero changed pixels. Screenshot captures are local
  `/tmp/bb-before-reuse-fix.png`, `/tmp/bb-fixed-record.png`, `/tmp/bb-fixed-reuse.png`.
- Normal opt-in title and Home/resume passed without fallback; each renderer
  created one swapchain and completed 120 frames per approximately two seconds.
  See `lifecycle.txt`. The device is left on the playable Vulkan title.
- This is one device, a frozen scene and one moving tow fixture. All-stage native pixel regressions,
  sustained thermal runs, cross-vendor checks and rotation/resize coverage remain.

## Reproduce

Build with `BARGEBOSS_PROFILE=1 BARGEBOSS_VULKAN=1 ./build-android.sh`.
Use `python3 tools/profile-gpu-host.py NAME` for normal Vulkan recording;
add `--reuse-commands` or `--minimal` for diagnostic controls. For animated towing,
add `--scene tow-tight-circle --mode replay`. The controls require a developer
session. Keep the phone awake through `tools/phone-awake.sh` and restore afterward.
Raw CSV, presentation timestamps, launch arguments and native logs are retained
in each capture directory.

Final integration check after rebasing onto `c50f07e`: full Android checks/build
passed, APK installed with the existing key, then a shorter 240-warmup/180-sample
frozen capture completed without fallback: 11.29 ms mean displayed interval,
16.67 ms p95. This is a smoke test of the combined changes, not a same-scene A/B.
