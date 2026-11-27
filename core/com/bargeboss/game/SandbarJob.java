package com.bargeboss.game;

/** SANDBAR: a big yacht cut the corner and ran hard aground on the sandbar off Montrose Beach. Lasso it
 * and haul: the tug's line tension rocks it loose (watch the meter), then it pops free in a burst of sand
 * and spray. Tow it out to the deep-water anchorage, clear of the swimmers. */
final class SandbarJob extends MontroseJob {
    static final float BAR_X=1200,BAR_Y=-2780,BAR_ANGLE=.6f,DOCK_X=1750,DOCK_Y=-3750,DOCK_REACH=115;
    /** Tension-seconds of hauling needed to break the hull free. */
    static final float BREAKOUT=330;
    Body yacht;boolean stuck=true;float progress;int popTicks,warp;

    void setup(Game g){
        yacht=new Body(g.nextId++,2,BAR_X,BAR_Y,BAR_ANGLE,true);
        yacht.awaitingPickup=true;yacht.tiedX=yacht.x;yacht.tiedY=yacht.y;yacht.tiedAngle=yacht.angle;g.cargo[0]=yacht;
        start(g,1500,-3150,(float)Math.PI*.8f);
    }
    void objective(Game g,Painter canvas){
        if(yacht.deliveryRecorded)return;
        if(stuck)arrow(g,canvas,yacht.x,yacht.y,"YACHT AGROUND");else arrow(g,canvas,DOCK_X,DOCK_Y,"DEEP ANCHORAGE");
    }
    String status(Game g){
        boolean towed=g.tow.connected(yacht,g.tug);
        if(stuck)return towed?"HAUL AWAY FROM THE BAR / "+Math.round(100*progress/BREAKOUT)+"% FREE":"YACHT AGROUND / LASSO IT FROM THE TUG";
        return "TOW HER OUT TO THE DEEP ANCHORAGE RING / CLEAR OF THE SWIMMERS";
    }
    void step(Game g){
        super.step(g);
        if(popTicks>0)popTicks--;
        if(yacht.deliveryRecorded)return;
        boolean towed=g.tow.connected(yacht,g.tug);
        if(stuck){
            // Untowed it rests frozen; once a line is on, motion() pins it so the line can load up.
            if(!towed){yacht.awaitingPickup=true;progress=Math.max(0,progress-.5f);return;}
            float pull=Math.min(60,g.tow.pullLoad(g.tug));
            progress+=pull/60;
            // Rock the stuck hull as the line loads.
            yacht.tiedAngle=BAR_ANGLE+.05f*(progress/BREAKOUT)*(float)StrictMath.sin(ticks*.6f);
            if(progress>=BREAKOUT){
                stuck=false;yacht.awaitingPickup=false;popTicks=50;
                RiverCamera c=g.layout.camera;g.splashes.shake.hit(.9f,c.screenX(1,1)-c.screenX(0,0),c.screenY(1,1)-c.screenY(0,0),true);
                g.haptic(Game.HAPTIC_IMPACT);pop(yacht.x,yacht.y,"POPPED FREE!");g.tell("SHE'S FREE! TOW HER OUT TO DEEP WATER");
                for(int k=-1;k<=1;k++)g.smoke.shore(yacht.x+k*20,yacht.y,4,.4f);
            }
            return;
        }
        if(warp>0){
            float t=Math.min(1,++warp/70f);yacht.x+=(DOCK_X-yacht.x)*.06f;yacht.y+=(DOCK_Y-yacht.y)*.06f;yacht.vx=yacht.vy=0;
            if(t>=1){secure(yacht,yacht.x,yacht.y,yacht.angle);score(g,DOCK_X,DOCK_Y,2500+yacht.health*10,"YACHT SAFE AT ANCHOR");}
            return;
        }
        if(Physics.length(yacht.x-DOCK_X,yacht.y-DOCK_Y)<DOCK_REACH && yacht.speed()<35){warp=1;g.tow.detachBody(g,yacht);g.tell("DROPPING THE HOOK");}
    }
    void motion(Game g){
        if(!stuck || yacht.deliveryRecorded)return;
        yacht.x=yacht.tiedX;yacht.y=yacht.tiedY;yacht.angle=yacht.tiedAngle;yacht.vx=yacht.vy=yacht.spin=0;
    }
    boolean towable(Body b){return b!=yacht || !yacht.deliveryRecorded && warp==0;}
    void water(Game g,Painter p){
        if(yacht.deliveryRecorded)return;
        // The shoal itself: pale sand showing through the shallows, with a line of breaking foam.
        {RiverCamera c=g.layout.camera;float sx=c.screenX(BAR_X,BAR_Y),sy=c.screenY(BAR_X,BAR_Y),r=150*c.scale;
            float surf=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.05f);
            p.material(sx,sy,r*2.8f,0,0,r*1.2f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffc8ac74,.55f));
            p.material(sx,sy,r*1.7f,0,0,r*.7f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffeedcae,.45f+.1f*surf));}
        if(stuck){
            // Churned sand around the hull, growing as it works loose.
            ring(g,p,BAR_X,BAR_Y,70,0xffe0c27a,.6f+.4f*progress/BREAKOUT);
        }else ring(g,p,DOCK_X,DOCK_Y,DOCK_REACH*.7f,0xff6dffb0,g.tow.connected(yacht,g.tug)?1:.5f);
        if(popTicks>0){
            RiverCamera c=g.layout.camera;float u=1-popTicks/50f,r=(40+u*160)*c.scale,sx=c.screenX(BAR_X,BAR_Y),sy=c.screenY(BAR_X,BAR_Y);
            p.material(sx,sy,r*2,0,0,r*2*RiverCamera.DEPTH,EffectMaterial.RING,.06f,1,SoftCue.alpha(0xfff5e2b0,(1-u)*.9f));
        }
    }
    void overlay(Game g,Painter p){
        if(stuck && g.tow.connected(yacht,g.tug)){
            // Breakout meter above the yacht.
            RiverCamera c=g.layout.camera;float x=c.screenX(yacht.x,yacht.y)-40,y=c.screenY(yacht.x,yacht.y)-70*c.scale,f=progress/BREAKOUT;
            p.rect(x-2,y-2,84,10,0xe6050a10);p.rect(x,y,80,6,0xcc0e1a24);p.rect(x,y,80*f,6,f>.8f?0xffff5a4a:0xffffd166);
            Lettering.center(p,"HAUL!",x+40,y-12,14,0xffffe27a);
        }
        super.overlay(g,p);
    }
}
