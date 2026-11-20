# Ripple floor and Vulkan clip reuse

Power 0 now matches the prior ambient surface appearance instead of reducing
visible detail: 1,728,000 exact Java2D pixels across ticks 0, 45, 90 against the old
WaveView. Sheltered water has no ambient swell, but retains these quiet ripples.

The first full-density wide-view capture exposed repeated float-array cloning
in Vulkan clip push, mostly for moored boats. A separate sampled trace assigned
about 50% of main-thread CPU to cloning; ripple drawing itself was about 5.6%.
The encoder now records each mask's vertices once and reuses the captured range
on pop. It snapshots caller geometry through those vertices, with fixed 64-level
range storage. Nested clips, empty clips, underflow, source mutation and range
reuse pass tools/VulkanClipEncodingTest.java. No shader or stencil-rule change.

Native frozen-scene screenshots before/after were exactly equal across 2,602,800
pixels. Shared/native parity on the audio-rebased source: 3c9b8743:2:1350.
Full Android build/checks passed. Device uses the existing debug signing key.

Wide Montrose replay, zoom .65, 120 warmup / 180 measured frames:

| Capture | Median CPU draw ms | Median presented ms | p95 presented ms | Mean presented ms |
| --- | ---: | ---: | ---: | ---: |
| Restored floor, old clip encoder | 34.75 | 41.62 | 41.66 | 39.54 |
| Restored floor, reused clip vertices | 17.12 | 16.67 | 25.00 | 20.17 |

These short sequential captures do not control thermals or establish locked
60 FPS. The final build also includes origin/main's licensed audio integration.
Raw trace/captures remain untracked under out/profiles/. No physical iOS-device
validation. The comparison fixture and images remain under out/ripple-floor-*.

Host encoder check after building Android (adjust SDK path if needed):

```sh
javac -cp build/android/classes:$ANDROID_HOME/platforms/android-36/android.jar -d out/ripple-android-classes tools/VulkanClipEncodingTest.java
java -cp out/ripple-android-classes:build/android/classes:$ANDROID_HOME/platforms/android-36/android.jar com.bargeboss.game.VulkanClipEncodingTest
```
