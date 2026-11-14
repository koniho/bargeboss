# Raised industrial ground and corrugated steel quay

The interior surface uses a new transparent asphalt/concrete diamond generated
with the built-in image tool from `interior-guide.png` and the industrial concept
as a material reference. Source pixels are unchanged. Runtime source:
`assets/sprites/ground-industrial-v1.png`; prompt:
`docs/art-prompts/ground-v1/interior-v1.txt`.

The actual output is 1254 × 1254. Alpha>=128 bounds are [159,355,1094,898], with
zero canvas-edge pixels. Four measured edge angles are within0.22degrees of the
shared ±30degree ground axes. The registered pivot is (.499601275,.499202549),
and one uniform size121.389435 maps the diamond onto a64 ×64world footprint.
`ground-join-review.png` shows3 ×3copies over water-colored, dark and light
backgrounds. Broad wear repeats mildly; runtime texture opacity.48 keeps the
service yard quiet. Small residual alpha-edge irregularity is under a world unit;
source output has soft, mostly near-opaque interior alpha rather than uniform255.
No source thresholding, stretching or perspective warp was applied.

The terrain has one shared deck rise of18 display units. `TrainingGroundView`
subtracts the exact connected water rectangles from the raised land plane.
Wholly dry cells receive the bitmap; partial shore cells form a concrete apron.
A shared geometry layer extrudes the actual outer water boundary into corrugated
steel retaining faces, with folded ribs, rust streaks, wet dark bases, concrete
caps and fastening points. There are no walls at internal joins or across the
canal/harbor mouth. This is a composed ground/wall tileset, not a claim that the
interior PNG contains side walls or is a seamless shoreline module.

Visible wall faces and foreground caps use the same play-object opacity rule as
scenery. Factory foundations, deck fixtures, lighting/smoke anchors and raised
bollard cues share the deck height. Navigation and collision remain at the
original water-plane coordinates. The ground tile has no lamps, chimneys or
steam outlets; those anchor categories are explicitly empty in metadata.

`GroundGeometryGuide`, `GroundTextureReview`, and `GroundWallPreview` reproduce
the construction guide, source measurements/join review, and terrain-only
concave-corner/mouth/berth captures. Terrain-only captures deliberately omit
factories while their independent sprite generation is in progress.
