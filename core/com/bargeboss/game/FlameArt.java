package com.bargeboss.game;

/** Five planted sprite emitters per pile; all animation and smoke timing use the shared clock. */
final class FlameArt {
    static final int INSTANCES=5,FRAMES=16;
    // Back-to-front anchors on the timber pile, in world XY. Smoke uses these exact points.
    private static final float[] X={-18,14,0,-16,16},Y={-10,-12,0,13,12};
    private static final float[] SIZE={142,174,150,124,154},ALPHA={.60f,.68f,.78f,.62f,.70f};
    private static final int[] PERIOD={92,73,81,107,66},OFFSET={0,23,41,17,51};
    static float x(int i){return x(0,i);}
    static float y(int i){return y(0,i);}
    static float x(int pile,int i){return FireworksLesson.x(pile)+X[i];}
    static float y(int pile,int i){return FireworksLesson.y(pile)+Y[i];}
    static float phase(int tick,int i){return Math.floorMod(tick+OFFSET[i],PERIOD[i])*FRAMES/(float)PERIOD[i];}
    static float strength(Game g,int pile){return .35f+.65f*g.tutorial.fireworks.heat(pile)/FireworksLesson.PILE_HEAT;}
    /** Each burning pile feeds one flame-top puff into the shared exhaust pool every
     * EMIT_PERIOD ticks, cycling its five anchors, so six fires together emit no faster than
     * the former two-pile rate and never evict vessel exhaust from the bounded pool. The dense
     * column itself is FireworksLesson's analytic plume. */
    static final int EMIT_PERIOD=10;
    static void step(Game g){
        if(g.tutorial.fireworks.heat<=0)return;
        for(int pile=0;pile<FireworksLesson.PILES;pile++)if(g.tutorial.fireworks.heat(pile)>0){
            int tick=g.visualTicks+pile*3;if(Math.floorMod(tick,EMIT_PERIOD)!=0)continue;
            int i=Math.floorMod(tick/EMIT_PERIOD+pile,INSTANCES);
            g.smoke.shore(x(pile,i),y(pile,i),GroundArt.LAND_RISE+SIZE[i]*.42f*strength(g,pile),.8f);
        }
    }
    static void draw(Game g,Painter p){draw(g,p,0);}
    static void draw(Game g,Painter p,int pile){
        RiverCamera c=g.layout.camera;float strength=strength(g,pile);
        for(int i=0;i<INSTANCES;i++){
            float phase=phase(g.visualTicks+pile*31,i),size=SIZE[i]*strength*c.scale;int frame=(int)phase;
            p.spriteBlend(SpriteAtlas.FLAME,frame,(frame+1)%FRAMES,c.screenX(x(pile,i),y(pile,i)),
                    c.screenY(x(pile,i),y(pile,i))-GroundArt.LAND_RISE*c.scale,size,size,0,phase-frame,ALPHA[i]);
        }
    }
    private FlameArt(){}
}
