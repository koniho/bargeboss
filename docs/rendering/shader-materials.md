# Shader rendering and shared materials

The production iOS renderer uses Metal for every submitted graphic: sprites,
cached terrain and lettering use sampled textures; panels, polygons, lines and
circles use geometry/coverage shaders. Android hardware Canvas submits its
standard geometry and textures through Skia's GPU renderer. Custom local effect
kernels run through RuntimeShader on API 33+, with shared drawing fallbacks for
older Android, software Canvas, Core Graphics and desktop Java2D.

This keeps painted artwork. Shader rendering does not require replacing every
asset with procedural artwork, nor moving gameplay or scene composition into a
shader. Static terrain and lettering can still be prepared as cached rasters.
There is no claim that every preparation operation runs on the GPU.

| Graphics | Production path |
| --- | --- |
| Watercraft, cargo, rigid scenery, shadows, surface light, lettering | Registered textures and shader compositing |
| Terrain, sky strips, panels, HUD shapes, impact flashes and sparks | Textures or native GPU geometry/coverage |
| Bow/shoulder foam, prop wash, expanding arc crests | Shared `water-foam.inc`, modes 0–5 |
| Light reflections and dark exhaust | Shared `water-foam.inc`, modes 6–7 |
| Tow ropes and attachment coils | Shared `rope.inc` |
| Light halos, guidance dots, progress rings | Shared `effects.inc` via `Painter.material` |

Materials are centered affine patches. Java supplies the full basis, mode,
normalized detail, amount and ARGB. Camera/opacity wrappers transform them;
native hosts only evaluate coverage. Existing stencil/Canvas clips and painter
ordering still apply. No scene texture reads, full-screen passes, independent
native clocks or extra game state are introduced. The two material scalar fields
fit the existing 48-byte Metal vertex alignment.

Halos no longer allocate color-keyed raster kernels. Rings use three patches for
feather, middle and core regardless of progress; each patch reserves a gutter
outside its stroke. Reference fallback shapes approximate the procedural falloff,
so native pixels require separate review. Water-plane clipping wrappers that
cannot forward arbitrary materials intentionally expand the shared fallback to
preserve their clipping semantics.

The original expanding arc motion stays intact. Prop-wash whirl modes 2/3 use
70% of their previous opacity on every host, including texture fallbacks; bow,
shoulder and arc crest opacity remains unchanged.

Validation must distinguish shader submission counts from frame intervals.
`material_patches` in the Metal diagnostics verifies that real scenes reach the
native material path. Android build success does not validate RuntimeShader
compilation, appearance or timing on a device.
