# Montrose compact Vulkan geometry

Pixel11 Pro, Android17, instrumented Vulkan APK. Baseline is c943ea9:
full expanded overview at scale0.11134045,120 warmup and360 measured frames,
90.44/94.07ms mean (11.06/10.63FPS). All final timing runs used the same fixture,
mode, dimensions and sample counts, without sampling or builds running.

## Findings and implementation

A10-second simpleperf capture recorded6,033 samples with none lost. WaveView
accounted for62.7% of sampled process CPU including callees; native frame encoding
accounted for18.4%. These inclusive percentages overlap other reported entries.
The old frame carried506,727 vertices (32,430,528bytes); native GPU execution was
about3.8ms while command encoding took roughly16–18ms on its separate worker.
ART method sampling stalled and produced no usable trace; its timings are excluded.

- Flat polygon vertices now use position and full-precision premultiplied color:
  24bytes instead of64. A dedicated vertex shader produces the same outputs as
  the original flat-fill path. Textured, lit and retained geometry keeps the full
  format. Triangle order, color arithmetic, clipping, blend state and artwork
  are unchanged. Command starts now address floats, not fixed-stride vertices;
  both native bounds scans and draws handle the selected stride. No cache budget
  was raised. The32MiB packet limit remains.
- Vulkan declines shared polygon grouping which it otherwise expands back into
  individual fills. Its native compatible-command merging remains enabled.
- Wave rendering prepares direction/time/intensity constants once per draw and
  removes duplicate drift sine calls. A render-only square root uses Math's
  correctly-rounded intrinsic; physics retains StrictMath. Height/displacement
  match scalar physics bit-for-bit. No ripple density or intensity reduction.

Rejected experiments: local vertex cursors plus grouping removal measured89.67ms,
not a meaningful improvement by itself. Bulk clearing full vertex storage and
writing fewer triangle fields measured94.44ms and was removed. Prepared wave
constants/render math alone measured85.97ms before compact encoding was added.

## Final device results

| Scene | Mean interval | p95 interval | Notes |
| --- | ---: | ---: | --- |
| Full overview A |69.59ms|78.92ms|14.37FPS |
| Full overview B |75.16ms|83.07ms|13.30FPS |
| Beach |16.65ms|17.73ms|60.05FPS |
| Beacon |16.68ms|17.71ms|59.96FPS |
| Cold inlet replay |18.69ms|24.32ms|max515.93ms |
| Cold tutorial replay |20.87ms|25.04ms|max1360.28ms |

Overview mean interval is about22% lower averaged over the two repeats. This is
not a thermally controlled reverse-install A/B. Payload is17,514,888bytes per frame,
46% lower; this is data copied/submitted, not total process or GPU-resident memory.
Native GPU time was about3.17ms, command encoding14–15ms. No packets were replaced
in final overview captures. First completed draw including preparation took
6.20/6.87seconds versus7.62/7.70seconds previously. Loading remains conspicuous.

Both overview launches completed all480 frames on Vulkan. Beach, beacon, inlet
and tutorial also stayed on Vulkan. Inlet replay's360 tick/zoom pairs match the
baseline exactly. Replay results include cold preparation and advance one shared
step per completed draw; they are not steady live-gameplay FPS guarantees.

## Verification and limits

- Overview, beach and beacon screenshots each match their pre-change native image
  exactly:0 differing pixels of2,602,800. Native inlet/tutorial images reviewed.
- 10,368,000 actual shared-renderer ripple pixels match the earlier reference across
  powers0/1/3, three bearings and two ticks.100,000 render-length inputs include
  NaNs/infinities; prepared wave fields cover24 bearings and three powers.
- Full checks and APK build passed; JVM/J2ObjC parity stays da53558f:2:1350.
  All-heading shoreline regression passed2,434,414 assertions.
- VulkanEncodingTest validates2,526,477 normalized vertex fields/clip markers
  against the original encoder across buffer growth, frame reuse, mixed strides,
  nested clips and retained relocation. Existing clip mutation/depth-limit checks
  also pass. Both checks now run during Android builds and are never packaged.
- Raw images, CSVs and native logs remain in out/android-overview-review; simpleperf
  data is retained there too. Compact summaries and sampling attribution are here.

The full overview is still CPU-bound and far from60FPS. Analytic ripple evaluation
and generation of roughly60,000 ambient polygons remain substantial work. Cold
terrain preparation and camera-transition hitches remain open. No physical iOS
performance/visual claim is made from shared parity. Existing lawn seams remain.
