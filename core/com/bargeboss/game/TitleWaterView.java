package com.bargeboss.game;

/** Body-aligned tow wakes and title-only horizon perspective, without advancing gameplay. */
public final class TitleWaterView {
    private TitleWaterView() {}
    private static float sin(double a) { return (float)StrictMath.sin(a); }
    /** Clip title wake geometry to its water plane before shore sprites cover it. */
    public static Painter below(Painter target,float top) {return new WaterPlane(target,top);}
    /** Water-plane effects flatten continuously at the horizon; upright artwork is untouched. */
    public static Painter perspective(Painter target,float top,float foreground){
        return new Perspective(below(target,top),top,foreground);
    }
    static float projectedY(float y,float top,float foreground){
        float h=Math.max(1,foreground-top),t=(y-top)/h;
        if(t<=0)return top;if(t>=1)return y;
        return top+h*t*t*(2-t);
    }
    private static final class Perspective implements Painter {
        final Painter p;final float top,foreground;
        Perspective(Painter p,float top,float foreground){this.p=p;this.top=top;this.foreground=foreground;}
        float y(float y){return projectedY(y,top,foreground);}
        float thin(float y){return Physics.clamp((y-top)/Math.max(1,foreground-top),.06f,1);}
        public void waterFoam(float x,float yy,float ax,float ay,float bx,float by,float phase,int kind,float alpha){
            p.waterFoam(x,y(yy),ax,y(yy+ay*.5f)-y(yy-ay*.5f),bx,y(yy+by*.5f)-y(yy-by*.5f),phase,kind,alpha);
        }
        public void line(float x,float yy,float xx,float y2,float w,int color){p.line(x,y(yy),xx,y(y2),w*thin((yy+y2)*.5f),color);}
        public void poly(float[] xy,int color){float[] q=xy.clone();for(int i=1;i<q.length;i+=2)q[i]=y(q[i]);p.poly(q,color);}
        public void rect(float x,float yy,float w,float h,int color){p.rect(x,y(yy),w,y(yy+h)-y(yy),color);}
        public void circle(float x,float yy,float r,int color){float[] q=new float[24];for(int i=0;i<12;i++){double a=i*Math.PI/6;q[i*2]=x+r*(float)StrictMath.cos(a);q[i*2+1]=y(yy+r*sin(a));}p.poly(q,color);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new IllegalStateException("Title perspective is water-only");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new IllegalStateException("Title perspective is water-only");}
    }
    private static final class WaterPlane implements Painter {
        final Painter p;final float top;
        WaterPlane(Painter p,float top){this.p=p;this.top=top;}
        public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
            float w=(Math.abs(ax)+Math.abs(bx))*.5f,h=(Math.abs(ay)+Math.abs(by))*.5f;
            if(y+h<=top)return;
            boolean clipped=y-h<top;
            if(clipped)p.pushClipOutside(new float[]{x-w-1,y-h-1,x+w+1,y-h-1,x-w-1,top,
                    x+w+1,y-h-1,x+w+1,top,x-w-1,top});
            try {p.waterFoam(x,y,ax,ay,bx,by,phase,kind,alpha);} finally {if(clipped)p.popClip();}
        }
        public void line(float x,float y,float xx,float yy,float w,int color) {
            float edge=top+w*.5f;
            if(y<edge && yy<edge)return;
            if(y<edge){x+=(xx-x)*(edge-y)/(yy-y);y=edge;}
            else if(yy<edge){xx+=(x-xx)*(edge-yy)/(y-yy);yy=edge;}
            p.line(x,y,xx,yy,w,color);
        }
        public void poly(float[] xy,int color) {
            float[] clipped=new float[xy.length*2];int count=0;
            for(int i=0,j=xy.length-2;i<xy.length;j=i,i+=2) {
                float x=xy[i],y=xy[i+1],xx=xy[j],yy=xy[j+1];
                if((y>=top)!=(yy>=top)){clipped[count++]=xx+(x-xx)*(top-yy)/(y-yy);clipped[count++]=top;}
                if(y>=top){clipped[count++]=x;clipped[count++]=y;}
            }
            if(count<6)return;
            float[] result=new float[count];System.arraycopy(clipped,0,result,0,count);p.poly(result,color);
        }
        public void rect(float x,float y,float w,float h,int color) {
            float bottom=y+h;y=Math.max(y,top);if(bottom>y)p.rect(x,y,w,bottom-y,color);
        }
        public void circle(float x,float y,float r,int color) {
            if(y+r<top)return;
            if(y-r>=top){p.circle(x,y,r,color);return;}
            float[] xy=new float[40];for(int i=0;i<20;i++){double a=i*Math.PI/10;xy[i*2]=x+r*(float)StrictMath.cos(a);xy[i*2+1]=y+r*sin(a);}poly(xy,color);
        }
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new IllegalStateException("Title water uses geometry only");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new IllegalStateException("Title water uses geometry only");}
    }
    /** Local side/forward coordinates rotate with the hull before shared projection. */
    private static void patch(Painter p,Body b,float side,float forward,float sx,float sy,float wx,float wy,float phase,int kind,float alpha){
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        p.waterFoam(b.x+side*s+forward*c,b.y-side*c+forward*s,sx*s+sy*c,-sx*c+sy*s,wx*s+wy*c,-wx*c+wy*s,phase,kind,alpha);
    }
    public static void water(Painter p,Body b,int tick,boolean tug) {
        float stern=-b.half-b.radius;
        // Only the powered tug and trailing barge need a long wake history. Other
        // barges keep their displacement foam, avoiding four overlapping full trails.
        if(tug)for(int i=0;i<20;i++){
            float age=((tick+i*13)%180)/180f;
            patch(p,b,sin(age*19+i)*(2+age*8),stern-3-age*72,0,14+age*18,9+age*14,0,
                WaterFoam.phase(tick,i),2,.55f*(1-age)*(1-age));
        }
        if(tug || b.id==TitleView.BARGES)for(int packet=7;packet>=0;packet--){
            int age=tick%20+packet*20;float t=age/160f;
            float spread=b.radius*.6f+age*.15f,back=stern-age*.38f;
            float alpha=(tug?.5f:.38f)*(1-t)*(1-t);
            // Curved rear-facing arcs spread outwards while their origin trails left.
            for(int part=0;part<12;part++){
                float a=(float)Math.PI+part*(float)Math.PI/12,z=a+(float)Math.PI/12;
                float x=(float)StrictMath.cos(a)*spread,y=back+sin(a)*spread*.55f;
                float dx=(float)StrictMath.cos(z)*spread-x,dy=back+sin(z)*spread*.55f-y,len=Physics.length(dx,dy);
                patch(p,b,x+dx*.5f,y+dy*.5f,dx,dy,-dy/len*4,dx/len*4,WaterFoam.phase(tick,packet*12+part),5,alpha);
            }
        }
        for(int side=-1;side<=1;side+=2)for(int i=0;i<9;i++){
            float t=((tick+i*13)%117)/117f;
            patch(p,b,side*(b.radius+1+sin(t*9+tick*.03)*1.3f),b.half+7-t*(b.half*2+17),
                side*.6f,-6,4,0,WaterFoam.phase(tick,i),i%2,.45f);
        }
        for(int side=-1;side<=1;side+=2)patch(p,b,side*b.radius*.45f,b.half+b.radius*.6f,
            side*11,-8,5,4,WaterFoam.phase(tick,side),4,.55f);
    }
}
