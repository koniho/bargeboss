package com.bargeboss.game;

/** Deterministic tutorial mishap. Only simulated nozzle droplets can extinguish it. */
final class FireworksLesson {
    static final float X=448,Y=-1160,RADIUS=29;
    static final float TOW_X=330,TOW_Y=-1230;
    static final float BERTH_X=-350,BERTH_Y=-1050;
    // Centerline runs south from the celebration basin through two generous bends.
    private static final float[] ROUTE_X={330,300,-50,-50,-240};
    private static final float[] ROUTE_Y={-1120,-1350,-1350,-1050,-1050};
    float target(Game g,boolean x){
        return TowRouteGuide.target(g.tug.x,g.tug.y,ROUTE_X,ROUTE_Y,Tutorial.direct(g,g.cargo[3]),x);
    }
    static final int PILES=2,PILE_HEAT=240,HEAT=PILES*PILE_HEAT,IGNITION=140;
    static float x(int pile){return X;}
    static float y(int pile){return Y-pile*120;}
    private final int[] pileHeat=new int[PILES],pileSteam=new int[PILES],pileSizzle=new int[PILES];
    int heat(int pile){return heat==0?0:pileHeat[pile];}
    int sizzle(int pile){return pileSizzle[pile];}
    int targetPile(){return heat(0)>0?0:1;}
    int remaining(){int n=0;for(int h:pileHeat)if(h>0)n++;return n;}
    void ignite(){ignited=true;heat=HEAT;for(int i=0;i<PILES;i++)pileHeat[i]=PILE_HEAT;}
    final FireSteam contactSteam=new FireSteam();
    final FireRescueRadio rescueRadio=new FireRescueRadio();
    int showTicks,heat,steam,sizzle;
    boolean launched,ignited,done;
    float launchX,launchY;
    void reset(){contactSteam.reset();for(int i=0;i<PILES;i++)pileHeat[i]=pileSteam[i]=pileSizzle[i]=0;rescueRadio.reset();showTicks=heat=steam=sizzle=0;launched=ignited=done=false;}
    void launched(Game g,Body b){
        if(g.tutorial.lesson!=Tutorial.SHOW || (b!=g.cargo[0] && b!=g.cargo[2]) || !g.passages.passable(TutorialTowBridges.ENTRY) || launched)return;
        launched=true;launchX=b.x;launchY=b.y;
    }
    void step(Game g){
        contactSteam.step();
        steam=sizzle=0;for(int i=0;i<PILES;i++){if(pileSteam[i]>0)pileSteam[i]--;if(pileSizzle[i]>0)pileSizzle[i]--;steam=Math.max(steam,pileSteam[i]);sizzle=Math.max(sizzle,pileSizzle[i]);}
        rescueRadio.step(g);
        if(launched && !ignited && ++showTicks>=IGNITION){ignite();g.radio.transmit(RadioAudio.TEACHER);}
        if(ignited && heat>0)FlameArt.step(g);
    }
    boolean waterHit(float x,float y){
        if(!ignited || done || heat==0)return false;
        for(int i=0;i<PILES;i++)if(pileHeat[i]>0 && Physics.length(x-x(i),y-y(i))<=RADIUS){
            contactSteam.hit(i,x,y);pileSizzle[i]=sizzle=24;pileHeat[i]--;heat--;
            if(pileHeat[i]==0)pileSteam[i]=steam=180;
            if(heat==0)done=true;
            return true;
        }
        return false;
    }
    boolean visible(){return launched && !ignited || heat>0 || steam>0;}
    float depth(Game g,int pile){return g.layout.camera.screenY(x(pile),y(pile));}
    InstructionPanel.Content content(Game g){
        boolean fire=g.tutorial.lesson>=Tutorial.FIRE_TOW;
        boolean collect=g.tutorial.lesson==Tutorial.FIRE_TOW;
        String title=fire?"THAT WAS NOT THE TINY BUTTON.":launched?"PERHAPS A LITTLE TOO FESTIVE...":"YOU PASSED! CUE THE FIREWORKS.";
        String first=fire?"SMALL UPDATE: WE LIT BOTH WOODPILES.":launched?"I SAID CONFETTI. THAT IS A SMALL SUN.":"TAP THE FIREWORKS BARGE, THEN ITS ROCKET.";
        String second=collect?(Tutorial.direct(g,g.cargo[3])?"TOW THE FIRE BARGE INTO THE MARKED WATER.":"FETCH THE FIRE BARGE THROUGH THE SOUTH CANAL."):fire?(remaining()==1?"ONE PILE OUT! SPRAY THE OTHER ONE.":"HOLD THE NOZZLE. SPRAY BOTH WOODPILES."):launched?"WHO PARKED ALL THAT FIREWOOD THERE?":"A TINY VICTORY DISPLAY. YOU HAVE EARNED IT.";
        InstructionPanel.Content radio=rescueRadio.content(g,second);if(radio!=null)return radio;
        return new InstructionPanel.Content("fireworks-lesson-"+g.tutorial.lesson+":"+launched,title,first,second,-1,fire?1-heat/(float)HEAT:0,"INSTRUCTOR / CHANNEL 08",fire?"FIRE RESPONSE":"EQUIPMENT PRACTICE");
    }
    void draw(Game g,Painter p,int pile){
        float X=x(pile),Y=y(pile);int heat=heat(pile),steam=pileSteam[pile];
        RiverCamera c=g.layout.camera;float scale=c.scale;
        float sx=c.screenX(X,Y),sy=c.screenY(X,Y)-GroundArt.LAND_RISE*scale;
        boolean onScreen=sx>=-180*scale && sx<=g.layout.width+180*scale && sy>=-180*scale && sy<=g.layout.height+180*scale;
        if(onScreen)FirewoodPile.draw(g,p,pile);
        if(launched && !ignited){
            float t=Physics.clamp((showTicks-65)/75f,0,1);
            if(t>0){float x=launchX+(X-launchX)*t,y=launchY+(Y-launchY)*t;
                float h=GroundArt.LAND_RISE+150*(1-t)+90*4*t*(1-t);
                float emberX=c.screenX(x,y),emberY=c.screenY(x,y)-h*scale;
                p.material(emberX,emberY,30*scale,0,0,30*scale,EffectMaterial.GLOW,0,1,0xffff7f2d);
                p.material(emberX,emberY,10*scale,0,0,10*scale,EffectMaterial.DOT,0,1,0xffffed9e);
            }return;
        }
        if(!onScreen)return;
        if(heat>0){
            float strength=.35f+.65f*heat/PILE_HEAT;
            int fireColor=FireLight.color(g,pile);
            p.material(sx,sy-30*scale,270*scale,0,0,200*scale,EffectMaterial.GLOW,0,FireLight.energy(g,pile),SoftCue.alpha(fireColor,.62f));
            p.material(sx,sy,160*scale,0,0,85*scale,EffectMaterial.GLOW,0,FireLight.energy(g,pile),SoftCue.alpha(fireColor,.85f));
            FlameArt.draw(g,p,pile);
            for(int i=0;i<20;i++){
                float age=(g.visualTicks*1.4f+i*17)%120,t=age/120;
                float x=sx+((i%7-3)*8+age*.16f*(float)StrictMath.sin(i))*scale,y=sy-age*.8f*scale;
                p.line(x,y,x-2*scale,y+5*scale,1.8f*scale,SoftCue.alpha(i%2==0?0xffffd329:0xffff692b,(1-t)*strength));
            }
        }
        contactSteam.draw(g,p,pile);
        if(heat==0 && steam>0){
            float t=1-steam/180f;
            for(int i=0;i<4;i++)SteamPuff.draw(p,sx+(i-1.5f)*13*scale-t*22*scale,sy-(8+t*45)*scale,(12+t*15)*scale,(1-t)*.8f);
        }
    }
}
