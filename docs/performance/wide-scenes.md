# Wide industrial scene performance

The baseline was the dense district at revision `0ab709c`, before this performance
round. Its compiled classes were preserved separately while agents changed the
renderer, so the same real tutorial fixtures could be replayed before and after.
No factory assets were resized, recompressed or replaced during this round.
These measurements isolate the optimization before the subsequent intentional
night palette and vessel lights; see [night-harbor.md](night-harbor.md) for the
combined final result.

## Diagnosis

The initial Android `gfxinfo` history covered 3,680 frames from mixed scenes:
median frame time 16 ms, p90 30 ms, p95 32 ms, 165 slow-UI frames and zero slow
bitmap uploads. GPU timing was approximately 4 ms median and 5 ms p95. Because
this history mixed scenes, it is diagnostic evidence rather than a controlled
before/after benchmark. It pointed toward UI-thread submission work rather than
GPU fill or recurring bitmap decoding.

The shared renderer confirmed the command volume. At 1333×600, the initial canal
view submitted 19,177 painter calls, including 9,062 circles; 3,409 circles were
wholly outside the viewport. Factory effects alone submitted 9,119 calls. The
middle of the canal submitted 20,873 calls. Ground drawing also traversed the
corners of an inverse-projected world rectangle that lay outside the actual view.

Read-only inspection of `CanvasPainter` confirmed cached source bitmaps and atlas
rectangles, with no per-frame bitmap decode. Each sprite still performs canvas
state/transform and draw operations. Factory sprites do not use the offscreen
blend layer; that path is used by blended watercraft frames. Full source-rectangle
coverage approached ten screens in a wide view, but the Android GPU evidence did
not justify degrading image resolution.

## Measured submission reduction

| Canvas | Fixture | Before calls | After calls | Reduction |
| --- | --- | ---: | ---: | ---: |
| 960×600 | Canal entry | 15,605 | 8,468 | 45.7% |
| 960×600 | Canal middle | 17,119 | 10,610 | 38.0% |
| 960×600 | Close harbor | 10,819 | 8,567 | 20.8% |
| 1333×600 | Canal entry | 19,177 | 10,132 | 47.2% |
| 1333×600 | Canal middle | 20,873 | 12,373 | 40.7% |
| 1333×600 | Close harbor | 12,483 | 9,745 | 21.9% |
| 1920×600 | Canal entry | 24,367 | 12,170 | 50.1% |
| 1920×600 | Canal middle | 26,727 | 14,339 | 46.4% |
| 1920×600 | Close harbor | 15,113 | 10,955 | 27.5% |

The final 1333-wide entry sends 2,142 circles, with only ten completely outside
the nominal viewport; conservative antialias margins account for the remaining
edge calls. Shared viewport rejection, per-emitter visibility checks, cheaper
distant glow and secondary smoke lobes, and ground/road culling provide the
reduction. The final compact smoke keeps all eighteen primary puffs so the plume
remains continuous; only alternate secondary lobes are retained. Near-view
effects retain their full detail. Cached glyph decoding saves repeated parsing
without changing glyph geometry or command counts.

The separate shared redraw gate avoids submitting an unchanged visual tick on
high-refresh hosts, while dirty input and state transitions still request a
draw. The command counts above are per rendered frame and do not include that
additional scheduling benefit.

## Visual checks

Nine identical tutorial states were captured with the production shared renderer
at 960, 1333 and 1920 widths. Every close-harbor image is pixel-identical to its
baseline. Wide images differ only slightly: mean channel error is 0.20–0.49 on
the 0–255 scale, with fewer than 0.08% of pixels having a summed RGB difference
greater than 60. A changed-pixel count alone is misleading because faint glow
changes affect broad areas by one or two color levels.

The first compact-smoke experiment separated the plume into conspicuous dots.
That version was rejected during visual review. The final eighteen-primary-puff
version was reviewed at all three widths and retains continuous industrial haze.
Factory density, source resolution, light/stack attachment points, water,
foreground fading and teaching UI remain intact. Ground/road-only comparisons
are pixel-identical across all nine states.

The full comparison is generated under `out/scene-render-before` and
`out/scene-render-after`. Selected final reviews were copied to Downloads as
`BargeBoss-wide-performance-before-after-v1.png` and
`BargeBoss-wide-performance-canal-v1.png`.

## Reproduction and guardrails

- `tools/ScenePerformanceBenchmark.java` exposes reusable real tutorial fixtures,
  per-component command counters, warmed shared-code timing, and optional
  Java2D raster timing. Default mode counts; argument `raster` measures Java2D.
- `tools/SceneRenderReview.java` exports identical-state captures and image
  difference statistics. Compile it against each frozen implementation to compare.
- `RenderBudgetTest` checks three scenes at all three widths using deterministic
  command, circle, offscreen-circle and ground-sprite budgets. Minimum factory
  and effect counts prevent deleting the scene to satisfy the budgets. Its sixty
  assertions pass on the optimized implementation; the preserved baseline fails
  immediately at the 960-wide entry's 15,605 calls.
- Raw counters, raster diagnostics and image difference statistics are retained
  beside this document. Timings are intentionally not CI assertions.

Java2D warm raster timing at 1333-wide entry fell from roughly 90.6 to 70.1 ms,
and canal-middle from 76.9 to 65.8 ms in these diagnostic runs. These are software
raster timings on the Termux JVM, **not Android frame rates**. JIT, CPU frequency,
GC and unrelated device load affect them; command-count reductions are the
repeatable result. First-raster numbers include newly decoded assets and must not
be compared as proof of a decoding improvement. Final Android installation and
controlled on-device frame measurements belong to the main validation workflow.
