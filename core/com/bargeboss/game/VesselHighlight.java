package com.bargeboss.game;

/** Cached alpha-derived hull bloom; only opacity changes during the pulse. */
public final class VesselHighlight {
    public static final int CELL=192,INNER=144,PAD=24;
    private VesselHighlight() {}
    public static boolean atlas(int atlas) {return atlas==SpriteAtlas.YACHT_GOLD || atlas==SpriteAtlas.YACHT_MINT || atlas==SpriteAtlas.BARGE_GOLD || atlas==SpriteAtlas.BARGE_MINT || atlas==SpriteAtlas.TUG_GOLD;}
    public static float pivot(float original) {return (PAD+INNER*original)/CELL;}
    public static int source(int atlas) {return atlas==SpriteAtlas.YACHT_GOLD || atlas==SpriteAtlas.YACHT_MINT?SpriteAtlas.YACHT:atlas==SpriteAtlas.TUG_GOLD?SpriteAtlas.TUG:SpriteAtlas.BARGE;}
    public static float scale(int atlas) {return (source(atlas)==SpriteAtlas.YACHT?YachtArt.SCALE:source(atlas)==SpriteAtlas.TUG?TugArt.SCALE:BargeArt.HULL_SCALE)*CELL/INNER;}
    static int style(Game g,Body b) {
        if(b.id<=0 || b.berth<0 || !b.active)return -1;
        Tutorial t=g.tutorial;
        if(t!=null && b==g.cargo[0] && (t.lesson==Tutorial.CONNECT || t.lesson==Tutorial.PULL || t.lesson==Tutorial.CASTOFF))return SpriteAtlas.BARGE_GOLD;
        if(b.lineAttached || b.deliveryRecorded)return -1;
        if(g.canTie(b) && !g.ropeDragging)return SpriteAtlas.BARGE_MINT;
        return t!=null && t.lesson>=Tutorial.CONNECT && t.lesson<Tutorial.CHALLENGE && (t.lesson==Tutorial.LINK?g.cargo[2]==b:g.cargo[0]==b)
                ?SpriteAtlas.BARGE_GOLD:-1;
    }
    static float opacity(int tick) {
        // A 1.5 second breath, with enough light at the trough to remain identifiable.
        float pulse=.5f-.5f*(float)StrictMath.cos((tick%90)*2*StrictMath.PI/90);
        return .24f+.30f*pulse;
    }
    public static void draw(Game g,Painter p,Body b,boolean tug,float x,float y,float size) {
        if(!b.active || tug)return;
        int atlas=b.yacht?(g.mission.settledTicks>0?SpriteAtlas.YACHT_MINT:SpriteAtlas.YACHT_GOLD):style(g,b);
        float alpha=atlas>=0?opacity(g.visualTicks):.12f+.035f*SceneryEffects.intensity(g.visualTicks,b.id*.73f);
        if(atlas<0)atlas=tug?SpriteAtlas.TUG_GOLD:SpriteAtlas.BARGE_GOLD;
        // Identical heading blend, waterline pivot and heave to the hull itself.
        // Draw with this vessel, before nearer scenery, never as a HUD overlay.
        SpriteAtlas.draw(p,atlas,b.angle,x,y,size,size,alpha);
    }
}
