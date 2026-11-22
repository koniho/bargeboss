# UI layout v2

Actual shared-renderer captures, 960×600. Replaces the palette-only pass with a
new title flow and differentiated component geometry. Training is separate from
voyage setup; harbor selection shows connected jobs and completed work. Settings
use rows, segmented hand selection and a haptics switch. Radio choices, mission
ribbons, instruction headers, map fader and throttle grip retain their own roles.

See [UI contract](../../art-direction/industrial-ui.md). Reproduce with
`tools/UiStyleReview.java`; output goes to `out/ui-layout-v2`. These captures are
review fixtures, not native-device screenshots. Wider 1333×600 fixtures are also
generated; menu input tests additionally cover 960×720.
