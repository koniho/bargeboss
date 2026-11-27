package com.bargeboss.game;

/** The one fire look, taken from the tutorial woodpiles, for every burning thing in the game: a hot
 * core glow, FlameArt's planted blended flame sprites and rising embers; fixed fires add a red
 * ground wash, a water reflection and an analytic smoke column. Callers own the heat; everything
 * here is a pure function of it and the visual clock. Heat is a 0..1 fraction of a full fire. */
final class FireArt {
    private FireArt(){}
    static final int SMOKE_LIFE=256,SMOKE_PUFFS=20,STEAM_PUFFS=4;
    static final float WASH_WIDTH=1450,WASH_HEIGHT=980,WASH_ALPHA=.3f;

    /** How hard the flames burn (sprite size), from heat. */
    static float strength(float heat){return .35f+.65f*heat;}
    /** Flickering light energy; seed desynchronises neighbouring fires. */
    static float energy(Game g,float heat,int seed){
        if(heat<=0)return 0;
        float flame=.77f+.14f*(float)StrictMath.sin(g.visualTicks*.19+seed)+.09f*(float)StrictMath.sin(g.visualTicks*.071+1.3+seed);
        return flame*(.25f+.75f*heat);
    }
    /** Wash and smoke density: full heat brightest, a cooled fire fading through its steam tail. */
    static float intensity(Game g,float heat,float steam,int seed){
        if(heat>0){float flame=.86f+.09f*(float)StrictMath.sin(g.visualTicks*.13+seed*1.7)+.05f*(float)StrictMath.sin(g.visualTicks*.057+seed);
            return flame*(.18f+.82f*heat);}
        return .18f*steam;
    }

    /** Flames at (wx,wy) with their base `rise` sprite units above the water. size 1 is a woodpile;
     * alpha fades the whole fire in or out; flames and embers bound the draw count. */
    static void burn(Game g,Painter p,float wx,float wy,float rise,float size,float heat,float alpha,int seed,int flames,int embers){
        if(heat<=0 || alpha<=.003f)return;
        RiverCamera c=g.layout.camera;float s=c.scale*size,sx=c.screenX(wx,wy),sy=c.screenY(wx,wy)-rise*c.scale;
        float strength=strength(heat);
        // One hot core glow sits on the flames; the wide wash, if any, is drawn beneath vessels.
        p.material(sx,sy-18*s,210*s,0,0,130*s,EffectMaterial.GLOW,0,energy(g,heat,seed),SoftCue.alpha(FireLight.color(g,seed),.8f*alpha));
        FlameArt.draw(g,p,wx,wy,rise,strength,seed,size,alpha,flames);
        for(int i=0;i<embers;i++){
            float age=(g.visualTicks*1.4f+i*17+seed*23)%120,t=age/120;
            float x=sx+((i%7-3)*8+age*.16f*(float)StrictMath.sin(i+seed))*s,y=sy-age*.8f*s;
            p.line(x,y,x-2*s,y+5*s,1.8f*s,SoftCue.alpha(i%2==0?0xffffd329:0xffff692b,(1-t)*strength*alpha));
        }
    }
    /** A full woodpile-sized fire: every emitter and twenty embers. */
    static void burn(Game g,Painter p,float wx,float wy,float rise,float heat,int seed){burn(g,p,wx,wy,rise,1,heat,1,seed,FlameArt.INSTANCES,20);}

    /** Steam rising off a fire that has just gone out; t runs 0..1 over the tail. */
    static void steamTail(Game g,Painter p,float wx,float wy,float rise,float t){
        RiverCamera c=g.layout.camera;float scale=c.scale,sx=c.screenX(wx,wy),sy=c.screenY(wx,wy)-rise*scale;
        for(int i=0;i<STEAM_PUFFS;i++)SteamPuff.draw(p,sx+(i-1.5f)*13*scale-t*22*scale,sy-(8+t*45)*scale,(12+t*15)*scale,(1-t)*.8f);
    }

    /** Mostly deep red with a slow amber breath from the shared flame palette. */
    static int washColor(Game g,int seed){int f=FireLight.color(g,seed);
        int green=Math.round(40+((f>>8&255)-82)*.25f),blue=Math.round(20+((f&255)-23)*.1f);
        return 0xffff0000|Math.max(0,Math.min(255,green))<<8|Math.max(0,Math.min(255,blue));}
    /** One screen-sized warm wash on the ground/water layer beneath vessels and buildings. */
    static void wash(Game g,Painter p,float wx,float wy,float rise,float intensity,int color){
        if(intensity<=.003f)return;
        RiverCamera c=g.layout.camera;float scale=c.scale;
        float sx=c.screenX(wx,wy),sy=c.screenY(wx,wy)-rise*scale,w=WASH_WIDTH*scale,h=WASH_HEIGHT*scale;
        if(sx+w*.5f<0 || sx-w*.5f>g.layout.width || sy+h*.5f<0 || sy-h*.5f>g.layout.height)return;
        p.material(sx,sy-40*scale,w,0,0,h,EffectMaterial.GLOW,0,intensity,SoftCue.alpha(color,WASH_ALPHA));
    }
    /** The fire's flickering streak on nearby water. */
    static void reflection(Game g,Painter world,float wx,float wy,float energy,int color,int seed){
        if(energy>0)WaterReflection.draw(g,world,wx,wy,65,135,SoftCue.alpha(color,energy*.65f),seed);
    }

    /** Analytic smoke column: a fixed set of puffs, each reborn every SMOKE_LIFE ticks, drifting
     * downwind (north-west, with vessel exhaust) as it rises. No particles are stored; wet[] is a
     * ring of "water was landing" flags by smoke tick, so each puff keeps the colour it was born
     * with: dark soot while dry, pale steam while being cooled, nothing once the steam tail ends.
     * Drawn on the world layer beneath vessels so a plume never hides the tug. */
    static void plume(Game g,Painter p,float wx,float wy,float rise,float intensity,boolean out,float steam,int ticks,boolean[] wet,int seed){
        if(intensity<=.003f)return;
        RiverCamera c=g.layout.camera;float scale=c.scale;
        float density=out?1:.45f+.55f*Math.min(1,intensity/.9f);
        for(int i=0;i<SMOKE_PUFFS;i++){
            int age=(ticks+i*SMOKE_LIFE/SMOKE_PUFFS+seed*37)&(SMOKE_LIFE-1);
            float t=age/(float)SMOKE_LIFE;
            boolean damp=out || wet[(ticks-age)&(SMOKE_LIFE-1)];
            float curl=(float)StrictMath.sin(age*.031+i*2.1+seed);
            float x=wx+((i%3)-1)*6-t*(70+curl*14),y=wy-t*(130+curl*10);
            float px=c.screenX(x,y),py=c.screenY(x,y)-(rise+38+t*t*70+age*.8f)*scale;
            float radius=(18+t*72)*scale*(.75f+.25f*density);
            float fade=Physics.clamp(age/14f,0,1)*(1-t)*(1-t);
            if(px+radius<0 || px-radius>g.layout.width || py+radius<0 || py-radius>g.layout.height)continue;
            if(damp)SteamPuff.draw(p,px,py,radius*.9f,fade*(out?steam:.75f));
            else p.smokePuff(px,py,radius,t,SoftCue.alpha(0xff000000|SmokePuff.SOOT,fade*.78f*density));
        }
    }
}
