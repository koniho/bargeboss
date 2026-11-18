# Factory texture experiments

Pixel 11 Pro, Shared Slip frozen at zoom .65 (actual scale .7834479).
120 warmup,240 sample frames in baseline/candidate/candidate/baseline60-frame
blocks. Drop eight transition frames per block. Raw data and rejected prototype
patches remain under local out/factory-textures; summary.json retains statistics.

## Kept: empty-margin cropping and preparation

Scan alpha once during factory atlas preparation, retain every nonzero-alpha
pixel and two transparent border texels, and compensate destination bounds
relative to the authored rectangle. Sprite, affine and blended paths preserve
shared frame/pivot/light metadata. Factory imagery now joins the Calumet level
preparation manifest. Other levels keep their existing preparation families.

Replace each factory's runtime source with its crop; do not keep original and
crop together in production. Cancellation cannot publish a trimmed bitmap as an
unregistered source for a later preparation. Debug comparisons may retain original
images explicitly. All16 images are a fixed bounded set, smaller than before.
Existing160MiB frame and48MiB terrain cache budgets are unchanged.

Native source check:25,160,256 pixels, zero lost alpha or changed retained pixels.
Source storage100,641,024->95,896,812 bytes, saving4.524MiB. Normal reported source
storage227.170->222.645MiB. Warm frame cache106.614MiB, no builds or evictions.
Native scene comparison:2615/2602800 pixels differ, RGB MAE.003331/255, largest
channel difference155 at sparse nearest-sampling boundaries. Source pixels are
lossless, but floating-point destination compensation is not raster-identical.
Reviewed the native difference image for alignment and silhouette preservation.

Intervals67.60/69.86/69.75/70.88ms: no repeatable FPS gain claimed. This is a
memory/preparation improvement. First implementation incorrectly retained crop
copies in the pinned frame cache, exceeded160MiB and caused292 frame builds per
sample; rejected and replaced with source ownership above.

## Rejected candidates

- Half-resolution factory mip images, built off-thread and selected only when
  projected pixel dimensions fit: referenced storage162.37->93.76MiB, intervals
  69.17/64.44/68.48/67.76ms. No consistent benefit in both directions. Removed
  the image-quality tradeoff and optional extra22.85MiB storage from production.
- Retained native factory bitmap commands at full resolution: intervals
  69.69/70.48/71.21/71.44ms. No consistent benefit; removed the extra node cache.

These tests do not support increasing caches or treating Android's slow-bitmap-
upload counter as a diagnosis. The factory omission result still shows cost,
but texture size alone did not explain it. Native render-thread/presentation
work remains unresolved; no60FPS or GPU-residency claim.

Reproduce the retained lossless crop test with a profile APK:
`--es bb_scene shared-slip-wide --es bb_mode freeze --ef bb_camera_zoom 0.65
--ei bb_warmup 120 --ei bb_frames 240 --ez bb_factory_sweep true`.
Mask256 uses original diagnostic images. `--ez bb_factory_review true` runs the
native pixel test separately; production does not retain original padded images.
