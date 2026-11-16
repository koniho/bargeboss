package com.bargeboss.game;

/** Shared opaque face-material identities. UV distance is measured in model/world units. */
public final class SurfaceMaterial {
    public static final int RUST=0,CONCRETE=1,SAFETY=2,STEEL=3,WOOD=4,CORRUGATED=5;
    /** Flat emissive model lenses bypass scenery grading and material shaders. */
    public static final int EMISSIVE=-2;
    private SurfaceMaterial(){}
}
