# Dense tutorial district review

The closer camera and packed factory layout were reviewed through complete real
gesture replays using the production shared renderer at 960×600 and 1333×600.
No separate scene or camera implementation was used for the captures.

`FactoryPlacementTest` passes 13,268 assertions over the full 64-unit planning
grid. The district contains 664 factory modules occupying 1,284 cells and 710
drawable road tiles. Every eligible non-road cell is occupied; the remaining
unfilled factory cells require shoreline clearance. Multi-cell footprints never
cross roads, overlap another footprint or enter the connected waterway. All
sixteen variants are present, including low, medium and tall structures. Taller
structures retain the larger inland setback. Permanent placement order is
already sorted by isometric ground depth.

| Actual-render audit | 960×600 | 1333×600 |
| --- | ---: | ---: |
| Completion ticks through all ten lessons | 3,291 | 3,286 |
| Distinct factory variants substantially visible | 16 | 16 |
| Maximum factory sprite draw calls in a captured frame | 81 | 105 |
| Maximum substantially visible factory source bounds | 59 | 82 |
| Factory footprint samples checked clear of water | 358,872 | 358,872 |

“Substantially visible” means at least 15% of the source's measured opaque-bound
rectangle intersects the canvas, with opacity at least 0.25. This is a conservative
screen-coverage audit, not a per-pixel visibility or occlusion calculation. The
preview rejects more than 128 factory sprite calls per capture and any legacy
scenery atlas. Drawing counts establish culling bounds, not native frame-rate or
touch-latency parity.

Reviewed the canal entrance, winding channel, harbor arrival, assisted push,
unloading and towing release at both widths. Factory rows and road intersections
read consistently, while the raised shoreline apron stays clear. Foreground
buildings become translucent around vessels and working ropes. The close berth
view keeps the tug, taught barge and berth target readable; the tutorial card
does not cover the taught towing action. Source foundations retain their approved
uniform scale and small service insets rather than stretching to fill a grid cell.

Reproduce with `tools/TutorialSceneryPreview.java`, passing width, height and an
optional output directory. This review used `out/tutorial-dense-960` and
`out/tutorial-dense-1333`. The full contact sheet and selected canal, unloading
and towing frames were copied to Downloads under descriptive
`BargeBoss-dense-industrial-...-v2.png` and `BargeBoss-close-...-v2.png` names.
Preview exports remain untracked build artifacts. The main workflow performs
the final shared checks and Android build/install; this review does not claim
independent iOS device validation.

## Camera and final integration checks

Working play targets 1.5× zoom. Docking can rise to 1.75×, capped to keep the tug,
active barge and actual raised bollard inside the usable view. Canal travel and
the middle bridge reach use 1×; the tutorial blends into its closer view by
position at the harbor mouth. The view follows nearby work rather than keeping
the whole stage visible. A delivery still gets its three-second overview before
returning to the closer default. Aim and rope flight freeze the camera; easing
preserves the finger's screen-space drag anchor.

`./check-art.sh` passed on JVM and Android ART with matching signature
`2a2fafbf:11:8970`. This includes 131,423 continuous-tutorial checks and 5,810
stage/camera/bridge checks. `./preview.sh` and the desktop package/persistence
checks passed. The signed Android APK installed successfully through Shizuku,
and the activity launch returned `Status: ok`.

Native iOS results remain dependent on GitHub CI. The preceding revision's run
43574522990 completed its Android and three desktop jobs successfully, while its
iOS job was cancelled; that run does not establish a passing native iOS UI test.
