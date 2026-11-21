# Specialized barges v1

Fire-barge design: built-in imagegen, 2026-11-20. References: the accepted
`barge-isometric-v1-design.png` (style and camera), and the Memphis Fire
Department photograph credited by Firehouse:
https://www.firehouse.com/rescue/hazardous-materials/article/21069063/hazmat-studies-memphis-evolution-of-hazmat-to-all-hazards-rescue
Photo URL: https://img.firehouse.com/files/base/cygnus/fhc/image/2019/02/Fire_barge.5c6c4743e0725.png
The reference is research, not redistributed production artwork.

Prompt: Create a high resolution transparent game asset design reference for
Barge Boss: one Memphis-inspired firefighting barge in a fixed orthographic
isometric 35.264 degree camera, bow toward lower left like image 1. Image 1 is
the exact game material/style/perspective reference, image 2 is real equipment
layout reference only. Preserve readable stylized detailed industrial
proportions, rust-red barge hull with yellow worn rim and black tire fenders.
On deck have red rectangular pump equipment cabins with pale roofs aft,
exposed pipes, a small yellow generator and two elevated red water monitors
forward. No crew, no water, no spray, no glow, no smoke, no detached shadow,
no lettering. Full vessel isolated with generous transparent margins. This is
one design proof, not a rotation sheet.

Result: `docs/art-studies/memphis-fire-barge-v1-design.png`. Design reference;
not a production sheet. Runtime deck equipment is an independently authored
rigid model baked by `tools/BakeBargeEquipment.java`: 64 genuine rotations,
256-square cells, stable waterline registration, retained normal/height and
material maps. The model is deliberately lower-detail than the design proof.
Barrels remain live geometry for continuous aiming. No AI turntable is claimed
geometrically consistent merely because it contains 64 cells.

The **existing hull/cargo family is still 32 poses**. Earlier rejected 64-pose
painting attempts are documented in `vessel-64-review.md`. This increment adds
64-pose equipment and mixed-count runtime support, not the complete requested
fleet upgrade. Do not duplicate or crossfade old frames and label those 64
new authored views. Future barge/cargo source models must deliver 64 actual
headings, matched anchors, shadow and light derivatives before replacement.

Reproduce the model exports and derivatives with `./tools/bake-barge-equipment.sh`.
The asset checker verifies all 128 equipment cells, coverage of corresponding
normal/material maps and transparent gutters. This verifies exported geometry
coverage, not artistic fidelity to the generated design proof.
