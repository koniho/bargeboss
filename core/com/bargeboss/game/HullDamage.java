package com.bargeboss.game;

/** Health of every watercraft other than the player's tug (whose hull stays in {@link Game#hull}),
 * the player's property-damage record, and the sinking sequence.
 *
 * Rules (shared by every level):
 * - A hard contact (closing speed at least {@link #HARD}, the tug's own threshold) costs a vessel
 *   health by hidden hit points (see {@link #TUG_HP}), at most once per {@link #COOLDOWN} ticks. Practice tutorials take no damage.
 * - Record: own = hull the tug lost; others = health every other vessel lost plus hard hits on
 *   moored marina boats.
 * - At 0 health a vessel sinks over {@link #SINK} ticks (flash, shake, smoke, ripples, lowered under
 *   its waterline), then leaves play. Mission cargo is required, so a sunk cargo vessel fails the
 *   mission FAIL_DELAY ticks after it goes under. */
final class HullDamage {
    private HullDamage(){}
    static final int COOLDOWN=75,SINK=150;
    /** Hidden hit points. A collision deals the same raw damage (HP) to whatever it hits, scaled by
     * closing speed; each hull's pool turns that into the health fraction its bar shows. The player
     * only ever sees fractions, never numbers. A big tug shrugs off what guts a runabout. */
    static final float TUG_HP=240,BARGE_HP=600,YACHT_HP=300,LIGHT_HP=70;
    /** Raw impact damage in hit points at a closing speed (none below BUMP_SPEED). */
    static float raw(float closing){return closing<BUMP_SPEED?0:10+(closing-BUMP_SPEED);}
    /** Health percent a hull with `hp` hit points loses from a hit at `closing` (at least 1 once it counts). */
    static int percent(float closing,float hp){return closing<BUMP_SPEED?0:Math.max(1,Math.round(raw(closing)*100/hp));}
    static float hp(Body b){return b.racing || b.cruiser?LIGHT_HP:b.yacht?YACHT_HP:BARGE_HP;}
    static float hp(MooredFleet.FloatBoat m){return 40+m.berth.length;}
    /** Light craft register a ram at lower closing speeds than working hulls: traffic from SMASH_SPEED,
     * moored marina boats from BUMP_SPEED, with damage scaled by how hard the hit was. */
    static final float SMASH_SPEED=40,BUMP_SPEED=30;
    static final int MOORED_COOLDOWN=40;
    static final float HARD=78;
    /** Health shown as recently lost (white chip) for this many ticks after a hit. */
    static final int CHIP=40;

    /** One new contact between a and b (b may be null for walls) at a closing speed. */
    static void contact(Game g,Watercraft a,Watercraft b,float closing){
        if(g.mode!=Game.PLAY || g.tutorial!=null || !(closing>=SMASH_SPEED))return;
        // Harbor traffic only takes damage the player causes; skippers bumping each other shrug it off.
        boolean hitA=(traffic(a)?CollisionSplashes.involvesPlayer(g,b):closing>=HARD) && hit(g,a,closing),hitB=(traffic(b)?CollisionSplashes.involvesPlayer(g,a):closing>=HARD) && hit(g,b,closing);
        // The captain winces when the tug or a lined vessel damages someone else's hull.
        if(hitA && CollisionSplashes.involvesPlayer(g,b) || hitB && CollisionSplashes.involvesPlayer(g,a))PlayerPortrait.wince(g);
    }
    private static boolean traffic(Watercraft w){return w instanceof Body && ((Body)w).traffic;}
    private static boolean hit(Game g,Watercraft w,float closing){
        if(!(w instanceof Body) || w==g.tug)return false;
        Body b=(Body)w;
        if(!b.active || b.sinkTicks>0 || b.health<=0 || g.ticks-b.damagedTick<COOLDOWN)return false;
        // Parked traffic hulls (a police boat, impounded or ticketed boats awaiting a tow) cannot be wrecked.
        if(b.traffic && b.awaitingPickup)return false;
        b.healthBefore=b.health;b.health=Math.max(0,b.health-percent(closing,hp(b)));b.damagedTick=g.ticks;
        g.othersDamage+=b.healthBefore-b.health;
        if(b.traffic)g.harborTraffic.rammed(b);
        if(b.health==0)sink(g,b);
        return true;
    }
    /** A hard hit on someone's moored marina boat: same health rules as every other vessel. */
    static void moored(Game g,MooredFleet.FloatBoat m,float closing){moored(g,m,null,closing);}
    /** As above; a hit by harbor traffic (by) costs nothing, since the player did not cause it. */
    static void moored(Game g,MooredFleet.FloatBoat m,Body by,float closing){
        if(by!=null && by.traffic)return;
        if(g.mode!=Game.PLAY || g.tutorial!=null || !(closing>=BUMP_SPEED) || !m.afloat() || m.health<=0 || g.ticks-m.damagedTick<MOORED_COOLDOWN)return;
        // A solid bump dents it; a full-speed ram caves it in.
        int damage=percent(closing,hp(m));
        m.healthBefore=m.health;m.health=Math.max(0,m.health-damage);m.damagedTick=g.ticks;g.mooredDamageTick=g.ticks;
        shake(g,Math.min(.5f,.15f+damage*.01f));
        g.othersDamage+=m.healthBefore-m.health;PlayerPortrait.wince(g);
        if(m.health>0)return;
        m.sinkTicks=1;shake(g,1);g.haptic(Game.HAPTIC_IMPACT);g.tell("BOAT SINKING!");
    }
    /** A marina wreck's fixed-step sinking: drifts to a stop, vents smoke, pops, plunges; the slick lingers. */
    static void stepMoored(Game g,MooredFleet.FloatBoat m){
        if(m.sinkTicks>=SINK+SLICK)return;
        int t=++m.sinkTicks;
        if(t>=SINK)return;
        m.vx*=.95f;m.vy*=.95f;m.x+=m.vx*Physics.DT;m.y+=m.vy*Physics.DT;
        m.yaw+=(float)StrictMath.sin(t*.13f)*.004f*t/SINK;m.updateHull();
        float depth=sinkDepth(t,m.berth.width*.5f);
        if(t==2)for(int k=-1;k<=1;k++)wreckSmoke(g,m,k*.3f,10);
        if(t<PLUNGE && t%6==0)wreckSmoke(g,m,((t/6)%3-1)*.25f,14-depth*.4f);
        for(int pop:POPS)if(t==pop){shake(g,.55f);g.haptic(Game.HAPTIC_IMPACT);wreckSmoke(g,m,0,8);}
        if(t==PLUNGE){shake(g,.7f);g.haptic(Game.HAPTIC_IMPACT);}
    }
    private static void wreckSmoke(Game g,MooredFleet.FloatBoat m,float along,float height){
        float a=m.berth.angle+m.yaw,l=m.berth.length*along;
        g.smoke.shore(m.x+(float)StrictMath.cos(a)*l,m.y+(float)StrictMath.sin(a)*l,height,.8f);
    }
    static float sinkDepth(int ticks,float radius){float t=Math.min(1,ticks/(float)SINK);return t*t*(2*radius+26);}
    private static void sink(Game g,Body b){
        b.sinkTicks=1;
        if(g.tow.incoming(b)!=null || b.lineAttached)g.tow.detachBody(g,b);
        if(g.ropeBody==b){g.ropePhase=g.ropeTicks=0;g.ropeBody=null;g.ropeTowTarget=null;}
        g.abilities.forget(b);
        RiverCamera c=g.layout.camera;
        g.splashes.shake.hit(1,c.screenX(1,1)-c.screenX(0,0),c.screenY(1,1)-c.screenY(0,0),true);
        g.haptic(Game.HAPTIC_IMPACT);
        g.tell(b.cruiser || b.yacht?"BOAT SINKING!":"BARGE SINKING!");
    }
    /** Secondary explosions while the hull goes down, and the final plunge. */
    static final int[] POPS={42,84};
    static final int PLUNGE=SINK-24,SLICK=240,FAIL_DELAY=45;
    private static final int[] BARGE_DEBRIS={0xff7a4a32,0xff4a3426,0xff9c6a46,0xffb0562c,0xff5d5048},CRAFT_DEBRIS={0xffe9ecef,0xffb9c3cc,0xff2f5d8a,0xff8a96a3,0xff4b4f55};
    /** Fixed-step sinking: the hull loses way and rolls, vents smoke, pops twice, plunges, then leaves
     * play; its oil slick lingers for SLICK ticks afterwards. */
    static void step(Game g){
        for(Body b:g.cargo)step(g,b,true);
        for(HarborTraffic.Craft c:g.harborTraffic.craft)step(g,c.body,false);
    }
    /** One hull's sinking step; only required (cargo) hulls fail the mission when they go down. */
    private static void step(Game g,Body b,boolean required){
        {
            if(b==null || b.sinkTicks<=0)return;
            if(!b.active){
                if(b.sunk && b.sinkTicks<SINK+SLICK)b.sinkTicks++;
                // Hold the fail screen a beat so the wreck's slick and boil are seen.
                if(required && b.sunk && !b.deliveryRecorded && b.sinkTicks>=SINK+FAIL_DELAY)g.cargoSunk=true;
                return;
            }
            int t=++b.sinkTicks;
            b.vx*=.95f;b.vy*=.95f;
            // Listing roll: the stricken hull swings, harder as it settles.
            b.spin=b.spin*.9f+(float)StrictMath.sin(t*.13f)*.35f*progress(b);
            if(t==2)for(int k=-1;k<=1;k++)g.smoke.equipment(g,b,k*b.half*.6f,0,10/ArticulatedModel.Z_SCALE);
            if(t<PLUNGE && t%6==0){
                float side=((t/6)%3-1)*b.half*.5f;
                g.smoke.equipment(g,b,side,0,(14-sinkDepth(b)*.4f)/ArticulatedModel.Z_SCALE);
            }
            for(int pop:POPS)if(t==pop){shake(g,.55f);g.haptic(Game.HAPTIC_IMPACT);g.smoke.equipment(g,b,0,0,8/ArticulatedModel.Z_SCALE);}
            if(t==PLUNGE){shake(g,.7f);g.haptic(Game.HAPTIC_IMPACT);}
            if(t>=SINK){b.active=false;b.sunk=true;}
        }
    }
    static void shake(Game g,float power){
        RiverCamera c=g.layout.camera;g.splashes.shake.hit(power,c.screenX(1,-1)-c.screenX(0,0),c.screenY(1,-1)-c.screenY(0,0),true);
    }
    static float progress(Body b){return b.sinkTicks<=0?0:Math.min(1,b.sinkTicks/(float)SINK);}
    /** World units the hull has settled below its float line (eased; most of the drop comes late). */
    static float sinkDepth(Body b){float t=progress(b);return t*t*(2*b.radius+26);}

    private static float rand(int id,int k,int salt){int h=(id*7919+k*104729+salt*31337)*0x45d9f3b;h=(h^(h>>>16))*0x45d9f3b;h^=h>>>16;return (h&65535)/65535f;}

    /** Fireball, debris, sparks, pops, plunge geyser, rings, bubbles, slick and caption, over the world. */
    static void effects(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        for(Body b:g.cargo){
            if(b==null || b.sinkTicks<=0 || !b.active && !b.sunk)continue;
            wreck(g,p,c,b.x,b.y,b.half,b.radius,b.id,b.cruiser || b.yacht,b.sinkTicks);
        }
        for(HarborTraffic.Craft t:g.harborTraffic.craft){
            Body b=t.body;
            if(b.sinkTicks<=0 || !b.active && !b.sunk)continue;
            wreck(g,p,c,b.x,b.y,b.half,b.radius,b.id,true,b.sinkTicks);
        }
        if(g.montroseStage())for(MooredFleet.FloatBoat m:g.mooredFleet.boats)if(m.sinkTicks>0 && m.sinkTicks<SINK+SLICK)
            wreck(g,p,c,m.x,m.y,m.berth.length*.5f,m.berth.width*.5f,MOORED_SEED+m.berth.frame+(int)m.berth.x,true,m.sinkTicks);
    }
    /** Seed offset so marina wrecks never share debris patterns with mission vessels. */
    static final int MOORED_SEED=100000;
    /** Water effects finish before hulls: expanding wakes never bleach an adjacent vessel. */
    static void water(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        for(Body b:g.cargo)if(b!=null && b.sinkTicks>0 && (b.active || b.sunk))wreckWater(g,p,c,b.x,b.y,b.half,b.radius,b.id,b.sinkTicks);
        for(HarborTraffic.Craft craft:g.harborTraffic.craft){Body b=craft.body;if(b.sinkTicks>0 && (b.active || b.sunk))wreckWater(g,p,c,b.x,b.y,b.half,b.radius,b.id,b.sinkTicks);}
        if(g.montroseStage())for(MooredFleet.FloatBoat m:g.mooredFleet.boats)if(m.sinkTicks>0 && m.sinkTicks<SINK+SLICK)
            wreckWater(g,p,c,m.x,m.y,m.berth.length*.5f,m.berth.width*.5f,MOORED_SEED+m.berth.frame+(int)m.berth.x,m.sinkTicks);
        TugDeath.water(g,p);
    }
    static void wreckWater(Game g,Painter p,RiverCamera c,float bx,float by,float half,float radius,int seed,int t){
        if(t<=0 || t>=SINK+SLICK)return;
        float sx=c.screenX(bx,by),sy=c.screenY(bx,by),s=c.scale,hull=half+radius,reach=hull*5.2f;
        if(sx+reach*s<0 || sx-reach*s>g.layout.width || sy+reach*s<0 || sy-reach*s>g.layout.height)return;
        p.pushClipOutside(ShoreOcclusion.region(g,c,bx-reach,by-reach,bx+reach,by+reach,Float.NEGATIVE_INFINITY,p.supportsUnionClips()));
        try{
            // Lingering oil slick: a dark sheen with a faint rainbow rim, spreading and thinning.
            float slick=Physics.clamp((t-PLUNGE)/40f,0,1)*(1-Physics.clamp((t-SINK-SLICK+80)/80f,0,1));
            if(slick>0){
                float r=hull*(1.1f+.9f*Physics.clamp((t-PLUNGE)/(float)(SLICK),0,1))*s;
                p.material(sx,sy,r*2.6f,0,0,r*2.6f*RiverCamera.DEPTH,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xff05080c,.55f*slick));
                p.material(sx-r*.17f,sy,r*1.7f,0,r*.2f,r*.85f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xff698a80,.12f*slick));
                p.material(sx+r*.22f,sy-r*.08f,r*1.1f,0,-r*.2f,r*.55f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xff826d96,.1f*slick));
            }
            // Rings: a wave every 24 ticks while going down, a big one at the plunge.
            for(int k=0;k<7;k++){
                int age=t-k*24;if(age<0 || age>=60 || k*24>PLUNGE)continue;
                float u=age/60f,r=hull*(.8f+(1-(1-u)*(1-u))*2.2f)*s;
                p.spriteAffine(SpriteAtlas.RIPPLES,Math.floorMod(seed+k,4),sx,sy,r*2.15f,0,0,r*1.85f*RiverCamera.DEPTH,(1-u)*(1-u)*.42f);
            }
            int plunge=t-PLUNGE;
            if(plunge>=0 && plunge<70){
                float u=plunge/70f,r=hull*(1+(1-(1-u)*(1-u))*3.6f)*s;
                p.spriteAffine(SpriteAtlas.RIPPLES,Math.floorMod(seed+3,4),sx,sy,r*2.1f,0,0,r*2*RiverCamera.DEPTH,(1-u)*(1-u)*.65f);
            }
        }finally{p.popClip();}
    }
    /** One sinking or sunk hull's effects at sink tick t; half and radius are its hull extents. */
    static void wreck(Game g,Painter p,RiverCamera c,float bx,float by,float half,float radius,int seed,boolean craft,int t){wreck(g,p,c,bx,by,half,radius,seed,craft,t,true);}
    static void wreck(Game g,Painter p,RiverCamera c,float bx,float by,float half,float radius,int seed,boolean craft,int t,boolean caption){
        {
            float sx=c.screenX(bx,by),sy=c.screenY(bx,by),s=c.scale,hull=half+radius;
            float pad=(hull+160)*s;
            if(sx<-pad || sx>g.layout.width+pad || sy<-pad || sy>g.layout.height+pad)return;
            float zs=ArticulatedModel.Z_SCALE*s;
            int plunge=t-PLUNGE;
            // Air boiling up around the settling hull (and over the wreck for a while after).
            float boil=t<SINK?1:1-Physics.clamp((t-SINK)/120f,0,1);
            if(boil>0)for(int k=0;k<12;k++){
                int age=(t+k*13)%40;float u=age/40f,a=k*2.39996f;
                float px=bx+(float)StrictMath.cos(a)*(half*.8f)*((k%3+1)/3f),py=by+(float)StrictMath.sin(a)*(radius*.9f);
                float size=(2.5f+u*5)*s;
                p.spriteAffine(SpriteAtlas.SPLASH,k%4,c.screenX(px,py),c.screenY(px,py)-u*6*s,size*2,0,0,size*1.4f,(1-u)*.7f*boil);
            }
            if(t>=SINK)return;
            // Opening blast: fireball, debris and sparks, then two smaller pops.
            explosion(p,c,bx,by,hull,seed,craft,t,0,1,sx,sy,zs);
            for(int i=0;i<POPS.length;i++)explosion(p,c,bx,by,hull,seed,craft,t-POPS[i],i+1,.5f,sx,sy,zs);
            // Plunge geyser: a column of spray thrown up as the hull goes under.
            if(plunge>=0 && plunge<26){
                float u=plunge/26f;
                for(int k=0;k<9;k++){
                    float h=(1-(1-u)*(1-u))*(40+rand(seed,k,7)*45)*(1-u*.35f),ox=(rand(seed,k,8)-.5f)*hull*1.1f,oy=(rand(seed,k,9)-.5f)*radius;
                    float px=c.screenX(bx+ox,by+oy),py=c.screenY(bx+ox,by+oy)-h*zs,size=(7+rand(seed,k,10)*7)*s*(1+u);
                    p.spriteAffine(SpriteAtlas.SPLASH,k%4,px,py,size*1.4f,-size*.8f,size*.6f,size*1.2f,(1-u)*.9f);
                }
            }
            // Arcade caption: SUNK! pops in, floats up and fades.
            if(caption && t>=PLUNGE-10){
                int a=t-(PLUNGE-10);float u=Physics.clamp(a/34f,0,1),pop=a<6?1.5f-a/12f:1;
                float size=Math.min(34,Math.max(18,22*s))*pop,y=sy-(46+u*30)*s;
                int alpha=Math.round(255*(1-Physics.clamp((a-18)/16f,0,1)));
                Lettering.center(p,"SUNK!",sx+1.5f,y+1.5f,size,(alpha*3/4<<24)|0x140608);
                Lettering.center(p,"SUNK!",sx,y,size,(alpha<<24)|0xff5a3c);
            }
        }
    }
    /** One blast `age` ticks old (none before 0): scale 1 is the opening explosion. */
    static void explosion(Painter p,RiverCamera c,float bx,float by,float hull,int seed,boolean craft,int age,int salt,float scale,float sx,float sy,float zs){
        if(age<0 || age>=70)return;
        float s=c.scale,sec=age/60f;
        if(age<12){
            float f=1-age/12f,size=hull*2.6f*scale*s*(1.25f-.25f*f);
            p.material(sx,sy-10*s,size,0,0,size*.75f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffff8a2b,f*.95f));
            p.material(sx,sy-12*s,size*.55f,0,0,size*.42f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffe6a8,f));
            if(age<5)p.material(sx,sy-12*s,size*.25f,0,0,size*.2f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffffff,1-age/5f));
        }
        // Brief broken amber rays release the initial impact into the longer ballistic debris.
        if(age<18){float u=age/18f,fade=(1-u)*(1-u);
            for(int k=0;k<6;k++){float a=k*1.0471976f+seed*.17f,dx=(float)StrictMath.cos(a),dy=(float)StrictMath.sin(a)*.68f;
                float inner=hull*(.35f+u*.9f)*scale*s,outer=inner+hull*.55f*(1-u)*scale*s;
                p.line(sx+dx*inner,sy-10*s+dy*inner,sx+dx*outer,sy-10*s+dy*outer,2.4f*s,SoftCue.alpha(0xffffb65f,fade*.8f));
                p.line(sx+dx*inner,sy-10*s+dy*inner,sx+dx*outer,sy-10*s+dy*outer,.9f*s,SoftCue.alpha(0xffffefd0,fade));
            }
        }
        int pieces=scale>=1?14:5;
        for(int k=0;k<pieces;k++){
            float a=rand(seed,k,salt*3+1)*6.2831855f,speed=(40+rand(seed,k,salt*3+2)*70)*scale,vz=(45+rand(seed,k,salt*3+3)*65)*scale;
            float z=8+vz*sec-48*sec*sec;if(z<0)continue;
            float x=bx+(float)StrictMath.cos(a)*speed*sec,y=by+(float)StrictMath.sin(a)*speed*sec;
            float px=c.screenX(x,y),py=c.screenY(x,y)-z*zs;
            if(k%3==0){
                // Spark: a short hot streak along its arc.
                float vx=(float)StrictMath.cos(a)*speed,vy=(float)StrictMath.sin(a)*speed,dz=vz-96*sec,dt=.04f;
                float tx=c.screenX(x-vx*dt,y-vy*dt),ty=c.screenY(x-vx*dt,y-vy*dt)-(z-dz*dt)*zs;
                float fade=1-age/70f;
                p.line(tx,ty,px,py,1.8f*s,SoftCue.alpha(0xffff9b53,fade));
                p.line(tx,ty,px,py,.65f*s,SoftCue.alpha(0xffffefb0,fade));
            }else{
                // Tumbling plank or plate in the hull's colours.
                float spin=age*(.25f+rand(seed,k,salt*3+4)*.35f)+k,len=(1.6f+rand(seed,k,salt*3+5)*2.6f)*s,w=(.7f+rand(seed,k,salt*3+6)*.9f)*s;
                float cx=(float)StrictMath.cos(spin),cy=(float)StrictMath.sin(spin),fade=Physics.clamp((70-age)/20f,0,1);
                // The vessel's own materials: fibreglass and trim for pleasure craft, rusted plate and timber for barges.
                int[] palette=craft?CRAFT_DEBRIS:BARGE_DEBRIS;int color=palette[k%palette.length];
                float ex=len+.25f*s,ew=w+.25f*s;
                p.convexQuad(px-cx*ex-cy*ew,py-cy*ex+cx*ew,px+cx*ex-cy*ew,py+cy*ex+cx*ew,px+cx*ex+cy*ew,py+cy*ex-cx*ew,px-cx*ex+cy*ew,py-cy*ex-cx*ew,SoftCue.alpha(0xff493f37,.8f*fade));
                p.convexQuad(px-cx*len-cy*w,py-cy*len+cx*w,px+cx*len-cy*w,py+cy*len+cx*w,px+cx*len+cy*w,py+cy*len-cx*w,px-cx*len+cy*w,py-cy*len-cx*w,SoftCue.alpha(p.lighting().scenery(color),fade));
            }
        }
    }
}
