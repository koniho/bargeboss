# Default Vulkan and 60 FPS pacing

Android now attempts Vulkan unless explicitly launched with `bb_renderer=canvas`.
Existing initialization/runtime fallback to Canvas remains. Standard APK builds
include native shaders/library; CI and release workflows install NDK 27.0.12077973.
`BARGEBOSS_VULKAN=0` permits Canvas-only host builds. Current native ABI is arm64.

Shared FramePacer uses host-supplied monotonic nanoseconds to admit 60 draws/sec.
It accumulates deadlines, tolerates 0.5 ms scheduling jitter, skips missed deadlines
without catch-up bursts and resets on resume. Android uses it before Vulkan scene
composition, including developer fixtures and touch-triggered invalidations.
Game.advance, audio, input and haptic transport continue at host callback cadence.
Command reuse remains OFF. iOS does not invoke this pacing gate.

Validation: full checks/build passed in isolated worktree; pacing assertions cover
60/90/120/144Hz plus long gaps/reset. Default non-profile APK installed with existing
key; plain Activity launch (no renderer extra) logs Vulkan active. Native log shows
120 completed frames per approximately 2 seconds, no replaced packets. Collected
683 compositor intervals: mean 16.67696 ms, median 16.65187 ms, p95 24.98958 ms. This is a
normal title smoke capture; not sustained gameplay or a guarantee of even 16.7 ms
spacing. Surface history includes a short pre-poll tail. Raw timestamps retained.

Cross-vendor, all-stage and sustained CPU variability remain outstanding. Vulkan
uses single-sample rasterization; Canvas fallback remains available. Build/release
workflow edits are validated locally, not by a completed remote CI run here.
