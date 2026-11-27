package com.bargeboss.game;

/** The tug's hull runs out: a big blast, two secondary explosions while it lists and goes under,
 * then a GAME OVER stamp over the darkening scene before the results screen. Drives the tug's
 * own sinkTicks so VesselView sinks the sprite through the waterline like any wreck. */
final class TugDeath {
    /** Ticks from the blast to the results screen. */
    static final int TICKS=240;
    /** When the GAME OVER overlay starts to fade in. */
    static final int OVERLAY=70;
    private static final int SEED=777;
    private TugDeath(){}

    static void start(Game g){
        g.deathTicks=1;
        g.cancel();g.overview.cancelTouch();g.tow.release(g);
        g.ropePhase=g.ropeTicks=0;g.ropeBody=null;g.ropeTowTarget=null;
        g.tug.sinkTicks=1;
        HullDamage.shake(g,1);HullDamage.shake(g,1);
        g.haptic(Game.HAPTIC_IMPACT);g.sounds.fireworkBurst();
        for(int k=-1;k<=1;k++)g.smoke.equipment(g,g.tug,k*g.tug.half*.6f,0,12/ArticulatedModel.Z_SCALE);
        g.tell("TUG DESTROYED!");
    }
    static void step(Game g){
        int t=++g.deathTicks;Body b=g.tug;
        b.sinkTicks=Math.min(t,HullDamage.SINK);
        b.vx*=.95f;b.vy*=.95f;
        b.spin=b.spin*.9f+(float)StrictMath.sin(t*.13f)*.45f*Math.min(1,t/(float)HullDamage.SINK);
        g.cancel();
        if(t<HullDamage.PLUNGE && t%5==0)g.smoke.equipment(g,b,((t/5)%3-1)*b.half*.5f,0,14/ArticulatedModel.Z_SCALE);
        for(int pop:HullDamage.POPS)if(t==pop){HullDamage.shake(g,.7f);g.haptic(Game.HAPTIC_IMPACT);g.sounds.fireworkBurst();g.smoke.equipment(g,b,0,0,10/ArticulatedModel.Z_SCALE);}
        if(t==HullDamage.PLUNGE){HullDamage.shake(g,.8f);g.haptic(Game.HAPTIC_IMPACT);}
    }

    /** World layer: the shared wreck effects plus an oversized opening blast and shockwave. */
    static void effects(Game g,Painter p){
        if(!g.dying())return;
        int t=g.deathTicks;Body b=g.tug;RiverCamera c=g.layout.camera;
        float s=c.scale,sx=c.screenX(b.x,b.y),sy=c.screenY(b.x,b.y),hull=b.half+b.radius,zs=ArticulatedModel.Z_SCALE*s;
        HullDamage.wreck(g,p,c,b.x,b.y,b.half,b.radius,SEED,false,Math.min(t,HullDamage.SINK+HullDamage.SLICK-1),false);
        if(t<HullDamage.SINK)HullDamage.explosion(p,c,b.x,b.y,hull,SEED+1,false,t,7,1.8f,sx,sy,zs);
        // Shockwave: two fast rings racing out over the water.
        for(int k=0;k<2;k++){
            int age=t-k*6;if(age<0 || age>=40)continue;
            float u=age/40f,r=hull*(1+(1-(1-u)*(1-u))*7)*s;
            p.material(sx,sy,r*2,0,0,r*2*RiverCamera.DEPTH,EffectMaterial.RING,.04f,1,SoftCue.alpha(0xffffe0b0,(1-u)*(1-u)*.9f));
        }
        if(t<30)p.material(sx,sy,hull*9*s,0,0,hull*9*s*RiverCamera.DEPTH,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffb040,(1-t/30f)*.8f));
    }

    /** Screen layer: a white blast flash, then the scene darkens under a punching GAME OVER stamp. */
    static void overlay(Game g,Painter p){
        if(!g.dying() || g.mode!=Game.PLAY)return;
        int t=g.deathTicks;float w=g.layout.width,h=g.layout.height;
        if(t<12)p.rect(0,0,w,h,SoftCue.alpha(0xfffff4e0,(1-t/12f)*.75f));
        if(t<OVERLAY)return;
        int a=t-OVERLAY;float fade=Physics.clamp(a/40f,0,1);
        p.rect(0,0,w,h,SoftCue.alpha(0xff0a0204,.55f*fade));
        p.rect(0,h*.5f-70,w,140,SoftCue.alpha(0xff3a0608,.7f*fade));
        float pop=a<10?2.2f-1.2f*a/10f:a<16?1-(float)StrictMath.sin((a-10)/6f*Math.PI)*.08f:1;
        float size=Math.min(96,w*.09f)*pop,cx=w*.5f,cy=h*.5f-size*.55f;
        int alpha=Math.round(255*Physics.clamp(a/8f,0,1));
        Lettering.center(p,"GAME OVER",cx+4,cy+4,size,(alpha*3/4<<24)|0x100204);
        Lettering.center(p,"GAME OVER",cx,cy,size,(alpha<<24)|0xff4a3a);
        if(a>18){int sub=Math.round(255*Physics.clamp((a-18)/14f,0,1));
            Lettering.center(p,"YOUR TUG WAS DESTROYED",cx,cy+size*1.15f,Math.min(22,w*.02f),(sub<<24)|0xffe0c8);}
    }
}
