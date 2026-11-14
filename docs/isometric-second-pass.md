# Isometric harbor, tutorial and effects pass

The tutorial now teaches the same ten steps in a rectilinear industrial canal
and harbor, using the new warehouse, crate, pipe-rack, tank and utility-shed
sprites. The seven ordered gates remain, but horizontal and vertical reaches
join at square bends. The collision and drawing code share the same canal
boundaries; native iOS and desktop replay drivers follow the same route. Existing
mass, thrust, damping, scoring and wake forces are unchanged.

The phone screenshot `Screenshot_20261114-004342.png` exposed a progression
bug: the tug was at the harbor while the HUD still required gate one. The old
23-unit center-radius check missed legitimate bank-side crossings. Gates now
span the full canal width, include swept forward crossing detection, and draw
their dashed markers across the waterway. Both banks are covered by regression
tests through the final transition into barge practice; these tests fail with
the old center-radius check. Gate ordering and solid-bank collision remain.
Every cleared gate flashes and sends mint/gold ripples and particles across the
water, clipped to the banks. A rising confirmation label marks the crossing;
the final gate retains a canal-complete banner after the harbor transition.
The 90-tick effect pauses with gameplay, never retriggers at an already cleared
gate, and resets with the lesson. Rendering never advances its timer.

A committed cargo push owns its touch until release or cancellation. Both the
move-time tow check and the release-time fallback respect that commitment. This
fixes a nearby/moving tug unexpectedly turning a push into a towline throw. A
fresh touch can still start a tow or mooring lasso. Tests reproduce the previous
failure and cover a tug moving under a stationary steering finger.

The approved tug now has a matching 32-view barge and separate black coal layer.
Both use one uniform scale per sheet and registered waterline pivots. Cargo
layers draw by projected depth while retaining their unloading identities.
The coal layer fades in place during unloading to preserve its coaming mask;
crane buckets carry dark coal as well. Steel/container payloads and moving shore
machinery retain transitional art.

Water has world-anchored, irregular moving crests, broken highlights, warm lamp
reflections and wake-driven shore ripples. Shared polygon clipping keeps waves,
reflections, persistent wakes and thick foam strokes inside actual water,
including rectilinear corners and solid piers. The clipping regions do not
overlap, avoiding brighter seams. Renderer changes do not advance the physical
wave field or alter its forces.

Scenery lamps and windows use several slow frequencies, soft layered falloff
and offset glow lobes. Separate chimney sprites have measured open-rim outlets
and lamp points; smoke expands and drifts from those outlets, fading with scenery
occlusion. Closed pipes on the other props emit no smoke. Watercraft smoke emits
three times the previous number of puffs: 15 at idle through 60 at full engine
load, retaining its registered stack position and engine-load response.

## Source artwork and review

All new raster artwork was created with the built-in image generation tool.
Source atlases, prompts and anchor metadata are tracked; review copies are also
in Downloads. No generated source pixels were edited by scripts.

- Barge and coal: [generation and registration review](art-studies/isometric-barge-runtime-review.md).
- Harbor/tutorial props: [review and anchor inventory](art-studies/tutorial-scenery/review.md).
- Chimneys: [effects review and emitter inventory](art-studies/scenery-effects/review.md).
- Guides and reusable tooling: [sprite-sheet learnings](art-studies/sprite-sheet-learnings.md).

Guide-derived barge fender and rope points remain explicitly unverified estimates.
Registration quality and clear atlas gutters do not prove every generated fitting
is geometrically identical across headings.

## Verification

The merged suites cover push gesture ownership, the complete rectilinear course,
registered barge/coal alignment during unloading, cargo depth order, exact water
clipping, deterministic wake rendering, and scenery light/smoke anchors and
opacity. Shared-renderer previews cover every tutorial lesson, all control states,
all 32 vessel headings, and working harbor scenes. Native validation is reported
separately from renderer/logic checks; the preceding commit's fully green iOS run
does not certify this new revision.

Final local checks: JVM and Android ART both pass with replay signature
`7e4242ca:11:8974`. The channel suite includes 31,454 assertions; the actual
shared-renderer replay completes all ten tutorial lessons in 3,002 ticks.
Desktop packaging and persistence checks pass, and the Android APK is signed
and verified. Updated gate-clear and canal-complete screenshots are exported
to Downloads alongside the tutorial contact sheet. Native iOS and hosted
desktop checks must still run for the pushed revision.

Level authoring remains code-based. The new [level design guide](level-design.md)
and [blank planning template](level-design/sketch-template.svg) let a designer
describe navigation, feeder slots, berths and challenge rules before a visual
level editor and shared loadable format are implemented.
