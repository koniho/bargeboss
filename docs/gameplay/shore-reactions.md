# Shore spectators and hose reactions

The finale's five hard-hat spectators are shared 3D meshes, not sprite rotations.
They stand at (440,-1090), (440,-1215), (440,-1340), (160,-1150), (160,-1450).
They use small idle gestures until fireworks launch, cheer during the display,
and point/flail toward the remaining fire while either pile burns. Relief restores
cheering. All movement is derived from fixed simulation ticks, never draw timing.

DockWorkers applies shoulder rotations, hip/leg swings and torso tilt to shared
vertices. Existing workshop crews keep their benches fixed while the person
reacts. Current foot positions drive depth sorting and screen-space hose aiming.
Water droplets must physically reach a worker's body height and footprint before
a reaction starts; merely aiming over someone does not trigger it.

A hit starts an 18-tick recoil, a bounded retreat, then recovery. Maximum retreat
is 34 world units. The full segment must remain dry and clear of tutorial timber
and factories so the return path is also safe. Repeat water hits may retrigger the
reaction after its initial cooldown. Native hosts do not own poses or reactions.

FireSteam independently tracks contact vapor for each pile: up to one puff per
four ticks, 48 total slots, 70-tick lifetime. Existing puffs rise/drift after the
hose moves away. Zero heat rejects further hits. Final extinction has a separate
three-second steam tail. SteamPuff is a pale prewarmed 40-square static layer;
never use the soot-only SmokePuff fallback for pale steam.
