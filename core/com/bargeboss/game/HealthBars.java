package com.bargeboss.game;

/** Clean thin health bars above every active watercraft, drawn over the world (under the HUD).
 * The player's tug reads {@link Game#hull} in its own colour and pulses below 25%; every other
 * vessel reads its {@link Body#health}. A white chip shows the health just lost, then drains.
 * Sinking vessels drop their bar (the sinking itself is the feedback). */
final class HealthBars {
    private HealthBars(){}
    static final int PLAYER=0xff3ee6b0,OTHER=0xffffb347,DANGER=0xffff4a3d,TRACK=0xcc0e1a24,EDGE=0xe6050a10,CHIP=0xffffffff;
    /** Bar height in screen units and width limits; width follows hull length. */
    static final float HEIGHT=4,MIN_WIDTH=26,MAX_WIDTH=46;
    static final float LOW=.25f;

    static void draw(Game g,Painter p){
        if(g.mode==Game.TITLE)return;
        bar(g,p,g.tug,true);
        for(Body b:g.cargo)if(b!=null && b.active && b.sinkTicks<=0)bar(g,p,b,false);
        if(g.tutorial==null && g.feeders!=null)for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE){bar(g,p,f.boat,false);if(f.load!=null && f.load.active)bar(g,p,f.load,false);}
        if(g.trafficActive())bar(g,p,g.trafficBoat,false);
    }
    private static void bar(Game g,Painter p,Body b,boolean player){
        if(b==null)return;
        RiverCamera c=g.layout.camera;
        // Above the tallest point of the hull: the tug's funnel, a deck load otherwise.
        float lift=(player?44:b.cruiser || b.yacht?34:28)*ArticulatedModel.Z_SCALE+WaterSurface.lift(g,b);
        float sx=c.screenX(b.x,b.y),sy=c.screenY(b.x,b.y)-lift*c.scale-10;
        float width=Physics.clamp((b.half+b.radius)*1.1f*c.scale,MIN_WIDTH,MAX_WIDTH);
        if(sx+width<0 || sx-width>g.layout.width || sy<-HEIGHT || sy-HEIGHT>g.layout.height)return;
        float health,before;int sinceHit;
        if(player){health=g.hull/100f;sinceHit=g.damageFlashTicks>0?40-g.damageFlashTicks:HullDamage.CHIP;before=Math.min(100,g.hull+9)/100f;}
        else {health=b.health/100f;sinceHit=g.ticks-b.damagedTick;before=b.healthBefore/100f;}
        health=Physics.clamp(health,0,1);
        float x=Math.round(sx-width*.5f),y=Math.round(sy-HEIGHT*.5f);
        int fill=player?PLAYER:OTHER;
        if(player && health<LOW){
            // Low hull: the bar throbs between the player colour and red about twice a second.
            float pulse=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.21f);
            fill=mix(fill,DANGER,.55f+.45f*pulse);
            p.material(sx,sy,width*1.7f,0,0,HEIGHT*5,EffectMaterial.GLOW,0,1,SoftCue.alpha(DANGER,.25f+.35f*pulse));
        }
        p.rect(x-1,y-1,width+2,HEIGHT+2,EDGE);
        p.rect(x,y,width,HEIGHT,TRACK);
        if(sinceHit<HullDamage.CHIP && before>health){
            // Chip: hold briefly, then drain toward the new value.
            float t=Physics.clamp((sinceHit-12)/(float)(HullDamage.CHIP-12),0,1),chip=before+(health-before)*t*t;
            p.rect(x,y,width*chip,HEIGHT,SoftCue.alpha(CHIP,.85f*(1-t*.5f)));
        }
        if(health>0)p.rect(x,y,Math.max(1,width*health),HEIGHT,fill);
        // A one-pixel top highlight keeps the thin bar crisp on dark water.
        if(health>0)p.rect(x,y,Math.max(1,width*health),1,SoftCue.alpha(0xffffffff,.35f));
    }
    private static int mix(int a,int b,float t){
        int r=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t),gg=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t),bb=Math.round((a&255)*(1-t)+(b&255)*t);
        return 0xff000000|(r<<16)|(gg<<8)|bb;
    }
}
