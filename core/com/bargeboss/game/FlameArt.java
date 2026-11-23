package com.bargeboss.game;

/** Five planted sprite emitters; all animation and smoke timing use the shared clock. */
final class FlameArt {
    static final int INSTANCES=5,FRAMES=16;
    // Back-to-front anchors on the timber pile, in world XY. Smoke uses these exact points.
    private static final float[] X={-18,14,0,-16,16},Y={-10,-12,0,13,12};
    private static final float[] SIZE={142,174,150,124,154},ALPHA={.60f,.68f,.78f,.62f,.70f};
    private static final int[] PERIOD={92,73,81,107,66},OFFSET={0,23,41,17,51};
    static float x(int i){return FireworksLesson.X+X[i];}
    static float y(int i){return FireworksLesson.Y+Y[i];}
    static float phase(int tick,int i){return Math.floorMod(tick+OFFSET[i],PERIOD[i])*FRAMES/(float)PERIOD[i];}
    static float strength(Game g,int pile){return .35f+.65f*g.tutorial.fireworks.heat(pile)/FireworksLesson.PILE_HEAT;}
    static void step(Game g){
        if(g.tutorial.fireworks.heat<=0)return;
        for(int pile=0;pile<FireworksLesson.PILES;pile++)if(g.tutorial.fireworks.heat(pile)>0)
            for(int i=0;i<INSTANCES;i++)if(Math.floorMod(g.visualTicks+i*3+pile*7,16)==0)
                g.smoke.shore(x(i),y(i)+FireworksLesson.y(pile)-FireworksLesson.Y,GroundArt.LAND_RISE+SIZE[i]*.42f*strength(g,pile),.8f);
    }
    static void draw(Game g,Painter p){draw(g,p,0);}
    static void draw(Game g,Painter p,int pile){
        RiverCamera c=g.layout.camera;float strength=strength(g,pile);
        for(int i=0;i<INSTANCES;i++){
            float phase=phase(g.visualTicks+pile*31,i),size=SIZE[i]*strength*c.scale;int frame=(int)phase;
            p.spriteBlend(SpriteAtlas.FLAME,frame,(frame+1)%FRAMES,c.screenX(x(i),y(i)+FireworksLesson.y(pile)-FireworksLesson.Y),
                    c.screenY(x(i),y(i)+FireworksLesson.y(pile)-FireworksLesson.Y)-GroundArt.LAND_RISE*c.scale,size,size,0,phase-frame,ALPHA[i]);
        }
    }
    private FlameArt(){}
}
