package com.bargeboss.game;

/** Fixed-size, detached spray, impact flashes and heavy-hit sparks shared by every watercraft contact. */
public final class CollisionSplashes {
    public static final int CAPACITY=24,STAMPS=6,LIFE=72,COOLDOWN=14;
    private final Watercraft[] first=new Watercraft[CAPACITY],second=new Watercraft[CAPACITY];
    final int[] age=new int[CAPACITY];
    final float[] originX=new float[CAPACITY],originY=new float[CAPACITY],direction=new float[CAPACITY],power=new float[CAPACITY];
    final float[] x=new float[CAPACITY*STAMPS],y=new float[CAPACITY*STAMPS],z=new float[CAPACITY*STAMPS],
            vx=new float[CAPACITY*STAMPS],vy=new float[CAPACITY*STAMPS],vz=new float[CAPACITY*STAMPS];
    private int cursor;
    public int emitted;
    public CollisionSplashes(){for(int i=0;i<CAPACITY;i++)age[i]=LIFE;}
    /** Contact velocities include angular motion and are captured BEFORE the collision impulse.
     * Use the faster incoming participant as striker so solver argument order cannot reverse spray. */
    public void contact(Watercraft a,Watercraft b,float px,float py,float ax,float ay,float bx,float by,float closing){
        if(closing<3 || !Float.isFinite(closing) || !Float.isFinite(px) || !Float.isFinite(py))return;
        for(int i=0;i<CAPACITY;i++)if(age[i]<COOLDOWN && (first[i]==a && second[i]==b || first[i]==b && second[i]==a))return;
        float aa=ax*ax+ay*ay,bb=bx*bx+by*by;
        boolean reverse=b!=null && (bb>aa || bb==aa && (b.waterX()<a.waterX() || b.waterX()==a.waterX() && b.waterY()<a.waterY()));
        float dx=reverse?bx-ax:ax-bx,dy=reverse?by-ay:ay-by,len=Physics.length(dx,dy);
        if(len<.001f || !Float.isFinite(len))return;
        int i=cursor;cursor=(cursor+1)%CAPACITY;first[i]=a;second[i]=b;age[i]=0;emitted++;
        originX[i]=px;originY[i]=py;direction[i]=(float)StrictMath.atan2(dy,dx);
        power[i]=Physics.clamp(closing/70,.12f,1);
        float spread=.16f+.48f*Physics.clamp(closing/len,0,1);
        for(int k=0;k<STAMPS;k++){
            int j=i*STAMPS+k;float fan=(k-(STAMPS-1)*.5f)/((STAMPS-1)*.5f);
            float angle=direction[i]+fan*spread,speed=(9+power[i]*35)*(.65f+.35f*((k*3+1)%7)/6f);
            x[j]=px;y[j]=py;z[j]=.3f+k*.12f;
            vx[j]=(float)StrictMath.cos(angle)*speed;vy[j]=(float)StrictMath.sin(angle)*speed;
            vz[j]=(23+power[i]*48)*(.65f+.35f*((k*5+2)%7)/6f);
        }
    }
    /** Age at 60 real ticks, independent of the two motion/collision substeps and visibility. */
    public void step(Game g){
        for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE){
            age[i]++;
            if(age[i]>=LIFE){first[i]=second[i]=null;continue;}
            for(int k=0;k<STAMPS;k++){
                int j=i*STAMPS+k;if(z[j]<0)continue;
                float xx=x[j]+vx[j]*Physics.DT,yy=y[j]+vy[j]*Physics.DT;
                if(WaterEffectsPainter.contains(g,xx,yy)){x[j]=xx;y[j]=yy;}
                else {vx[j]*=.35f;vy[j]*=.35f;}
                vz[j]-=85*Physics.DT;z[j]+=vz[j]*Physics.DT;vx[j]*=.985f;vy[j]*=.985f;
            }
        }
    }
    /** Low spreading foam stays on the water underneath all hulls. Broken outward foam crown. */
    public void water(Game g,Painter p){
        if(!(p instanceof RiverPainter))return;
        RiverPainter river=(RiverPainter)p;Painter canvas=river.canvas();RiverCamera c=river.camera;
        for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE){
            float t=age[i]/(float)LIFE,alpha=(.45f+.5f*power[i])*Math.min(1,(age[i]+1)/4f)*(1-t)*(1-t);
            float length=16+power[i]*26+t*38,width=10+power[i]*16+t*20;
            float dx=(float)StrictMath.cos(direction[i]),dy=(float)StrictMath.sin(direction[i]);
            float xx=originX[i]+dx*(2+t*7),yy=originY[i]+dy*(2+t*7);
            if(!visible(g,xx,yy,0,length+width))continue;
            canvas.pushClipOutside(ShoreOcclusion.region(g,c,xx-length-width,yy-length-width,xx+length+width,yy+length+width,Float.NEGATIVE_INFINITY));
            try{p.waterFoam(xx,yy,dx*length,dy*length,-dy*width,dx*width,t*2,9,alpha);}
            finally{canvas.popClip();}
        }
    }
    private static boolean visible(Game g,float x,float y,float z,float extent){
        RiverCamera c=g.layout.camera;float sx=c.screenX(x,y),sy=c.screenY(x,y)-z*c.scale,pad=extent*c.scale;
        return sx+pad>=0 && sx-pad<=g.layout.width && sy+pad>=0 && sy-pad<=g.layout.height;
    }
    public float depth(Game g,int j){return g.layout.camera.screenY(x[j],y[j]);}
    public int[] ordered(Game g){
        int[] order=new int[CAPACITY*STAMPS];int n=0;
        for(int j=0;j<order.length;j++)if(age[j/STAMPS]<LIFE && z[j]>=0 && visible(g,x[j],y[j],z[j],48)){
            int at=n++;while(at>0 && depth(g,order[at-1])>depth(g,j)){order[at]=order[at-1];at--;}order[at]=j;
        }
        int[] result=new int[n];System.arraycopy(order,0,result,0,n);return result;
    }
    /** A tapered spray finger stretched along projected travel, lifted ballistically above water. */
    public void draw(Game g,Painter canvas,int j){
        int i=j/STAMPS;float t=age[i]/(float)LIFE;
        float alpha=(.55f+.45f*power[i])*Math.min(1,(age[i]+1)/3f)*(1-t);
        float lifeFade=Physics.clamp(z[j]/1.5f,0,1);if(vz[j]<0)alpha*=lifeFade;
        if(z[j]<0 || alpha<.01f)return;
        RiverCamera c=g.layout.camera;float dx=RiverCamera.Q*(vx[j]+vy[j]),dy=RiverCamera.Q*RiverCamera.DEPTH*(vy[j]-vx[j])-vz[j];
        float len=Math.max(.001f,Physics.length(dx,dy));dx/=len;dy/=len;
        float variation=.7f+(j%STAMPS)*.07f;
        float length=(12+power[i]*24)*(1-t*.55f)*c.scale*variation,width=(5+power[i]*9)*(1-t*.6f)*c.scale*variation;
        float sx=c.screenX(x[j],y[j]),sy=c.screenY(x[j],y[j])-z[j]*c.scale;
        float extent=64+z[j]*2;
        canvas.pushClipOutside(ShoreOcclusion.region(g,c,x[j]-extent,y[j]-extent,x[j]+extent,y[j]+extent,depth(g,j)));
        try{
            canvas.waterFoam(sx,sy,dx*length,dy*length,-dy*width,dx*width,
                    j*.73f+t*.8f,8,alpha);
            // Small separated droplets trail each liquid finger, following its ballistic velocity.
            for(int drop=0;drop<3;drop++){
                float behind=(.35f+drop*.21f)*length,side=((j+drop)%3-1)*width*.32f;
                float radius=(.65f+power[i]*1.2f)*(1-drop*.17f)*c.scale;
                canvas.circle(sx-dx*behind-dy*side,sy-dy*behind+dx*side,radius,(Math.round(alpha*210)<<24)|0xe5fcf7);
            }
            // One brief burst per contact, kept in the same depth/shore scope as the spray.
            if(j%STAMPS==0 && age[i]<10) {
                float fade=1-age[i]/10f,r=(7+power[i]*14+age[i]*1.4f)*c.scale;
                canvas.circle(sx,sy,r,(Math.round(95*fade)<<24)|0xe5fcf7);
                for(int ray=0;ray<7;ray++) {
                    float angle=ray*(float)(StrictMath.PI*2/7)+direction[i];
                    float rx=(float)StrictMath.cos(angle),ry=(float)StrictMath.sin(angle);
                    canvas.line(sx+rx*r*.35f,sy+ry*r*.35f,sx+rx*r,sy+ry*r,
                            (1.2f+power[i])*c.scale,(Math.round(230*fade)<<24)|0xf1fff1);
                }
            }
            // Six detached hot streaks only for substantial incoming impacts (>=45.5 units/s).
            if(power[i]>=.65f && age[i]<24) {
                float fade=1-age[i]/24f,tail=(5+power[i]*11)*c.scale;
                float lift=(4+age[i]*.5f)*c.scale;
                int a=Math.round(255*fade);
                canvas.line(sx-dx*tail,sy-lift-dy*tail,sx,sy-lift,3*c.scale,(a<<24)|0xff742b);
                canvas.line(sx-dx*tail*.65f,sy-lift-dy*tail*.65f,sx,sy-lift,1.2f*c.scale,(a<<24)|0xffef9b);
            }
        }
        finally{canvas.popClip();}
    }
}
