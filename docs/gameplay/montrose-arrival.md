# Montrose arrival and mooring activity

Normal location entry starts south of the inlet, source-plan (453,1500), heading
north (-world Y). Over360 fixed60-Hz ticks the tug advances to (453,1240), while
the camera pans north from source-planY1540 to1190. Both ease through one shared
phase. Input and objective clocks wait; water, smoke and moored-fleet motion
continue. Pause stops the sequence and resize preserves its phase. The tug's
velocity and engine load reach zero for the radio hold, avoiding stale wakes.

The first reception is Gavin: “WELCOME TO MONTROSE HARBOR, CAPTAIN!” After the
player replies, he dispatches the haulout job. The tug remains at the inlet;
there is no teleport to the travel lift. Later jobs retain their contiguous
location flow. Developer mission selection bypasses the arrival. Profiling
scenario `montrose-arrival` exercises it; existing work-area fixtures clear it.
Gavin uses the shared portrait/radio panel and level-scoped portrait preparation.

Three additional sailboats use existing western-field mooring balls1,9,17
(zero-based mooring IDs, distinct from entrance markers). Each has a bow pennant
to its moving ball. They remain normal solid moored boats. Only the balls yield
without pushing back on vessels: collision never changes the tug's position,
linear velocity or angular velocity. They dip, displace and spring back with
30 units of slack. Standard channel/navigation floats retain physical response.

Moored boats amplify propagated wake orbital force3x, while keeping the ambient
sea unchanged. Wake heave response is1.35 rather than0.55. Tethers limit movement
to4 units and yaw to0.10 radians; tighter berths retain shoreline clearance.
Ball contact splash and displacement remain simulation-owned; render calls
never advance motion or use wall-clock randomness.

Tests: MontroseArrivalTest, MooringActivityTest, MooredBoatsTest,
MontroseBuoyContactTest. Review exports: tools/MontroseArrivalReview.java.

## Beach trip and Chas (2026-11-23)

Sequence: haulout → beach exploration → raft rescue → yacht tow → swimmers.
Gavin's exploration invitation follows the completed lift cycle. Guidance uses
harbor fairway/inlet/offshore waypoints; the emergency is position-triggered east
of the headland (survey x>2250,y<550), not a timeout at the lift. No teleport.
The crew transmission introduces Chas aboard Rumrunner 2 with Never Wetter rafted
alongside, three kids aboard, weather turning and both batteries drained by music.
Player replies include snark or reassurance, followed by a practical rescue plan.
Reply transmissions finish before the crew responds. Existing legacy mission IDs
and rewards stay unchanged; delivered boats retain their actor slots.

The raft is outside/past the Wilson Avenue light/horn at survey2350,-1530. Shared
pointer bounds now follow the full playable boundary, including the beach extension;
legacy hardcoded bounds silently prevented lasso/taps north of the old marina.
Developer mission selection begins directly with Chas's call. Normal arrival still
opens with Gavin's welcome. No unrequested weather preset override is applied.

Single-point sailboat moorings constrain the bow to a20-unit slack radius around
the moving ball, applying tension torque and damping. Unlike two-line dock berths,
they can swing through full headings. Ball contacts remain one-way; vessel hulls
remain solid. Fixed-step force/torque and heading-selected sprites are shared.

## Ain't Broke and weather progression

The opening tow is the large red offshore racer Ain't Broke at star dock S, the
star center farthest from the N-M haulout (computed from surveyed centers). Its
southwest-facing berth is reserved from ambient fleet population. The larger
capsule and physical pickup clear the dock fingers. Push assist lines up outside
the narrowed well; hull-projected reach supports side pushes. It must fit fully
before near-target settling starts, preventing the larger bow from stopping short.
Boats already secured to the operating lift cannot accept new tow links.

Every Montrose entry starts at dusk. After the lift finishes, a30-second background
nightfall grade deepens the water and shore before vessels and local lights draw.
This is a continuous scene-composition grade, not a switch between native bitmap
lighting caches or a moving astronomical sun. Vessel colors and UI stay readable.
No sky is visible in the gameplay camera; title art is unchanged.

Chas's distress call introduces light rain and haze over10 seconds. Accumulating
60 seconds of moving, connected rescue tow ramps rain from0.35 to3 (heavy), fog
from1 to2 and sea-wave amplitude from1× to2×. Waves use the same multiplier for
rendering, heave and orbital forces. Conditions hold when stopped/unhooked and
remain through later jobs; reset clears mission progression. Pause freezes the
clock. Developer weather/time controls explicitly override automatic behavior.
Rain/fog retain existing fixed submission caps and use no new weather textures.
