package com.bargeboss.game;

/** Scrolling wake history and loaded push contact, sampled without advancing gameplay. */
public final class TitleWaterView {
    private TitleWaterView() {}
    private static float sin(double a) { return (float)StrictMath.sin(a); }
    /** Clip title wake geometry to its water plane before shore sprites cover it. */
    public static Painter below(Painter target,float top) {return new WaterPlane(target,top);}
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
    public static void water(Painter p,Body b,int tick,boolean tug) {
        float stern=b.y-b.half-b.radius;
        if(tug) {

            for(int i=0;i<28;i++) {
                float age=((tick+i*13)%180)/180f;
                float x=b.x+sin(age*19+i)*(2+age*10),y=stern-3-age*88;
                int alpha=(int)(150*(1-age)*(1-age));
                p.waterFoam(x,y,0,9+age*12,5+age*13,0,WaterFoam.phase(tick,i),2,alpha/255f*.55f);
            }
        }
        // The outer wake arms form the familiar widening V behind each hull.
        for(int side=-1;side<=1;side+=2) for(int i=1;i<=18;i++) {
            float a=(i-1)/18f,t=i/18f;
            float x=b.x+side*(b.radius*.6f+a*32),y=stern-a*82;
            float dx=side*(t-a)*32,dy=-(t-a)*82,len=Physics.length(dx,dy);
            p.waterFoam(x+dx*.5f,y+dy*.5f,dx,dy,-dy/len*6,dx/len*6,WaterFoam.phase(tick,i),5,95*(1-t)/255f);
        }
        // Each packet recedes astern while its curved crest expands sideways.
        // New packets form continuously; troughs add a stronger crest to the train.
        int spacing=18,life=180;
        for(int packet=life/spacing;packet>=0;packet--) {
            int age=tick%spacing+packet*spacing;if(age>=life) continue;
            float t=age/(float)life;
            float impact=.55f+.45f*Math.max(0,sin((tick-age)*.048+Math.PI/2));
            float spread=b.radius*.6f+age*.15f,back=stern-age*.38f;
            int alpha=(int)((tug?85:55)*(1-t)*(1-t)*impact);
            arc(p,b.x,back,spread,spread*.55f,(float)Math.PI,(float)Math.PI*2,1.1f,(alpha<<24)|0xa4eee7);
            arc(p,b.x,back-2,spread,spread*.55f,(float)Math.PI,(float)Math.PI*2,.55f,((alpha/2)<<24)|0xf1fff4);
        }
        // Water follows both sides of the displacement hull from bow to stern.
        for(int side=-1;side<=1;side+=2) for(int i=0;i<9;i++) {
            float t=((tick+i*13)%117)/117f;
            float x=b.x+side*(b.radius+1+sin(t*9+tick*.03)*1.3f);
            float y=b.y+b.half+7-t*(b.half*2+17);
            p.waterFoam(x,y,side*.6f,-6,4,0,WaterFoam.phase(tick,i),i%2,.45f);
        }
        float lift=.6f+.4f*Math.max(0,sin(tick*.048+Math.PI/2));
        for(int ring=0;ring<3;ring++) {
            float radius=b.radius+2+ring*3;
            arc(p,b.x,b.y+b.half,radius,radius*.58f,0,(float)Math.PI,
                    ring==0?1.7f:.7f,((int)((170-ring*45)*lift)<<24)|0xe1fff2);
        }
    }
    public static void contact(Painter p,Body barge,int tick) {
        float contact=barge.y-barge.half-barge.radius;
        float pressure=.5f+.5f*sin(tick*.065);
        // Two rubber push knees visibly compress between the bow and the load.
        for(int side=-1;side<=1;side+=2) {
            float x=barge.x+side*7;
            p.line(x-3,contact,x+3,contact,4-pressure,VisualStyle.OUTLINE);
            p.line(x-2.4f,contact-.5f,x+2.4f,contact-.5f,1.1f,0xff76838c);
            p.line(x-2.4f,contact+.7f,x+2.4f,contact+.7f,.7f,VisualStyle.GOLD);
        }
    }
    public static void push(Painter p,Body barge,int tick) {
        float contact=barge.y-barge.half-barge.radius;
        // A short traveling pressure cue under the coupling points toward the load.
        for(int i=0;i<3;i++) {
            float phase=((tick+i*12)%54)/54f;
            float x=barge.x-barge.radius-3,y=contact-8+phase*15;
            int color=((int)(190*(1-phase))<<24)|0xffdd85;
            p.line(x-1.8f,y-2,x,y,1,color);p.line(x+1.8f,y-2,x,y,1,color);
        }
        // Small bow splashes rise and fall rather than blinking on/off.
        for(int side=-1;side<=1;side+=2) for(int i=0;i<6;i++) {
            float t=((tick+i*11)%66)/66f;
            float x=barge.x+side*(barge.radius*.7f+t*7);
            float y=barge.y+barge.half+barge.radius*.45f-t*8;
            p.circle(x+sin(t*Math.PI)*3,y,1.1f-t*.6f,((int)(170*(1-t))<<24)|0xeefff5);
        }
    }
    private static void arc(Painter p,float x,float y,float rx,float ry,float start,float end,float width,int color) {
        for(int i=1;i<=18;i++) {
            float a=start+(end-start)*(i-1)/18,b=start+(end-start)*i/18;
            float xx=x+(float)StrictMath.cos(a)*rx,yy=y+sin(a)*ry;
            float dx=x+(float)StrictMath.cos(b)*rx-xx,dy=y+sin(b)*ry-yy,len=Physics.length(dx,dy);
            p.waterFoam(xx+dx*.5f,yy+dy*.5f,dx,dy,-dy/len*width*5,dx/len*width*5,
                    y*.07f+i*.7f,5,(color>>>24)/255f);
        }
    }
}
