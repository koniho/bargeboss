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
