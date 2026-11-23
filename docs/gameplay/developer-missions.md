# Developer mission picker

In a development build, tap **DEV PANEL** at the top left during play.
The **Missions** tab lists the selected location; **Weather / Time** contains
wave strength/direction, rain, lightning, fog, time of day, test flash and reset.
Only missions in the currently selected location are listed.
Select a different location through the normal title screen location selector.
During training, the Missions tab offers only a tutorial restart. Close or Back dismisses
the picker. Opening it cancels held controls and suspends simulation.

Android enables the shared capability only for debuggable APKs (currently built
with `BARGEBOSS_PROFILE=1`). iOS uses `BB_IOS_PROFILE`; desktop uses
`-Dbargeboss.dev=true`. Ordinary release sessions have neither a button nor
permission to start developer missions. Profiling fixtures still ignore touch;
launch normally to use the interactive picker.

Mission labels use current contiguous Calumet objectives, rather than obsolete
standalone challenge descriptions. Calumet jobs spawn through the production
handoff logic; the bridge convoy gets empty exchange hulls. The tug starts on a
nearby river route point. Montrose initializes the exact haulout, yacht tow,
cruiser rescue or swimmer rescue mission. Indiana initializes its steel tow.
The mission continues normally from there.

Tests run in a disposable child Game with its own career. Launching or completing
them does not award stars or unlocks to the saved career. Settings remain shared.
Returning to title discards the test session. Native hosts only enable the
capability: layout, catalog, input routing and mission setup live in core/.

The title screen has no developer panel, separate Learn to Drive button or time
selector. Select Training Harbor and Set Sail for the full driving tutorial.
