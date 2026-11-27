package com.bargeboss.game;

/** Five planted sprite emitters per fire; all animation and smoke timing use the shared clock.
 * The tutorial woodpiles set the look; FireArt plants the same emitters on any burning thing,
 * scaled by size (1 = a woodpile) and lifted to its base height. */
final class FlameArt {
    static final int INSTANCES=5,FRAMES=16;
    // Back-to-front anchors on a timber pile, in world XY at size 1. Smoke uses these exact points.
    private static final float[] X={-18,14,0,-16,16},Y={-10,-12,0,13,12};
    private static final float[] SIZE={142,174,150,124,154},ALPHA={.60f,.68f,.78f,.62f,.70f};
    private static final int[] PERIOD={92,73,81,107,66},OFFSET={0,23,41,17,51};
    static float x(int i){return x(0,i);}
    static float y(int i){return y(0,i);}
    static float x(int pile,int i){return FireworksLesson.x(pile)+X[i];}
    static float y(int pile,int i){return FireworksLesson.y(pile)+Y[i];}
    static float phase(int tick,int i){return Math.floorMod(tick+OFFSET[i],PERIOD[i])*FRAMES/(float)PERIOD[i];}
    static float strength(Game g,int pile){return .35f+.65f*g.tutorial.fireworks.heat(pile)/FireworksLesson.PILE_HEAT;}
    /** Each burning fire feeds one flame-top puff into the shared exhaust pool every
     * EMIT_PERIOD ticks, cycling its five anchors, so six fires together emit no faster than
     * the former two-pile rate and never evict vessel exhaust from the bounded pool. The dense
     * column itself is FireArt's analytic plume. */
    static final int EMIT_PERIOD=10;
    static void step(Game g){
        if(g.tutorial.fireworks.heat<=0)return;
        for(int pile=0;pile<FireworksLesson.PILES;pile++)if(g.tutorial.fireworks.heat(pile)>0)
            emit(g,FireworksLesson.x(pile),FireworksLesson.y(pile),GroundArt.LAND_RISE,strength(g,pile),pile,1);
    }
    /** One fire's share of the exhaust pool: a puff from one anchor every EMIT_PERIOD ticks. */
    static void emit(Game g,float ax,float ay,float rise,float strength,int seed,float size){
        int tick=g.visualTicks+seed*3;if(Math.floorMod(tick,EMIT_PERIOD)!=0)return;
        int i=Math.floorMod(tick/EMIT_PERIOD+seed,INSTANCES);
        g.smoke.shore(ax+X[i]*size,ay+Y[i]*size,rise+SIZE[i]*.42f*strength*size,.8f);
    }
    static void draw(Game g,Painter p){draw(g,p,0);}
    static void draw(Game g,Painter p,int pile){
        draw(g,p,FireworksLesson.x(pile),FireworksLesson.y(pile),GroundArt.LAND_RISE,strength(g,pile),pile,1,1,INSTANCES);
    }
    /** The first `count` emitters of a fire anchored at (ax,ay), base `rise` sprite units up. */
    static void draw(Game g,Painter p,float ax,float ay,float rise,float strength,int seed,float size,float alpha,int count){
        RiverCamera c=g.layout.camera;
        for(int i=0;i<Math.min(count,INSTANCES);i++){
            float wx=ax+X[i]*size,wy=ay+Y[i]*size;
            float phase=phase(g.visualTicks+seed*31,i),sz=SIZE[i]*strength*size*c.scale;int frame=(int)phase;
            p.spriteBlend(SpriteAtlas.FLAME,frame,(frame+1)%FRAMES,c.screenX(wx,wy),
                    c.screenY(wx,wy)-rise*c.scale,sz,sz,0,phase-frame,ALPHA[i]*alpha);
        }
    }
    private FlameArt(){}
}
