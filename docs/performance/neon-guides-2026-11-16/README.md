# Neon guide validation — 2026-11-16

Full shared checks, 1,439,937 shoreline pixel assertions, 197,334 foam/splash
assertions and JVM/J2ObjC parity `a13d1f49:2:1350` pass. Android and Metal builds
succeed. Final Metal screenshot reviewed: `out/neon-cues-metal-final.png` (ignored).

Android was installed with the retained signing key and launched. Device activity
switched away from Barge Boss during the replay. The resulting CSV has zero sample
frames and is rejected. No timing comparison or final Android pixel claim is made.
The previous lighting build's CSV must not be attributed to this guide revision.
Awaiting an uninterrupted device window for the final Montrose capture.
