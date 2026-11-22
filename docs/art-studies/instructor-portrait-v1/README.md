# Instructor radio portrait

User supplied captain_talking_preview.gif from Downloads, November 21, 2026.
Original preserved in assets/sprites/source/instructor/captain-talking.gif.
Six coalesced frames, authored delays 23/12/15/13/18/20 centiseconds.
Run sh tools/pack-instructor-portrait.sh to create the 3x2, 192px-cell atlas.
Preserve original canvas and white portrait matte; no generated replacement art.

SpeakerPortrait owns speaker gating, frame timing and screen-center facing.
The authored face points right; a negative shared affine X basis mirrors it
when its center is on the right half of the display. No host-specific animation.
It sits 18 units from the right screen edge, 16 below the instruction card,
always facing left toward screen center. It draws outside the instruction content
wipe, without shifting the fixed panel or obscuring its text.
The instructor portrait does not stand in for player or bridge transmissions.
Tutorial preparation includes the atlas; other locations do not load it.

Run tools/SpeakerPortraitReview.java with the desktop renderer and ProfileScenario
to verify identity gating/timing/facing and export 960/1333px gameplay views.

## Current layout and transparent art (v2)
Supersedes the outside-panel placement above. The tutorial panel is 544x124;
its unchanged 432-unit text region sits left of a 112-unit portrait region.
The instructor stays visible throughout instructor instructions/conversations,
including player replies, resting on frame zero between transmissions. Bridge
dialogues retain their own speaker identity and never show this instructor.

Background extracted with built-in imagegen, transparent_background=true, using
instructor-portrait-v1.png as the edit target. Prompt: "Remove ONLY the white
background to true transparent alpha. Preserve all six faces, mouth expressions,
palette, black outlines, exact registration, framing, size and 3 columns by 2
rows grid. Input is 576x384 with six 192x192 cells. Do not redesign, reposition,
enlarge, crop, add borders or add shadows. The six frames are an animation;
their positions must remain identical. No white matte or checkerboard baked
into output." Original output saved as assets/sprites/source/instructor/
captain-transparent.png; runtime downsample is assets/sprites/
instructor-portrait-v2.png. Reviewed against the dark in-game panel.
