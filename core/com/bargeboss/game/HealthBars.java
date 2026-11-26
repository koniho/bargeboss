package com.bargeboss.game;

/** Clean thin health bars above watercraft, drawn over the world (under the HUD). A bar appears only
 * for {@link #SHOW} ticks after that vessel's health changes, then fades, so the screen stays clear;
 * the player's bar also stays up while its hull is below 25%.
 * The player's tug reads {@link Game#hull} in its own colour and pulses below 25%; every other
 * vessel reads its {@link Body#health}. A white chip shows the health just lost, then drains.
 * Sinking vessels drop their bar (the sinking itself is the feedback). */
final class HealthBars {
    private HealthBars(){}
    static final int PLAYER=0xff3ee6b0,OTHER=0xffffb347,DANGER=0xffff4a3d,TRACK=0xcc0e1a24,EDGE=0xe6050a10,CHIP=0xffffffff;
    /** Bar height in screen units and width limits; width follows hull length. */
    static final float HEIGHT=4,MIN_WIDTH=26,MAX_WIDTH=46;
    static final float LOW=.25f;
    /** Ticks a bar stays up after a health change, the last FADE of them fading out. */
    static final int SHOW=150,FADE=30;

    static void draw(Game g,Painter p){
        if(g.mode==Game.TITLE)return;
        bar(g,p,g.tug,true);
        for(Body b:g.cargo)if(b!=null && b.active && b.sinkTicks<=0)bar(g,p,b,false);
        if(g.tutorial==null && g.feeders!=null)for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE){bar(g,p,f.boat,false);if(f.load!=null && f.load.active)bar(g,p,f.load,false);}
        if(g.trafficActive())bar(g,p,g.trafficBoat,false);
        for(HarborTraffic.Craft c:g.harborTraffic.craft)if(c.body.active && c.body.sinkTicks<=0)bar(g,p,c.body,false);
        if(g.montroseStage())for(MooredFleet.FloatBoat m:g.mooredFleet.boats)if(m.afloat())
            bar(g,p,m.x,m.y,m.heave,30*ArticulatedModel.Z_SCALE,(m.berth.length+m.berth.width)*.55f,m.health/100f,m.healthBefore/100f,g.ticks-m.damagedTick,false);
    }
    private static void bar(Game g,Painter p,Body b,boolean player){
        if(b==null)return;
        // Above the tallest point of the hull: the tug's funnel, a deck load otherwise.
        float lift=(player?44:b.cruiser || b.yacht?34:28)*ArticulatedModel.Z_SCALE+WaterSurface.lift(g,b);
        float health,before;int sinceHit;
        if(player){health=g.hull/100f;sinceHit=g.ticks-g.hullDamagedTick;before=Math.min(100,g.hull+9)/100f;}
        else {health=b.health/100f;sinceHit=g.ticks-b.damagedTick;before=b.healthBefore/100f;}
        bar(g,p,b.x,b.y,0,lift,(b.half+b.radius)*1.1f,health,before,sinceHit,player);
    }
    private static void bar(Game g,Painter p,float wx,float wy,float heave,float lift,float hull,float health,float before,int sinceHit,boolean player){
        boolean low=player && health<LOW;
        if(sinceHit<0 || sinceHit>=SHOW && !low)return;
        float shown=low?1:Physics.clamp((SHOW-sinceHit)/(float)FADE,0,1);
        RiverCamera c=g.layout.camera;
        float sx=c.screenX(wx,wy),sy=c.screenY(wx,wy)-(lift+heave)*c.scale-10;
        float width=Physics.clamp(hull*c.scale,MIN_WIDTH,MAX_WIDTH);
        if(sx+width<0 || sx-width>g.layout.width || sy<-HEIGHT || sy-HEIGHT>g.layout.height)return;
        health=Physics.clamp(health,0,1);
        float x=Math.round(sx-width*.5f),y=Math.round(sy-HEIGHT*.5f);
        int fill=player?PLAYER:OTHER;
        if(low){
            // Low hull: the bar throbs between the player colour and red about twice a second.
            float pulse=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.21f);
            fill=mix(fill,DANGER,.55f+.45f*pulse);
            p.material(sx,sy,width*1.7f,0,0,HEIGHT*5,EffectMaterial.GLOW,0,1,SoftCue.alpha(DANGER,(.25f+.35f*pulse)*shown));
        }
        p.rect(x-1,y-1,width+2,HEIGHT+2,SoftCue.alpha(EDGE,shown));
        p.rect(x,y,width,HEIGHT,SoftCue.alpha(TRACK,shown));
        if(sinceHit<HullDamage.CHIP && before>health){
            // Chip: hold briefly, then drain toward the new value.
            float t=Physics.clamp((sinceHit-12)/(float)(HullDamage.CHIP-12),0,1),chip=before+(health-before)*t*t;
            p.rect(x,y,width*chip,HEIGHT,SoftCue.alpha(CHIP,.85f*(1-t*.5f)*shown));
        }
        if(health>0)p.rect(x,y,Math.max(1,width*health),HEIGHT,SoftCue.alpha(fill,shown));
        // A one-pixel top highlight keeps the thin bar crisp on dark water.
        if(health>0)p.rect(x,y,Math.max(1,width*health),1,SoftCue.alpha(0xffffffff,.35f*shown));
    }
    private static int mix(int a,int b,float t){
        int r=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t),gg=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t),bb=Math.round((a&255)*(1-t)+(b&255)*t);
        return 0xff000000|(r<<16)|(gg<<8)|bb;
    }
}
