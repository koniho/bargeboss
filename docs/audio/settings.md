# Audio settings

Settings has independent Sound and Music sliders (0–100%) and mute buttons.
Muting preserves the selected level. Adjusting a muted slider keeps it muted;
unmuting restores that level. Zero volume is silent without changing the mute flag.
Default levels are 100%, unmuted, preserving the existing mix.

Sound includes every current loop and one-shot: diesel idle/work, fire, water
sizzle, radio, contact, splash, fireworks and tutorial completion. Shared Java
applies the preference after existing ducking/mixing, without changing pitch,
simulation or haptics. Android, iOS and desktop consume the shared gains; native
hosts update playing effects as well as new events. Muted events are consumed
without replaying them later.

Music plays “A Long Way” by SergePavkinMusic across gameplay and menus, with
native looping and an independent shared gain. It pauses on music mute or host
background/focus loss and resumes from its current position. Audible radio calls
duck the music; muting Sound leaves it unchanged. See ambient-music-v1.md for
permission, preparation and host buffering details.

Game.saveProgress() now writes BBG2 with the career, joystick preference and audio
preferences. BBG1 and bare career saves remain readable with full-volume defaults.
Malformed audio values reject the entire snapshot before career/settings mutation.
Practice shares preferences with the parent game. Hosts retain opaque persistence
and watch progressVersion(), including slider movement and mute changes.

AudioSettingsTest covers migration, atomic rejection, remembered mute levels,
loop/one-shot scaling, practice, pointer ownership/cancellation, and slider input
at 960x600, 1333x600 and 960x720. The canonical native/JVM replay exercises both
channels and their save/restore round trip. iOS simulator review confirmed sliders,
mutes, legibility and persistence after relaunch. Physical loudness is not measured
by these checks.
