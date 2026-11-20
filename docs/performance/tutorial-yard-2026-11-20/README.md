# Connected tutorial yard

The finite 256-unit scenery margin remains unchanged. Clipped industrial avenues
are now joined through dry service-road tiles. All 241 paved tiles belong to one
network. Workshops occupy alternating plots along road frontage, with consistent
module choices per district: 67 low/medium buildings, separated by service yards.
The overhead chart consumes the same road/building layout.

Equipment shuttles use 63 short road runs in both world directions, stopping at
the ends. At most eight visible vehicles draw. Vertical route sections exclude
intersections occupied by horizontal routes; parked-truck aprons are excluded.
Twenty-two workshop crews work at pallets and maintenance benches in separate dry
plots, alternating arm/tool work with inspection pauses. Their feet and props
stay outside traffic lanes and building footprints. They share the existing
world-depth worker merge. Everything uses shared simulation visual ticks.

Road links, placements and routes are generated once, in bounded arrays. No new
sprite assets, native texture cache or platform-specific gameplay was added.
Vehicle orientation changes in world geometry before projection, never by rotating
a painted isometric sprite. Existing dusk materials and compact lights remain.

Validation: full `check.sh`/Android APK build; 1,461,230 all-heading shoreline
pixel assertions; connectivity/frontage/whole-vehicle clearance checks; native/JVM
parity `fcdc7a6f:2:1350`; production desktop and iPhone simulator visual review.
`TutorialYardReview` exports a site plan and production frames into
`out/tutorial-yard`. Native close-up fixture: `--ios-scene=tutorial-yard`.

Recorded native run: iPhone 17 simulator, iOS 26.5, Metal, harbor-close replay,
120 warmup and 120 sampled frames. Mean interval 16.667 ms, median 16.666 ms,
p95 17.235 ms; median CPU draw 6.923 ms, p95 8.447 ms. Peak frame textures
80.5 MB, retained 139.4 MB, zero evictions and no pending preparations at end.
Preparation counters include warmup. The scene's normal tutorial camera resumes
during replay, so the initial `--zoom=0.8` is not a fixed zoom throughout.

This is a bounded warmed simulator sample, not a before/after device benchmark
or a sustained 60 FPS guarantee. The phone is reserved for another agent; it was
not operated or installed to. Native screenshots and generated previews remain
untracked under `out/`. Raw timing and cache counters accompany this note.
