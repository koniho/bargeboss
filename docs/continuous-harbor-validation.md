# Continuous harbor and articulated bridge validation

This round joins the ten tutorial lessons into one navigable world, installs
sixteen registered factory modules and a raised textured yard with corrugated
steel retaining walls, aligns the thrown lasso with its preview and deck edge,
and flashes the tug when hull damage is actually charged. The bridge uses a
shared rigid XYZ hierarchy with attached lighting fixtures.

## Reviewed outputs

- [Factory inventory, measured foundations and emitter review](art-studies/factory-v1/review.md)
- [Ground texture and quay study](art-studies/ground-v1/)
- [Continuous tutorial behavior and coordinate contract](continuous-tutorial.md)
- [Rigid bridge implementation and anchors](bridge-mechanism.md)
- [Articulated bridge contact sheet](art-studies/bridge-rig-v1/BargeBoss-articulated-bridge-v1-contact.png)

Production source images are preserved unchanged; review copies were also saved
to the phone's Downloads folder. Actual shared-renderer previews cover the canal,
harbor entry, unloading, towing, lasso flight/recoil, hull flash and bridge poses.
The continuous tutorial preview completes all ten lessons in 3,291 simulation
ticks at both 960×600 and 1333×600.

## Native UI regression

The preceding revision's GitHub run 43568520822 passed shared JVM/J2ObjC parity
but failed the iOS interactive tutorial test in the drag-to-push lesson. Its
half-second pushes, separated by slow simulator automation, repeatedly lost the
contact needed to finish. The updated driver holds the drag for three seconds,
uses the continuous canal/harbor route and approaches the existing second barge.
Shared replay checks cover automation pauses of three, five and eight seconds.
This reproduces and addresses the driver failure; a fresh native CI result is
still required to establish the current iOS UI outcome.

## Final local results

- `./check-art.sh`: complete shared suite passed on JVM and Android ART;
  both produced `d7f2d0a5:11:8974`.
- Factory source/registration/emitter pixel checks: 87,313 passed.
- Continuous tutorial: 125,383 assertions passed; raised ground: 91,927;
  placements: 596; lasso/damage feedback: 2,154.
- Rigid bridge: 277,287 assertions passed. The separate 121-pose benchmark
  measured a maximum of 1,705 BSP splits and 2,131 painted polygons.
- `./preview.sh`: shared renderer exports and desktop sprite-pixel checks passed.
- `./desktop.sh --build-only`: packaged the desktop build; persistence checks
  passed, including the intentional failed-save isolation case.
- Android APK built, signed and installed through Shizuku (`Success`). Activity
  launch returned `Status: ok`. The phone was screen-off/locked, so this confirms
  installation and launch, not an interactive native visual or memory review.

Native iOS testing runs in GitHub CI; it cannot be performed in this Termux
workspace. Shared parity does not establish identical native rasterization,
performance, lifecycle behavior or touch latency.
