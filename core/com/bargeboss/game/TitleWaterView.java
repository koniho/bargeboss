package com.bargeboss.game;

/** Scrolling wake history and loaded push contact, sampled without advancing gameplay. */
public final class TitleWaterView {
    private TitleWaterView() {}
    private static float sin(double a) { return (float)StrictMath.sin(a); }
    public static void water(Painter p,Body b,int tick,boolean tug) {
        float stern=b.y-b.half-b.radius;
        if(tug) {
            p.poly(new float[]{b.x-5,stern,b.x+5,stern,b.x+15,stern-90,b.x-15,stern-90},0x3766bfc4);
            for(int i=0;i<28;i++) {
                float age=((tick+i*13)%180)/180f;
                float x=b.x+sin(age*19+i)*(2+age*10),y=stern-3-age*88;
                int alpha=(int)(150*(1-age)*(1-age));
                p.circle(x,y,1+age*2,(alpha<<24)|0xd9fff2);
            }
        }
        // The outer wake arms form the familiar widening V behind each hull.
        for(int side=-1;side<=1;side+=2) for(int i=1;i<=18;i++) {
            float a=(i-1)/18f,t=i/18f;
            p.line(b.x+side*(b.radius*.6f+a*32),stern-a*82,
                    b.x+side*(b.radius*.6f+t*32),stern-t*82,
                    1.2f,((int)(95*(1-t))<<24)|0xa4eee7);
        }
        // Each packet recedes astern while its curved crest expands sideways.
        // New packets form continuously; troughs add a stronger crest to the train.
        int spacing=12,life=216;
        for(int packet=life/spacing;packet>=0;packet--) {
            int age=tick%spacing+packet*spacing;if(age>=life) continue;
            float t=age/(float)life;
            float impact=.55f+.45f*Math.max(0,sin((tick-age)*.048+Math.PI/2));
            float spread=b.radius*.6f+age*.15f,back=stern-age*.38f;
            int alpha=(int)((tug?125:90)*(1-t)*(1-t)*impact);
            arc(p,b.x,back,spread,spread*.55f,(float)Math.PI,(float)Math.PI*2,1.5f,(alpha<<24)|0xa4eee7);
            arc(p,b.x,back-2,spread,spread*.55f,(float)Math.PI,(float)Math.PI*2,.55f,((alpha/2)<<24)|0xf1fff4);
        }
        // Water follows both sides of the displacement hull from bow to stern.
        for(int side=-1;side<=1;side+=2) for(int i=0;i<9;i++) {
            float t=((tick+i*13)%117)/117f;
            float x=b.x+side*(b.radius+1+sin(t*9+tick*.03)*1.3f);
            float y=b.y+b.half+7-t*(b.half*2+17);
            p.line(x,y,x+side*.6f,y-4,.8f,0x99cffbed);
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
            p.line(x+(float)StrictMath.cos(a)*rx,y+sin(a)*ry,x+(float)StrictMath.cos(b)*rx,y+sin(b)*ry,width,color);
        }
    }
}
