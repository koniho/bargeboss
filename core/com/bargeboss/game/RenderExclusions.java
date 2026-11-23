package com.bargeboss.game;

/** Developer-only render experiments. Zero keeps normal drawing unchanged.
 * Scene omissions measure marginal whole-pass cost, not additive GPU timings. */
public final class RenderExclusions {
    private RenderExclusions(){}
    public static final int FACTORY_ART=1,FACTORY_SMOKE=2,FACTORY_LIGHTS=4;
    public static final int AMBIENT_WATER=8,SHORE_GEOMETRY=16;
    /** Native submission exclusions retain shared preparation work. */
    public static final int FOAM_SUBMISSION=32,FLAT_SURFACES=64;
    public static final int SCENERY_POINT_LIGHTS=128;
    public static final int LIVE_MARINA_BOATS=512;
    public static final int ALL_FACTORIES=FACTORY_ART|FACTORY_SMOKE|FACTORY_LIGHTS;
}
