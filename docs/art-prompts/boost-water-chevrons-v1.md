# Water boost chevrons

User direction: "just make the boosts chevrons that are water color themed and
highlight in glowing cyan when triggered". Supersedes both the holographic
lightning design and painted canister design.

Three muted blue-green chevrons lie on the water, aligned to the local boost
route. On collection the same shapes glow cyan and fade across the existing
40-tick feedback window. Spent pickups remain hidden until gameplay re-arms them.
No upright prop, surrounding ring, foam spiral, floating glyph or new texture.

Shared Java draws three polygons at rest, with eighteen bounded highlight strokes
only during collection. Water depth/shore cutouts are preserved. Speed, pickup
radius, cooldown, scoring and combo timing are unchanged. The canister runtime
texture, manifest entry and authoring tool are removed; archived source art is
retained only as design history.
