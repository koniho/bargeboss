# Android bulk foam vertex staging — 2026-11-19

Pixel 11 Pro, Skia Vulkan, frozen barge-push tutorial at minimum .65 zoom.
Static and live surface meshes enabled in both cases. Fresh ART sampling ranked
FoamMesh.add at 7.8% inclusive main-thread sampled CPU (not exact invocation time).
Native per-float stores remained after the previous surface-buffer optimization.

Foam now stages exactly the same six vertices/patch in a bounded heap float array
and copies them into the native buffer once per flush. Extra storage: 73,728 bytes
(72 KiB). Batch capacity, geometry, coverage shader, tint, clipping, order and
animation are unchanged. Legacy writes remain selectable for developer A/B.

120 warmup + 480 measured frames, eight 60-frame ABBA blocks. First eight frames
of each block discarded; 208 retained samples per variant. No tracing during timing.

| Buffer | Median interval | p95 interval | Median CPU draw | p95 CPU draw |
| --- | ---: | ---: | ---: | ---: |
| Per-float native stores | 23.09 ms | 28.26 ms | 14.47 ms | 16.24 ms |
| Bulk copy | 22.19 ms | 26.00 ms | 13.09 ms | 14.08 ms |

CPU improvement about 1.38 ms/9.5%; end-to-end interval improvement .90 ms/3.9%.
Not 60 FPS. Separate default-enabled run: 21.36 ms median interval, 13.64 ms CPU.
Device-state variation means do not compare that directly to prior-round 24 ms.
An initial capture stopped at 116 warmup frames and was discarded entirely.

Frozen native screenshots of old/new buffer paths are pixel-identical across
2,602,800 pixels. Full check.sh/Android build passed. The separate requested light
change removes dark rectangular housings behind watercraft navigation lenses;
all-heading actual renderer sheets reviewed. Canonical drawing trace changed to
b0c81ead:2:1350 and matches JVM/J2ObjC; navigation colors/sectors remain unchanged.

Remaining sampled contributors include plain-color path submission, machinery
lamp occlusion ray tests, and unbatched shader effects. The next promising work is
batching opaque plain-color machinery faces with the already-batched material
faces, preserving the exact order. No cache increase is indicated by these runs.

Reproduce with developer extras `bb_scene=barge-push`, `bb_mode=freeze`,
`bb_camera_zoom=.65`, `bb_warmup=120`, `bb_frames=480`,
`bb_foam_buffer_sweep=true`. CSV column `bulk_foam` identifies each block.
For standalone A/B use `bb_bulk_foam=true/false`. Force-stop the previous app
before removing its output file, and reject incomplete CSVs before analysis.

Final moving replay: 300 samples completed, median interval 20.28 ms, p95 26.97 ms.
This smoke check uses the held minimum-zoom projection and real replay ticks.
