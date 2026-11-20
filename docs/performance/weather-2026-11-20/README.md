# Rain and lightning experiment

Montrose has session-only weather controls in WAVES + WEATHER. Rain offers
OFF/LIGHT/STEADY/HEAVY; lightning offers OFF/LOW/HIGH, plus TEST FLASH and RESET
ALL. Existing wave power and direction remain independently adjustable. Defaults
are clear; RESET ALL resets waves and weather. Panel taps and cancelled touches
are consumed without steering.

Shared WeatherView adds a cool storm tint, slanted rain in three apparent depth
ranges, and diffuse lightning illumination. It draws after scenery/vessels/smoke
and before guidance/HUD/controls. Lightning is a restrained single 24-tick pulse;
LOW repeats every12 seconds, HIGH every7 seconds. This first experiment is visual:
no rain-driven physics, thunder audio, extra shadow passes or new wet-material maps.

Cost is independent of world area and camera zoom: at most192 streaks and two
translucent full-viewport rectangles. Rain positions derive from simulation ticks
and fixed seed tables, with no mutable particle simulation, runtime raster layers
or new textures/caches. Off emits no primitives. Time freezes with gameplay.

Validation:4,294 weather budget/determinism/input/flash assertions pass, full
Android build/checks and native encoder regressions pass. JVM/J2ObjC translation
parity remains38137f35:2:1350. The planned Android comparison uses the inlet fixture at
zoom0.65,120 warmup/360 measured frames, clear/heavy-plus-peak-flash/clear order.
This is a player-reachable camera, not the full-stage developer stress view.


The checked APK installed successfully, but the phone was locked when profiling
launched. Window policy reported showing=true/inputRestricted=true. The runs
were stopped and provide no valid performance samples. Native visual, animation
and performance verification remains pending an unlock; no FPS claim is made.
The user was asked to unlock the device. No keyguard setting was changed.

Actual shared-renderer rain/panel and peak-lightning frames were exported and
visually reviewed using tools/WeatherReview.java, to out/weather-rain.png and
out/weather-lightning.png. Labels fit, the panel ends above the steering control,
and the HUD remains readable. Desktop pixels do not prove native antialiasing.
