# Chas / Rumrunner 2 portrait

Source likeness: user-provided phone screenshot `Screenshot_20261123-013016.png`.
Style reference: accepted Gavin portrait. Built-in imagegen produced six transparent
head-and-shoulder frames: neutral, small speech, wide speech, sheepish smile,
blink, small speech. Prompt preserves backward charcoal cap, dark sunglasses,
full beard and colorful abstract-print shirt. Right-facing source is mirrored by
the shared radio panel to face inward. No boat/background or detached effects.

Canonical source: `docs/art-studies/chas/chas-source-v1.png` (1536×1024).
Runtime: `assets/sprites/portrait-chas-v1.png` (576×384, six192px cells).
`PackGavinPortrait` now accepts source/output arguments and uses one uniform
scale for all frames, registering shoulders at a shared baseline. No per-frame
stretch. All six measured bottom anchors are95.5,182. The registration review
verifies identical displayed anchors; glasses conceal the blink in normal use.

Radio exchanges identify Chas separately from Gavin. Idle portrait remains visible
through answers; talking frames follow the shared radio phase. No new native
rendering or audio pathway.
