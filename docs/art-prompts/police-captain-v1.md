# M3 police captain radio portrait, v1

Generated with the built-in imagegen tool, using the existing Gavin sheet only
as a painting style and six-cell layout reference. Canonical source:
`docs/art-studies/police-captain/captain-source-v1.png`. Runtime sheet:
`assets/sprites/portrait-police-captain-v1.png`.

Prompt: Create a new production game radio portrait animation sheet for the
Chicago Police M3 patrol-boat captain. Match polished painted arcade comic style,
bold ink contours, cream/gold edge highlights, navy shadows and an expressive
face. A capable middle-aged Black woman marine police captain, short natural
hair under a navy police peaked cap with a small gold shield badge, navy marine
police uniform and life vest, shoulder radio and radio headset. Calm, firm,
slightly wry expression. Head and shoulders facing right in three-quarter
profile. Six frames in a 3 columns x 2 rows grid: neutral mouth closed, small
speech mouth, open speech mouth, slight satisfied smile, blink, small speech
mouth. Fixed shoulder/head/cap/headset placement, only lips and eyelids change.
Transparent gutters; no scenery, panels, labels, typography or glow.

Follow-up: background extraction only, preserve every portrait and expression,
remove the gray backdrop with fully transparent alpha. The six separate alpha
components were verified by the existing `PackGavinPortrait` packer. It uses a
single uniform scale and shoulder registration, not per-frame stretching.
The portrait registration checker includes the new captain's half-pixel anchors.

Repack: compile tools/PackGavinPortrait.java and invoke with the source and
runtime filenames above. Use `-Djava.awt.headless=true` on the host.

The captain has a distinct shared radio transmitter identity. Real tow
connections enqueue one call per speeder; actual impound intake enqueues one
handover acknowledgement. Calls are nonmodal, queued and deduplicated. Final
job dispatch waits until the acknowledgements finish, as the existing rescue
radio does. Audio uses the existing CB transmission cue, not generated voice.
