package com.bargeboss.game;

/** Navy Pier fire: sparks from the Centennial Wheel set the timber pavilions on the pier's
 * south edge alight. The wheel stands 72 m back from the wall, beyond monitor reach, so it
 * only smokes; the pavilions face the harbor within reach. Only simulated monitor droplets
 * reduce heat; render code reads this state and never advances it. */
final class ChicagoFire {
    static final int PILES=3,PILE_HEAT=420,HEAT=PILES*PILE_HEAT,STEAM_TICKS=180;
    private static final float U=ChicagoSurvey.UNITS_PER_METRE;
    /** Pavilion offsets along the pier from the wheel (world Y), set back from the south wall,
     * and flame height above the deck (model Z), in metres times U. Flames stay low enough
     * for the monitors' flat arc. */
    private static final float[] ALONG={-16*U,0,14*U},RISE={6*U,3.5f*U,5*U};
    static final float SETBACK=8*U;
    private static final float[][] AT=positions();
    static final float HIT_RADIUS=26,HIT_HEIGHT=20;
    private final int[] heat=new int[PILES],steam=new int[PILES],sizzle=new int[PILES];
    final FireSteam contactSteam=new FireSteam(PILES);
    int total;
    boolean ignited;
    void reset(){contactSteam.reset();for(int i=0;i<PILES;i++){heat[i]=steam[i]=sizzle[i]=0;}total=0;ignited=false;}
    void ignite(){ignited=true;total=HEAT;for(int i=0;i<PILES;i++)heat[i]=PILE_HEAT;}
    static float x(int pile){return AT[pile][0];}
    static float y(int pile){return AT[pile][1];}
    /** March south from the wheel's line to the pier's south wall, then set back onto the deck. */
    private static float[][] positions(){float[][] out=new float[PILES][];
        for(int i=0;i<PILES;i++){float x=ChicagoRiver.WHEEL_X,y=ChicagoRiver.WHEEL_Y+ALONG[i];int guard=0;
            while(!ChicagoRiver.contains(x,y) && guard++<2000)x-=1;
            out[i]=new float[]{x+SETBACK,y};}
        return out;}
    private static final class Models {static final ArticulatedModel.Rigid PAVILION=pavilion();}
    /** Open-sided timber pavilion: posts, a pitched-look roof slab and a scorched counter. */
    private static ArticulatedModel.Rigid pavilion(){ArticulatedModel.Builder b=new ArticulatedModel.Builder();b.node("root",-1,0,0,0,0,0);
        float w=5*U,d=4*U,h=3.2f*U;int timber=0xff6b4a33,roof=0xff3a2c26;
        for(int sx=-1;sx<=1;sx+=2)for(int sy=-1;sy<=1;sy+=2)b.box(0,sx*w*.45f,sy*d*.45f,h*.5f,1.2f,1.2f,h,timber,SurfaceMaterial.WOOD);
        b.box(0,0,0,h+1,w+3,d+3,2,roof,SurfaceMaterial.WOOD);
        b.box(0,0,d*.3f,2.5f,w*.8f,1.6f,5,0xff4a3a30,SurfaceMaterial.WOOD);
        return b.build().rigid();}
    static void prepare(){Models.PAVILION.fragmentCount();}
    /** Model-unit height above the deck at the fire's centre. */
    static float rise(int pile){return RISE[pile];}
    int heat(int pile){return heat[pile];}
    int steam(int pile){return steam[pile];}
    boolean out(){return ignited && total==0;}
    int remaining(){int n=0;for(int h:heat)if(h>0)n++;return n;}
    float progress(){return ignited?1-total/(float)HEAT:0;}
    void step(Game g){contactSteam.step();for(int i=0;i<PILES;i++){if(steam[i]>0)steam[i]--;if(sizzle[i]>0)sizzle[i]--;}}
    /** Droplet test in shared world/model units; true consumes the droplet. */
    boolean hit(float x,float y,float z,float floor){
        if(!ignited || total==0)return false;
        for(int i=0;i<PILES;i++){if(heat[i]==0)continue;
            if(Physics.length(x-x(i),y-y(i))>HIT_RADIUS || Math.abs(z-floor-RISE[i])>HIT_HEIGHT)continue;
            contactSteam.hit(i,x,y);sizzle[i]=24;heat[i]--;total--;
            if(heat[i]==0)steam[i]=STEAM_TICKS;
            return true;
        }
        return false;
    }
    float energy(Game g,int pile){if(heat[pile]<=0)return 0;
        float flame=.77f+.14f*(float)StrictMath.sin(g.visualTicks*.19+pile)+.09f*(float)StrictMath.sin(g.visualTicks*.071+1.3+pile);
        return flame*(.3f+.7f*heat[pile]/(float)PILE_HEAT);
    }
    float depth(Game g,int pile){return g.layout.camera.screenY(x(pile),y(pile));}
    void lights(Game g,SceneryLighting out){for(int i=0;i<PILES;i++){float e=energy(g,i);
        if(e>0)out.add(x(i),y(i),GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE+RISE[i]+20,240,1.2f*e,FireLight.color(g,i));}}
    /** Smoke from the wheel's scorched lower south gondolas while the pier burns. */
    void wheelSmoke(Game g,Painter p,float sx,float sy){if(!ignited || total==0)return;float scale=g.layout.camera.scale,strength=total/(float)HEAT;
        for(int i=0;i<9;i++){float age=(g.visualTicks*.5f+i*23)%240,t=age/240;
            p.smokePuff(sx+(t*90+(i%3)*8)*scale,sy-age*1.2f*scale,(14+t*44)*scale,t,SoftCue.alpha(0xff2b2a2e,(1-t)*.45f*strength));}}
    void draw(Game g,Painter p,int pile){
        RiverCamera c=g.layout.camera;float scale=c.scale,X=x(pile),Y=y(pile);
        float sx=c.screenX(X,Y),sy=c.screenY(X,Y)-(GroundArt.LAND_RISE+RISE[pile]*ArticulatedModel.Z_SCALE)*scale;
        if(sx<-220*scale || sx>g.layout.width+220*scale || sy<-260*scale || sy>g.layout.height+220*scale)return;
        Models.PAVILION.drawSolid(g,p,X,Y,GroundArt.LAND_RISE,0,0,0);
        int h=heat[pile];
        if(h>0){float strength=.35f+.65f*h/PILE_HEAT,e=energy(g,pile);int color=FireLight.color(g,pile);
            p.material(sx,sy-26*scale,230*scale,0,0,180*scale,EffectMaterial.GLOW,0,e,SoftCue.alpha(color,.55f));
            p.material(sx,sy,120*scale,0,0,70*scale,EffectMaterial.GLOW,0,e,SoftCue.alpha(color,.85f));
            for(int i=0;i<18;i++){float age=(g.visualTicks*1.5f+i*19+pile*37)%110,t=age/110;
                float x=sx+((i%6-2.5f)*7+age*.18f*(float)StrictMath.sin(i+pile))*scale,y=sy-age*.75f*scale;
                p.line(x,y,x-2*scale,y+6*scale,(2.4f-t)*scale,SoftCue.alpha(i%3==0?0xffffed9a:i%2==0?0xffffb52e:0xffff5217,(1-t)*strength));
            }
            // Dark plume drifts east off the pier over the harbor.
            for(int i=0;i<7;i++){float age=(g.visualTicks*.6f+i*29+pile*13)%200,t=age/200;
                p.smokePuff(sx+(t*70+(i%3)*6)*scale,sy-(40+age*1.1f)*scale,(10+t*34)*scale,t,SoftCue.alpha(0xff2b2a2e,(1-t)*.55f*strength));}
        }
        contactSteam.draw(g,p,pile,RISE[pile]*ArticulatedModel.Z_SCALE);
        if(h==0 && steam[pile]>0){float t=1-steam[pile]/(float)STEAM_TICKS;
            for(int i=0;i<4;i++)SteamPuff.draw(p,sx+(i-1.5f)*12*scale-t*20*scale,sy-(6+t*50)*scale,(12+t*16)*scale,(1-t)*.8f);}
    }
}
