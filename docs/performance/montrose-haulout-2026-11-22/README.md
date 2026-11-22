# Montrose haulout native evidence

Connected Pixel11Pro, Vulkan, profile Debug APK. `freeze-frames.csv` captures the
Dockmaster panel after60warmup frames,60samples. `replay-frames.csv` captures850
frames of the production public-input tow/crew handoff (no warmup). Inspected the
native end image: the disabled cruiser is secured inside the well, the tug is
still outside, and the active job is the subsequent yacht tow.

These are functional/visual captures, not an isolated before/after performance
comparison. Desktop validation and simulator activity overlapped this round.
The replay includes startup, radio, camera movement, towing and handoff; its
percentiles describe that sequence rather than a stationary renderer budget.
Raw counters retain decode/preparation work and frame-interval outliers.

The simulator daylight and dusk review used iPhone17Pro/iOS26.5 Metal. The full
shared trace including haulout is `e501393e:2:1350` on JVM and J2ObjC. These checks
do not establish physical-iPhone performance or end-to-end four-mission play.
