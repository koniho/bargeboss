package com.bargeboss.game;

/** Bounded world-space surface decals. Released foam never follows its source vessel. */
public final class VesselFoam {
    static final int CAPACITY=160;
    final float[] x=new float[CAPACITY],y=new float[CAPACITY],vx=new float[CAPACITY],vy=new float[CAPACITY],
            angle=new float[CAPACITY],width=new float[CAPACITY],length=new float[CAPACITY],strength=new float[CAPACITY];
    final int[] age=new int[CAPACITY],life=new int[CAPACITY],kind=new int[CAPACITY];
    private int cursor;
    int emitted,propEmitted;
    public void step() {
        for(int i=0;i<CAPACITY;i++)if(age[i]<life[i]) {
            age[i]++;x[i]+=vx[i];y[i]+=vy[i];vx[i]*=.982f;vy[i]*=.982f;
            if(kind[i]>=2)angle[i]+=(i%2==0?1:-1)*.006f;
        }
    }
    public void emit(Game g,Body b) {
        float speed=b.speed(),c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        if(speed>4 && Math.floorMod(g.visualTicks+b.id*7,8)==0) {
            float dx=b.vx/speed,dy=b.vy/speed,motion=Math.min(1,speed/65);
            float along=dx*c+dy*s;
            float end=b.half*Physics.clamp(along/.3f,-1,1);
            float shoulder=b.radius+(1-Math.min(1,Math.abs(along)/.3f))*b.half;
            // Shoulders of the leading end of the capsule, including stern-first travel and drift.
            for(int side=-1;side<=1;side+=2) {
                float xx=b.x+c*end+b.radius*dx*.62f-shoulder*dy*side*.8f;
                float yy=b.y+s*end+b.radius*dy*.62f+shoulder*dx*side*.8f;
                add(xx,yy,b.vx*.0015f-dy*side*.09f,b.vy*.0015f+dx*side*.09f,
                        (float)StrictMath.atan2(dy,dx),22+motion*15,10+motion*8,.16f+motion*.38f,90,side<0?0:1);
            }
        }
        float load=Physics.clamp((b.engineLoad-.12f)/.88f,0,1);
        if(b.berth>=0 || load<=.02f || Math.floorMod(g.visualTicks+b.id*3,10)!=0)return;
        boolean astern=b==g.tug && g.reversing;float direction=astern?1:-1;
        for(int side=-1;side<=1;side+=2) {
            float xx=b.x-c*(b.half+b.radius*.7f)-s*side*b.radius*.35f;
            float yy=b.y-s*(b.half+b.radius*.7f)+c*side*b.radius*.35f;
            float swirl=side*(.1f+load*.1f)+(float)StrictMath.sin(g.visualTicks*.17+b.id)*.12f;
            add(xx,yy,c*direction*(.13f+load*.35f)-s*swirl,s*direction*(.13f+load*.35f)+c*swirl,
                    b.angle,22+load*16,18+load*18,.12f+load*.45f,150,2);propEmitted++;
        }
    }
    private void add(float xx,float yy,float dx,float dy,float a,float l,float w,float power,int duration,int texture) {
        int i=cursor;cursor=(cursor+1)%CAPACITY;age[i]=0;life[i]=duration;kind[i]=texture;
        x[i]=xx;y[i]=yy;vx[i]=dx;vy[i]=dy;angle[i]=a;length[i]=l;width[i]=w;strength[i]=power;emitted++;
    }
    public void draw(Game g,Painter p) {
        if(!(p instanceof RiverPainter))return;
        RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;Painter canvas=river.canvas();
        for(int n=0;n<CAPACITY;n++) {
            int i=(cursor+n)%CAPACITY;if(age[i]>=life[i])continue;
            float t=age[i]/(float)life[i],alpha=strength[i]*Math.min(1,age[i]/6f)*(1-t)*(1-t);
            if(alpha<.008f)continue;
            float l=length[i]*(1+t*.8f),w=width[i]*(1+t*1.5f);
            float sx=c.screenX(x[i],y[i]),sy=c.screenY(x[i],y[i]),bound=(l+w)*c.scale;
            if(sx+bound<0 || sy+bound<0 || sx-bound>g.layout.width || sy-bound>g.layout.height)continue;
            float[] clip=ShoreOcclusion.region(g,c,x[i]-l-w,y[i]-l-w,x[i]+l+w,y[i]+l+w,Float.NEGATIVE_INFINITY);
            if(clip.length>0)canvas.pushClipOutside(clip);
            try {
                int texture=kind[i];
                if(texture==2 && t>.35f) {
                    float mix=Math.min(1,(t-.35f)/.3f);
                    patch(p,2,x[i],y[i],angle[i],l,w,alpha*(1-mix));
                    patch(p,3,x[i],y[i],angle[i],l,w,alpha*mix);
                } else patch(p,texture,x[i],y[i],angle[i],l,w,alpha);
            } finally {if(clip.length>0)canvas.popClip();}
        }
    }
    static void patch(Painter p,int frame,float x,float y,float angle,float length,float width,float alpha) {
        if(alpha<=.005f)return;
        float c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);
        p.spriteAffine(SpriteAtlas.FOAM,frame,x,y,c*length,s*length,-s*width,c*width,alpha);
    }
}
