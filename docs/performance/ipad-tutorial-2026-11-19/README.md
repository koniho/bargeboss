# iPad tutorial timing — 2026-11-19

Physical iPad Pro 12.9-inch (6th generation, M2), iPadOS 26.6.1. Signed Release
build, Metal renderer, default full native resolution, requested 60 Hz. Developer
Mode enabled and development provisioning refreshed to include the device.

Shared `barge-push` fixture. 120 warmup draws followed by 300 wide, 301 close,
and 302 replay samples. Distinct counts prevent retrieving a previous capture.
Frozen comparisons use `--ios-zoom=.65` (player minimum) and `--ios-zoom=1.0`.
Replay initializes at .65 then resumes normal shared camera behavior; it is a
moving smoke test, not a fixed-zoom comparison. Raw CSV and Metal counters included.

| Run | Median interval | p95 interval | Median CPU draw | Median GPU | p95 GPU |
| --- | ---: | ---: | ---: | ---: | ---: |
| Frozen minimum zoom | 16.678 ms | 16.725 ms | 7.819 ms | 5.412 ms | 8.260 ms |
| Frozen normal zoom | 16.676 ms | 16.745 ms | 6.810 ms | 6.214 ms | 8.733 ms |
| Moving replay | 16.675 ms | 16.759 ms | 6.488 ms | 6.617 ms | 9.049 ms |

These runs sustain the requested 60 Hz. Minimum zoom increases CPU work modestly
without the Android slowdown. Metal already batches surface triangles; Android's
new surface submission optimization does not provide a comparable iOS change.
CPU and GPU times overlap and must not be added into a serial frame estimate.
GPU values come from the most recently completed command buffer and can lag CPU
samples; compare distributions, not row-by-row subtraction.

No texture eviction or pending preparation at the final wide capture. About
101 MiB texture bytes against a 160 MiB budget, 16 MiB vertex capacity. Counters
are diagnostic snapshots, not all per-frame values. There were 4434 material
surface patches and 867 foam patches in the final frozen wide frame.

Shared profile zoom anchor/validation tests and signed native build passed.
Installed and launched successfully; returned to normal gameplay afterward.
No fresh device screenshot/pixel comparison was available in this round. Different
aspect ratio, resolution and hardware prevent direct FPS comparisons with the
Android phone. These short tutorial captures do not guarantee every stage or
older iPhone performance, nor sustained thermal headroom over long sessions.
