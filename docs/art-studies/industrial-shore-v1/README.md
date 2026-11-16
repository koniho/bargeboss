# Industrial shore source art

User-supplied references: `tileset-reference.png` (primary) and
`scene-reference.png` (secondary). Target: painted industrial steelworks,
rust-streaked steel, worn concrete, chipped yellow rails and dense machinery.

New source sheets live under `assets/sprites/shore-*-v1.png`, with exact prompts,
cell measurements and limitations in `docs/art-prompts/`. The steelworks factory
has its own source PNG and prompt. These are new versioned authoring assets, not
runtime replacements. Scene registration, split foreground occlusion surfaces,
artwork scale, mip/filter gutters and seams require shared-renderer integration
and review before enabling them in levels. The Android phone was not used.

## Bridge and lock material direction

Keep the existing bridge/lock form, shared rigid parts, pivots and animation.
`BridgeModel` already builds trusses, towers and rails; `LockView` builds gate
slabs and stiffeners, with angles supplied by `LockDoors`. Their flat appearance
comes in part from `ArticulatedModel.Poly.paint`, which fills each face with one
shaded RGB. More detailed painted sprites alone must not replace moving parts.

A surface material shader can add reference-style oxidized red steel, rust at
joints, flaked safety-yellow paint, rivet rows and greasy waterline stains. It
needs a new polygon primitive with stable part-local UVs, material ID and seed.
Keep local coordinates through BSP splits, transform positions and normals in
shared Java, and shade only inside each existing face. The current
`Painter.material` handles bounded effect patches; it does not yet supply these
surface coordinates or arbitrary polygon materials.

Use an opaque material result in place of the face fill, preserving ordering and
shore cutouts. Do not add unmasked fullscreen rust overlays, camera-space noise
that slides over moving gates, or rotating painted bridge bitmaps. Emission,
lamps and smoke remain separate shared anchored effects. New furnace artwork
supplies the large factory silhouette and structural detail; shaders complement
it with local light and grime, rather than inventing missing geometry.

This material pass is a documented next implementation, not shipped code in this
asset-creation round. Validate closed, opening and open bridge/lock states,
all-heading shore occlusion, native pixels and repeated frame pacing before
claiming 60 FPS headroom.

## Source catalog

- [Bollards and mooring fittings](../../../assets/sprites/shore-bollards-v1.png): six variants.
- [Static loading equipment](../../../assets/sprites/shore-loading-v1.png): hopper, conveyor, coil cradle and platform.
- [Dock trucks](../../../assets/sprites/shore-trucks-v1.png): empty/loaded opposing views.
- [Shore modules](../../../assets/sprites/shore-tiles-v1.png): six concrete/steel modules with rail, pipe and ladder variants.
- [Large steelworks factory](../../../assets/sprites/steelworks-factory-v1.png): furnace, stacks, catwalks, pipework and brick buildings.
