# Moored geometric normals and collision spray

Pixel 11 Pro, identical `montrose-docks` replay, 60 warmup + 300 measured frames,
measured simulation ticks 2596–2895. Baseline is the preceding floating-NPC build.
Sources and frame timings are summarized alongside this file; full raw captures
remain in ignored `out/android-profile/normal-*` directories.

The first attempted after capture still used flat RGB lighting because
`SceneryOcclusion` had not forwarded the new primitive. It is deliberately excluded
from the final comparison. `NormalBoatReview` now verifies a lit, visible NPC in
the full scene reaches `normalSprite`, in addition to checking 192 exact alpha
registrations and 34,554 unit normals on face interiors. Antialiased face seams
are blended vectors and are renormalized by shaders.

Native Metal screenshot review uses the simulator, not an iOS-device performance
measurement. Android older than API 33 and the Core Graphics reference fallback
retain flat RGB reception. Geometric normals do not provide per-pixel positional
lighting, cast shadows, specular response or full 3D occlusion. One direction per
color is aggregated at the boat center, using an effective 12-unit source elevation.
Opposite same-color lights can cancel the directional component.

Collision spray is no longer propwash: shared foam modes 8/9 implement tapered
liquid fingers and a broken outward foam crown. Three detached droplets per packet
follow ballistic motion, with existing heavy-hit sparks preserved. Pools, lifetimes,
collision impulses and shore/depth clipping are unchanged. Native AGSL and Metal
compile the same foam kernel; desktop/older hosts use bounded geometry fallbacks.
The Montrose timing run is not a maximum-simultaneous-impact stress test.

| Capture | Draw p50/p95 ms | Interval p50/p95 ms | Effective FPS |
| --- | --- | --- | --- |
| Baseline | 12.265 / 15.378 | 19.136 / 34.483 | 43.24 |
| Normals | 12.809 / 16.100 | 19.948 / 42.735 | 41.83 |
| Normals repeat | 12.754 / 15.990 | 19.597 / 35.642 | 42.69 |

The repeated draw median adds about 0.5 ms. Interval tails vary, so these short
runs do not establish a precise GPU cost. None meets the 16.7 ms frame target.
Android frame storage rose 96.521→108.521 MiB, decoded source storage
102.524→114.524 MiB; no measured-frame frame-texture builds or evictions occurred.
