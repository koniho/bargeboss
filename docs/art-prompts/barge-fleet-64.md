# Existing hull and cargo: 64 real headings

User requested the existing hull and cargo upgrade along with the specialized
barge work. Use the established industrial palette, open hold, yellow coaming,
rust sides, charcoal tires, six steel coils, three container bays and coal mound.
All fit the same hull. No smoke, wake, ground shadow or glow in source colors.

Generation mode: deterministic offline rigid geometry, not image generation.
`tools/BargePoseGuide.java` owns the model, `tools/BakeBargeFleet.java` exports
64 model rotations plus registered material maps. The existing isometric
industrial concept remains the style reference. `tools/bake-barge-fleet.sh`
regenerates sources, derivatives, clipping masks and runtime frame packs.
See the fleet README for dimensions, anchors, limitations and review outputs.
