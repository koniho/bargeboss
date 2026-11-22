# Stable radio portrait bases

Checked all 42 frames of the instructor plus six NPCs against the actual painted
base, not the cell center. The night bridge operator shifted 12/24 pixels between
columns; the dock foreman shifted up to10 pixels horizontally and3 vertically.
The instructor shifted up to5 pixels; the lock keeper had a1-pixel vertical shift.
Steel bridge, dispatcher and fire rescue bases were already stable.

`PortraitRegistration` records the midpoint of the occupied bottom nine rows and
the last occupied row (alpha>128), in each192px cell. `SpriteAtlas` derives per-frame
pivots relative to frame0. This keeps the accepted neutral pose/layout, preserves
all painted pixels, and works with either affine facing direction on every host.
Only expressions change; no runtime recentering or artwork resampling is needed.

All seven portraits are registered in SpriteAtlas, but the four unused NPCs remain
outside the upcoming-level prefetch lists. No additional portraits are loaded by
this change. Original PNG sheets are unchanged.

`tools/PortraitRegistrationReview.java` validates the source-derived base after
applying the shared pivot for every frame; check.sh runs it. It also exports the
corrected mirrored sheet. Compare [before](before.png) and [after](after.png).
Future portrait revisions must rerun this check and review at the actual100px UI
size, including the final-to-first frame transition. Do not anchor to a moving
mouth, hat tip or whole-silhouette centroid.
