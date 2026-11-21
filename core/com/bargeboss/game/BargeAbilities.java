package com.bargeboss.game;

/** Shared, pointer-owned equipment actions. Selection never alters tow topology. */
public final class BargeAbilities {
    public static final int NONE=0,FIREWORKS=1,FIRE=2,CAPACITY=192;
    public Body selected;
    public int owner=-1,heldTicks,visibleTicks,launches;
    public boolean spraying;
    private int pressed=-1,next;
    private float aimX,aimY;
    private final float[] x=new float[CAPACITY],y=new float[CAPACITY],z=new float[CAPACITY],vx=new float[CAPACITY],vy=new float[CAPACITY],vz=new float[CAPACITY];
    private final boolean[] rocket=new boolean[CAPACITY];
    private final int[] life=new int[CAPACITY],color=new int[CAPACITY];
    public void reset(){selected=null;cancel();visibleTicks=launches=next=0;for(int i=0;i<CAPACITY;i++)life[i]=0;}
    public void cancel(){owner=-1;pressed=-1;spraying=false;heldTicks=0;}
    public boolean reveal(Game g,Body b){
        if(b==null || !b.active || b.ability==NONE)return false;
        selected=b;visibleTicks=600;g.haptic(Game.HAPTIC_TICK);return true;
    }
    public float buttonX(Game g,int button){float sx=g.layout.camera.screenX(selected.x,selected.y);return Physics.clamp(sx,78,g.layout.width-78)+(button==0?-28:28);}
    public float buttonY(Game g){return Physics.clamp(g.layout.camera.screenY(selected.x,selected.y)-82*g.layout.camera.scale,110,g.layout.height-110);}
    private boolean visible(Game g){return g.mode==Game.PLAY && !g.conversation() && selected!=null && selected.active && visibleTicks>0;}
    private boolean hit(Game g,float px,float py,int button){return Math.abs(px-buttonX(g,button))<=24 && Math.abs(py-buttonY(g))<=24;}
    public boolean down(Game g,int id,float px,float py){
        if(!visible(g) || owner!=-1 || g.pointer)return false;
        int which=hit(g,px,py,0)?0:g.tow.incoming(selected)!=null && hit(g,px,py,1)?1:-1;
        if(which<0)return false;
        owner=id;pressed=which;heldTicks=0;aimX=selected.x+(float)StrictMath.cos(selected.angle)*180;aimY=selected.y+(float)StrictMath.sin(selected.angle)*180;visibleTicks=600;return true;
    }
    public boolean move(Game g,int id,float px,float py){
        if(owner!=id)return false;
        if(selected.ability==FIRE && pressed==0){aimX=g.layout.camera.worldX(px,py);aimY=g.layout.camera.worldY(px,py);}
        return true;
    }
    public boolean up(Game g,int id,float px,float py){
        if(owner!=id)return false;
        if(selected!=null && selected.active && heldTicks<=18 && hit(g,px,py,pressed)){
            if(pressed==1){g.releaseTow(selected);selected=null;}
            else if(selected.ability==FIREWORKS && selected.abilityCooldown==0){selected.abilityTicks=180;selected.abilityCooldown=240;launches++;g.haptic(Game.HAPTIC_SUCCESS);}
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
                if(b.abilityTicks%30==0)burst(b,180-b.abilityTicks);
                b.abilityTicks--;
            }
        }
        if(spraying && selected!=null){
            float dx=aimX-selected.x,dy=aimY-selected.y,len=Math.max(1,Physics.length(dx,dy));
            float range=Physics.clamp(len,40,210);float flight=.9f;
            for(int side=-1;side<=1;side+=2)emit(BargeEquipment.anchorX(selected,side)+dx/len*9,BargeEquipment.anchorY(selected,side)+dy/len*9,22,dx/len*range/flight,dy/len*range/flight,30,70,0xffb9e9f4);
            selected.abilityAim=(float)StrictMath.atan2(dy,dx);
            if(g.visualTicks%6==0)g.smoke.equipment(g,selected,-24,6,30.5f);
        }
        for(int i=0;i<CAPACITY;i++)if(life[i]>0){
            life[i]--;x[i]+=vx[i]/60;y[i]+=vy[i]/60;z[i]+=vz[i]/60;vz[i]-=80f/60;
            if(z[i]<0)life[i]=0;
            if(life[i]==0 && rocket[i]){rocket[i]=false;explode(x[i],y[i],z[i],color[i]);}
        }
    }
    private void burst(Body b,int tick){
        int tint=tick%90==0?0xffffc777:tick%90==30?0xff8ce6de:0xffff929e;
        emit(b.x,b.y,12,0,0,155,48,tint);rocket[(next-1)%CAPACITY]=true;
    }
    private void explode(float px,float py,float pz,int tint){
        for(int i=0;i<24;i++){
            double a=i*StrictMath.PI*2/24;float speed=30+(i%3)*12;
            emit(px,py,pz,(float)StrictMath.cos(a)*speed,(float)StrictMath.sin(a)*speed,25+(i%4)*12,100,tint);
        }
    }
    private void emit(float xx,float yy,float zz,float dx,float dy,float dz,int ticks,int tint){int i=next++%CAPACITY;rocket[i]=false;x[i]=xx;y[i]=yy;z[i]=zz;vx[i]=dx;vy[i]=dy;vz[i]=dz;life[i]=ticks;color[i]=tint;}
    boolean alive(int i){return life[i]>0;}
    float depth(Game g,int i){return g.layout.camera.screenY(x[i],y[i]);}
    void drawParticle(Game g,Painter p,int i){
        if(life[i]<=0)return;RiverCamera c=g.layout.camera;
        float sx=c.screenX(x[i],y[i]),sy=c.screenY(x[i],y[i])-z[i]*ArticulatedModel.Z_SCALE*c.scale;
        if(sx< -40 || sx>g.layout.width+40 || sy< -40 || sy>g.layout.height+40)return;
        float fade=Math.min(1,life[i]/20f),size=(color[i]==0xffb9e9f4?4:6)*c.scale;
        // Query only the stamp's projected footprint. A tall rocket does not need
        // every shoreline between its launch point and its current screen position.
        float lift=z[i]*ArticulatedModel.Z_SCALE,px=x[i]+RiverCamera.riseX(lift),py=y[i]+RiverCamera.riseY(lift),extent=40;
        float[] clip=ShoreOcclusion.region(g,c,px-extent,py-extent,px+extent,py+extent,depth(g,i));
        if(clip.length>0)p.pushClipOutside(clip);
        try{if(color[i]==0xffb9e9f4)p.spriteAffine(SpriteAtlas.SPLASH,i%4,sx,sy,size*3,0,0,size*2,fade*.6f);
        else {float tx=c.screenX(x[i]-vx[i]*.06f,y[i]-vy[i]*.06f),ty=c.screenY(x[i]-vx[i]*.06f,y[i]-vy[i]*.06f)-(z[i]-vz[i]*.06f)*ArticulatedModel.Z_SCALE*c.scale;
            p.line(tx,ty,sx,sy,Math.max(.8f,c.scale),SoftCue.alpha(color[i],fade*.65f));p.material(sx,sy,size,0,0,size,EffectMaterial.DOT,0,1,SoftCue.alpha(color[i],fade));}}finally{if(clip.length>0)p.popClip();}
    }
    public void drawUI(Game g,Painter p){
        if(!visible(g))return;
        for(int button=0;button<(g.tow.incoming(selected)!=null?2:1);button++){
            float cx=buttonX(g,button),cy=buttonY(g);PilotGlass.button(p,cx-23,cy-23,46,46,button==0);
            int ink=PilotGlass.buttonText(button==0);
            int tint=selected.abilityCooldown>0 && button==0?0xff695b3b:ink;
            if(button==1){p.line(cx-10,cy-7,cx-2,cy+1,2,tint);p.line(cx+2,cy-1,cx+10,cy+7,2,tint);p.line(cx-5,cy+8,cx+6,cy-9,1.5f,ink);}
            else if(selected.ability==FIRE){p.line(cx-8,cy+8,cx+5,cy-5,3,tint);for(int j=0;j<3;j++)p.line(cx+5,cy-5,cx+12+j*2,cy-12+j*4,1,ink);}
            else for(int j=0;j<8;j++){double a=j*StrictMath.PI/4;p.line(cx+(float)StrictMath.cos(a)*5,cy+(float)StrictMath.sin(a)*5,cx+(float)StrictMath.cos(a)*14,cy+(float)StrictMath.sin(a)*14,1.5f,tint);}
        }
    }
}
