# Instructor radio portrait

User supplied captain_talking_preview.gif from Downloads, November 21, 2026.
Original preserved in assets/sprites/source/instructor/captain-talking.gif.
Six coalesced frames, authored delays 23/12/15/13/18/20 centiseconds.
Run sh tools/pack-instructor-portrait.sh to create the 3x2, 192px-cell atlas.
Preserve original canvas and white portrait matte; no generated replacement art.

SpeakerPortrait owns speaker gating, frame timing and screen-center facing.
The authored face points right; a negative shared affine X basis mirrors it
when its center is on the right half of the display. No host-specific animation.
It draws outside the instruction content wipe, without shifting the fixed panel.
The instructor portrait does not stand in for player or bridge transmissions.
Tutorial preparation includes the atlas; other locations do not load it.

Run tools/SpeakerPortraitReview.java with the desktop renderer and ProfileScenario
to verify identity gating/timing/facing and export 960/1333px gameplay views.
