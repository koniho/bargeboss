# Stage-entry lighting: iOS simulator evidence

Dedicated iPhone 17 Pro simulator, iOS 26.5; 30 warmup draws and 60 measured frozen-scene draws per capture. Profiles: 0 dawn, 1 daylight, 2 dusk, 3 night. This is simulator evidence, not physical-device frame pacing, touch latency, thermals or memory-pressure validation.

| Scene / profile | Interval median / p95 (ms) | Draw median (ms) | Entry preparation (ms) | Textures (MiB) | Evictions |
|---|---:|---:|---:|---:|---:|
| calumet-bridge-0 | 16.66 / 16.74 | 8.69 | 279.1 | 87.3 | 0 |
| calumet-bridge-1 | 16.67 / 16.77 | 8.72 | 264.4 | 87.3 | 0 |
| calumet-bridge-2 | 16.67 / 16.76 | 8.67 | 251.4 | 87.3 | 0 |
| calumet-bridge-3 | 16.66 / 16.79 | 8.74 | 271.5 | 87.3 | 0 |
| fallback-coregraphics | 66.67 / 66.68 | 16.33 | 0.0 | 0.0 | 0 |
| fallback-water-textures | 20.06 / 33.35 | 15.26 | 1154.3 | 160.0 | 314 |
| indiana-bridges-0 | 17.23 / 19.02 | 14.47 | 195.4 | 66.2 | 0 |
| indiana-bridges-1 | 17.70 / 19.76 | 14.70 | 197.2 | 66.2 | 0 |
| indiana-bridges-2 | 19.30 / 21.12 | 16.06 | 208.3 | 66.2 | 0 |
| indiana-bridges-3 | 33.33 / 33.36 | 16.56 | 212.1 | 66.2 | 0 |
| montrose-inlet-0 | 16.67 / 17.23 | 12.87 | 394.6 | 140.9 | 0 |
| montrose-inlet-1 | 16.67 / 17.15 | 12.82 | 386.8 | 140.9 | 0 |
| montrose-inlet-2 | 16.67 / 17.05 | 12.71 | 408.3 | 140.9 | 0 |
| montrose-inlet-3 | 16.65 / 17.02 | 12.62 | 398.7 | 140.9 | 0 |
| tutorial-yard-0 | 16.66 / 18.67 | 11.95 | 1027.7 | 160.0 | 315 |
| tutorial-yard-1 | 16.67 / 16.73 | 12.34 | 976.5 | 160.0 | 314 |
| tutorial-yard-2 | 16.67 / 16.74 | 11.77 | 921.5 | 160.0 | 315 |
| tutorial-yard-3 | 16.67 / 16.75 | 12.16 | 898.6 | 160.0 | 314 |

Metal retains its 160 MiB texture budget. Upload and eviction totals include entry preparation: a full manifest can exceed that budget, so prewarming does not promise that every frame stays resident. Preparation currently blocks at stage entry; a loading presentation is follow-up work. Core Graphics does not expose Metal texture/preparation counters (shown as zero).

The sixteen native screenshots and two fallback captures were reviewed from `out/ios-profile/lighting-clean-*`; large generated screenshots remain untracked. Earlier captures on the account-linked simulator were obscured by an account reminder and discarded. Actual menu interaction also verified daylight → title → night in one process.

Shared validation: full checks and Android APK build passed; JVM/J2ObjC replay and drawing parity passed (`28c1c74c:2:1350`); 2,304,000 exact desktop cache round-trip pixels matched across four locations; 1,461,230 all-heading shoreline pixels passed. No Android installation or physical-phone validation was performed. Daylight factory/steelworks windows retain baked illumination; see [lighting architecture and art follow-ups](../../rendering/lighting-profiles.md).

Indiana night reached a 33.3 ms median and Core Graphics reached 66.7 ms; these are unresolved pacing limits, not a 60 FPS claim. The fallback preserves the selected palette but uses simpler flat materials. No controlled baseline or physical-device run attributes those differences to this change.
