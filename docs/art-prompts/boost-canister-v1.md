# Painted boost canister v1

Supersedes the rejected holographic lightning/frame design in
`boost-energy-pickup-v1.md`. The user approved compact painted canisters, warm
highlights and foam spirals to fit the harbor's existing painted material style.

Generated with the built-in image-generation tool, transparent background:

> Create ONE production game sprite on a truly transparent background, no ground
> or shadow. A compact floating BOOST FUEL CANISTER for a hand-painted isometric
> harbor tugboat game at dusk. Exactly one object centered with transparent
> margins. Three-quarter view looking downward about 30 degrees, see top and
> front-right side, vertical upright. A squat cylindrical industrial pressure
> canister, warm weathered ochre-gold enamel with dark navy steel protective rims,
> two chunky dark side handles, small brass valve on top, recessed seams, chipped
> paint and restrained worn edge highlights. Broad readable masses, painterly
> maritime miniature texture and realistic dimensional shading, soft cool ambient
> fill and warm upper-left key. Small OFF-WHITE painted double-chevron speed emblem
> on the FRONT, integrated into worn paint. About twice as tall as wide; short top
> valve, no mast or navigation beacon. Dark heavy round flotation collar partially
> hiding the bottom. No letters, floating symbols, energy frame, neon, aura, ring,
> sparks, water or background. Full object visible, clean alpha silhouette.
> Finely illustrated game prop, not flat vector art or shiny science fiction.

Canonical source: `sources/boost-canister-v1.png` (1024x1536 RGBA). Runtime export:
`assets/sprites/boost-canister-v1.png`, 192x288, 216 KiB decoded. Rebuild with:

```
java -Djava.awt.headless=true tools/InstallBoostCanister.java docs/art-prompts/sources/boost-canister-v1.png
```

Shared registration: one fixed perspective, pivot (0.5, 0.95), 16x24 world units.
Only wave heave/roll moves the prop; no inferred directional frames. This is a
collectible prop, not a navigable watercraft. Painted chevrons stay on its body.
A small wet collar seam, local contact ripple and ten low-opacity foam stamps
form broken slow spirals. No luminous ring, overhead glyph, recharge HUD or
holographic frame. Collection fades/lifts the prop briefly with five small warm
sparkles and one expanding water ripple. Spent pickups disappear until re-armed.
