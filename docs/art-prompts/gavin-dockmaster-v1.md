# Gavin — Montrose dockmaster, v1

Generated with the built-in image-generation tool using
`assets/sprites/portrait-dock-foreman-v1.png` as style/grid reference. Canonical
transparent result: `docs/art-studies/gavin/gavin-source-v1.png`. Production
six-frame sheet: `assets/sprites/portrait-gavin-dockmaster-v1.png`.

Prompt: Create production game portrait sheet for Gavin, the Montrose Harbor
dockmaster. Use the supplied portrait sheet only as a style and grid reference.
Match bold ink outlines, warm cream/gold highlights, dark navy/burgundy shadows,
and polished hand-painted arcade comic style. Friendly weathered middle-aged
male marina dockmaster, short sandy hair, neat salt-and-pepper beard, navy harbor
cap with simple gold anchor, navy work jacket and radio headset. Head/shoulders
facing right in three-quarter profile. Six frames in a 3x2 grid: neutral closed
mouth, small speech mouth, open speech mouth, smiling closed mouth, blink, small
speech mouth. Fixed shoulders/head/cap/headset positions; only mouth/eyelids move.
Transparent background, no panel/text/labels/glow/scenery; generous padding.

Follow-up: background extraction only, preserve character/grid/expressions and
colors, remove backdrop with true transparent alpha. The resulting PNG has real
alpha (verified), not a painted checkerboard. Its six alpha components are packed
by `tools/PackGavinPortrait.java` using one uniform scale and shoulder registration.
No per-frame stretching. `PortraitRegistration` corrects the remaining half-pixel
base difference; the all-portrait registration check includes Gavin. He appears
on the right of the instruction panel, mirrored to face inward, and remains
visible through the welcome and later Montrose dispatch conversations.
