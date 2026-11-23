package com.bargeboss.game;

/** Shared, pointer-owned equipment actions. Selection never alters tow topology. */
public final class BargeAbilities {
    public static final int NONE=0,FIREWORKS=1,FIRE=2,CAPACITY=320;
    public Body selected;
    public int owner=-1,heldTicks,visibleTicks,launches;
    public boolean spraying;
    private int pressed=-1,next,screenFlash;
    private float aimX,aimY,stickX,stickY,blend;
    private Body panelBody;
    static final float AIM_RADIUS=60;
    private final float[] x=new float[CAPACITY],y=new float[CAPACITY],z=new float[CAPACITY],vx=new float[CAPACITY],vy=new float[CAPACITY],vz=new float[CAPACITY];
    private final boolean[] rocket=new boolean[CAPACITY],flash=new boolean[CAPACITY];
    private final float[] travel=new float[CAPACITY];
    private final int[] age=new int[CAPACITY];
    private final int[] life=new int[CAPACITY],color=new int[CAPACITY];
    public void reset(){selected=panelBody=null;blend=0;cancel();visibleTicks=launches=next=screenFlash=0;for(int i=0;i<CAPACITY;i++)life[i]=0;}
    public void cancel(){owner=-1;pressed=-1;spraying=false;heldTicks=0;}
    public boolean reveal(Game g,Body b){
        if(b==null || !b.active || b.ability==NONE)return false;
        if(selected==b){selected=null;visibleTicks=0;cancel();g.haptic(Game.HAPTIC_TICK);return true;}
        cancel();selected=panelBody=b;visibleTicks=600;g.overview.cancelTouch();g.haptic(Game.HAPTIC_TICK);return true;
    }
    public float buttonX(Game g,int button){return buttonPoint(g,true);}
    public float buttonY(Game g){return buttonPoint(g,false);}
    private float buttonPoint(Game g,boolean horizontal){
        Layout.Box box=OverviewMap.bounds(g);return horizontal?box.centerX():box.y+102;
    }
    float opacity(){return blend*blend*(3-2*blend);}
    boolean replacesMap(){return selected!=null || blend>0;}
    private boolean available(Game g){return g.mode==Game.PLAY && (g.tutorial==null || g.tutorial.lesson<Tutorial.DONE);}
    private boolean visible(Game g){return available(g) && !g.conversation() && selected!=null && selected.active;}
    private boolean hit(Game g,float px,float py,int button){return selected!=null && selected.ability==FIREWORKS?Math.abs(px-buttonX(g,0))<=68 && Math.abs(py-buttonY(g))<=62:Physics.length(px-buttonX(g,0),py-buttonY(g))<=AIM_RADIUS+10;}
    public boolean down(Game g,int id,float px,float py){
        if(!available(g) || g.conversation() || !replacesMap())return false;
        Layout.Box box=OverviewMap.bounds(g);
        if(px<box.x || px>box.x+box.w || py<box.y || py>g.layout.height-4)return false;
        // This is HUD space even when a vessel happens to pass behind it.
        if(owner!=-1 || !visible(g) || !hit(g,px,py,0))return true;
        owner=id;pressed=0;heldTicks=0;stickX=stickY=0;
        aimX=selected.x+(float)StrictMath.cos(selected.angle)*180;
        aimY=selected.y+(float)StrictMath.sin(selected.angle)*180;
        if(selected.ability==FIRE)aim(g,px,py);
        return true;
    }
    private void aim(Game g,float px,float py){
        float dx=px-buttonX(g,0),dy=py-buttonY(g),len=Physics.length(dx,dy);
        if(len<4)return;
        float limited=Math.min(AIM_RADIUS,len);stickX=dx/len*limited;stickY=dy/len*limited;
        RiverCamera c=g.layout.camera;float cx=buttonX(g,0),cy=buttonY(g);
        float wx=c.worldX(cx+dx,cy+dy)-c.worldX(cx,cy),wy=c.worldY(cx+dx,cy+dy)-c.worldY(cx,cy);
        float world=Math.max(.001f,Physics.length(wx,wy)),range=Physics.clamp(limited/AIM_RADIUS*210,40,210);
        aimX=selected.x+wx/world*range;aimY=selected.y+wy/world*range;
    }
    public boolean move(Game g,int id,float px,float py){
        if(owner!=id)return false;
        if(selected!=null && selected.ability==FIRE)aim(g,px,py);
        return true;
    }
    public boolean up(Game g,int id,float px,float py){
        if(owner!=id)return false;
        if(selected!=null && selected.active && heldTicks<=18 && hit(g,px,py,pressed)){
            if(selected.ability==FIREWORKS && selected.abilityCooldown==0){selected.abilityTicks=180;selected.abilityCooldown=240;launches++;if(g.tutorial!=null)g.tutorial.fireworks.launched(g,selected);g.haptic(Game.HAPTIC_SUCCESS);}
        }
        cancel();return true;
    }
    public void step(Game g){
        if(screenFlash>0)screenFlash--;
        if(selected!=null && selected.active)visibleTicks=600;
        blend=Physics.clamp(blend+(selected!=null && selected.active?.1f:-.1f),0,1);
        if(selected!=null && !selected.active){selected=null;visibleTicks=0;cancel();}
        if(blend==0 && selected==null)panelBody=null;
        if(owner!=-1){heldTicks++;spraying=pressed==0 && selected!=null && selected.ability==FIRE && heldTicks>=12;}
        for(Body b:g.cargo)if(b!=null && b.active){
            if(b.abilityCooldown>0)b.abilityCooldown--;
            if(b.abilityTicks>0){
                if(b.abilityTicks%24==0){
                    burst(b,180-b.abilityTicks);g.sounds.firework();
                    // Deck-level launch cloud follows the boat only at emission.
                    for(int side=-1;side<=1;side++)g.smoke.equipment(g,b,side*7,0,12/ArticulatedModel.Z_SCALE);
                }
                if(b.abilityTicks%12==0)g.smoke.equipment(g,b,0,0,12/ArticulatedModel.Z_SCALE);
                b.abilityTicks--;
            }
        }
        if(spraying && selected!=null){
            if(Physics.length(stickX,stickY)>=4)aim(g,buttonX(g,0)+stickX,buttonY(g)+stickY);
            float dx=aimX-selected.x,dy=aimY-selected.y,len=Math.max(1,Physics.length(dx,dy));
            float range=Physics.clamp(len,40,210),flight=.9f;
            float targetX=selected.x+dx/len*range,targetY=selected.y+dy/len*range;
            for(int side=-1;side<=1;side+=2){
                float sx=BargeEquipment.anchorX(selected,side),sy=BargeEquipment.anchorY(selected,side);
                emit(sx,sy,22,(targetX-sx)/flight,(targetY-sy)/flight,30,70,0xffb9e9f4);
            }
            selected.abilityAim=(float)StrictMath.atan2(dy,dx);
            if(g.visualTicks%6==0)g.smoke.equipment(g,selected,-24,6,30.5f);
        }
        for(int i=0;i<CAPACITY;i++)if(life[i]>0){
            life[i]--;age[i]++;if(flash[i])continue;if(color[i]==0xffb9e9f4)travel[i]+=Physics.length(vx[i],vy[i])/60;
            x[i]+=vx[i]/60;y[i]+=vy[i]/60;z[i]+=vz[i]/60;vz[i]-=80f/60;
            if(color[i]==0xffb9e9f4 && z[i]>=GroundArt.LAND_RISE-2 && z[i]<=GroundArt.LAND_RISE+23 && g.crew.hit(g,x[i],y[i],vx[i],vy[i]))life[i]=0;
            if(life[i]>0 && color[i]==0xffb9e9f4 && z[i]<=GroundArt.LAND_RISE && g.tutorial!=null
                    && g.tutorial.fireworks.waterHit(x[i],y[i]))life[i]=0;
            if(z[i]<0)life[i]=0;
            if(life[i]==0 && rocket[i]){rocket[i]=false;explode(x[i],y[i],z[i],color[i]);screenFlash=8;g.sounds.fireworkBurst();}
        }
    }
    /** Brief warm exposure pulse; shared timing and a single rectangle behind HUD. */
    void drawScreenFlash(Game g,Painter p){
        if(screenFlash<=0)return;
        float t=screenFlash/8f;
        p.rect(0,0,g.layout.width,g.layout.height,SoftCue.alpha(0x38fff0c7,t*t));
    }
    int screenFlashTicks(){return screenFlash;}
    private void burst(Body b,int tick){
        int hue=(tick/24)%3;int tint=hue==0?0xffffd329:hue==1?0xff28e7ff:0xffff534b;
        emit(b.x,b.y,12,(hue-1)*34,(tick%5-2)*17,210,54,tint);rocket[(next-1)%CAPACITY]=true;
    }
    private void explode(float px,float py,float pz,int tint){
        for(int i=0;i<40;i++){
            double a=i*StrictMath.PI*2/40;float speed=85+(i%3)*22;
            emit(px,py,pz,(float)StrictMath.cos(a)*speed,(float)StrictMath.sin(a)*speed,40+(i%4)*18,110,tint);
        }
        emit(px,py,pz,0,0,0,28,tint);flash[(next-1)%CAPACITY]=true;
    }
    private void emit(float xx,float yy,float zz,float dx,float dy,float dz,int ticks,int tint){int i=next++%CAPACITY;rocket[i]=flash[i]=false;age[i]=0;travel[i]=0;x[i]=xx;y[i]=yy;z[i]=zz;vx[i]=dx;vy[i]=dy;vz[i]=dz;life[i]=ticks;color[i]=tint;}
    boolean alive(int i){return life[i]>0;}
    float depth(Game g,int i){return g.layout.camera.screenY(x[i],y[i]);}
    void drawParticle(Game g,Painter p,int i){
        if(life[i]<=0)return;RiverCamera c=g.layout.camera;
        float sx=c.screenX(x[i],y[i]),sy=c.screenY(x[i],y[i])-z[i]*ArticulatedModel.Z_SCALE*c.scale;
        if(sx< -120 || sx>g.layout.width+120 || sy< -120 || sy>g.layout.height+120)return;
        float fade=Math.min(1,life[i]/24f),size=(color[i]==0xffb9e9f4?2.5f+Math.min(210,travel[i])*.05f:11)*c.scale;
        float tail=Math.min(age[i]/60f,rocket[i]?.34f:.28f);
        float lift=z[i]*ArticulatedModel.Z_SCALE,px=x[i]+RiverCamera.riseX(lift),py=y[i]+RiverCamera.riseY(lift);
        float tz=z[i]-vz[i]*tail-40*tail*tail;
        float tx=x[i]-vx[i]*tail+RiverCamera.riseX(tz*ArticulatedModel.Z_SCALE),ty=y[i]-vy[i]*tail+RiverCamera.riseY(tz*ArticulatedModel.Z_SCALE);
        float extent=flash[i]?120:color[i]==0xffb9e9f4?40:18;
        float[] clip=ShoreOcclusion.region(g,c,Math.min(px,tx)-extent,Math.min(py,ty)-extent,Math.max(px,tx)+extent,Math.max(py,ty)+extent,depth(g,i));
        if(clip.length>0)p.pushClipOutside(clip);
        try{
            if(color[i]==0xffb9e9f4)p.spriteAffine(SpriteAtlas.SPLASH,i%4,sx,sy,size*3,0,0,size*2,fade*.6f);
            else if(flash[i]){
                float t=age[i]/28f,r=(12+100*t)*c.scale;
                p.material(sx,sy,r*2,0,0,r*2,EffectMaterial.RING,.018f,1,SoftCue.alpha(color[i],(1-t)*.7f));
                if(age[i]<8)p.material(sx,sy,85*c.scale,0,0,85*c.scale,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffffefba,(1-age[i]/8f)*.8f));
            }else{
                // Analytic ballistic history keeps long curved trails registered to each spark.
                float lastX=sx,lastY=sy;
                for(int part=1;part<=3;part++){
                    float dt=tail*part/3,zz=z[i]-vz[i]*dt-40*dt*dt;
                    float xx=c.screenX(x[i]-vx[i]*dt,y[i]-vy[i]*dt),yy=c.screenY(x[i]-vx[i]*dt,y[i]-vy[i]*dt)-zz*ArticulatedModel.Z_SCALE*c.scale;
                    float alpha=fade*(1-part*.22f),width=(rocket[i]?3.4f:2.2f)*c.scale*(1-part*.16f);
                    p.line(lastX,lastY,xx,yy,width+3*c.scale,SoftCue.alpha(color[i],alpha*.2f));
                    p.line(lastX,lastY,xx,yy,Math.max(.8f,width),SoftCue.alpha(color[i],alpha));
                    lastX=xx;lastY=yy;
                }
                p.material(sx,sy,size,0,0,size,EffectMaterial.GLOW,0,1,SoftCue.alpha(color[i],fade));
                p.circle(sx,sy,(rocket[i]?2.2f:1.5f)*c.scale,SoftCue.alpha(0xfffff4d2,fade));
                if(i%7==0){float r=3*c.scale;p.line(sx-r,sy,sx+r,sy,c.scale,SoftCue.alpha(color[i],fade));p.line(sx,sy-r,sx,sy+r,c.scale,SoftCue.alpha(color[i],fade));}
            }
        }finally{if(clip.length>0)p.popClip();}
    }
    /** Map and equipment share the same HUD slot; only the ten-tick transition draws both. */
    public void drawMap(Game g,Painter p){
        float alpha=1-opacity();if(alpha<=0)return;
        OverviewMap.draw(g,alpha>=1?p:new OpacityPainter(p,alpha));
    }
    public void drawUI(Game g,Painter p){
        if(!available(g) || g.conversation() || panelBody==null || blend<=0)return;
        p=new OpacityPainter(p,opacity());Layout.Box box=OverviewMap.bounds(g);
        float cx=buttonX(g,0),cy=buttonY(g);boolean fire=panelBody.ability==FIRE;
        PilotGlass.panel(p,box.x,box.y,box.w,g.layout.height-4-box.y,PilotGlass.FILL);
        Lettering.center(p,fire?"FIRE MONITOR":"FIREWORKS",cx,box.y+12,13,PilotGlass.TEXT);
        if(fire){
            SoftCue.ring(p,cx,cy,AIM_RADIUS,2.4f,0xcc28e7ff,1);
            SoftCue.ring(p,cx,cy,22,1,0x4059b8c8,1);
            for(int i=0;i<4;i++){double a=i*StrictMath.PI/2;float dx=(float)StrictMath.cos(a),dy=(float)StrictMath.sin(a);p.line(cx+dx*51,cy+dy*51,cx+dx*60,cy+dy*60,2,0xaa28e7ff);}
            float sx=owner>=0?stickX:0,sy=owner>=0?stickY:0;
            p.line(cx,cy,cx+sx,cy+sy,3,0xb828e7ff);
            p.circle(cx+sx,cy+sy,15,owner>=0?PilotGlass.AMBER:0xff224052);
            ArcadeIcons.draw(p,ArcadeIcons.WATER,cx+sx,cy+sy,.55f,owner>=0?PilotGlass.NAVY:PilotGlass.TEXT);
            Lettering.center(p,"HOLD + DRAG TO AIM",cx,box.y+174,9,PilotGlass.TEXT);
            Lettering.center(p,"DRAG FARTHER FOR REACH",cx,box.y+194,7,PilotGlass.MUTED);
        }else{
            boolean ready=panelBody.abilityCooldown==0;
            PilotGlass.button(p,cx-68,cy-62,136,124,ready);
            ArcadeIcons.draw(p,ArcadeIcons.FIREWORK,cx,cy-8,1.65f,ready?PilotGlass.NAVY:PilotGlass.MUTED);
            Lettering.center(p,ready?"LAUNCH":"RELOADING",cx,cy+37,12,ready?PilotGlass.NAVY:PilotGlass.MUTED);
            Lettering.center(p,"TAP TO CELEBRATE",cx,box.y+184,9,PilotGlass.TEXT);
        }
    }
}
