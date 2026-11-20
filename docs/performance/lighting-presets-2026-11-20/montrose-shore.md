# Montrose grass and shoreline follow-up

Tested existing commit cb15b0b on the dedicated iPhone 17 Pro iOS 26.5 simulator, using `montrose-green-shore` and `montrose-overview`, each at dawn/daylight/dusk/night. Captures use 45 warmup draws and 30 measured frozen-scene draws. No gameplay or artwork changes were made.

## Findings

- Grass, limestone, promenade and pier respond to the selected profile. Dawn is warmer, daylight is brighter, and dusk/night are subdued and cool. No terrain retaining a previous preset was observed in these fresh-launch captures.
- Grass is visibly tiled: daylight exposes repeating diamond boundaries and repeated texture; the overview shows a dense repeating pattern. This is an art/material continuity issue, not evidence of a stale palette.
- The current Montrose implementation has no separately authored sand-beach material. The pale entrance shore is limestone revetment plus concrete promenade/steps (`MontroseEntranceShore`, `MontroseShoreArt`). Sand-beach lighting is therefore unverified.
- Overview performance fails a smooth-frame target on this simulator. Close shore is around 16.7 ms; overview is substantially slower across presets. Cumulative direct-layer fallback counts are high, but these captures alone do not prove the cause or establish a regression against main.

| Scene/profile | Median / p95 interval (ms) | Texture MiB | Cumulative layer fallbacks |
|---|---:|---:|---:|
| montrose-green-shore-0 | 16.67 / 17.24 | 147.2 | 12 |
| montrose-green-shore-1 | 16.66 / 17.18 | 147.1 | 12 |
| montrose-green-shore-2 | 16.67 / 16.99 | 147.1 | 12 |
| montrose-green-shore-3 | 16.67 / 16.68 | 147.1 | 12 |
| montrose-overview-0 | 233.32 / 266.66 | 159.3 | 27024 |
| montrose-overview-1 | 233.31 / 266.61 | 159.3 | 27024 |
| montrose-overview-2 | 216.66 / 216.78 | 159.3 | 27024 |
| montrose-overview-3 | 216.67 / 216.76 | 159.3 | 27024 |

Raw native screenshots: `out/ios-profile/lighting-shore-*`. Comparison sheet: `out/lighting-review/montrose-shore.png`. Generated images remain untracked. This is simulator-only validation; no phone was accessed or installed. Follow-ups are seamless lawn material, clarification/authorship of a sand beach, and isolated overview cache/performance investigation.
