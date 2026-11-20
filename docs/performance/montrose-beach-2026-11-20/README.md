# Expanded Montrose native validation

Android Vulkan, connected development phone, default command/foam paths. Captures use shared fixed-step replay with 120 warm-up + 180 measured frames, except the enlarged whole-stage stress view (180 warm-up). These are SurfaceFlinger presentation intervals, not just UI/CPU timing. These captures precede the final requested corrugated-waterline follow-up; its shared checks/build/parity pass, but final device deployment is blocked as described below. Native summaries are retained beside this file; raw CSV, native logs and compositor timestamps remain under `out/profiles/montrose-expansion-*`.

| Scene | Zoom | Median presentation ms | Mean presentation ms | CPU draw median ms |
| --- | ---: | ---: | ---: | ---: |
| Main before expansion, original marina camera | .65 | 16.67 | 20.17 | 17.12 |
| Expanded stage, original marina camera, first | .65 | 24.96 | 21.77 | 18.60 |
| Expanded stage, original marina camera, final repeat | .65 | 24.97 | 21.63 | 18.42 |
| New beach, final | .55 | 16.65 | 16.65 | 7.15 |
| New beacon, final | .80 | 16.65 | 16.65 | 7.16 |
| Enlarged whole-stage stress view | .25 | 33.30 | 33.26 | 30.35 |

The new beach and beacon views are near 60 FPS; the original wide marina is approximately 46 FPS by mean interval, versus about 50 FPS in the previous main capture. The expanded whole-stage stress view remains about 30 FPS. There was no old-APK reinstall or controlled thermal A/B sequence in this round, so the small marina difference is not isolated to a specific expansion change. These limits remain open; terrain cache-fit alone does not establish full-frame performance.

The first native beach image revealed filtered cache seams. The final tile painter overlaps both sand and its identical deterministic detail by four world units; the final native image confirms continuous sand without the tile grid. Per the final requested shoreline treatment, all hard-bank vertical faces are corrugated around the horn and stop at the sandy beach; walking tops/caps stay concrete. Actual renderer overview/beach/beacon/pavilion/inlet exports and native beach/beacon screenshots are untracked in `out/montrose-beach/`.

Full shared checks, asset checks and Android developer build passed using the existing debug signing key. JVM/J2ObjC gameplay/drawing parity passed `d0f13157:2:1350` after integration with main `12b881b` and the final seawall treatment. Dedicated new geography and seawall-transition checks: 62. All-heading shoreline pixel checks: 2,434,414. Terrain cache/density/purity checks: 3,966. All three added developer scenes passed deterministic setup and one-step replay checks. The broader `ProfileScenarioTest` then hits its existing barge-push camera-anchor tolerance assertion; the same failure was reproduced with unchanged main `ProfileScenario` source.

No physical iOS or Android Canvas validation was performed for this stage expansion. J2ObjC parity does not establish native raster identity or device latency.

The beach/concrete-polish build was installed and visually reviewed, then left in interactive beach play with its awake scope restored. During deployment of the subsequent requested corrugated-waterline build, wireless ADB dropped offline. The incremental install reported success but no package path could be verified; the full non-incremental retry failed when the connection closed. ADB rediscovery currently finds no device. Final seawall APK installation, native review and screen-timeout restoration await reconnection. The task awake-scope file retains the prior 300000 ms timeout at `out/phone-screen-timeout-before.txt`; do not discard it before restoration. No uninstall, data-clear or security-setting change was requested.
