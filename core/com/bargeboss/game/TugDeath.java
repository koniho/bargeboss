package com.bargeboss.game;

/** The tug's hull runs out. A cinematic in four acts, about eleven seconds, while the camera pushes in
 * and letterbox bars close:
 *  1. Breach (1.5 s): sparks spray, fires catch along the deck, small pops, the hull flashes red.
 *  2. Blast: a staggered multi-fireball explosion, white flash, rising fireball column, three
 *     shockwaves, and debris thrown high that splashes down and keeps burning on the water.
 *  3. Burn and sink (5.5 s): flames and embers, a chain of secondary explosions, a smoke column, the
 *     hull listing through the waterline; then fire spreads on the slick.
 *  4. GAME OVER: the scene darkens, letters drop in one by one, a red glow pulses; tap to continue.
 * Drives the tug's own sinkTicks so VesselView sinks the sprite like any wreck. Visuals are pure
 * functions of deathTicks (no extra simulation state). */
final class TugDeath {
    static final int BLAST=90,SUNK=BLAST+330,OVERLAY=400,LETTERS=7,SKIP=OVERLAY+110,TICKS=660;
    private static final int[] PRE_POPS={18,38,54,68,80};
    private static final int[] CHAIN={150,205,262,318,372};
    private static final int[] BLASTS={0,5,10,17};
    private static final int SEED=777,DEBRIS=26,FLAMES=3,EMBERS=22;
    private static final String TITLE="GAME OVER";
    private TugDeath(){}

    static void start(Game g){
        g.deathTicks=1;g.deathZoom=g.layout.camera.zoom;
        g.cancel();g.overview.cancelTouch();g.tow.release(g);
        g.ropePhase=g.ropeTicks=0;g.ropeBody=null;g.ropeTowTarget=null;
        HullDamage.shake(g,.5f);g.haptic(Game.HAPTIC_IMPACT);
        g.tell("HULL BREACH!");
    }
    /** Tug sink progress in shared wreck ticks (0 until the blast, SINK when it is under). */
    private static int wreckTicks(int t){
        if(t<BLAST)return 0;
        if(t<SUNK)return 1+(t-BLAST)*(HullDamage.SINK-1)/(SUNK-BLAST);
        return Math.min(HullDamage.SINK+(t-SUNK),HullDamage.SINK+HullDamage.SLICK-1);
    }
    static void step(Game g){
        int t=++g.deathTicks;Body b=g.tug;
        b.sinkTicks=Math.min(wreckTicks(t),HullDamage.SINK);
        b.vx*=.96f;b.vy*=.96f;
        float list=t<BLAST?.15f:Math.min(1,(t-BLAST)/(float)(SUNK-BLAST));
        b.spin=b.spin*.9f+(float)StrictMath.sin(t*.09f)*.4f*list;
        g.cancel();
        if(t<BLAST){
            if(t%8==0)g.damageFlashTicks=40;
            if(t%5==0)g.smoke.equipment(g,b,((t/5)%3-1)*b.half*.5f,0,(8+t*.08f)/ArticulatedModel.Z_SCALE);
            for(int pop:PRE_POPS)if(t==pop){HullDamage.shake(g,.35f+t*.003f);g.haptic(Game.HAPTIC_CONTACT);g.sounds.splash(.6f);}
            if(t==60)g.tell("SHE'S GONNA BLOW!");
        }
        if(t==BLAST){
            HullDamage.shake(g,1);HullDamage.shake(g,1);HullDamage.shake(g,1);
            g.haptic(Game.HAPTIC_IMPACT);g.sounds.fireworkBurst();
            for(int k=-1;k<=1;k++)g.smoke.equipment(g,b,k*b.half*.7f,0,16/ArticulatedModel.Z_SCALE);
            g.tell("TUG DESTROYED!");
        }
        for(int k=1;k<BLASTS.length;k++)if(t==BLAST+BLASTS[k]){HullDamage.shake(g,.8f);g.haptic(Game.HAPTIC_IMPACT);}
        if(t>BLAST && t<SUNK && t%4==0)g.smoke.equipment(g,b,((t/4)%3-1)*b.half*.4f,0,(18+(t%12))/ArticulatedModel.Z_SCALE);
        for(int c:CHAIN)if(t==c){HullDamage.shake(g,.65f);g.haptic(Game.HAPTIC_IMPACT);g.sounds.fireworkBurst();g.smoke.equipment(g,b,0,0,12/ArticulatedModel.Z_SCALE);}
        if(t==SUNK-25){HullDamage.shake(g,.8f);g.haptic(Game.HAPTIC_IMPACT);g.sounds.splash(1);}
    }
    /** Slow push-in toward the wreck. */
    static float zoom(Game g){return Math.min(4.2f,g.deathZoom*(1+.75f*ease(Math.min(1,g.deathTicks/(float)SUNK))));}
    static boolean skippable(Game g){return g.deathTicks>=SKIP;}

    private static float ease(float u){return u*u*(3-2*u);}
    private static float rand(int k,int salt){int h=(SEED*7919+k*104729+salt*31337)*0x45d9f3b;h=(h^(h>>>16))*0x45d9f3b;h^=h>>>16;return (h&65535)/65535f;}
    private static float flicker(int t,int k){return .7f+.3f*(float)StrictMath.sin(t*.55f+k*1.7f)*(float)StrictMath.sin(t*.31f+k*2.9f);}
    private static int mix(int a,int b,float u){
        u=Physics.clamp(u,0,1);int r=(int)(((a>>16)&255)+(((b>>16)&255)-((a>>16)&255))*u),gg=(int)(((a>>8)&255)+(((b>>8)&255)-((a>>8)&255))*u),bb=(int)((a&255)+((b&255)-(a&255))*u);
        return 0xff000000|r<<16|gg<<8|bb;
    }
    private static void glow(Painter p,float x,float y,float r,int color,float alpha){if(alpha>0.003f)p.material(x,y,r*2,0,0,r*2*.8f,EffectMaterial.GLOW,0,1,SoftCue.alpha(color,Math.min(1,alpha)));}

    static void water(Game g,Painter p){
        if(!g.dying() || g.deathTicks<BLAST)return;Body b=g.tug;
        HullDamage.wreckWater(g,p,g.layout.camera,b.x,b.y,b.half,b.radius,SEED,wreckTicks(g.deathTicks));
    }
    /** World layer. */
    static void effects(Game g,Painter p){
        if(!g.dying())return;
        int t=g.deathTicks;Body b=g.tug;RiverCamera c=g.layout.camera;
        float s=c.scale,zs=ArticulatedModel.Z_SCALE*s,hull=b.half+b.radius,sx=c.screenX(b.x,b.y),sy=c.screenY(b.x,b.y);
        float ca=(float)StrictMath.cos(b.angle),sa=(float)StrictMath.sin(b.angle);
        // Shared wreck art (rings, geyser, boil, slick, its own slow-motion pops), stretched over the sink.
        if(t>=BLAST)HullDamage.wreck(g,p,c,b.x,b.y,b.half,b.radius,SEED,false,wreckTicks(t),false);
        float sinking=t<BLAST?0:Math.min(1,(t-BLAST)/(float)(SUNK-BLAST));
        // Act 1: breach. Spark fountains and small pops.
        if(t<BLAST+20){
            float heat=Math.min(1,t/(float)BLAST);
            for(int k=0;k<10;k++){
                int age=(t*3+k*11)%28;if(t>=BLAST && age>t-BLAST)continue;
                float u=age/28f,a=rand(k,1)*6.283f+t*.02f,v=(30+rand(k,2)*50)*(.5f+heat),vz=50+rand(k,3)*60;
                float along=(rand(k,4)-.5f)*b.half*1.4f,ox=b.x+ca*along,oy=b.y+sa*along;
                float x=ox+(float)StrictMath.cos(a)*v*u*.6f,y=oy+(float)StrictMath.sin(a)*v*u*.6f,z=10+vz*u*.6f-40*u*u;
                float x0=x-(float)StrictMath.cos(a)*v*.03f,y0=y-(float)StrictMath.sin(a)*v*.03f;
                float px=c.screenX(x,y),py=c.screenY(x,y)-z*zs,qx=c.screenX(x0,y0),qy=c.screenY(x0,y0)-(z-2)*zs;
                p.line(qx,qy,px,py,2.2f*s,SoftCue.alpha(0xffff8a2b,(1-u)*heat));p.line(qx,qy,px,py,.9f*s,SoftCue.alpha(0xfffff3c0,(1-u)*heat));
            }
            for(int i=0;i<PRE_POPS.length;i++)HullDamage.explosion(p,c,b.x+ca*(i%2==0?1:-1)*b.half*.4f,b.y+sa*(i%2==0?1:-1)*b.half*.4f,hull,SEED+10+i,false,t-PRE_POPS[i],i+2,.45f,sx,sy,zs);
        }
        // Flames along the deck, growing in act 1, roaring through act 3, drowned at the plunge.
        float fire=t<BLAST?Math.min(1,t/(float)BLAST)*.7f:1-Physics.clamp((t-(SUNK-40))/40f,0,1);
        // The shared woodpile fire (FireArt), planted along the deck and riding it down.
        if(fire>0)for(int k=0;k<FLAMES;k++){
            float along=((k+.5f)/FLAMES-.5f)*b.half*1.5f,x=b.x+ca*along,y=b.y+sa*along;
            FireArt.burn(g,p,x,y,12*ArticulatedModel.Z_SCALE*(1-sinking*.7f),.3f,fire,Math.min(1,fire*1.5f),SEED+k,FlameArt.INSTANCES,6);
        }
        // Embers drifting up out of the fire.
        if(fire>0)for(int k=0;k<EMBERS;k++){
            int period=50+(int)(rand(k,5)*40),age=(t+k*17)%period;float u=age/(float)period;
            float along=(rand(k,6)-.5f)*b.half*1.6f,x=b.x+ca*along+(float)StrictMath.sin(t*.05f+k)*8*u,y=b.y+sa*along;
            float z=10+u*(90+rand(k,7)*80),px=c.screenX(x,y)+u*22*s,py=c.screenY(x,y)-z*zs;
            p.circle(px,py,(1.6f-u)*s+.4f,SoftCue.alpha(mix(0xffffd070,0xffff3a10,u),(1-u)*fire));
        }
        // Act 2: the blast.
        int a=t-BLAST;
        if(a>=0){
            for(int i=0;i<BLASTS.length;i++){
                float off=i==0?0:(rand(i,8)-.5f)*b.half*1.4f;
                HullDamage.explosion(p,c,b.x+ca*off,b.y+sa*off,hull,SEED+20+i,false,a-BLASTS[i],i+7,i==0?2.6f:1.6f,c.screenX(b.x+ca*off,b.y+sa*off),c.screenY(b.x+ca*off,b.y+sa*off),zs);
            }
            // Rising fireball column cooling into smoke.
            if(a<150){
                float u=a/150f;
                for(int k=0;k<5;k++){
                    float rise=(30+k*26)*ease(Math.min(1,a/60f))+u*40,r=(hull*(1.1f-k*.12f)+u*30)*s;
                    int col=mix(k<2?0xffffc060:0xffff6a20,0xff2a2624,Math.min(1,u*1.6f+k*.1f));
                    glow(p,sx+(float)StrictMath.sin(k*1.3f+a*.03f)*6*s,sy-rise*zs,r*1.6f,col,(1-u)*.85f);
                }
            }
            for(int k=0;k<3;k++){
                int age=a-k*7;if(age<0 || age>=70)continue;
                float u=age/70f,r=hull*(1+(1-(1-u)*(1-u))*(10-k*2))*s;
                p.material(sx,sy,r*2,0,0,r*2*RiverCamera.DEPTH,EffectMaterial.RING,.035f,1,SoftCue.alpha(k==0?0xffffe6b8:0xffffffff,(1-u)*(1-u)*.85f));
            }
            if(a<50)glow(p,sx,sy,hull*7*s,0xffffa040,(1-a/50f)*.9f);
            debris(g,p,c,b,a,s,zs);
            // Chain of secondary explosions as the fire reaches fuel and stores.
            for(int i=0;i<CHAIN.length;i++){
                float off=(rand(i,9)-.5f)*b.half*1.2f;
                HullDamage.explosion(p,c,b.x+ca*off,b.y+sa*off,hull,SEED+40+i,false,t-CHAIN[i],i+12,1-i*.08f,c.screenX(b.x+ca*off,b.y+sa*off),c.screenY(b.x+ca*off,b.y+sa*off),zs);
            }
        }
        // Burning slick after the plunge.
        int burn=t-(SUNK-30);
        if(burn>0 && burn<260){
            float fade=Math.min(1,burn/30f)*(1-Physics.clamp((burn-140)/120f,0,1));
            for(int k=0;k<9;k++){
                float ang=rand(k,10)*6.283f,dist=(rand(k,11)*.9f+.2f)*hull*(1+burn/200f),x=b.x+(float)StrictMath.cos(ang)*dist,y=b.y+(float)StrictMath.sin(ang)*dist;
                FireArt.burn(g,p,x,y,2,.16f,.75f,fade,SEED+20+k,3,0);
            }
        }
    }
    /** Hull pieces thrown high; they splash down, float and burn out. */
    private static void debris(Game g,Painter p,RiverCamera c,Body b,int a,float s,float zs){
        for(int k=0;k<DEBRIS;k++){
            float ang=rand(k,12)*6.283f,v=60+rand(k,13)*120,vz=90+rand(k,14)*150,grav=110;
            float flight=2*vz/grav,sec=a/60f,fs=Math.min(sec,flight);
            float d=v*fs*(1-fs/(flight*2.2f)),drift=sec>flight?(sec-flight)*6:0;
            float x=b.x+(float)StrictMath.cos(ang)*(d+drift),y=b.y+(float)StrictMath.sin(ang)*(d+drift),z=Math.max(0,vz*fs-.5f*grav*fs*fs+6);
            float px=c.screenX(x,y),py=c.screenY(x,y)-z*zs;
            boolean landed=sec>=flight;float since=sec-flight;
            if(landed && since>7)continue;
            if(landed && since<.8f){float u=since/.8f,r=(5+u*14)*s;p.material(c.screenX(x,y),c.screenY(x,y),r*2,0,0,r*2*RiverCamera.DEPTH,EffectMaterial.RING,.06f,1,SoftCue.alpha(0xffeaf7ff,(1-u)*.7f));}
            float spin=landed?k:a*(.12f+rand(k,15)*.2f)+k,len=(2.4f+rand(k,16)*3.4f)*s,w=(1+rand(k,17)*1.2f)*s,cx=(float)StrictMath.cos(spin),cy=(float)StrictMath.sin(spin);
            float fade=landed?1-Physics.clamp((since-5)/2f,0,1):1;
            int color=k%3==0?0xff7a2a1a:k%3==1?0xff2a2a2e:0xff9a6a3a;
            p.convexQuad(px-cx*len-cy*w,py-cy*len+cx*w,px+cx*len-cy*w,py+cy*len+cx*w,px+cx*len+cy*w,py+cy*len-cx*w,px-cx*len+cy*w,py-cy*len-cx*w,SoftCue.alpha(color,fade));
            // Most pieces burn: a trail while flying, a small fire once floating.
            if(k%4!=3){float burnOut=landed?1-Physics.clamp(since/(3+rand(k,18)*4),0,1):1;
                if(landed)FireArt.burn(g,p,x,y,1,.09f,.6f,burnOut*fade,SEED+40+k,2,0);
                else{float f=flicker(a,k+40);glow(p,px,py,5*s*(.7f+.4f*f),0xffff7a2a,.75f*fade);glow(p,px,py,2.4f*s,0xffffe0a0,.9f*fade*f);}}
        }
    }

    /** Screen layer: blast flash, letterbox bars, then GAME OVER. */
    static void overlay(Game g,Painter p){
        if(!g.dying() || g.mode!=Game.PLAY)return;
        int t=g.deathTicks;float w=g.layout.width,h=g.layout.height;
        // Red alarm pulse at the screen edges during the breach.
        if(t<BLAST){float u=(float)StrictMath.max(0,StrictMath.sin(t*.35f))*Math.min(1,t/30f)*.35f;
            p.rect(0,0,w,18,SoftCue.alpha(0xffff2a1a,u));p.rect(0,h-18,w,18,SoftCue.alpha(0xffff2a1a,u));p.rect(0,0,18,h,SoftCue.alpha(0xffff2a1a,u));p.rect(w-18,0,18,h,SoftCue.alpha(0xffff2a1a,u));}
        int a=t-BLAST;
        if(a>=0 && a<20)p.rect(0,0,w,h,SoftCue.alpha(0xfffff6e8,(1-a/20f)*.85f));
        if(a>=0 && a<45)p.rect(0,0,w,h,SoftCue.alpha(0xffff7a20,(1-a/45f)*.18f));
        // Cinematic bars.
        float bars=ease(Physics.clamp((t-BLAST*.5f)/60f,0,1))*h*.09f;
        if(bars>0){p.rect(0,0,w,bars,0xff000000);p.rect(0,h-bars,w,bars,0xff000000);}
        if(t<OVERLAY)return;
        int o=t-OVERLAY;float fade=ease(Physics.clamp(o/120f,0,1));
        p.rect(0,bars,w,h-2*bars,SoftCue.alpha(0xff060103,.62f*fade));
        float cy=h*.46f,pulse=.75f+.25f*(float)StrictMath.sin(o*.08f);
        p.material(w*.5f,cy,w*.9f,0,0,h*.55f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffb01810,.45f*fade*pulse));
        float band=ease(Physics.clamp((o-10)/30f,0,1));
        p.rect(0,cy-62*band,w,124*band,SoftCue.alpha(0xff2a0406,.75f*fade));
        p.rect(0,cy-62*band,w,2,SoftCue.alpha(0xffff4a3a,.8f*band));p.rect(0,cy+62*band-2,w,2,SoftCue.alpha(0xffff4a3a,.8f*band));
        // Letters drop in one at a time with a bounce.
        float size=Math.min(100,w*.095f),total=Lettering.width(TITLE,size),x=w*.5f-total*.5f;
        for(int i=0;i<TITLE.length();i++){
            String ch=TITLE.substring(i,i+1);float cw=Lettering.width(ch,size);
            int la=o-30-i*LETTERS;
            if(la>=0 && ch.charAt(0)!=' '){
                float u=Math.min(1,la/14f),drop=u<1?-(1-u)*(1-u)*h*.45f:(float)(StrictMath.abs(StrictMath.sin((la-14)*.45f))*StrictMath.exp(-(la-14)*.18f))*-14;
                int alpha=Math.round(255*Math.min(1,la/6f));float ly=cy-size*.55f+drop;
                Lettering.draw(p,ch,x+5,ly+5,size,(alpha*3/4<<24)|0x0a0103);
                Lettering.draw(p,ch,x,ly,size,(alpha<<24)|0xff4a3a);
                if(la>=14 && la<26)Lettering.draw(p,ch,x,ly,size,(Math.round(255*(1-(la-14)/12f))<<24)|0xffe0c8);
            }
            x+=cw;
        }
        int sub=o-30-TITLE.length()*LETTERS-10;
        if(sub>0)Lettering.center(p,"YOUR TUG WAS DESTROYED",w*.5f,cy+size*.62f,Math.min(22,w*.02f),(Math.round(255*Math.min(1,sub/20f))<<24)|0xffe0c8);
        if(t>=SKIP){float blink=.55f+.45f*(float)StrictMath.sin((t-SKIP)*.12f);
            Lettering.center(p,"TAP TO CONTINUE",w*.5f,h-bars-44,14,SoftCue.alpha(0xffffe0c8,blink*Math.min(1,(t-SKIP)/20f)));}
    }
}
