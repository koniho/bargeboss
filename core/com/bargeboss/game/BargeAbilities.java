package com.bargeboss.game;

/** Shared, pointer-owned equipment actions. Selection never alters tow topology. */
public final class BargeAbilities {
    public static final int NONE=0,FIREWORKS=1,FIRE=2,CAPACITY=320;
    public Body selected;
    public int owner=-1,heldTicks,visibleTicks,launches;
    public boolean spraying;
    private int pressed=-1,next;
    private float aimX,aimY;
    private final float[] x=new float[CAPACITY],y=new float[CAPACITY],z=new float[CAPACITY],vx=new float[CAPACITY],vy=new float[CAPACITY],vz=new float[CAPACITY];
    private final boolean[] rocket=new boolean[CAPACITY],flash=new boolean[CAPACITY];
    private final int[] age=new int[CAPACITY];
    private final int[] life=new int[CAPACITY],color=new int[CAPACITY];
    public void reset(){selected=null;cancel();visibleTicks=launches=next=0;for(int i=0;i<CAPACITY;i++)life[i]=0;}
    public void cancel(){owner=-1;pressed=-1;spraying=false;heldTicks=0;}
    public boolean reveal(Game g,Body b){
        if(b==null || !b.active || b.ability==NONE)return false;
        selected=b;visibleTicks=600;g.haptic(Game.HAPTIC_TICK);return true;
    }
    public float buttonX(Game g,int button){float sx=g.layout.camera.screenX(selected.x,selected.y);return Physics.clamp(sx,78,g.layout.width-78);}
    public float buttonY(Game g){return Physics.clamp(g.layout.camera.screenY(selected.x,selected.y)-82*g.layout.camera.scale,110,g.layout.height-110);}
    private boolean visible(Game g){return g.mode==Game.PLAY && !g.conversation() && selected!=null && selected.active && visibleTicks>0;}
    private boolean hit(Game g,float px,float py,int button){return Math.abs(px-buttonX(g,button))<=24 && Math.abs(py-buttonY(g))<=24;}
    public boolean down(Game g,int id,float px,float py){
        if(!visible(g) || owner!=-1 || g.pointer)return false;
        int which=hit(g,px,py,0)?0:-1;
        if(which<0)return false;
        owner=id;pressed=which;heldTicks=0;aimX=selected.x+(float)StrictMath.cos(selected.angle)*180;aimY=selected.y+(float)StrictMath.sin(selected.angle)*180;visibleTicks=600;return true;
    }
    public boolean move(Game g,int id,float px,float py){
        if(owner!=id)return false;
        if(selected.ability==FIRE && pressed==0){aimX=g.layout.camera.worldX(px,py);aimY=g.layout.camera.worldY(px,py);
            if(g.tutorial!=null && g.tutorial.fireworks.heat>0){
                RiverCamera c=g.layout.camera;float fx=c.screenX(FireworksLesson.X,FireworksLesson.Y),fy=c.screenY(FireworksLesson.X,FireworksLesson.Y)-GroundArt.LAND_RISE*c.scale;
                if(Math.abs(px-fx)<38*c.scale && py>fy-65*c.scale && py<fy+20*c.scale){aimX=FireworksLesson.X;aimY=FireworksLesson.Y;}
            }}
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
        if(visibleTicks>0 && owner==-1)visibleTicks--;
        if(selected!=null && !selected.active){selected=null;cancel();}
        if(owner!=-1){heldTicks++;spraying=pressed==0 && selected!=null && selected.ability==FIRE && heldTicks>=12;}
        for(Body b:g.cargo)if(b!=null && b.active){
            if(b.abilityCooldown>0)b.abilityCooldown--;
            if(b.abilityTicks>0){
                if(b.abilityTicks%24==0)burst(b,180-b.abilityTicks);
                b.abilityTicks--;
            }
        }
        if(spraying && selected!=null){
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
            life[i]--;age[i]++;if(flash[i])continue;x[i]+=vx[i]/60;y[i]+=vy[i]/60;z[i]+=vz[i]/60;vz[i]-=80f/60;
            if(color[i]==0xffb9e9f4 && z[i]<=GroundArt.LAND_RISE && g.tutorial!=null
                    && g.tutorial.fireworks.waterHit(x[i],y[i]))life[i]=0;
            if(z[i]<0)life[i]=0;
            if(life[i]==0 && rocket[i]){rocket[i]=false;explode(x[i],y[i],z[i],color[i]);}
        }
    }
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
    private void emit(float xx,float yy,float zz,float dx,float dy,float dz,int ticks,int tint){int i=next++%CAPACITY;rocket[i]=flash[i]=false;age[i]=0;x[i]=xx;y[i]=yy;z[i]=zz;vx[i]=dx;vy[i]=dy;vz[i]=dz;life[i]=ticks;color[i]=tint;}
    boolean alive(int i){return life[i]>0;}
    float depth(Game g,int i){return g.layout.camera.screenY(x[i],y[i]);}
    void drawParticle(Game g,Painter p,int i){
        if(life[i]<=0)return;RiverCamera c=g.layout.camera;
        float sx=c.screenX(x[i],y[i]),sy=c.screenY(x[i],y[i])-z[i]*ArticulatedModel.Z_SCALE*c.scale;
        if(sx< -120 || sx>g.layout.width+120 || sy< -120 || sy>g.layout.height+120)return;
        float fade=Math.min(1,life[i]/24f),size=(color[i]==0xffb9e9f4?4:11)*c.scale;
        float tail=Math.min(age[i]/60f,rocket[i]?.34f:.28f);
        float lift=z[i]*ArticulatedModel.Z_SCALE,px=x[i]+RiverCamera.riseX(lift),py=y[i]+RiverCamera.riseY(lift);
        float tz=z[i]-vz[i]*tail-40*tail*tail;
        float tx=x[i]-vx[i]*tail+RiverCamera.riseX(tz*ArticulatedModel.Z_SCALE),ty=y[i]-vy[i]*tail+RiverCamera.riseY(tz*ArticulatedModel.Z_SCALE);
        float extent=flash[i]?120:18;
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
    public void drawUI(Game g,Painter p){
        if(!visible(g))return;
        for(int button=0;button<1;button++){
            float cx=buttonX(g,button),cy=buttonY(g);PilotGlass.button(p,cx-23,cy-23,46,46,button==0);
            int ink=PilotGlass.buttonText(button==0);
            int tint=selected.abilityCooldown>0 && button==0?0xff695b3b:ink;
            int icon=selected.ability==FIRE?ArcadeIcons.WATER:ArcadeIcons.FIREWORK;
            float pulse=button==0 && selected.abilityCooldown<=0?1+.055f*(float)StrictMath.sin(g.visualTicks*.1):1;
            ArcadeIcons.draw(p,icon,cx,cy,.85f*pulse,tint);
        }
    }
}
